package app.vdvoem.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vdvoem.core.domain.model.CalendarEvent
import app.vdvoem.core.domain.model.Pair
import app.vdvoem.core.domain.repository.CalendarRepository
import app.vdvoem.core.domain.repository.GiftRepository
import app.vdvoem.core.domain.repository.PairRepository
import app.vdvoem.core.domain.repository.WishRepository
import app.vdvoem.core.domain.time.Dates
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class DashboardUiState(
    val pair: Pair? = null,
    val nextEvent: CalendarEvent? = null,
    val eventCount: Int = 0,
    val wishCount: Int = 0,
    val giftCount: Int = 0
)

class DashboardViewModel(
    private val pairRepository: PairRepository,
    private val calendarRepository: CalendarRepository,
    private val wishRepository: WishRepository,
    private val giftRepository: GiftRepository
) : ViewModel() {

    val state = combine(
        pairRepository.observe(),
        calendarRepository.observeEvents(),
        wishRepository.observeWishes(),
        giftRepository.observeGifts()
    ) { pair, events, wishes, gifts ->
        DashboardUiState(
            pair = pair,
            nextEvent = events.minByOrNull {
                Dates.nextOccurrence(it.dateMillis, it.annual)
            },
            eventCount = events.size,
            wishCount = wishes.size,
            giftCount = gifts.size
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
