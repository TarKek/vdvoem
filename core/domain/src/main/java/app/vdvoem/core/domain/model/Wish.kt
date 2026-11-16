package app.vdvoem.core.domain.model

enum class WishStatus { WANTED, RESERVED, DONE }

/**
 * Желание из общего списка пары.
 *
 * [priority] — 0 обычный, 1 низкий, 2 высокий; [surprise] скрывает желание от партнёра,
 * поэтому [ownerDeviceId] запоминает устройство автора (иначе свой сюрприз тоже пропадёт).
 * [deletedAt] — tombstone: удаление должно уехать партнёру, а не исчезнуть молча.
 */
data class Wish(
    val id: String,
    val pairId: String,
    val title: String,
    val note: String = "",
    val priceEstimate: Long? = null,
    val status: WishStatus = WishStatus.WANTED,
    val updatedAt: Long = System.currentTimeMillis(),
    val priority: Int = PRIORITY_NORMAL,
    val url: String = "",
    val surprise: Boolean = false,
    val ownerDeviceId: String = "",
    val deletedAt: Long? = null
) {
    /** Приоритет в допустимых границах: старые копии могли прислать что угодно. */
    val safePriority: Int
        get() = priority.coerceIn(PRIORITY_NORMAL, PRIORITY_HIGH)

    companion object {
        /** Обычный приоритет — значение по умолчанию, в карточке не показывается. */
        const val PRIORITY_NORMAL = 0
        const val PRIORITY_LOW = 1
        const val PRIORITY_HIGH = 2
    }
}