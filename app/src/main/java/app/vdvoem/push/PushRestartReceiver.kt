package app.vdvoem.push

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import app.vdvoem.core.sync.SyncSettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

/**
 * Возвращает канал мгновенных уведомлений к жизни без участия пользователя.
 *
 * Приёмник намеренно не начинается с постоянного уведомления вслепую: сначала проверяем,
 * что пара настроена и синхронизация включена, иначе на телефоне висела бы плашка без пользы.
 * Проверка настроек асинхронная, поэтому результат приёмника держим через [goAsync].
 */
abstract class PushReviveReceiver : BroadcastReceiver(), KoinComponent {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = get<SyncSettingsStore>().current()
                if (settings.isConfigured && settings.enabled) {
                    PushService.start(appContext)
                }
            } catch (error: Exception) {
                // Koin ещё не поднялся или фоновый старт запрещён — не роняем приёмник.
                Log.w(TAG, "push revive skipped: ${error.message}")
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "PushRevive"
    }
}

/**
 * Перезагрузка телефона и обновление приложения (в том числе через встроенный апдейтер):
 * после них сервис не переживает процесс, и канал молчал до первого открытия «Вдвоём».
 *
 * `exported=true` безопасен: оба действия защищённые, прислать их может только система.
 */
class PushRestartReceiver : PushReviveReceiver()

/**
 * Будильник после свайпа приложения из «недавних» (см. [PushService.onTaskRemoved]).
 * Отдельный приёмник, чтобы действие нельзя было дёрнуть из другого приложения.
 */
class PushAlarmReceiver : PushReviveReceiver()