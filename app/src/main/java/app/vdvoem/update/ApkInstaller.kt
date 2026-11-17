package app.vdvoem.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.core.content.IntentCompat

/**
 * Установка APK через штатный [PackageInstaller].
 *
 * Важно: на MIUI/HyperOS установка через `ACTION_VIEW` + FileProvider падает
 * с `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, хотя подпись совпадает. Сессия PackageInstaller
 * работает одинаково на всех прошивках.
 */
object ApkInstaller {

    /** Открывает системный диалог установки. Возвращает id сессии или null, если не вышло. */
    fun install(context: Context, apk: java.io.File): Boolean = runCatching {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            try {
                apk.inputStream().use { input ->
                    session.openWrite("base.apk", 0, apk.length()).use { output ->
                        input.copyTo(output)
                        session.fsync(output)
                    }
                }
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                val pending = PendingIntent.getBroadcast(
                    context,
                    sessionId,
                    Intent(context, InstallResultReceiver::class.java)
                        .setAction(InstallResultReceiver.ACTION_INSTALL_RESULT),
                    flags
                )
                session.commit(pending.intentSender)
            } catch (error: Throwable) {
                session.abandon()
                throw error
            }
        }
        true
    }.getOrDefault(false)
}

/** Слушает результат установки: на ошибке показывает короткое сообщение. */
class InstallResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_INSTALL_RESULT) return
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            // Система просит подтверждение — показываем её диалог, иначе установка молча висит.
            val confirm = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)
                ?: return
            confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(confirm) }
            return
        }
        if (status == PackageInstaller.STATUS_SUCCESS) return
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        val text = if (message.isNullOrBlank()) {
            context.getString(app.vdvoem.R.string.more_update_failed)
        } else {
            message
        }
        android.widget.Toast.makeText(context, text, android.widget.Toast.LENGTH_LONG).show()
    }

    companion object {
        const val ACTION_INSTALL_RESULT = "app.vdvoem.INSTALL_RESULT"
    }
}