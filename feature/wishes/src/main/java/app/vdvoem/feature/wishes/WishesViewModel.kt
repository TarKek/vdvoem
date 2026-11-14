package app.vdvoem.feature.wishes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vdvoem.core.domain.model.Gift
import app.vdvoem.core.domain.model.GiftStatus
import app.vdvoem.core.domain.model.Wish
import app.vdvoem.core.domain.model.WishStatus
import app.vdvoem.core.domain.repository.GiftRepository
import app.vdvoem.core.domain.repository.PairRepository
import app.vdvoem.core.domain.repository.WishRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/** Фильтр списка желаний. */
enum class WishFilter { ALL, WANTED, RESERVED, DONE }

/** Порядок сортировки списка желаний. */
enum class WishSort { NEWEST, PRICE_DESC, ALPHABET }

data class WishesUiState(
    val wishes: List<Wish> = emptyList(),
    val loading: Boolean = true
)

class WishesViewModel(
    private val wishRepository: WishRepository,
    private val pairRepository: PairRepository,
    private val giftRepository: GiftRepository
) : ViewModel() {

    val state: StateFlow<WishesUiState> = wishRepository.observeWishes()
        .map { wishes ->
            WishesUiState(
                wishes = wishes.sortedByDescending { it.updatedAt },
                loading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WishesUiState()
        )

    private val _filter = MutableStateFlow(WishFilter.ALL)
    val filter: StateFlow<WishFilter> = _filter.asStateFlow()

    private val _sort = MutableStateFlow(WishSort.NEWEST)
    val sort: StateFlow<WishSort> = _sort.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    fun setFilter(filter: WishFilter) {
        _filter.value = filter
    }

    fun setSort(sort: WishSort) {
        _sort.value = sort
    }

    fun setQuery(query: String) {
        _query.value = query
    }

    fun resetQuery() {
        _query.value = ""
    }

    fun add(title: String, note: String, price: Long?) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            val pairId = pairRepository.current()?.id ?: LOCAL_PAIR_ID
            wishRepository.upsert(
                Wish(
                    id = UUID.randomUUID().toString(),
                    pairId = pairId,
                    title = clean,
                    note = note.trim(),
                    priceEstimate = price,
                    status = WishStatus.WANTED,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun update(id: String, title: String, note: String, price: Long?) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        val wish = state.value.wishes.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            wishRepository.upsert(
                wish.copy(
                    title = clean,
                    note = note.trim(),
                    priceEstimate = price,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun setStatus(id: String, status: WishStatus) {
        val wish = state.value.wishes.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            wishRepository.upsert(wish.copy(status = status, updatedAt = System.currentTimeMillis()))
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { wishRepository.delete(id) }
    }

    /**
     * Желание сразу становится идеей подарка, а само желание помечается «уже дарят»,
     * чтобы партнёр не купил то же самое дважды.
     */
    fun makeGift(wishId: String) {
        viewModelScope.launch {
            val wish = wishRepository.observeWishes().first()
                .firstOrNull { it.id == wishId } ?: return@launch
            val pairId = wish.pairId.ifBlank { pairRepository.current()?.id ?: LOCAL_PAIR_ID }
            val now = System.currentTimeMillis()
            giftRepository.upsert(
                Gift(
                    id = UUID.randomUUID().toString(),
                    pairId = pairId,
                    title = wish.title,
                    note = NOTE_FROM_WISH,
                    budget = wish.priceEstimate,
                    status = GiftStatus.IDEA,
                    updatedAt = now
                )
            )
            wishRepository.upsert(wish.copy(status = WishStatus.RESERVED, updatedAt = now))
        }
    }

    private companion object {
        const val LOCAL_PAIR_ID = "local"

        /** Метка «подарок пришёл из желаний»; её же ищет экран подарков. */
        const val NOTE_FROM_WISH = "Из желаний"
    }
}