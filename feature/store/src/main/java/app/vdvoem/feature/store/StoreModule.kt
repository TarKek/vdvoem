package app.vdvoem.feature.store

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.vdvoem.core.domain.module.AppModule
import app.vdvoem.core.domain.module.ModuleCategory
import app.vdvoem.core.domain.module.ModuleManifest
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Экран «Возможности» — он же настройка видимости модулей в навигации.
 *
 * Модуль всегда доступен и не показывается в собственном списке.
 */
class StoreModule : AppModule {
    override val manifest = ModuleManifest(
        id = ID,
        titleRes = R.string.store_module_title,
        descriptionRes = R.string.store_module_description,
        category = ModuleCategory.CORE,
        order = 90,
        defaultEnabled = true
    )

    override val route = "store"
    override val icon: ImageVector = Icons.AutoMirrored.Filled.List

    @Composable
    override fun Screen(modifier: Modifier) {
        // Экран открывается из корневого экрана и получает зависимости оттуда,
        // поэтому собственный Screen не используется.
    }

    companion object {
        const val ID = "core.store"
    }
}

val storeFeatureModule: Module = module {
    single { StoreModule() }
}
