package app.vdvoem.core.data.repository

import app.vdvoem.core.data.LocalChangeSignal
import app.vdvoem.core.data.db.TaskDao
import app.vdvoem.core.data.db.TaskEntity
import app.vdvoem.core.domain.model.SharedTask
import app.vdvoem.core.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomTaskRepository(
    private val dao: TaskDao
) : TaskRepository {

    override fun observe(): Flow<List<SharedTask>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeIncludingDeleted(): Flow<List<SharedTask>> =
        dao.observeAllIncludingDeleted().map { list -> list.map { it.toDomain() } }

    override suspend fun save(task: SharedTask) {
        dao.upsert(task.toEntity())
        LocalChangeSignal.notifyLocalWrite()
    }

    override suspend fun delete(task: SharedTask) {
        val now = maxOf(System.currentTimeMillis(), task.updatedAt + 1)
        dao.softDelete(id = task.id, now = now)
        LocalChangeSignal.notifyLocalWrite()
    }
}

private fun TaskEntity.toDomain() = SharedTask(
    id = id,
    pairId = pairId,
    title = title,
    note = note,
    dueAt = dueAt,
    isCompleted = isCompleted,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    version = version
)

private fun SharedTask.toEntity() = TaskEntity(
    id = id,
    pairId = pairId,
    title = title,
    note = note,
    dueAt = dueAt,
    isCompleted = isCompleted,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    version = version
)
