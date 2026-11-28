package app.vdvoem.core.domain.model

/**
 * Запись общего дневника пары.
 *
 * Дата хранится как UTC-полночь — так же, как даты событий календаря: запись
 * относится к дню, а не к моменту времени. Запись видна обоим партнёрам.
 */
data class DiaryEntry(
    val id: String,
    val pairId: String,
    val dateMillis: Long,
    val mood: DiaryMood = DiaryMood.CALM,
    /** Необязательный заголовок: у короткой записи его может не быть. */
    val title: String = "",
    val body: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

/**
 * Настроение записи. В базе и в синхронизации хранится именем,
 * поэтому порядок значений менять нельзя — только добавлять новые в конец.
 */
enum class DiaryMood {
    JOY,
    CALM,
    NEUTRAL,
    SAD,
    TIRED
}