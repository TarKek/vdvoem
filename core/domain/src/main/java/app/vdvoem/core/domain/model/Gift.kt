package app.vdvoem.core.domain.model

enum class GiftStatus { IDEA, BOUGHT, GIVEN }

/**
 * Подарок: идея, покупка, вручение. Бюджет в рублях, без копеек.
 *
 * [wishId] — ссылка на желание, из которого вырос подарок (вместо старой метки в заметке).
 * [deletedAt] — tombstone, чтобы удаление уезжало партнёру.
 */
data class Gift(
    val id: String,
    val pairId: String,
    val title: String,
    val note: String = "",
    val budget: Long? = null,
    val status: GiftStatus = GiftStatus.IDEA,
    val updatedAt: Long = System.currentTimeMillis(),
    val wishId: String? = null,
    val deletedAt: Long? = null
)