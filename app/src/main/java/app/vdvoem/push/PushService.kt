package app.vdvoem.push

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
 * Надёжность доставки — три независимых слоя:
 * 1. WebSocket с пингами (`PushClient`) — события приходят за секунду;
 * 2. переподключение при смене сети (`ConnectivityManager`);
 * 3. «сторожевой» синк раз в [WATCHDOG_SYNC_MS] — даже если сокет умер тихо,
 *    уведомление придёт не позже чем через пять минут, а не «когда откроют приложение».
 *
 * Запуск/остановка — только через [start]/[stop]:
 * - при открытии приложения и настроенной паре (см. документацию в конце файла);
 * - после перезагрузки телефона и обновления приложения ([PushRestartReceiver]);
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

    /** Смена Wi-Fi/LTE рвёт старый TCP молча — на этом событии пересоздаём сокет. */
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            client?.restart()
        }
    }

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        val push = PushClient(settingsStore)
        client = push
        push.start {
            // Событие редкое, но если в этот момент нет сети — следующий придёт через сторожевой
            // синк. Синхронизируемся напрямую (не через WorkManager): так доставка за секунду,
            // без квот фоновых задач.
            try {
                controller.syncNow()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                // Сеть недоступна или сервер ответил ошибкой — повторим на следующем событии.
            }
        }
        runCatching {
            getSystemService(ConnectivityManager::class.java)
                ?.registerDefaultNetworkCallback(networkCallback)
        }
        startWatchdog()
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

    /** Свайп приложения из «недавних» тоже не должен выключать уведомления. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        scheduleRestart()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        runCatching {
            getSystemService(ConnectivityManager::class.java)
                ?.unregisterNetworkCallback(networkCallback)
        }
        client?.close()
        client = null
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Страховка от «тихой смерти» сокета: раз в три минуты делаем обычный проход синка.
     * Без него пропущенное событие всплывало только при следующем открытии приложения.
     */
    private fun startWatchdog() {
        scope.launch {
            while (isActive) {
                delay(WATCHDOG_SYNC_MS)
                if (client?.isRunning != true) client?.restart()
                try {
                    controller.syncNow()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    // Сеть недоступна — следующий тик попробует снова.
                }
            }
        }
    }

    /** Перезапуск сервиса после свайпа из «недавних»: точный будильник недоступен без разрешения. */
    private fun scheduleRestart() {
        val alarm = getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(this, PushAlarmReceiver::class.java)
            .setAction(ACTION_RESTART)
        val pending = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val at = System.currentTimeMillis() + RESTART_DELAY_MS
        runCatching { alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending) }
    }

    private fun startAsForeground() {
        if (foregroundStarted) return
        ensureChannel()
        val notification = buildNotification()
        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                foregroundServiceType()
            )
            foregroundStarted = true
        } catch (error: Exception) {
            // Например, запрет фонового старта: сервис бесполезен без уведомления — выходим.
            stopSelf()
        }
    }

    /**
     * `dataSync` на Android 15+ живёт максимум 6 часов, после чего система зовёт
     * `onTimeout` и сервис умирает до следующего открытия приложения. `specialUse`
     * такого лимита не имеет — именно он нужен постоянному соединению.
     */
    private fun foregroundServiceType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
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

        /** Действие будильника «подними пуш»; слушает [PushAlarmReceiver]. */
        const val ACTION_RESTART = "app.vdvoem.push.RESTART"

        /** Период страховочного синка, если сокет промолчал. */
        internal const val WATCHDOG_SYNC_MS = 5 * 60_000L

        /** Пауза перед перезапуском после свайпа из «недавних». */
        private const val RESTART_DELAY_MS = 3_000L

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