package app.vdvoem.core.domain.repository

import app.vdvoem.core.domain.model.CalendarEvent
import kotlinx.coroutines.flow.Flow

interface CalendarRepository {
    fun observeEvents(): Flow<List<CalendarEvent>>
    fun observeEventsIncludingDeleted(): Flow<List<CalendarEvent>>
    suspend fun upsert(event: CalendarEvent)
    suspend fun delete(id: String)
}
