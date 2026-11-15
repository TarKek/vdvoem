package app.vdvoem.core.sync

import app.vdvoem.core.domain.model.CalendarEvent
import app.vdvoem.core.domain.model.Gift
import app.vdvoem.core.domain.model.GiftStatus
import app.vdvoem.core.domain.model.Pair
import app.vdvoem.core.domain.model.Wish
import app.vdvoem.core.domain.model.WishStatus

/**
 * Единственное место, где домен превращается в DTO и обратно.
 * Новые синхронизируемые сущности добавляются здесь — движок и API не меняются.
 */
object SyncMappers {

    fun Pair.toDto(): PairDto = PairDto(
        id = id,
        nameA = nameA,
        nameB = nameB,
        sinceMillis = sinceMillis,
        birthdayA = birthdayA,
        birthdayB = birthdayB,
        updatedAt = updatedAt
    )

    fun PairDto.toDomain(): Pair = Pair(
        id = id,
        nameA = nameA,
        nameB = nameB,
        sinceMillis = sinceMillis,
        birthdayA = birthdayA,
        birthdayB = birthdayB,
        updatedAt = updatedAt
    )

    fun CalendarEvent.toDto(): EventDto = EventDto(
        id = id,
        title = title,
        dateMillis = dateMillis,
        annual = annual,
        updatedAt = updatedAt,
        pairId = pairId
    )

    fun EventDto.toDomain(fallbackPairId: String = ""): CalendarEvent = CalendarEvent(
        id = id,
        pairId = pairId?.takeIf { it.isNotBlank() } ?: fallbackPairId,
        title = title,
        dateMillis = dateMillis,
        annual = annual,
        updatedAt = updatedAt
    )

    fun Wish.toDto(): WishDto = WishDto(
        id = id,
        title = title,
        status = status.name,
        updatedAt = updatedAt,
        pairId = pairId,
        note = note,
        priceEstimate = priceEstimate
    )

    fun WishDto.toDomain(fallbackPairId: String = ""): Wish = Wish(
        id = id,
        pairId = pairId?.takeIf { it.isNotBlank() } ?: fallbackPairId,
        title = title,
        note = note,
        priceEstimate = priceEstimate,
        status = runCatching { WishStatus.valueOf(status) }.getOrDefault(WishStatus.WANTED),
        updatedAt = updatedAt
    )

    fun Gift.toDto(): GiftDto = GiftDto(
        id = id,
        title = title,
        status = status.name,
        updatedAt = updatedAt,
        pairId = pairId,
        note = note,
        budget = budget
    )

    fun GiftDto.toDomain(fallbackPairId: String = ""): Gift = Gift(
        id = id,
        pairId = pairId?.takeIf { it.isNotBlank() } ?: fallbackPairId,
        title = title,
        note = note,
        budget = budget,
        status = runCatching { GiftStatus.valueOf(status) }.getOrDefault(GiftStatus.IDEA),
        updatedAt = updatedAt
    )
}