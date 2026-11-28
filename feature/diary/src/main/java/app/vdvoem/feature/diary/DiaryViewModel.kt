package app.vdvoem.feature.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vdvoem.core.domain.model.DiaryEntry
import app.vdvoem.core.domain.model.DiaryMood
import app.vdvoem.core.domain.model.DiaryReminder
import app.vdvoem.core.domain.repository.DiaryReminderRepository
import app.vdvoem.core.domain.repository.DiaryRepository
import app.vdvoem.core.domain.repository.PairRepository
import app.vdvoem.core.domain.time.Dates
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class DiaryUiState(
    val entries: List<DiaryEntry> = emptyList(),
    /** Актуальные напоминания: не удалённые и не выполненные. */
    val reminders: List<DiaryReminder> = emptyList(),
    val loading: Boolean = true
)

/**
 * Общий дневник пары: список записей, напоминания и сохранение/удаление.
 *
 * Репозиторий уже отдаёт записи по дате вниз — экран ничего не пересортировывает.
 */
class DiaryViewModel(
    private val diaryRepository: DiaryRepository,
    private val diaryReminderRepository: DiaryReminderRepository,
    private val pairRepository: PairRepository
) : ViewModel() {

    val state: StateFlow<DiaryUiState> = combine(
        diaryRepository.observeEntries(),
        diaryReminderRepository.observeReminders()
    ) { entries, reminders ->
        DiaryUiState(
            entries = entries,
            reminders = reminders.filter { it.isActive },
            loading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DiaryUiState()
    )

    /**
     * Метки удаления по id.
     *
     * `upsert` переписывает строку целиком, поэтому при правке нужно вернуть исходный
     * `deletedAt`: иначе запись, которую партнёр уже удалил, «ожила» бы от нашей копии.
     * Кэш живёт с экраном и не зависит от подписки UI на список.
     */
    private val deletedMarks: StateFlow<Map<String, Long>> =
        diaryRepository.observeEntriesIncludingDeleted()
            .map { entries ->
                entries.mapNotNull { entry ->
                    entry.deletedAt?.let { deletedAt -> entry.id to deletedAt }
                }.toMap()
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyMap()
            )

    /**
     * Все напоминания, включая выполненные и удалённые: при правке нужны исходные
     * `doneAt`/`deletedAt`, а по паре id — решение о смене приватности.
     */
    private val allReminders: StateFlow<List<DiaryReminder>> =
        diaryReminderRepository.observeRemindersIncludingDeleted()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList()
            )

    /** Создаёт запись при `id == null`, иначе обновляет существующую с тем же id. */
    fun saveEntry(
        id: String?,
        dateMillis: Long,
        mood: DiaryMood,
        title: String,
        body: String
    ) {
        val cleanBody = body.trim()
        // Пустая запись ничего не рассказывает — не сохраняем.
        if (cleanBody.isEmpty()) return
        viewModelScope.launch {
            val pairId = pairRepository.current()?.id ?: LOCAL_PAIR_ID
            diaryRepository.upsert(
                DiaryEntry(
                    id = id ?: UUID.randomUUID().toString(),
                    pairId = pairId,
                    dateMillis = Dates.startOfDay(Dates.toLocalDate(dateMillis)),
                    mood = mood,
                    title = title.trim(),
                    body = cleanBody,
                    updatedAt = System.currentTimeMillis(),
                    deletedAt = id?.let { deletedMarks.value[it] }
                )
            )
        }
    }

    /**
     * Создаёт напоминание при `id == null`, иначе обновляет существующее.
     *
     * Смена «общее → личное» делается новой строкой: старое общее сначала получает
     * tombstone, иначе партнёр так и видел бы напоминание, которое мы спрятали.
     * Обратный переход («личное → общее») безопасен с тем же id.
     */
    fun saveReminder(
        id: String?,
        text: String,
        remindAt: Long,
        isPrivate: Boolean
    ) {
        val cleanText = text.trim()
        // Пустое напоминание не о чем напоминать, прошедшее — уже не напомнит.
        if (cleanText.isEmpty()) return
        if (remindAt <= System.currentTimeMillis()) return
        viewModelScope.launch {
            val pairId = pairRepository.current()?.id ?: LOCAL_PAIR_ID
            val existing = id?.let { target -> allReminders.value.firstOrNull { it.id == target } }
            val now = System.currentTimeMillis()

            if (existing != null && !existing.isPrivate && isPrivate) {
                diaryReminderRepository.upsert(
                    existing.copy(updatedAt = now, deletedAt = now)
                )
                diaryReminderRepository.upsert(
                    DiaryReminder(
                        id = UUID.randomUUID().toString(),
                        pairId = pairId,
                        text = cleanText,
                        remindAt = remindAt,
                        isPrivate = true,
                        updatedAt = now
                    )
                )
                return@launch
            }

            diaryReminderRepository.upsert(
                DiaryReminder(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    pairId = existing?.pairId ?: pairId,
                    text = cleanText,
                    remindAt = remindAt,
                    isPrivate = isPrivate,
                    doneAt = existing?.doneAt,
                    updatedAt = now,
                    deletedAt = existing?.deletedAt
                )
            )
        }
    }

    fun deleteReminder(id: String) {
        viewModelScope.launch { diaryReminderRepository.delete(id) }
    }

    fun setReminderDone(id: String, done: Boolean) {
        viewModelScope.launch { diaryReminderRepository.setDone(id, done) }
    }

    fun deleteEntry(id: String) {
        viewModelScope.launch { diaryRepository.delete(id) }
    }

    private companion object {
        const val LOCAL_PAIR_ID = "local"
    }
}