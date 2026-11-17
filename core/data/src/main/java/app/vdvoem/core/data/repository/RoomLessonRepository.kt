package app.vdvoem.core.data.repository

import app.vdvoem.core.data.LocalChangeSignal
import app.vdvoem.core.data.db.LessonDao
import app.vdvoem.core.data.db.LessonEntity
import app.vdvoem.core.domain.model.Lesson
import app.vdvoem.core.domain.repository.LessonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomLessonRepository(
    private val dao: LessonDao
) : LessonRepository {

    override fun observe(): Flow<List<Lesson>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeIncludingDeleted(): Flow<List<Lesson>> =
        dao.observeAllIncludingDeleted().map { list -> list.map { it.toDomain() } }

    override suspend fun save(lesson: Lesson) {
        dao.upsert(lesson.toEntity())
        LocalChangeSignal.notifyLocalWrite()
    }

    override suspend fun delete(lesson: Lesson) {
        val now = System.currentTimeMillis()
        dao.softDelete(id = lesson.id, now = now)
        LocalChangeSignal.notifyLocalWrite()
    }
}

private fun LessonEntity.toDomain() = Lesson(
    id = id,
    pairId = pairId,
    subject = subject,
    weekday = weekday,
    startMinute = startMinute,
    endMinute = endMinute,
    room = room,
    teacher = teacher,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

private fun Lesson.toEntity() = LessonEntity(
    id = id,
    pairId = pairId,
    subject = subject,
    weekday = weekday,
    startMinute = startMinute,
    endMinute = endMinute,
    room = room,
    teacher = teacher,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)