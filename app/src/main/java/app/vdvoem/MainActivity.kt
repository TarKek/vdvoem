package app.vdvoem

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vdvoem.core.domain.settings.AppSettings
import app.vdvoem.core.domain.settings.AppSettingsRepository
import app.vdvoem.core.domain.settings.ThemeMode
import app.vdvoem.core.ui.theme.VdvoemTheme
import app.vdvoem.ui.VdvoemRoot
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestInternetPermissionIfNeeded()
        enableEdgeToEdge()
        setContent {
            val settingsRepository: AppSettingsRepository = koinInject()
            val settings by settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings())

            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            VdvoemTheme(
                darkTheme = darkTheme,
                dynamicColor = settings.dynamicColor
            ) {
                VdvoemRoot()
            }
        }
    }

    /**
     * На части прошивок (HyperOS, VoltageOS и др.) INTERNET выдаётся как обычное
     * чувствительное разрешение и по умолчанию не выдано — просим его явно.
     */
    private fun requestInternetPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val granted = checkSelfPermission(Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestPermissions(arrayOf(Manifest.permission.INTERNET), INTERNET_REQUEST)
        }
    }

    private companion object {
        const val INTERNET_REQUEST = 1001
    }
}