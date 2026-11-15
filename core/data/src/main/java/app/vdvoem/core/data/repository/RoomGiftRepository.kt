package app.vdvoem.core.data.repository

import app.vdvoem.core.data.LocalChangeSignal
import app.vdvoem.core.data.db.GiftDao
import app.vdvoem.core.data.db.GiftEntity
import app.vdvoem.core.domain.model.Gift
import app.vdvoem.core.domain.model.GiftStatus
import app.vdvoem.core.domain.repository.GiftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomGiftRepository(
    private val dao: GiftDao
) : GiftRepository {

    override fun observeGifts(): Flow<List<Gift>> =
        dao.observeAll().map { gifts -> gifts.map { it.toDomain() } }

    override suspend fun upsert(gift: Gift) {
        dao.upsert(gift.toEntity())
        LocalChangeSignal.notifyLocalWrite()
    }

    override suspend fun delete(id: String) {
        dao.softDelete(id = id, now = System.currentTimeMillis())
        LocalChangeSignal.notifyLocalWrite()
    }
}

private fun GiftEntity.toDomain() = Gift(
    id = id,
    pairId = pairId,
    title = title,
    note = note,
    budget = budget,
    status = runCatching { GiftStatus.valueOf(status) }.getOrDefault(GiftStatus.IDEA),
    updatedAt = updatedAt
)

private fun Gift.toEntity() = GiftEntity(
    id = id,
    pairId = pairId,
    title = title,
    note = note,
    budget = budget,
    status = status.name,
    updatedAt = updatedAt
)
