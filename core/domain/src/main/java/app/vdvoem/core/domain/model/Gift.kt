package app.vdvoem.core.domain.model

enum class GiftStatus { IDEA, BOUGHT, GIVEN }

/** Подарок: идея, покупка, вручение. Бюджет в рублях, без копеек. */
data class Gift(
    val id: String,
    val pairId: String,
    val title: String,
    val note: String = "",
    val budget: Long? = null,
    val status: GiftStatus = GiftStatus.IDEA,
    val updatedAt: Long = System.currentTimeMillis()
)
