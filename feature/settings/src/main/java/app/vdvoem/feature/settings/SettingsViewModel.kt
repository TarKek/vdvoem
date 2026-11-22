package app.vdvoem.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vdvoem.core.domain.settings.AppearanceSettings
import app.vdvoem.core.domain.settings.AppSettings
import app.vdvoem.core.domain.settings.AppSettingsRepository
import app.vdvoem.core.domain.settings.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: AppSettingsRepository
) : ViewModel() {

    val state: StateFlow<AppSettings> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings()
    )

    fun setThemeMode(mode: ThemeMode) {
        if (state.value.themeMode == mode) return
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        if (state.value.dynamicColor == enabled) return
        viewModelScope.launch { repository.setDynamicColor(enabled) }
    }

    fun setAppearance(appearance: AppearanceSettings) {
        viewModelScope.launch { repository.setAppearance(appearance) }
    }

    fun updateAppearance(transform: (AppearanceSettings) -> AppearanceSettings) {
        setAppearance(transform(state.value.appearance))
    }
}