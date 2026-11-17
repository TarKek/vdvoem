package app.vdvoem.core.data.repository

import app.vdvoem.core.data.LocalChangeSignal
import app.vdvoem.core.data.db.EventDao
import app.vdvoem.core.data.db.EventEntity
import app.vdvoem.core.domain.model.CalendarEvent
import app.vdvoem.core.domain.repository.CalendarRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomCalendarRepository(
    private val dao: EventDao
) : CalendarRepository {

    override fun observeEvents(): Flow<List<CalendarEvent>> =
        dao.observeAll().map { events -> events.map { it.toDomain() } }

    override fun observeEventsIncludingDeleted(): Flow<List<CalendarEvent>> =
        dao.observeAllIncludingDeleted().map { events -> events.map { it.toDomain() } }

    override suspend fun upsert(event: CalendarEvent) {
        dao.upsert(event.toEntity())
        LocalChangeSignal.notifyLocalWrite()
    }

    override suspend fun delete(id: String) {
        dao.softDelete(id = id, now = System.currentTimeMillis())
        LocalChangeSignal.notifyLocalWrite()
    }
}

private fun EventEntity.toDomain() = CalendarEvent(
    id = id,
    pairId = pairId,
    title = title,
    dateMillis = dateMillis,
    annual = annual,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

private fun CalendarEvent.toEntity() = EventEntity(
    id = id,
    pairId = pairId,
    title = title,
    dateMillis = dateMillis,
    annual = annual,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)
