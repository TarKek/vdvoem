package app.vdvoem.feature.calendar

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.vdvoem.core.domain.repository.CalendarRepository
import app.vdvoem.core.domain.time.Dates
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Показывает одно напоминание о событии календаря.
 *
 * Идемпотентность: идентификатор уведомления считается из id события и смещения,
 * поэтому повторный запуск той же задачи заменяет уже показанное уведомление,
 * а не добавляет второе. Если уведомления выключены в системе — задача просто
 * завершается успешно, без ретраев.
 */
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params), KoinComponent {

    private val repository: CalendarRepository by inject()
    private val scheduler: ReminderScheduler by inject()
    private val preferences: ReminderPreferences by inject()

    override suspend fun doWork(): Result {
        val context = applicationContext
        val eventId = inputData.getString(KEY_EVENT_ID) ?: return Result.failure()
        val snapshotTitle = inputData.getString(KEY_TITLE)?.takeIf { it.isNotBlank() } ?: return Result.success()
        val dateMillis = inputData.getLong(KEY_DATE_MILLIS, 0L)
        if (dateMillis <= 0L) return Result.success()
        val event = repository.observeEventsIncludingDeleted().first().firstOrNull { it.id == eventId }
            ?: return Result.success()
        val occurrence = Dates.toLocalDate(dateMillis)
        if (event.deletedAt != null || event.title != snapshotTitle ||
            Dates.nextOccurrence(event.dateMillis, event.annual, occurrence) != occurrence
        ) {
            ReminderScheduler(applicationContext).cancel(eventId)
            return Result.success()
        }
        val title = event.title

        ensureChannel(context)

        // Разрешение POST_NOTIFICATIONS (API 33+) и системный выключатель уведомлений.
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return Result.success()

        val days = Dates.daysUntil(Dates.toLocalDate(dateMillis)).coerceAtLeast(0L)
        val whenText = if (days == 0L) {
            context.getString(R.string.calendar_reminder_today)
        } else {
            context.getString(
                R.string.calendar_reminder_in_days,
                context.resources.getQuantityString(
                    R.plurals.calendar_days_left,
                    days.toInt(),
                    days.toInt()
                )
            )
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_calendar_reminder)
            .setContentTitle(title)
            .setContentText(whenText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(whenText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openApp(context))
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(notificationId(eventId, inputData.getInt(KEY_OFFSET_DAYS, 0)), notification)
            scheduler.scheduleNextAnnual(event, occurrence, preferences.settings.first())
            Result.success()
        } catch (error: SecurityException) {
            // Разрешение отозвали между проверкой и показом — повторы не помогут.
            Result.success()
        }
    }

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.calendar_reminder_channel),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.calendar_reminder_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    private fun openApp(context: Context): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: return null
        return PendingIntent.getActivity(
            context,
            notificationId(context.packageName, 0),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun notificationId(eventId: String, offsetDays: Int): Int =
        "$eventId#$offsetDays".hashCode()

    companion object {
        const val CHANNEL_ID = "calendar_reminders"

        const val KEY_EVENT_ID = "event_id"
        const val KEY_TITLE = "event_title"
        const val KEY_DATE_MILLIS = "event_date_millis"
        const val KEY_OFFSET_DAYS = "offset_days"
    }
}