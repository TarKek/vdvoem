package app.vdvoem.feature.notes.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Room-реализация репозитория заметок: чистое локальное хранилище, без сети. */
class RoomNotesRepository(
    private val dao: NoteDao
) : NotesRepository {

    override fun observeAll(): Flow<List<Note>> =
        dao.observeAll().map { entities -> entities.map { it.toModel() } }

    override suspend fun save(note: Note) {
        dao.upsert(note.toEntity())
    }

    override suspend fun delete(id: String) {
        dao.deleteById(id)
    }
}

private fun NoteEntity.toModel() = Note(
    id = id,
    title = title,
    body = body,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun Note.toEntity() = NoteEntity(
    id = id,
    title = title,
    body = body,
    createdAt = createdAt,
    updatedAt = updatedAt
)