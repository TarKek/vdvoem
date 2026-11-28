package app.vdvoem.core.domain.repository

import app.vdvoem.core.domain.model.DiaryEntry
import kotlinx.coroutines.flow.Flow

/**
 * Записи общего дневника пары.
 *
 * Как и календарь, дневник работает офлайн: запись сначала попадает в локальную
 * базу, а синхронизация отвозит её партнёру. Удаление — мягкое (tombstone),
 * чтобы удалённая запись не «оживала» от старой копии на другом телефоне.
 */
interface DiaryRepository {
    fun observeEntries(): Flow<List<DiaryEntry>>
    fun observeEntriesIncludingDeleted(): Flow<List<DiaryEntry>>
    suspend fun upsert(entry: DiaryEntry)
    suspend fun delete(id: String)
}