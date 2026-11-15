package app.vdvoem.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.vdvoem.MainActivity
import app.vdvoem.R
import app.vdvoem.core.domain.model.Poke
import app.vdvoem.core.sync.IncomingPokeHandler

/**
 * «Сердечко» приехало с сервера — показываем уведомление.
 *
 * Реализация живёт в приложении: ядро синка платформенных API не знает.
 * Канал создаётся заранее в [VdvoemApplication], разрешение спрашиваем в [MainActivity].
 */
class PokeNotifier(private val context: Context) : IncomingPokeHandler {

    override suspend fun onIncoming(pokes: List<Poke>) {
        val latest = pokes.maxByOrNull { it.createdAt } ?: return
        if (latest.text.isBlank()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannel(context)

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.poke_notification_title))
            .setContentText(latest.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(latest.text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val CHANNEL_ID = "pokes"
        private const val NOTIFICATION_ID = 2001

        /** Канал можно создавать многократно: повторный вызов ничего не ломает. */
        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.poke_notification_channel),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.poke_notification_channel_text)
                }
            )
        }
    }
}