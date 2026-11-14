package app.vdvoem.feature.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vdvoem.core.domain.module.AppModule
import app.vdvoem.core.domain.module.ModuleCategory
import app.vdvoem.core.domain.module.ModuleManifest
import org.koin.androidx.compose.koinViewModel
import org.koin.dsl.module

class SettingsModule : AppModule {
    override val manifest = ModuleManifest(
        id = ID,
        titleRes = R.string.settings_module_title,
        descriptionRes = R.string.settings_module_description,
        category = ModuleCategory.CORE,
        order = 95,
        defaultEnabled = true
    )

    override val route = "settings"
    override val icon = Icons.Filled.Settings

    @Composable
    override fun Screen(modifier: Modifier) {
        val viewModel: SettingsViewModel = koinViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        SettingsScreen(
            state = state,
            onThemeModeChange = viewModel::setThemeMode,
            onDynamicColorChange = viewModel::setDynamicColor,
            modifier = modifier
        )
    }

    companion object {
        const val ID = "core.settings"
    }
}

val settingsFeatureModule = module {
    single { SettingsModule() }
}