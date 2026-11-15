package app.vdvoem.core.domain.repository

import app.vdvoem.core.domain.model.Poke
import kotlinx.coroutines.flow.Flow

interface PokeRepository {
    /** Все сердечки пары, новые сверху. */
    fun observe(): Flow<List<Poke>>

    suspend fun save(poke: Poke)
}