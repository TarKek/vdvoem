package app.vdvoem.core.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.atomic.AtomicInteger

/**
 * Сигнал «на устройстве изменились данные».
 *
 * Репозитории дёргают [notifyLocalWrite] после каждой записи, а слой приложения
 * отправляет синхронизацию. Так правка уезжает на сервер сразу, а не через 15 минут.
 *
 * Записи, которые делает сам движок синка, оборачиваются в [muted] — иначе
 * применённые с сервера данные вызывали бы новый проход по кругу.
 */
object LocalChangeSignal {

    private val _changes = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    val changes: SharedFlow<Unit> = _changes.asSharedFlow()

    private val muted = AtomicInteger(0)

    fun notifyLocalWrite() {
        if (muted.get() == 0) _changes.tryEmit(Unit)
    }

    suspend fun <T> muted(block: suspend () -> T): T {
        muted.incrementAndGet()
        return try {
            block()
        } finally {
            muted.decrementAndGet()
        }
    }
}