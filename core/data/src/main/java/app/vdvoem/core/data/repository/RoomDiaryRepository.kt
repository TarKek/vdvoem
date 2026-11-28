package app.vdvoem.core.data.repository

import app.vdvoem.core.data.LocalChangeSignal
import app.vdvoem.core.data.db.DiaryDao
import app.vdvoem.core.data.db.DiaryEntryEntity
import app.vdvoem.core.domain.model.DiaryEntry
import app.vdvoem.core.domain.model.DiaryMood
import app.vdvoem.core.domain.repository.DiaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomDiaryRepository(
    private val dao: DiaryDao
) : DiaryRepository {

    override fun observeEntries(): Flow<List<DiaryEntry>> =
        dao.observeAll().map { entries -> entries.map { it.toDomain() } }

    override fun observeEntriesIncludingDeleted(): Flow<List<DiaryEntry>> =
        dao.observeAllIncludingDeleted().map { entries -> entries.map { it.toDomain() } }

    override suspend fun upsert(entry: DiaryEntry) {
        dao.upsert(entry.toEntity())
        LocalChangeSignal.notifyLocalWrite()
    }

    override suspend fun delete(id: String) {
        dao.softDelete(id = id, now = System.currentTimeMillis())
        LocalChangeSignal.notifyLocalWrite()
    }
}

private fun DiaryEntryEntity.toDomain() = DiaryEntry(
    id = id,
    pairId = pairId,
    dateMillis = dateMillis,
    mood = runCatching { DiaryMood.valueOf(mood) }.getOrDefault(DiaryMood.CALM),
    title = title,
    body = body,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

private fun DiaryEntry.toEntity() = DiaryEntryEntity(
    id = id,
    pairId = pairId,
    dateMillis = dateMillis,
    mood = mood.name,
    title = title,
    body = body,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)
