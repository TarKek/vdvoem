package app.vdvoem.feature.gifts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vdvoem.core.domain.model.Gift
import app.vdvoem.core.domain.model.GiftStatus
import app.vdvoem.core.domain.repository.GiftRepository
import app.vdvoem.core.domain.repository.PairRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/** Порядок сортировки списка подарков. */
enum class GiftSort { NEWEST, BUDGET_DESC, ALPHABET }

data class GiftsUiState(
    val ideas: List<Gift> = emptyList(),
    val bought: List<Gift> = emptyList(),
    val given: List<Gift> = emptyList(),
    val ideaBudget: Long = 0,
    val boughtBudget: Long = 0,
    val givenBudget: Long = 0,
    val query: String = "",
    val sort: GiftSort = GiftSort.NEWEST,
    /** Есть хотя бы один подарок без учёта поиска. */
    val hasGifts: Boolean = false
) {
    /** Подарков нет вообще — показываем приглашение добавить первый. */
    val isEmpty: Boolean get() = !hasGifts

    /** Подарки есть, но под поиск ничего не подошло. */
    val isSearchEmpty: Boolean get() = hasGifts && ideas.isEmpty() && bought.isEmpty() && given.isEmpty()
}

class GiftsViewModel(
    private val giftRepository: GiftRepository,
    private val pairRepository: PairRepository
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val sort = MutableStateFlow(GiftSort.NEWEST)

    val state: StateFlow<GiftsUiState> = combine(
        giftRepository.observeGifts(),
        query,
        sort
    ) { gifts, currentQuery, currentSort ->
        buildState(gifts, currentQuery, currentSort)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GiftsUiState()
    )

    fun setQuery(value: String) {
        query.value = value
    }

    fun resetQuery() {
        query.value = ""
    }

    fun setSort(value: GiftSort) {
        sort.value = value
    }

    fun add(title: String, note: String, budget: Long?) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        viewModelScope.launch {
            giftRepository.upsert(
                Gift(
                    id = UUID.randomUUID().toString(),
                    pairId = pairRepository.current()?.id ?: LOCAL_PAIR_ID,
                    title = cleanTitle,
                    note = note.trim(),
                    budget = budget,
                    status = GiftStatus.IDEA,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun update(id: String, title: String, note: String, budget: Long?, status: GiftStatus) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        viewModelScope.launch {
            val gift = loadGift(id) ?: return@launch
            giftRepository.upsert(
                gift.copy(
                    title = cleanTitle,
                    note = note.trim(),
                    budget = budget,
                    status = status,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun setStatus(id: String, status: GiftStatus) {
        viewModelScope.launch {
            val gift = loadGift(id) ?: return@launch
            giftRepository.upsert(gift.copy(status = status, updatedAt = System.currentTimeMillis()))
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { giftRepository.delete(id) }
    }

    /** Берём запись из хранилища целиком: отфильтрованный список может её не содержать. */
    private suspend fun loadGift(id: String): Gift? =
        giftRepository.observeGifts().first().firstOrNull { it.id == id }

    private fun buildState(gifts: List<Gift>, query: String, sort: GiftSort): GiftsUiState {
        val needle = query.trim()
        val visible = gifts
            .filter { it.matches(needle) }
            .sortedWith(sort.comparator)
        val ideas = visible.filter { it.status == GiftStatus.IDEA }
        val bought = visible.filter { it.status == GiftStatus.BOUGHT }
        val given = visible.filter { it.status == GiftStatus.GIVEN }
        return GiftsUiState(
            ideas = ideas,
            bought = bought,
            given = given,
            ideaBudget = ideas.sumOf { it.budget ?: 0L },
            boughtBudget = bought.sumOf { it.budget ?: 0L },
            givenBudget = given.sumOf { it.budget ?: 0L },
            query = query,
            sort = sort,
            hasGifts = gifts.isNotEmpty()
        )
    }

    private companion object {
        const val LOCAL_PAIR_ID = "local"
    }
}

private fun Gift.matches(needle: String): Boolean =
    needle.isEmpty() || title.contains(needle, ignoreCase = true) || note.contains(needle, ignoreCase = true)

private val GiftSort.comparator: Comparator<Gift>
    get() = when (this) {
        GiftSort.NEWEST -> compareByDescending { it.updatedAt }
        GiftSort.BUDGET_DESC -> compareBy<Gift> { it.budget ?: -1L }.reversed()
        GiftSort.ALPHABET -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
    }