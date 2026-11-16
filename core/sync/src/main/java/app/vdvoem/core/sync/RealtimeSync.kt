package app.vdvoem.core.sync

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

/** Базовый интервал «живого» опроса, пока приложение на экране. */
internal const val REALTIME_BASE_INTERVAL_MS = 12_000L

/** Пауза после неудачного прохода: не долбим сервер, если он недоступен. */
internal const val REALTIME_FAILURE_BACKOFF_MS = 30_000L

/**
 * Решение о паузе до следующего тика. Вынесено отдельной чистой функцией,
 * чтобы правило проверялось JVM-тестом без корутин и Android.
 */
internal fun nextDelayMs(
    lastResultFailed: Boolean,
    baseIntervalMs: Long = REALTIME_BASE_INTERVAL_MS,
    failureBackoffMs: Long = REALTIME_FAILURE_BACKOFF_MS
): Long = if (lastResultFailed) failureBackoffMs else baseIntervalMs

/**
 * «Живая» синхронизация, пока приложение видно на экране.
 *
 * Зачем: FCM/пушей нет (нет google-services.json), а WorkManager даёт минимум 15 минут —
 * партнёры видят правки друг друга слишком поздно. Пока приложение активно, раз в
 * [REALTIME_BASE_INTERVAL_MS] делаем один лёгкий проход через [SyncController.syncNow]
 * (это [SyncEngine.syncOnce] под его мьютексом, без новой задачи WorkManager), чтобы
 * pull/push не дублировался и не плодил `enqueueUniqueWork` на каждом тике.
 *
 * Батарея: цикл живёт только между `start()` и `stop()`; активити зовёт их из
 * `onStart`/`onStop`, поэтому в фоне (свёрнутое приложение, экран выключен) опроса нет
 * вообще — там остаётся обычный `SyncWorker`. Короткий интервал выбран как компромисс:
 * 12 с — заметная «реалтаймовость» при лёгком pull'е и без ощутимого расхода.
 *
 * Пропуск тика: если предыдущий проход ещё держит [gate] (например, долгий сетевой
 * вызов или параллельный `stop()`/`start()`), тик не встаёт в очередь, а просто
 * переносится на следующий — busy loop'а нет.
 */
class RealtimeSync(
    private val controller: SyncController,
    private val scope: CoroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineName("RealtimeSync")
    ),
    private val baseIntervalMs: Long = REALTIME_BASE_INTERVAL_MS,
    private val failureBackoffMs: Long = REALTIME_FAILURE_BACKOFF_MS
) {

    private val lock = Any()

    /** Не даёт двум проходам идти одновременно (в т.ч. после быстрого stop/start). */
    private val gate = Mutex()

    /** null — цикл не запущен; отменённая/завершённая job тоже не мешает новому старту. */
    private var job: Job? = null

    val isRunning: Boolean
        get() = synchronized(lock) { job?.isActive == true }

    /** Идемпотентно: повторный вызов при уже работающем цикле ничего не делает. */
    fun start() {
        synchronized(lock) {
            if (job?.isActive == true) return
            job = scope.launch { loop() }
        }
    }

    /** Идемпотентно: вызов без запущенного цикла ничего не делает. */
    fun stop() {
        synchronized(lock) {
            job?.cancel()
            job = null
        }
    }

    private suspend fun loop() {
        var lastFailed = false
        while (true) {
            // Первый тик тоже через паузу: при возврате в приложение стартовый синк
            // уже делает VdvoemApplication, дублировать его незачем.
            delay(nextDelayMs(lastFailed, baseIntervalMs, failureBackoffMs))

            if (!gate.tryLock()) {
                // Предыдущий проход ещё идёт — не копим очередь и не крутимся вхолостую.
                Log.d(TAG, "tick skipped: previous sync still running")
                continue
            }
            try {
                lastFailed = runTick()
            } finally {
                gate.unlock()
            }
        }
    }

    /** Один проход; true — если он закончился ошибкой (следующая пауза будет длиннее). */
    private suspend fun runTick(): Boolean = try {
        val settings = controller.currentSettings()
        if (!settings.isConfigured || !settings.enabled) {
            // Не настроено или выключено — сеть не трогаем и не считаем это сбоем.
            false
        } else {
            controller.syncNow() is SyncResult.Error
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Log.w(TAG, "realtime sync failed", error)
        true
    }

    private companion object {
        const val TAG = "RealtimeSync"
    }
}