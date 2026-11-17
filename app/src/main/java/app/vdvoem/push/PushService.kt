package app.vdvoem.push

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import app.vdvoem.MainActivity
import app.vdvoem.R
import app.vdvoem.core.sync.PushClient
import app.vdvoem.core.sync.SyncController
import app.vdvoem.core.sync.SyncSettingsStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent

/**
 * Foreground-сервис «мгновенный пуш» (docs/CYCLE_SPEC.md §8).
 *
 * Держит WebSocket на `/v1/ws` и на каждое событие `{"type":"changed"}` запускает
 * синхронизацию. Так партнёр видит правку за ~1 секунду, а 12-секундный опрос
 * ([app.vdvoem.core.sync.RealtimeSync]) остаётся fallback'ом, когда сокета нет.
 *
 * Почему foreground: Android убивает обычные сокеты в фоне; постоянное уведомление
 * низкого приоритета — честная плата за предсказуемую доставку. Пока пара не настроена
 * или синхронизация выключена, сервис гасит себя сам.
 *
 * Запуск/остановка — только через [start]/[stop]:
 * - при открытии приложения и настроенной паре (см. документацию в конце файла);
 * - при выходе из пары ([SyncController.leavePair]).
 */
class PushService : Service(), KoinComponent {

    private val controller: SyncController by inject()
    private val settingsStore: SyncSettingsStore by inject()

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineName("PushService")
    )

    private var client: PushClient? = null
    private var foregroundStarted = false

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        val push = PushClient(settingsStore)
        client = push
        push.start {
            // Сеть и запись в Room — не в главном потоке; движок сам держит мьютекс.
            controller.syncInBackground()
        }
        // Пара могла быть забыта между стартом сервиса и его запуском — не висим зря.
        // И следим дальше: после «Выйти из пары» сокет закрывается вместе с сервисом.
        scope.launch {
            settingsStore.settings.collect { settings ->
                if (!settings.isConfigured || !settings.enabled) stopSelf()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        // Повторный start идемпотентен: сокет уже слушает, уведомление уже висит.
        startAsForeground()
        return START_STICKY
    }

    override fun onTimeout(startId: Int) {
        stopSelf(startId)
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        stopSelf(startId)
    }

    override fun onDestroy() {
        client?.close()
        client = null
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startAsForeground() {
        if (foregroundStarted) return
        ensureChannel()
        val notification = buildNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            foregroundStarted = true
        } catch (error: Exception) {
            // Например, запрет фонового старта: сервис бесполезен без уведомления — выходим.
            stopSelf()
        }
    }

    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.push_service_title))
            .setContentText(getString(R.string.push_service_text))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setContentIntent(openApp)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.push_service_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.push_service_channel_text)
                setShowBadge(false)
            }
        )
    }

    companion object {
        /** Канал постоянного уведомления; отдельный от «сердечек», чтобы не будить звуком. */
        const val CHANNEL_ID = "sync_push"

        private const val NOTIFICATION_ID = 2002
        private const val ACTION_STOP = "app.vdvoem.push.STOP"

        /**
         * Запустить канал мгновенного пуша. Безопасно звать при каждом открытии приложения:
         * повторный вызов ничего не перезапускает.
         *
         * ```kotlin
         * PushService.start(this)   // Activity/Application
         * ```
         */
        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context.applicationContext,
                Intent(context.applicationContext, PushService::class.java)
            )
        }

        /**
         * Остановить канал. Звать при выходе из пары (`SyncController.leavePair()`),
         * при выключении синхронизации и при первом запуске без пары.
         */
        fun stop(context: Context) {
            context.applicationContext.stopService(
                Intent(context.applicationContext, PushService::class.java)
            )
        }
    }
}