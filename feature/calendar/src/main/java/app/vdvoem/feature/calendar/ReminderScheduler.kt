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
 * На каждое событие — два отложенных напоминания: «за N дней» (N из настроек, по умолчанию 7)
 * и в день события, в выбранный час локального времени. Имена задач включают смещение
 * (`event_<id>_<дни>`) и политика REPLACE дают идемпотентность: повторный sync перезаписывает
 * задачи, а не плодит дубли.
 */
class ReminderScheduler(private val context: Context) {

    private val lock = Mutex()

    private val workManager: WorkManager
        get() = WorkManager.getInstance(context)

    /** Пересобирает расписание под текущий список событий и настройки. */
    suspend fun sync(
        events: List<CalendarEvent>,
        settings: ReminderSettings = ReminderSettings(enabled = true)
    ) = lock.withLock {
        withContext(Dispatchers.IO) {
            workManager.cancelAllWorkByTag(TAG)
            if (!settings.enabled) return@withContext
            val now = System.currentTimeMillis()
            events.forEach { event ->
                val date = Dates.nextOccurrence(event.dateMillis, event.annual)
                // Прошедшие негодовщины не переносим: ближайшее вступление уже посчитано.
                enqueue(event, date, settings.leadDays, settings.hour, now)
                enqueue(event, date, OFFSET_DAY, settings.hour, now)
            }
        }
    }

    /** Снимает напоминания конкретного события (удаление или прошедшая дата). */
    fun cancel(eventId: String) {
        workManager.cancelAllWorkByTag(eventTag(eventId))
    }

    /** Снимает все напоминания календаря — например, когда переключатель выключили. */
    fun cancelAll() {
        workManager.cancelAllWorkByTag(TAG)
    }

    private fun enqueue(
        event: CalendarEvent,
        date: LocalDate,
        offsetDays: Int,
        hour: Int,
        now: Long
    ) {
        val triggerAt = date.minusDays(offsetDays.toLong())
            .atTime(hour.coerceIn(0, 23), 0)
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
            .addTag(eventTag(event.id))
            .build()

        workManager.enqueueUniqueWork(
            workName(event.id, offsetDays),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun workName(eventId: String, offsetDays: Int): String =
        "event_${eventId}_$offsetDays"

    private fun eventTag(eventId: String): String = "calendar_event_$eventId"

    companion object {
        const val TAG = "calendar_reminder"

        /** Напоминание в день события. */
        const val OFFSET_DAY = 0
    }
}