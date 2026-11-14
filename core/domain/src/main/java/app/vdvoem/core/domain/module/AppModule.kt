package app.vdvoem.core.domain.module

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Контракт подключаемого модуля.
 *
 * Правила:
 * - ядро и другие модули не импортируют реализацию модуля, только [AppModule];
 * - модуль сам объявляет маршрут, иконку, экран и тексты;
 * - включение/выключение делает магазин через [app.vdvoem.core.domain.repository.ModuleSettingsRepository].
 */
interface AppModule {
    val manifest: ModuleManifest

    /** Уникальный маршрут для NavHost. */
    val route: String

    val icon: ImageVector

    @Composable
    fun Screen(modifier: Modifier)
}
