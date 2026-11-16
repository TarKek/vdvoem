package app.vdvoem.feature.notes.data

import kotlinx.coroutines.flow.Flow

/**
 * Хранилище личных заметок. Интерфейс объявлен внутри модуля сознательно:
 * ядро и синк не должны даже знать о существовании заметок.
 */
interface NotesRepository {
    fun observeAll(): Flow<List<Note>>
    suspend fun save(note: Note)
    suspend fun delete(id: String)
}