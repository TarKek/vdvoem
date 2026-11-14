package app.vdvoem.core.domain.repository

import app.vdvoem.core.domain.model.Pair
import kotlinx.coroutines.flow.Flow

interface PairRepository {
    fun observe(): Flow<Pair?>
    suspend fun save(pair: Pair)
    suspend fun current(): Pair?
}
