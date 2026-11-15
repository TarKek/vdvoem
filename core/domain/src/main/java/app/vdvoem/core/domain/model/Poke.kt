package app.vdvoem.core.domain.model

/**
 * «Сердечко» — короткое сообщение партнёру, которое живёт в общем наборе данных пары.
 *
 * Синхронизируется как остальные сущности (UUID, pairId, updatedAt, tombstone),
 * поэтому доходит до второго телефона и переживает переустановку.
 */
data class Poke(
    val id: String,
    val pairId: String = "",
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val deletedAt: Long? = null
)