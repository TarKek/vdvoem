package app.vdvoem.ui.presents

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Общая выбранная вкладка экрана «Желания и подарки».
 *
 * Нужен, чтобы с главной можно было открыть не только модуль целиком, но и сразу нужную
 * вкладку: `PresentsModule.Screen(modifier)` не принимает параметров, поэтому вкладку
 * передаём через это состояние (Koin single).
 */
class PresentsTabController {

    private val _tab = MutableStateFlow(TAB_WISHES)
    val tab: StateFlow<Int> = _tab.asStateFlow()

    fun select(tab: Int) {
        _tab.value = tab.coerceIn(TAB_WISHES, TAB_GIFTS)
    }

    companion object {
        const val TAB_WISHES = 0
        const val TAB_GIFTS = 1
    }
}