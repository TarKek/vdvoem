package app.vdvoem.feature.calendar

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import app.vdvoem.core.domain.model.CalendarEvent
import app.vdvoem.core.domain.time.Dates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Планировщик напоминаний о событиях календаря.
 *
 * На каждое событие — две отложенные задачи WorkManager: «за 7 дней» и «в день события»
 * (09:00 локального времени). Уникальные имена `event_<id>_week` / `event_<id>_day`
 * и политика REPLACE дают идемпотентность: повторный sync перезаписывает задачи,
 * а не плодит дубли.
 */
class ReminderScheduler(private val context: Context) {

    private val lock = Mutex()

    private val workManager: WorkManager
        get() = WorkManager.getInstance(context)

    /** Пересобирает расписание под текущий список событий: устаревшие задачи снимаются. */
    suspend fun sync(events: List<CalendarEvent>) = lock.withLock {
        withContext(Dispatchers.IO) {
            workManager.cancelAllWorkByTag(TAG)
            val now = System.currentTimeMillis()
            events.forEach { event ->
                val date = Dates.nextOccurrence(event.dateMillis, event.annual)
                // Прошедшие негодовщины не переносим: ближайшее вступление уже посчитано.
                enqueue(event, date, OFFSET_WEEK, now)
                enqueue(event, date, OFFSET_DAY, now)
            }
        }
    }

    /** Снимает напоминания конкретного события (удаление или прошедшая дата). */
    fun cancel(eventId: String) {
        workManager.cancelUniqueWork(workName(eventId, OFFSET_WEEK))
        workManager.cancelUniqueWork(workName(eventId, OFFSET_DAY))
    }

    /** Снимает все напоминания календаря — например, когда переключатель выключили. */
    fun cancelAll() {
        workManager.cancelAllWorkByTag(TAG)
    }

    private fun enqueue(event: CalendarEvent, date: LocalDate, offsetDays: Int, now: Long) {
        val triggerAt = date.minusDays(offsetDays.toLong())
            .atTime(REMINDER_HOUR, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        // Момент уже прошёл — напоминание бессмысленно.
        if (triggerAt <= now) return

        val request: OneTimeWorkRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(triggerAt - now, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    ReminderWorker.KEY_EVENT_ID to event.id,
                    ReminderWorker.KEY_TITLE to event.title,
                    ReminderWorker.KEY_DATE_MILLIS to Dates.startOfDay(date),
                    ReminderWorker.KEY_OFFSET_DAYS to offsetDays
                )
            )
            .addTag(TAG)
            .build()

        workManager.enqueueUniqueWork(
            workName(event.id, offsetDays),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun workName(eventId: String, offsetDays: Int): String =
        "event_${eventId}_" + if (offsetDays == OFFSET_WEEK) "week" else "day"

    companion object {
        const val TAG = "calendar_reminder"

        /** За сколько дней до события приходит первое напоминание. */
        const val OFFSET_WEEK = 7

        /** Напоминание в день события. */
        const val OFFSET_DAY = 0

        /** Локальный час отправки напоминаний. */
        private const val REMINDER_HOUR = 9
    }
}