package app.vdvoem.core.data.repository

import androidx.room.withTransaction
import app.vdvoem.core.data.db.VdvoemDatabase
import app.vdvoem.core.domain.model.Gift
import app.vdvoem.core.domain.model.GiftStatus
import app.vdvoem.core.domain.model.WishStatus
import app.vdvoem.core.domain.repository.GiftRepository
import app.vdvoem.core.domain.repository.PairRepository
import app.vdvoem.core.domain.repository.WishGiftConverter
import app.vdvoem.core.domain.repository.WishRepository
import kotlinx.coroutines.flow.first
import java.util.UUID

class RoomWishGiftConverter(
    private val database: VdvoemDatabase,
    private val wishRepository: WishRepository,
    private val giftRepository: GiftRepository,
    private val pairRepository: PairRepository
) : WishGiftConverter {
    override suspend fun convert(wishId: String) = database.withTransaction {
        val current = wishRepository.observeWishesIncludingDeleted().first()
            .firstOrNull { it.id == wishId && it.deletedAt == null } ?: return@withTransaction
        val existing = giftRepository.observeGiftsIncludingDeleted().first()
            .firstOrNull { it.wishId == wishId && it.deletedAt == null }
        val now = System.currentTimeMillis()
        if (existing == null) {
            giftRepository.upsert(
                Gift(
                    id = UUID.randomUUID().toString(),
                    pairId = current.pairId.ifBlank { pairRepository.current()?.id ?: "local" },
                    title = current.title,
                    note = "Из желаний",
                    budget = current.priceEstimate,
                    status = GiftStatus.IDEA,
                    updatedAt = now,
                    wishId = current.id,
                    surprise = current.surprise,
                    ownerDeviceId = current.ownerDeviceId
                )
            )
        }
        if (current.status != WishStatus.RESERVED) {
            wishRepository.upsert(current.copy(status = WishStatus.RESERVED, updatedAt = now))
        }
    }
}
