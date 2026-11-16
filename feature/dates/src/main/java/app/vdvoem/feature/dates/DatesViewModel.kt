package app.vdvoem.feature.dates

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

data class DatesUiState(
    /** Выбранный бюджет; `null` — «Всё». */
    val budget: Budget? = null,
    val randomIdea: DateIdea? = null,
    val shownCount: Int = 0
) {
    val ideas: List<DateIdea> = DateIdeas.byBudget(budget)
}

/**
 * Экран идей свиданий: фильтр по бюджету и «покажи идею».
 *
 * Данных нет: коллекция статичная, поэтому достаточно одного StateFlow в памяти.
 */
class DatesViewModel : ViewModel() {

    private val _state = MutableStateFlow(DatesUiState())
    val state: StateFlow<DatesUiState> = _state.asStateFlow()

    private var current: DatesUiState = _state.value

    fun selectBudget(budget: Budget?) {
        // Идея из прошлой выборки может не попадать в новый фильтр — сбрасываем.
        current = current.copy(budget = budget, randomIdea = null)
        _state.value = current
    }

    fun showIdea() {
        val ideas = current.ideas
        if (ideas.isEmpty()) return
        val previousId = current.randomIdea?.id
        val pool = if (ideas.size > 1) ideas.filter { it.id != previousId } else ideas
        val pick = pool[Random.nextInt(pool.size)]
        current = current.copy(randomIdea = pick, shownCount = current.shownCount + 1)
        _state.value = current
    }
}