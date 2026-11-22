package app.vdvoem.feature.timetable

import app.vdvoem.core.domain.model.Lesson
import app.vdvoem.core.domain.repository.LessonRepository
import app.vdvoem.core.domain.repository.PairRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.util.UUID

/** Сколько дней показываем в горизонтальной ленте дат: две недели от понедельника. */
const val TIMETABLE_STRIP_DAYS = 14

/** Понедельник недели, в которую попадает [date]. */
fun weekStart(date: LocalDate): LocalDate =
    date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/**
 * Сколько ждать до следующей полуночи. Небольшой запас гарантирует, что после
 * пробуждения `LocalDate.now()` уже вернёт новый день.
 */
internal fun millisUntilNextMidnight(now: LocalDateTime): Long =
    Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay())
        .toMillis()
        .coerceAtLeast(1_000L) + 1_000L

/** Черновик урока в диалоге: null — диалог закрыт. */
data class LessonDraft(
    val id: String? = null,
    val subject: String = "",
    val weekday: Int = 1,
    val start: String = "9:00",
    val end: String = "9:45",
    val room: String = "",
    val teacher: String = "",
    val textbookBy: String = "",
    val error: Int? = null
)

data class TimetableUiState(
    val lessons: List<Lesson> = emptyList(),
    val draft: LessonDraft? = null,
    val selectedDate: LocalDate = LocalDate.now(),
    /** Текущая дата: двигается в полночь, даже если экран остаётся открытым. */
    val today: LocalDate = LocalDate.now()
) {
    /** Уроки выбранного дня, по времени. */
    val dayLessons: List<Lesson>
        get() = lessons
            .filter { it.weekday == selectedDate.dayOfWeek.value }
            .sortedBy { it.startMinute }

    val isEmpty: Boolean
        get() = lessons.isEmpty()
}

class TimetableViewModel(
    private val repository: LessonRepository,
    private val pairRepository: PairRepository,
    private val todayProvider: () -> LocalDate = LocalDate::now
) : ViewModel() {

    private val initialToday = todayProvider()
    private val draft = MutableStateFlow<LessonDraft?>(null)
    private val selectedDate = MutableStateFlow(initialToday)
    private val today = MutableStateFlow(initialToday)
    private val _state = MutableStateFlow(
        TimetableUiState(selectedDate = initialToday, today = initialToday)
    )

    val state: StateFlow<TimetableUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observe(),
                draft,
                selectedDate,
                today
            ) { lessons, editing, date, currentDate ->
                TimetableUiState(
                    lessons = lessons.filter { it.deletedAt == null },
                    draft = editing,
                    selectedDate = date,
                    today = currentDate
                )
            }.collect { _state.value = it }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(millisUntilNextMidnight(LocalDateTime.now()))
                refreshToday()
            }
        }
    }

    /** Обновляет «сегодня» при возврате в foreground и после полуночи. */
    fun refreshToday() {
        today.value = todayProvider()
    }

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
    }

    /**
     * Читает выбранную дату напрямую, а не из асинхронно обновлённого UI-state:
     * иначе быстрый тап «другая дата → добавить» создаёт урок со старым weekday.
     */
    fun addLesson(weekday: Int = selectedDate.value.dayOfWeek.value) {
        draft.value = LessonDraft(weekday = weekday)
    }

    fun edit(lesson: Lesson) {
        draft.value = LessonDraft(
            id = lesson.id,
            subject = lesson.subject,
            weekday = lesson.weekday,
            start = Lesson.formatMinute(lesson.startMinute),
            end = Lesson.formatMinute(lesson.endMinute),
            room = lesson.room,
            teacher = lesson.teacher,
            textbookBy = lesson.textbookBy
        )
    }

    fun update(transform: (LessonDraft) -> LessonDraft) {
        draft.value = draft.value?.let(transform)
    }

    fun dismiss() {
        draft.value = null
    }

    fun save() {
        val current = draft.value ?: return
        val subject = current.subject.trim()
        if (subject.isBlank()) {
            draft.value = current.copy(error = R.string.timetable_error_subject)
            return
        }
        val start = Lesson.parseMinute(current.start)
        val end = Lesson.parseMinute(current.end)
        if (start == null || end == null) {
            draft.value = current.copy(error = R.string.timetable_error_time)
            return
        }
        if (end <= start) {
            draft.value = current.copy(error = R.string.timetable_error_order)
            return
        }
        val lesson = Lesson(
            id = current.id ?: UUID.randomUUID().toString(),
            pairId = state.value.lessons.firstOrNull { it.id == current.id }?.pairId.orEmpty(),
            subject = subject,
            weekday = current.weekday.coerceIn(1, 7),
            startMinute = start,
            endMinute = end,
            room = current.room.trim(),
            teacher = current.teacher.trim(),
            textbookBy = current.textbookBy.trim()
        )
        viewModelScope.launch {
            val pairId = lesson.pairId.ifBlank { pairRepository.current()?.id.orEmpty() }
            repository.save(lesson.copy(pairId = pairId))
            draft.value = null
        }
    }

    fun delete() {
        val current = draft.value ?: return
        val id = current.id ?: return
        val existing = state.value.lessons.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            repository.delete(existing)
            draft.value = null
        }
    }
}