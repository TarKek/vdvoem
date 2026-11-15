package app.vdvoem.core.data.repository

import app.vdvoem.core.data.LocalChangeSignal
import app.vdvoem.core.data.db.WishDao
import app.vdvoem.core.data.db.WishEntity
import app.vdvoem.core.domain.model.Wish
import app.vdvoem.core.domain.model.WishStatus
import app.vdvoem.core.domain.repository.WishRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomWishRepository(
    private val dao: WishDao
) : WishRepository {

    override fun observeWishes(): Flow<List<Wish>> =
        dao.observeAll().map { wishes -> wishes.map { it.toDomain() } }

    override suspend fun upsert(wish: Wish) {
        dao.upsert(wish.toEntity())
        LocalChangeSignal.notifyLocalWrite()
    }

    override suspend fun delete(id: String) {
        dao.softDelete(id = id, now = System.currentTimeMillis())
        LocalChangeSignal.notifyLocalWrite()
    }
}

private fun WishEntity.toDomain() = Wish(
    id = id,
    pairId = pairId,
    title = title,
    note = note,
    priceEstimate = priceEstimate,
    status = runCatching { WishStatus.valueOf(status) }.getOrDefault(WishStatus.WANTED),
    updatedAt = updatedAt
)

private fun Wish.toEntity() = WishEntity(
    id = id,
    pairId = pairId,
    title = title,
    note = note,
    priceEstimate = priceEstimate,
    status = status.name,
    updatedAt = updatedAt
)
