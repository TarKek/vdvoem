package app.vdvoem.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vdvoem.core.domain.model.CalendarEvent
import app.vdvoem.core.domain.repository.CalendarRepository
import app.vdvoem.core.domain.repository.PairRepository
import app.vdvoem.core.domain.time.Dates
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

data class EventUi(
    val id: String,
    val title: String,
    val date: LocalDate,
    val annual: Boolean,
    val daysLeft: Long
)

data class CalendarUiState(
    val events: List<EventUi> = emptyList(),
    val next: EventUi? = null,
    val loading: Boolean = true
)

class CalendarViewModel(
    private val calendarRepository: CalendarRepository,
    private val pairRepository: PairRepository
) : ViewModel() {

    val state = calendarRepository.observeEvents()
        .map { events ->
            val today = Dates.today()
            val items = events
                .map { event ->
                    val date = Dates.nextOccurrence(event.dateMillis, event.annual, today)
                    EventUi(
                        id = event.id,
                        title = event.title,
                        date = date,
                        annual = event.annual,
                        daysLeft = Dates.daysUntil(date, today)
                    )
                }
                .sortedBy { it.date }
            CalendarUiState(events = items, next = items.firstOrNull(), loading = false)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CalendarUiState()
        )

    fun addEvent(title: String, dateMillis: Long, annual: Boolean) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            val pairId = pairRepository.current()?.id ?: LOCAL_PAIR_ID
            calendarRepository.upsert(
                CalendarEvent(
                    id = UUID.randomUUID().toString(),
                    pairId = pairId,
                    title = clean,
                    dateMillis = dateMillis,
                    annual = annual
                )
            )
        }
    }

    fun deleteEvent(id: String) {
        viewModelScope.launch { calendarRepository.delete(id) }
    }

    private companion object {
        const val LOCAL_PAIR_ID = "local"
    }
}
