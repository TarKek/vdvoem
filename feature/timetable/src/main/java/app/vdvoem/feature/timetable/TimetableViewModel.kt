package app.vdvoem.feature.timetable

import app.vdvoem.core.domain.model.Lesson
import app.vdvoem.core.domain.repository.LessonRepository
import app.vdvoem.core.domain.repository.PairRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.util.UUID

/** Черновик урока в диалоге: null — диалог закрыт. */
data class LessonDraft(
    val id: String? = null,
    val subject: String = "",
    val weekday: Int = 1,
    val start: String = "9:00",
    val end: String = "9:45",
    val room: String = "",
    val teacher: String = "",
    val error: Int? = null
)

data class TimetableUiState(
    val lessons: List<Lesson> = emptyList(),
    val draft: LessonDraft? = null
) {
    /** Уроки по дням недели: 1..7, пустые дни не попадают. */
    val byDay: List<Pair<Int, List<Lesson>>>
        get() = lessons
            .groupBy { it.weekday }
            .toSortedMap()
            .map { (day, list) -> day to list.sortedBy { it.startMinute } }

    val isEmpty: Boolean
        get() = lessons.isEmpty()
}

class TimetableViewModel(
    private val repository: LessonRepository,
    private val pairRepository: PairRepository
) : ViewModel() {

    private val draft = MutableStateFlow<LessonDraft?>(null)
    private val _state = MutableStateFlow(TimetableUiState())

    val state: StateFlow<TimetableUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repository.observe(), draft) { lessons, editing ->
                TimetableUiState(lessons = lessons.filter { it.deletedAt == null }, draft = editing)
            }.collect { _state.value = it }
        }
    }

    fun addLesson(weekday: Int = 1) {
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
            teacher = lesson.teacher
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
            teacher = current.teacher.trim()
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