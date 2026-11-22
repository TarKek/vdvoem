package app.vdvoem.core.domain.repository

import app.vdvoem.core.domain.model.SharedTask
import kotlinx.coroutines.flow.Flow

/** Общие дела: local-first, все записи проходят через репозиторий для синка и tombstone. */
interface TaskRepository {
    fun observe(): Flow<List<SharedTask>>
    fun observeIncludingDeleted(): Flow<List<SharedTask>>

    suspend fun save(task: SharedTask)

    suspend fun delete(task: SharedTask)
}
