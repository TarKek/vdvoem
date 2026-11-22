package app.vdvoem.core.sync

import app.vdvoem.core.domain.model.Gift
import app.vdvoem.core.domain.model.GiftStatus
import app.vdvoem.core.domain.model.SharedTask
import app.vdvoem.core.domain.model.Wish
import app.vdvoem.core.domain.model.WishStatus
import app.vdvoem.core.sync.SyncMappers.toDomain
import app.vdvoem.core.sync.SyncMappers.toDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Мапперы «домен ↔ DTO»: потеря поля здесь — потеря данных у партнёра. */
class SyncMappersTest {

    @Test
    fun `wish survives a round trip with every field`() {
        val wish = Wish(
            id = "w1",
            pairId = "pair",
            title = "Наушники",
            note = "чёрные",
            priceEstimate = 12_000,
            status = WishStatus.RESERVED,
            updatedAt = 1_700_000_000_000,
            priority = Wish.PRIORITY_HIGH,
            url = "https://example.com/headphones",
            surprise = true,
            ownerDeviceId = "device-a",
            deletedAt = null
        )

        val back = wish.toDto().toDomain(fallbackPairId = "fallback")

        assertEquals(wish, back)
    }

    @Test
    fun `wish tombstone is transmitted and restored`() {
        val deleted = Wish(
            id = "w2",
            pairId = "pair",
            title = "Удалённое",
            updatedAt = 1_700_000_000_500,
            deletedAt = 1_700_000_000_500
        )

        val dto = deleted.toDto()
        assertEquals(1_700_000_000_500, dto.deletedAt)

        val back = dto.toDomain(fallbackPairId = "pair")
        assertEquals(1_700_000_000_500, back.deletedAt)
    }

    @Test
    fun `wish falls back to the pair id from settings when dto has none`() {
        val dto = WishDto(id = "w3", title = "Без пары")
        assertEquals("settings-pair", dto.toDomain(fallbackPairId = "settings-pair").pairId)
    }

    @Test
    fun `gift keeps the wish link and the tombstone`() {
        val gift = Gift(
            id = "g1",
            pairId = "pair",
            title = "Наушники",
            note = "на день рождения",
            budget = 9_000,
            status = GiftStatus.BOUGHT,
            updatedAt = 1_700_000_000_999,
            wishId = "w1",
            deletedAt = 1_700_000_001_000
        )

        val back = gift.toDto().toDomain(fallbackPairId = "pair")

        assertEquals(gift, back)
        assertEquals("w1", back.wishId)
        assertEquals(1_700_000_001_000, back.deletedAt)
    }

    @Test
    fun `gift without a wish links to nothing`() {
        val back = Gift(id = "g2", pairId = "pair", title = "Цветы")
            .toDto()
            .toDomain(fallbackPairId = "pair")
        assertNull(back.wishId)
        assertNull(back.deletedAt)
    }

    @Test
    fun `shared task keeps due date completion and tombstone`() {
        val task = SharedTask(
            id = "t1",
            pairId = "pair",
            title = "Купить продукты",
            note = "Сыр",
            dueAt = 1_762_086_400_000L,
            isCompleted = true,
            createdBy = "device-a",
            createdAt = 1_762_000_000_000L,
            updatedAt = 1_762_000_000_100L,
            deletedAt = 1_762_000_000_200L,
            version = 4L
        )

        val back = task.toDto().toDomain(fallbackPairId = "fallback")

        assertEquals(task, back)
    }

    @Test
    fun `task without a pair id uses the settings pair`() {
        val dto = TaskDto(id = "t2", title = "Позвонить")

        val back = dto.toDomain(fallbackPairId = "settings-pair")

        assertEquals("settings-pair", back.pairId)
        assertNull(back.dueAt)
        assertEquals(1L, back.version)
    }
}