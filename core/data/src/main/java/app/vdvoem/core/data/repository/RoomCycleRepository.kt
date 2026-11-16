package app.vdvoem.core.data.repository

import app.vdvoem.core.data.LocalChangeSignal
import app.vdvoem.core.data.db.CycleDayDao
import app.vdvoem.core.data.db.CycleDayEntity
import app.vdvoem.core.data.db.CycleSettingsDao
import app.vdvoem.core.data.db.CycleSettingsEntity
import app.vdvoem.core.domain.model.CycleDay
import app.vdvoem.core.domain.model.CycleSettings
import app.vdvoem.core.domain.model.Flow as FlowIntensity
import app.vdvoem.core.domain.repository.CycleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomCycleRepository(
    private val dayDao: CycleDayDao,
    private val settingsDao: CycleSettingsDao
) : CycleRepository {

    override fun observeDays(): Flow<List<CycleDay>> =
        dayDao.observeAll().map { days -> days.map { it.toDomain() } }

    override fun observeDaysIncludingDeleted(): Flow<List<CycleDay>> =
        dayDao.observeAllIncludingDeleted().map { days -> days.map { it.toDomain() } }

    override fun observeSettings(): Flow<CycleSettings?> =
        settingsDao.observe().map { settings -> settings?.toDomain() }

    override fun observeSettingsIncludingDeleted(): Flow<List<CycleSettings>> =
        settingsDao.observeAllIncludingDeleted().map { list -> list.map { it.toDomain() } }

    override suspend fun saveDay(day: CycleDay) {
        dayDao.upsert(day.toEntity())
        LocalChangeSignal.notifyLocalWrite()
    }

    override suspend fun saveSettings(settings: CycleSettings) {
        settingsDao.upsert(settings.toEntity())
        LocalChangeSignal.notifyLocalWrite()
    }
}

private fun CycleDayEntity.toDomain() = CycleDay(
    id = id,
    pairId = pairId,
    dateMillis = dateMillis,
    period = period,
    flow = runCatching { FlowIntensity.valueOf(flow) }.getOrDefault(FlowIntensity.NONE),
    symptoms = symptoms,
    mood = mood,
    weight = weight,
    note = note,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

private fun CycleDay.toEntity() = CycleDayEntity(
    id = id,
    pairId = pairId,
    dateMillis = dateMillis,
    period = period,
    flow = flow.name,
    symptoms = symptoms,
    mood = mood,
    weight = weight,
    note = note,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

private fun CycleSettingsEntity.toDomain() = CycleSettings(
    id = id,
    pairId = pairId,
    cycleLength = cycleLength,
    periodLength = periodLength,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

private fun CycleSettings.toEntity() = CycleSettingsEntity(
    id = id,
    pairId = pairId,
    cycleLength = cycleLength,
    periodLength = periodLength,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)