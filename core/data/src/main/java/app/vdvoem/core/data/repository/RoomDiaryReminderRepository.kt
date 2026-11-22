package app.vdvoem.core.data.repository

import app.vdvoem.core.data.LocalChangeSignal
import app.vdvoem.core.data.db.DiaryReminderDao
import app.vdvoem.core.data.db.DiaryReminderEntity
import app.vdvoem.core.domain.model.DiaryReminder
import app.vdvoem.core.domain.repository.DiaryReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomDiaryReminderRepository(
    private val dao: DiaryReminderDao
) : DiaryReminderRepository {

    override fun observeReminders(): Flow<List<DiaryReminder>> =
        dao.observeAll().map { reminders -> reminders.map { it.toDomain() } }

    override fun observeRemindersIncludingDeleted(): Flow<List<DiaryReminder>> =
        dao.observeAllIncludingDeleted().map { reminders -> reminders.map { it.toDomain() } }

    override suspend fun upsert(reminder: DiaryReminder) {
        dao.upsert(reminder.toEntity())
        LocalChangeSignal.notifyLocalWrite()
    }

    override suspend fun setDone(id: String, done: Boolean) {
        val now = System.currentTimeMillis()
        dao.setDone(id = id, doneAt = if (done) now else null, now = now)
        LocalChangeSignal.notifyLocalWrite()
    }

    override suspend fun delete(id: String) {
        dao.softDelete(id = id, now = System.currentTimeMillis())
        LocalChangeSignal.notifyLocalWrite()
    }
}

private fun DiaryReminderEntity.toDomain() = DiaryReminder(
    id = id,
    pairId = pairId,
    text = text,
    remindAt = remindAt,
    isPrivate = isPrivate,
    doneAt = doneAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

private fun DiaryReminder.toEntity() = DiaryReminderEntity(
    id = id,
    pairId = pairId,
    text = text,
    remindAt = remindAt,
    isPrivate = isPrivate,
    doneAt = doneAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)