package app.vdvoem.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vdvoem.core.domain.model.CalendarEvent
import app.vdvoem.core.domain.model.Pair
import app.vdvoem.core.domain.repository.CalendarRepository
import app.vdvoem.core.domain.repository.PairRepository
import app.vdvoem.core.domain.time.Dates
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class DashboardUiState(
    val pair: Pair? = null,
    val nextEvent: CalendarEvent? = null
)

class DashboardViewModel(
    private val pairRepository: PairRepository,
    private val calendarRepository: CalendarRepository
) : ViewModel() {

    val state = combine(
        pairRepository.observe(),
        calendarRepository.observeEvents()
    ) { pair, events ->
        DashboardUiState(
            pair = pair,
            nextEvent = events.minByOrNull {
                Dates.nextOccurrence(it.dateMillis, it.annual)
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState()
    )

    fun savePair(nameA: String, nameB: String, sinceMillis: Long) {
        val first = nameA.trim().ifEmpty { "Я" }
        val second = nameB.trim().ifEmpty { "Партнёр" }
        viewModelScope.launch {
            val existing = pairRepository.current()
            pairRepository.save(
                Pair(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    nameA = first,
                    nameB = second,
                    sinceMillis = sinceMillis
                )
            )
        }
    }
}
