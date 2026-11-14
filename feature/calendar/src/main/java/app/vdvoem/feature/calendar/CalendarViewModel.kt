package app.vdvoem.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vdvoem.core.domain.model.CalendarEvent
import app.vdvoem.core.domain.repository.CalendarRepository
import app.vdvoem.core.domain.repository.PairRepository
import app.vdvoem.core.domain.time.Dates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

/** Фильтр списка по ближайшему вступлению события. */
enum class CalendarFilter { ALL, TODAY, WEEK, MONTH }

data class EventUi(
    val id: String,
    val pairId: String,
    val title: String,
    /** Ближайшее вступление: для годовщин — этот год или следующий. */
    val date: LocalDate,
    /** Исходная сохранённая дата — нужна для режима правки. */
    val dateMillis: Long,
    val annual: Boolean,
    val daysLeft: Long
)

data class CalendarUiState(
    val events: List<EventUi> = emptyList(),
    val totalCount: Int = 0,
    val filter: CalendarFilter = CalendarFilter.ALL,
    val next: EventUi? = null,
    val loading: Boolean = true
)

class CalendarViewModel(
    private val calendarRepository: CalendarRepository,
    private val pairRepository: PairRepository
) : ViewModel() {

    private val filter = MutableStateFlow(CalendarFilter.ALL)

    val state: StateFlow<CalendarUiState> =
        combine(calendarRepository.observeEvents(), filter) { events, selected ->
            buildState(events, selected)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CalendarUiState()
        )

    fun setFilter(value: CalendarFilter) {
        filter.value = value
    }

    /** Создаёт событие при `id == null`, иначе обновляет существующее. */
    fun saveEvent(id: String?, title: String, dateMillis: Long, annual: Boolean) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            val pairId = pairRepository.current()?.id ?: LOCAL_PAIR_ID
            calendarRepository.upsert(
                CalendarEvent(
                    id = id ?: UUID.randomUUID().toString(),
                    pairId = pairId,
                    title = clean,
                    dateMillis = Dates.startOfDay(Dates.toLocalDate(dateMillis)),
                    annual = annual,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteEvent(id: String) {
        viewModelScope.launch { calendarRepository.delete(id) }
    }

    private fun buildState(events: List<CalendarEvent>, selected: CalendarFilter): CalendarUiState {
        val today = Dates.today()
        val items = sortEvents(
            events.map { event ->
                val date = Dates.nextOccurrence(event.dateMillis, event.annual, today)
                EventUi(
                    id = event.id,
                    pairId = event.pairId,
                    title = event.title,
                    date = date,
                    dateMillis = event.dateMillis,
                    annual = event.annual,
                    daysLeft = Dates.daysUntil(date, today)
                )
            }
        )
        return CalendarUiState(
            events = items.filter { it.matches(selected, today) },
            totalCount = items.size,
            filter = selected,
            next = items.firstOrNull { it.daysLeft >= 0 },
            loading = false
        )
    }

    /**
     * Ближайшее событие сверху, прошедшие — ниже и от свежих к старым.
     * Годовщины уже приведены к следующему вступлению, поэтому сортировка по дате корректна.
     */
    private fun sortEvents(items: List<EventUi>): List<EventUi> {
        val (upcoming, past) = items.partition { it.daysLeft >= 0 }
        val byDate = compareBy<EventUi>({ it.date }, { it.title })
        return upcoming.sortedWith(byDate) +
            past.sortedWith(compareByDescending<EventUi> { it.date }.thenBy { it.title })
    }

    private fun EventUi.matches(selected: CalendarFilter, today: LocalDate): Boolean =
        when (selected) {
            CalendarFilter.ALL -> true
            CalendarFilter.TODAY -> daysLeft == 0L
            CalendarFilter.WEEK -> daysLeft in 0..WEEK_FILTER_DAYS
            CalendarFilter.MONTH ->
                daysLeft >= 0 && date.year == today.year && date.month == today.month
        }

    private companion object {
        const val LOCAL_PAIR_ID = "local"
        const val WEEK_FILTER_DAYS = 7L
    }
}