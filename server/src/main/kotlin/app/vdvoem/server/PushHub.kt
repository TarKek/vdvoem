package app.vdvoem.server

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel

/**
 * Реестр открытых WebSocket-подключений по парам (docs/CYCLE_SPEC.md §8).
 *
 * Зачем: `POST /v1/sync` должен мгновенно сообщить второму телефону пары «есть
 * изменения», чтобы тот сделал pull через секунду, а не через 12 секунд опроса.
 *
 * Инварианты:
 * - [notifyPair] никогда не бросает исключений и безопасен, когда никто не подключён:
 *   HTTP-путь синка не должен падать из-за пуша.
 * - Отправка идёт через буферизованный канал устройства, поэтому медленный или мёртвый
 *   сокет не тормозит запрос партнёра и не ломает остальные устройства пары.
 * - Одно устройство = одно подключение: по `tokenHash` узнаём автора изменения и не шлём
 *   ему его же событие.
 */
object PushHub {

    /** Ёмкость очереди исходящих событий одного сокета. События идемпотентны, старые можно терять. */
    private const val OUTGOING_CAPACITY = 8

    private object Lock

    /** Одно подключённое устройство пары. */
    class Connection internal constructor(
        val pairId: String,
        /** SHA-256 токена устройства — так же, как он лежит в таблице `devices`. */
        val tokenHash: String
    ) {
        /** Исходящие кадры; читает корутина-отправитель в [PushRoutes]. */
        internal val outgoing: Channel<String> = Channel(
            capacity = OUTGOING_CAPACITY,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        internal val revoked = kotlinx.coroutines.CompletableDeferred<Unit>()
    }

    private val byPair = HashMap<String, MutableSet<Connection>>()

    /** Подключить устройство к паре. */
    fun register(pairId: String, tokenHash: String): Connection {
        val connection = Connection(pairId, tokenHash)
        val previous = synchronized(Lock) {
            val connections = byPair.getOrPut(pairId) { HashSet() }
            val replaced = connections.filter { it.tokenHash == tokenHash }
            connections.removeAll(replaced.toSet())
            connections.add(connection)
            replaced
        }
        previous.forEach { it.revoked.complete(Unit) }
        return connection
    }

    /** Отключить устройство (в т.ч. при разрыве сокета или ошибке). Идемпотентно. */
    fun unregister(connection: Connection) {
        synchronized(Lock) {
            val connections = byPair[connection.pairId] ?: return
            connections.remove(connection)
            if (connections.isEmpty()) byPair.remove(connection.pairId)
        }
        connection.outgoing.close()
    }

    /** Сколько устройств пары сейчас на связи (для логов и тестов). */
    fun connectionCount(pairId: String): Int = synchronized(Lock) {
        byPair[pairId]?.size ?: 0
    }

    fun revokeToken(tokenHash: String) {
        val targets = synchronized(Lock) {
            byPair.values.flatMap { connections -> connections.filter { it.tokenHash == tokenHash } }
        }
        targets.forEach { it.revoked.complete(Unit) }
    }

    /**
     * Сообщить остальным устройствам пары, что данные изменились.
     *
     * @param exceptTokenHash хеш токена автора изменения (его устройство не уведомляем).
     * @param serverTime серверное время события — партнёр использует его только как сигнал.
     */
    fun notifyPair(
        pairId: String,
        exceptTokenHash: String? = null,
        serverTime: Long = System.currentTimeMillis()
    ) {
        val payload = """{"type":"changed","serverTime":$serverTime}"""
        val targets = try {
            synchronized(Lock) {
                byPair[pairId]
                    ?.filter { exceptTokenHash == null || it.tokenHash != exceptTokenHash }
                    .orEmpty()
            }
        } catch (ignored: Throwable) {
            // Реестр не должен ломать HTTP-путь синка.
            return
        }
        for (connection in targets) {
            try {
                connection.outgoing.trySend(payload)
            } catch (ignored: Throwable) {
                // Сокет уже закрывается — забыли его, остальные устройства не страдают.
                unregister(connection)
            }
        }
    }
}