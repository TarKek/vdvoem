package app.vdvoem.feature.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vdvoem.feature.notes.data.Note
import app.vdvoem.feature.notes.data.NotesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.UUID

/** Черновик заметки в редакторе: null — редактор закрыт. */
data class NoteDraft(
    val id: String? = null,
    val title: String = "",
    val body: String = "",
    val error: Int? = null
)

data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val draft: NoteDraft? = null,
    /** Заметка, ожидающая подтверждения удаления. */
    val pendingDelete: Note? = null
) {
    val isEmpty: Boolean
        get() = notes.isEmpty()
}

class NotesViewModel(
    private val repository: NotesRepository
) : ViewModel() {

    private val draft = MutableStateFlow<NoteDraft?>(null)
    private val pendingDelete = MutableStateFlow<Note?>(null)
    private val _state = MutableStateFlow(NotesUiState())

    val state: StateFlow<NotesUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repository.observeAll(), draft, pendingDelete) { notes, editing, deleting ->
                NotesUiState(notes = notes, draft = editing, pendingDelete = deleting)
            }.collect { _state.value = it }
        }
    }

    fun addNote() {
        draft.value = NoteDraft()
    }

    fun edit(note: Note) {
        draft.value = NoteDraft(id = note.id, title = note.title, body = note.body)
    }

    fun update(transform: (NoteDraft) -> NoteDraft) {
        draft.value = draft.value?.let(transform)
    }

    fun dismiss() {
        draft.value = null
    }

    fun save() {
        val current = draft.value ?: return
        val title = current.title.trim()
        val body = current.body.trim()
        if (title.isEmpty() && body.isEmpty()) {
            draft.value = current.copy(error = R.string.notes_error_empty)
            return
        }
        val now = System.currentTimeMillis()
        val existing = current.id?.let { id -> state.value.notes.firstOrNull { it.id == id } }
        val note = Note(
            id = existing?.id ?: UUID.randomUUID().toString(),
            title = title,
            body = body,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        viewModelScope.launch {
            repository.save(note)
            draft.value = null
        }
    }

    /** Удаление всегда через подтверждение: заметки личные, восстановить их неоткуда. */
    fun requestDelete() {
        val current = draft.value ?: return
        val id = current.id ?: return
        val note = state.value.notes.firstOrNull { it.id == id } ?: return
        pendingDelete.value = note
    }

    fun cancelDelete() {
        pendingDelete.value = null
    }

    fun confirmDelete() {
        val note = pendingDelete.value ?: return
        viewModelScope.launch {
            repository.delete(note.id)
            pendingDelete.value = null
            draft.value = null
        }
    }
}