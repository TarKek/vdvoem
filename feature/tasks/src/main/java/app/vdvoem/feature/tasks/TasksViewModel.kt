package app.vdvoem.feature.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vdvoem.core.domain.device.DeviceIdentity
import app.vdvoem.core.domain.model.SharedTask
import app.vdvoem.core.domain.repository.PairRepository
import app.vdvoem.core.domain.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/** Черновик общего дела: null — редактор закрыт. */
data class TaskDraft(
    val id: String? = null,
    val title: String = "",
    val note: String = "",
    val dueAt: Long? = null,
    val isCompleted: Boolean = false,
    val error: Int? = null
)

data class TasksUiState(
    val tasks: List<SharedTask> = emptyList(),
    val draft: TaskDraft? = null,
    val pendingDelete: SharedTask? = null,
    val isLoading: Boolean = true,
    val error: Int? = null
) {
    val isEmpty: Boolean
        get() = !isLoading && tasks.isEmpty()
}

/** Общие дела пары: local-first, с мягким удалением и LWW-синхронизацией. */
class TasksViewModel(
    private val repository: TaskRepository,
    private val pairRepository: PairRepository,
    private val deviceIdentity: DeviceIdentity
) : ViewModel() {

    private val draft = MutableStateFlow<TaskDraft?>(null)
    private val pendingDelete = MutableStateFlow<SharedTask?>(null)
    private val error = MutableStateFlow<Int?>(null)
    private val _state = MutableStateFlow(TasksUiState())

    val state: StateFlow<TasksUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val tasks = repository.observe().catch {
                error.value = R.string.tasks_error_load
                emit(emptyList())
            }
            combine(tasks, draft, pendingDelete, error) { items, editing, deleting, message ->
                TasksUiState(
                    tasks = items,
                    draft = editing,
                    pendingDelete = deleting,
                    isLoading = false,
                    error = message
                )
            }.collect { _state.value = it }
        }
    }

    fun addTask() {
        draft.value = TaskDraft()
    }

    fun edit(task: SharedTask) {
        draft.value = TaskDraft(
            id = task.id,
            title = task.title,
            note = task.note,
            dueAt = task.dueAt,
            isCompleted = task.isCompleted
        )
    }

    fun update(transform: (TaskDraft) -> TaskDraft) {
        draft.value = draft.value?.let(transform)
    }

    fun dismiss() {
        draft.value = null
    }

    fun dismissError() {
        error.value = null
    }

    fun save() {
        val current = draft.value ?: return
        val title = current.title.trim()
        if (title.isEmpty()) {
            draft.value = current.copy(error = R.string.tasks_error_empty_title)
            return
        }
        viewModelScope.launch {
            runCatching {
                val existing = current.id?.let { id -> _state.value.tasks.firstOrNull { it.id == id } }
                val pairId = existing?.pairId?.takeIf { it.isNotBlank() }
                    ?: pairRepository.current()?.id.orEmpty()
                check(pairId.isNotBlank()) { "pair is not configured" }
                val now = System.currentTimeMillis()
                val task = if (existing == null) {
                    SharedTask.create(
                        id = UUID.randomUUID().toString(),
                        pairId = pairId,
                        createdBy = deviceIdentity.deviceId(),
                        title = title,
                        note = current.note,
                        dueAt = current.dueAt,
                        now = now
                    )
                } else {
                    existing.edit(title = title, note = current.note, dueAt = current.dueAt, now = now)
                }
                repository.save(task)
            }.onSuccess {
                draft.value = null
            }.onFailure {
                draft.value = draft.value?.copy(error = R.string.tasks_error_save)
            }
        }
    }

    fun toggleCompleted(task: SharedTask) {
        viewModelScope.launch {
            runCatching {
                repository.save(task.withCompletion(!task.isCompleted, System.currentTimeMillis()))
            }.onFailure {
                error.value = R.string.tasks_error_save
            }
        }
    }

    fun requestDelete() {
        val current = draft.value ?: return
        val id = current.id ?: return
        pendingDelete.value = _state.value.tasks.firstOrNull { it.id == id } ?: return
    }

    fun cancelDelete() {
        pendingDelete.value = null
    }

    fun confirmDelete() {
        val task = pendingDelete.value ?: return
        viewModelScope.launch {
            runCatching { repository.delete(task) }
                .onSuccess {
                    pendingDelete.value = null
                    draft.value = null
                }
                .onFailure { error.value = R.string.tasks_error_save }
        }
    }
}