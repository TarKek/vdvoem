package app.vdvoem.core.domain.repository

/** Атомарное преобразование желания в идею подарка. */
interface WishGiftConverter {
    suspend fun convert(wishId: String)
}
