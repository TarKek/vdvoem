package app.vdvoem.core.domain.repository

import app.vdvoem.core.domain.model.Wish
import kotlinx.coroutines.flow.Flow

interface WishRepository {
    fun observeWishes(): Flow<List<Wish>>

    /**
     * Как [observeWishes], но с tombstone — движку синка удаления нужны,
     * иначе партнёр никогда не узнает, что желание стёрли.
     */
    fun observeWishesIncludingDeleted(): Flow<List<Wish>>

    suspend fun upsert(wish: Wish)
    suspend fun delete(id: String)
}