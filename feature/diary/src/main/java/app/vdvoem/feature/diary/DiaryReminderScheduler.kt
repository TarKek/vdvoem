package app.vdvoem.feature.diary

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import app.vdvoem.core.domain.model.DiaryReminder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Планировщик локальных уведомлений дневника.
 *
 * На каждое актуальное напоминание — одна отложенная задача с точным временем.
 * Имя задачи содержит id, а политика REPLACE делает sync идемпотентным: повторный
 * пересчёт перезаписывает задачи, а не плодит дубли.
 */
class DiaryReminderScheduler(private val context: Context) {

    private val lock = Mutex()

    private val workManager: WorkManager
        get() = WorkManager.getInstance(context)

    /**
     * Пересобирает расписание, не трогая задачи, время которых уже наступило.
     *
     * Важно не отменять всё подряд: приложение стартует как раз потому, что
     * WorkManager поднял задачу напоминания. Если на старте снять её по тегу,
     * только что сработавшее уведомление потеряется. Поэтому снимаем задачу
     * только у выполненных и удалённых напоминаний, а будущие переставляем
     * по имени (REPLACE) — это идемпотентно.
     */
    suspend fun sync(reminders: List<DiaryReminder>) = lock.withLock {
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            reminders.forEach { reminder ->
                when {
                    !reminder.isActive -> {
                        workManager.cancelUniqueWork(workName(reminder.id))
                        // Уже показанное уведомление тоже убираем: выполненное не должно висеть в шторке.
                        cancelNotification(reminder.id)
                    }
                    reminder.remindAt > now -> enqueue(reminder, now)
                    // Время уже наступило: не отменяем — задача либо выполняется,
                    // либо вот-вот выполнится и сама покажет уведомление.
                }
            }
        }
    }

    /** Снимает уведомление конкретного напоминания (удаление или отметка «выполнено»). */
    fun cancel(id: String) {
        workManager.cancelAllWorkByTag(reminderTag(id))
        cancelNotification(id)
    }

    private fun cancelNotification(id: String) {
        // id уведомления совпадает с тем, что показывает DiaryReminderWorker.
        runCatching { NotificationManagerCompat.from(context).cancel(id.hashCode()) }
    }

    private fun enqueue(reminder: DiaryReminder, now: Long) {
        val request = OneTimeWorkRequestBuilder<DiaryReminderWorker>()
            .setInitialDelay(reminder.remindAt - now, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    DiaryReminderWorker.KEY_ID to reminder.id,
                    DiaryReminderWorker.KEY_TEXT to reminder.text,
                    DiaryReminderWorker.KEY_REMIND_AT to reminder.remindAt
                )
            )
            .addTag(TAG)
            .addTag(reminderTag(reminder.id))
            .build()

        workManager.enqueueUniqueWork(
            workName(reminder.id),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun workName(id: String): String = "diary_reminder_$id"

    private fun reminderTag(id: String): String = "diary_reminder_$id"

    companion object {
        /** Общий тег всех напоминаний дневника: по нему снимается весь план. */
        const val TAG = "diary_reminder"
    }
}