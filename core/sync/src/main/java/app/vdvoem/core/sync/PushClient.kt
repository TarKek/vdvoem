package app.vdvoem.core.sync

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.random.Random
import java.util.concurrent.TimeUnit

/** Путь канала мгновенных уведомлений на сервере (docs/CYCLE_SPEC.md §8). */
private const val WS_PATH = "/v1/ws"

/** Первая пауза перед переподключением. */
private const val INITIAL_BACKOFF_MS = 2_000L

/** Потолок паузы: сервер может лежать долго, долбить его раз в 2 с нельзя. */
private const val MAX_BACKOFF_MS = 60_000L

/** Разброс паузы, чтобы два телефона пары не переподключались синхронно. */
private const val JITTER_MS = 1_000L

/**
 * Клиент канала мгновенного пуша.
 *
 * Сервер присылает `{"type":"changed","serverTime":<millis>}` остальным устройствам пары
 * сразу после успешного `POST /v1/sync`, поэтому партнёр узнаёт об изменении за секунду,
 * а не за 12 секунд опроса ([RealtimeSync] остаётся fallback'ом).
 *
 * Цикл: подключиться → читать кадры → на обрыв/ошибку подождать (2 с → 60 с, с джиттером)
 * → подключиться снова. Токен и адрес берутся из [SyncSettingsStore] на каждую попытку,
 * так что смена сервера или пары подхватывается без пересоздания клиента.
 *
 * Живёт внутри `PushService` (foreground, `specialUse`), поэтому цикл не ограничен
 * жизнью активити. Сеть — только пока пара настроена и включена.
 */
class PushClient(private val settings: SyncSettingsStore) {

    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient(OkHttp) {
        install(WebSockets)
        // Keep-alive: мобильные операторы и роутеры молча выбрасывают простаивающий TCP
        // через 3–10 минут. Без пингов сокет «умирал» тихо — клиент ждал кадр, которого
        // уже не будет, и партнёр видел уведомления только после открытия приложения.
        engine {
            config {
                pingInterval(20, TimeUnit.SECONDS)
                retryOnConnectionFailure(true)
            }
        }
    }

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineName("PushClient")
    )

    private val lock = Any()

    /** null — цикл не запущен; отменённая job не мешает новому старту. */
    private var job: Job? = null

    /** Колбэк последнего [start]: нужен, чтобы переподключиться без потери подписчика. */
    private var onChanged: (suspend () -> Unit)? = null

    val isRunning: Boolean
        get() = synchronized(lock) { job?.isActive == true }

    /** Идемпотентно: повторный вызов при уже работающем цикле ничего не делает. */
    fun start(onChanged: suspend () -> Unit) {
        synchronized(lock) {
            this.onChanged = onChanged
            if (job?.isActive == true) return
            job = scope.launch { listen(onChanged) }
        }
    }

    /**
     * Пересоздать сокет: например, после смены сети (Wi-Fi → LTE), когда старый TCP
     * уже мёртв, а цикл ещё об этом не узнал. Подписчик сохраняется.
     */
    fun restart() {
        synchronized(lock) {
            val callback = onChanged ?: return
            job?.cancel()
            job = scope.launch { listen(callback) }
        }
    }

    /** Идемпотентно: вызов без запущенного цикла ничего не делает. */
    fun stop() {
        synchronized(lock) {
            job?.cancel()
            job = null
        }
    }

    /** Полная остановка: отменяет цикл и освобождает HTTP-клиент. */
    fun close() {
        stop()
        scope.cancel()
        client.close()
    }

    /**
     * Бесконечный цикл подключений. Отменяется через [stop]/[close].
     *
     * @param onChanged вызывается на каждое серверное событие (обычно — «сделай sync»).
     */
    suspend fun listen(onChanged: suspend () -> Unit) {
        var backoff = INITIAL_BACKOFF_MS
        while (currentCoroutineContext().isActive) {
            val cfg = try {
                settings.current()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w(TAG, "push settings unavailable", error)
                delay(backoff + Random.nextLong(JITTER_MS))
                backoff = grow(backoff)
                continue
            }

            if (!cfg.isConfigured || !cfg.enabled) {
                // Пара не настроена/выключена — не держим сеть, просто ждём изменений настроек.
                delay(INITIAL_BACKOFF_MS)
                continue
            }

            try {
                client.webSocket(
                    urlString = webSocketUrl(cfg.baseUrl),
                    request = { header(HttpHeaders.Authorization, "Bearer ${cfg.deviceToken}") }
                ) {
                    // Подключились — счётчик пауз сбрасываем, чтобы обрыв не копил 60 с.
                    backoff = INITIAL_BACKOFF_MS
                    for (frame in incoming) {
                        if (frame !is Frame.Text) continue
                        if (isChanged(frame.readText())) onChanged()
                    }
                }
                Log.i(TAG, "push socket closed by server")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w(TAG, "push socket failed: ${error.message}")
            }

            // Обрыв или закрытие сокета: пауза с джиттером и снова в бой.
            delay(backoff + Random.nextLong(JITTER_MS))
            backoff = grow(backoff)
        }
    }

    /** 2 с → 4 с → 8 с → … → 60 с. */
    private fun grow(current: Long): Long = (current * 2).coerceAtMost(MAX_BACKOFF_MS)

    /** Любой текстовый кадр считаем сигналом, кроме явно чужого `type`. */
    private fun isChanged(text: String): Boolean = try {
        val type = json.parseToJsonElement(text).jsonObject["type"]?.jsonPrimitive?.contentOrNull
        type == null || type == "changed"
    } catch (error: Exception) {
        true
    }

    private companion object {
        const val TAG = "PushClient"
    }
}

/**
 * `http(s)://host` → `ws(s)://host/v1/ws`.
 *
 * Отдельная чистая функция: правило легко проверить тестом без сети и Android.
 */
internal fun webSocketUrl(serverUrl: String): String {
    val base = serverUrl.trim().trimEnd('/')
    val socket = when {
        base.startsWith("https://", ignoreCase = true) -> "wss://" + base.substring(8)
        base.startsWith("http://", ignoreCase = true) -> "ws://" + base.substring(7)
        else -> base
    }
    return socket.trimEnd('/') + WS_PATH
}