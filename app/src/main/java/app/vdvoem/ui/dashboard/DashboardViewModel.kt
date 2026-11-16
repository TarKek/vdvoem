package app.vdvoem.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vdvoem.core.domain.model.CalendarEvent
import app.vdvoem.core.domain.model.Pair
import app.vdvoem.core.domain.model.Poke
import app.vdvoem.core.domain.repository.CalendarRepository
import app.vdvoem.core.domain.repository.GiftRepository
import app.vdvoem.core.domain.repository.PairRepository
import app.vdvoem.core.domain.repository.PokeRepository
import app.vdvoem.core.domain.repository.WishRepository
import app.vdvoem.core.domain.time.Dates
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class DashboardUiState(
    val pair: Pair? = null,
    val events: List<CalendarEvent> = emptyList(),
    val nextEvent: CalendarEvent? = null,
    val eventCount: Int = 0,
    val wishCount: Int = 0,
    val giftCount: Int = 0,
    val lastPoke: Poke? = null,
    val pokes: List<Poke> = emptyList()
)

class DashboardViewModel(
    private val pairRepository: PairRepository,
    private val calendarRepository: CalendarRepository,
    private val wishRepository: WishRepository,
    private val giftRepository: GiftRepository,
    private val pokeRepository: PokeRepository
) : ViewModel() {

    val state = combine(
        pairRepository.observe(),
        calendarRepository.observeEvents(),
        wishRepository.observeWishes(),
        giftRepository.observeGifts(),
        pokeRepository.observe()
    ) { pair, events, wishes, gifts, pokes ->
        val today = Dates.today()
        val dated = events.map { event ->
            event to Dates.nextOccurrence(event.dateMillis, event.annual, today)
        }
        // Сначала ближайшее сегодняшнее/будущее; если таких нет — самое свежее прошедшее,
        // чтобы виджет не показывал отрицательные дни вроде «-1 день».
        val upcoming = dated.filter { (_, date) -> !date.isBefore(today) }
            .minByOrNull { (_, date) -> date }
        val latestPast = dated.maxByOrNull { (_, date) -> date }
        DashboardUiState(
            pair = pair,
            events = events.sortedBy { it.dateMillis },
            nextEvent = (upcoming ?: latestPast)?.first,
            eventCount = events.size,
            wishCount = wishes.size,
            giftCount = gifts.size,
            lastPoke = pokes.firstOrNull(),
            pokes = pokes
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState()
    )

    fun savePair(
        nameA: String,
        nameB: String,
        sinceMillis: Long,
        birthdayA: Long?,
        birthdayB: Long?
    ) {
        val first = nameA.trim().ifEmpty { "Я" }
        val second = nameB.trim().ifEmpty { "Партнёр" }
        viewModelScope.launch {
            val existing = pairRepository.current()
            pairRepository.save(
                Pair(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    nameA = first,
                    nameB = second,
                    sinceMillis = sinceMillis,
                    birthdayA = birthdayA,
                    birthdayB = birthdayB
                )
            )
        }
    }
}
