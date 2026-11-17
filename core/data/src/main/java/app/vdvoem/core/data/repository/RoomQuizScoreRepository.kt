package app.vdvoem.core.data.repository

import app.vdvoem.core.data.LocalChangeSignal
import app.vdvoem.core.data.db.QuizScoreDao
import app.vdvoem.core.data.db.QuizScoreEntity
import app.vdvoem.core.domain.model.QuizScore
import app.vdvoem.core.domain.repository.QuizScoreRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomQuizScoreRepository(
    private val dao: QuizScoreDao
) : QuizScoreRepository {

    override fun observe(): Flow<List<QuizScore>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeIncludingDeleted(): Flow<List<QuizScore>> =
        dao.observeAllIncludingDeleted().map { list -> list.map { it.toDomain() } }

    override suspend fun save(score: QuizScore) {
        dao.upsert(score.toEntity())
        LocalChangeSignal.notifyLocalWrite()
    }
}

private fun QuizScoreEntity.toDomain() = QuizScore(
    id = id,
    pairId = pairId,
    quizId = quizId,
    percent = percent,
    passedAt = passedAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

private fun QuizScore.toEntity() = QuizScoreEntity(
    id = id,
    pairId = pairId,
    quizId = quizId,
    percent = percent,
    passedAt = passedAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)