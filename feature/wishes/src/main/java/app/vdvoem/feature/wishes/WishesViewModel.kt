package app.vdvoem.feature.wishes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vdvoem.core.domain.device.DeviceIdentity
import app.vdvoem.core.domain.model.Wish
import app.vdvoem.core.domain.model.WishStatus
import app.vdvoem.core.domain.repository.PairRepository
import app.vdvoem.core.domain.repository.WishGiftConverter
import app.vdvoem.core.domain.repository.WishRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/** Фильтр списка желаний. */
enum class WishFilter { ALL, WANTED, RESERVED, DONE }

/** Порядок сортировки списка желаний. */
enum class WishSort { NEWEST, PRICE_DESC, PRIORITY, ALPHABET }

data class WishesUiState(
    val wishes: List<Wish> = emptyList(),
    val loading: Boolean = true
)

class WishesViewModel(
    private val wishRepository: WishRepository,
    private val pairRepository: PairRepository,
    private val wishGiftConverter: WishGiftConverter,
    private val deviceIdentity: DeviceIdentity
) : ViewModel() {

    /** Id установки нужен, чтобы отличать свои «сюрпризы» от чужих. */
    private val deviceId = MutableStateFlow("")

    init {
        viewModelScope.launch { deviceId.value = deviceIdentity.deviceId() }
    }

    val state: StateFlow<WishesUiState> = combine(
        wishRepository.observeWishes(),
        deviceId
    ) { wishes, localDeviceId ->
        WishesUiState(
            wishes = wishes
                .filter { wish -> wish.deletedAt == null }
                .filter { wish -> isVisible(wish, localDeviceId) }
                .sortedByDescending { it.updatedAt },
            // Пока id установки не прочитан, сюрпризы лучше не показывать вовсе.
            loading = localDeviceId.isBlank()
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

    fun add(
        title: String,
        note: String,
        price: Long?,
        priority: Int,
        url: String,
        surprise: Boolean
    ) {
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
                    updatedAt = System.currentTimeMillis(),
                    priority = priority.coerceIn(Wish.PRIORITY_NORMAL, Wish.PRIORITY_HIGH),
                    url = url.trim(),
                    surprise = surprise,
                    ownerDeviceId = deviceIdentity.deviceId()
                )
            )
        }
    }

    fun update(
        id: String,
        title: String,
        note: String,
        price: Long?,
        priority: Int,
        url: String,
        surprise: Boolean
    ) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        val wish = state.value.wishes.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            wishRepository.upsert(
                wish.copy(
                    title = clean,
                    note = note.trim(),
                    priceEstimate = price,
                    priority = priority.coerceIn(Wish.PRIORITY_NORMAL, Wish.PRIORITY_HIGH),
                    url = url.trim(),
                    surprise = surprise,
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
     * чтобы партнёр не купил то же самое дважды. Желание приходит из карточки —
     * лишний `observeWishes().first()` больше не нужен.
     */
    fun makeGift(wish: Wish) {
        viewModelScope.launch { wishGiftConverter.convert(wish.id) }
    }

    /** Свой сюрприз виден всегда, чужой — никогда. */
    private fun isVisible(wish: Wish, localDeviceId: String): Boolean = when {
        !wish.surprise -> true
        wish.ownerDeviceId.isBlank() -> true
        else -> wish.ownerDeviceId == localDeviceId
    }

    private companion object {
        const val LOCAL_PAIR_ID = "local"

        /** Метка «подарок пришёл из желаний»; нужна для совместимости со старыми данными. */
        const val NOTE_FROM_WISH = "Из желаний"
    }
}