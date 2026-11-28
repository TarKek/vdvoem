package app.vdvoem.feature.diary

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.vdvoem.core.domain.repository.DiaryReminderRepository
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Показывает одно напоминание дневника.
 *
 * Задача несёт только id и снимок текста: перед показом напоминание перечитывается
 * из базы, поэтому удалённое, выполненное или отредактированное напоминание никогда
 * не всплывёт со старым текстом. Идентификатор уведомления считается из id —
 * повторный запуск заменит уведомление, а не добавит второе.
 */
class DiaryReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params), KoinComponent {

    private val repository: DiaryReminderRepository by inject()

    override suspend fun doWork(): Result {
        val context = applicationContext
        val reminderId = inputData.getString(KEY_ID) ?: return Result.success()
        val reminder = repository.observeRemindersIncludingDeleted().first()
            .firstOrNull { it.id == reminderId }
            ?: return Result.success()
        // Удалено, выполнено или перенесено в прошлое — напоминать не о чем.
        if (!reminder.isActive) return Result.success()

        ensureChannel(context)

        // Разрешение POST_NOTIFICATIONS (API 33+) и системный выключатель уведомлений.
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return Result.success()

        val text = reminder.text.takeIf { it.isNotBlank() }
            ?: inputData.getString(KEY_TEXT).orEmpty()
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_diary_reminder)
            .setContentTitle(context.getString(R.string.diary_reminder_notification_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openApp(context))

        if (reminder.isPrivate) {
            // На экране блокировки личный текст не показываем — только нейтральную версию.
            builder
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(privatePublicVersion(context))
        }

        return try {
            NotificationManagerCompat.from(context).notify(reminderId.hashCode(), builder.build())
            Result.success()
        } catch (error: SecurityException) {
            // Разрешение отозвали между проверкой и показом — повторы не помогут.
            Result.success()
        }
    }

    private fun privatePublicVersion(context: Context) =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_diary_reminder)
            .setContentTitle(context.getString(R.string.diary_reminder_notification_title))
            .setContentText(context.getString(R.string.diary_reminder_notification_private))
            .build()

    private fun openApp(context: Context): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: return null
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val CHANNEL_ID = "diary_reminders"

        const val KEY_ID = "reminder_id"
        const val KEY_TEXT = "reminder_text"
        const val KEY_REMIND_AT = "reminder_at"

        /** Канал создаётся и при старте приложения, и в воркере: уведомление может прийти ночью. */
        fun ensureChannel(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.diary_reminder_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.diary_reminder_channel_description)
            }
            manager.createNotificationChannel(channel)
        }
    }
}