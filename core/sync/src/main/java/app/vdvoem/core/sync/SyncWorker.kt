package app.vdvoem.core.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.util.concurrent.TimeUnit

/**
 * Фоновая синхронизация. Зависимости — из Koin, поэтому кастомный WorkerFactory не нужен.
 *
 * Ошибка → [Result.retry]: WorkManager сам растянет повторы экспоненциально
 * (backoff задаётся в [SyncScheduler]). Не настроено — тихий успех, чтобы не будить сеть зря.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    override suspend fun doWork(): Result {
        // Синк выключен пользователем или ещё не настроен — не будим сеть.
        val settings = get<SyncSettingsStore>().current()
        if (!settings.isConfigured || !settings.enabled) return Result.success()

        val engine = get<SyncEngine>()
        return when (engine.syncOnce()) {
            is SyncResult.Success -> Result.success()
            is SyncResult.NotConfigured -> Result.success()
            is SyncResult.Error -> if (runAttemptCount >= MAX_ATTEMPTS) Result.failure() else Result.retry()
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 5
    }
}

/** Постановка задач синхронизации в WorkManager. */
class SyncScheduler(private val context: Context) {

    /** Периодическая синхронизация: 15 минут — минимальный интервал WorkManager. */
    fun schedulePeriodic() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(PERIOD_MINUTES, TimeUnit.MINUTES)
            .setConstraints(connectedConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .addTag(TAG_SYNC)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancelPeriodic() {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
    }

    /** Разовый проход по кнопке «Синхронизировать сейчас» или после правки данных. */
    fun syncNow() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(connectedConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .addTag(TAG_SYNC)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun connectedConstraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    companion object {
        const val PERIOD_MINUTES = 15L
        const val BACKOFF_SECONDS = 30L
        const val PERIODIC_WORK_NAME = "sync_periodic"
        const val ONE_TIME_WORK_NAME = "sync_now"
        const val TAG_SYNC = "sync"
    }
}