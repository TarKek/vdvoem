package app.vdvoem.core.domain.model

/**
 * Результат прохождения теста, которым делятся партнёры.
 *
 * Одно прохождение = одна запись: так видно и лучший результат, и динамику.
 * Как и остальные сущности, синхронизируется по `updatedAt`, удаление — `deletedAt`.
 */
data class QuizScore(
    val id: String,
    val pairId: String = "",
    val quizId: String,
    val percent: Int,
    val passedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = passedAt,
    val deletedAt: Long? = null
)