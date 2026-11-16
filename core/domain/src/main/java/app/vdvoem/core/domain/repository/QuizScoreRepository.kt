package app.vdvoem.core.domain.repository

import app.vdvoem.core.domain.model.QuizScore
import kotlinx.coroutines.flow.Flow

interface QuizScoreRepository {
    /** Все результаты пары, свежие сверху. */
    fun observe(): Flow<List<QuizScore>>

    suspend fun save(score: QuizScore)
}