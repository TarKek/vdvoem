package app.vdvoem.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vdvoem.core.domain.module.ModuleRegistry
import app.vdvoem.core.domain.repository.ModuleSettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ModulesViewModel(
    registry: ModuleRegistry,
    private val repository: ModuleSettingsRepository
) : ViewModel() {

    val enabledIds: StateFlow<Set<String>> = repository.enabledIds.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = registry.defaultEnabledIds
    )

    /** Сохранённый порядок модулей; пустой — ещё не переставляли. */
    val order: StateFlow<List<String>> = repository.order.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun setEnabled(moduleId: String, enabled: Boolean) {
        viewModelScope.launch { repository.setEnabled(moduleId, enabled) }
    }

    /** Новый порядок модулей списком идентификаторов (перестановка идёт из магазина). */
    fun setOrder(ids: List<String>) {
        viewModelScope.launch { repository.setOrder(ids) }
    }
}