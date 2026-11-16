package app.vdvoem.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vdvoem.core.domain.module.ModuleRegistry
import app.vdvoem.core.domain.repository.ModuleSettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Состояние модулей для навигации и магазина дополнений.
 *
 * Модули не включаются и не выключаются — всё из APK доступно всегда. Единственный выбор
 * пользователя: скрыть модуль из навигации или показать снова ([hiddenIds]/[setHidden]).
 */
class ModulesViewModel(
    registry: ModuleRegistry,
    private val repository: ModuleSettingsRepository
) : ViewModel() {

    /** Модули, скрытые из навигации. По умолчанию пусто — видны все. */
    val hiddenIds: StateFlow<Set<String>> = repository.hiddenIds.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptySet()
    )

    /**
     * Устаревший набор включённых модулей.
     *
     * Оставлен для совместимости исходников; навигация и магазин используют [hiddenIds].
     */
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

    /** Скрыть модуль из навигации или снова показать; данные модуля не трогаются. */
    fun setHidden(id: String, hidden: Boolean) {
        viewModelScope.launch { repository.setHidden(id, hidden) }
    }

    /** Устаревший аналог [setHidden]; см. примечание у [enabledIds]. */
    fun setEnabled(moduleId: String, enabled: Boolean) {
        viewModelScope.launch { repository.setEnabled(moduleId, enabled) }
    }

    /** Новый порядок модулей списком идентификаторов (перестановка идёт из магазина). */
    fun setOrder(ids: List<String>) {
        viewModelScope.launch { repository.setOrder(ids) }
    }
}