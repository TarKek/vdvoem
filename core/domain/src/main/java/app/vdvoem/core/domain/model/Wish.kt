package app.vdvoem.core.domain.model

enum class WishStatus { WANTED, RESERVED, DONE }

/** Желание из общего списка пары. */
data class Wish(
    val id: String,
    val pairId: String,
    val title: String,
    val note: String = "",
    val priceEstimate: Long? = null,
    val status: WishStatus = WishStatus.WANTED,
    val updatedAt: Long = System.currentTimeMillis()
)
