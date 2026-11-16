package app.vdvoem.core.domain.repository

import app.vdvoem.core.domain.model.Gift
import kotlinx.coroutines.flow.Flow

interface GiftRepository {
    fun observeGifts(): Flow<List<Gift>>

    /** Как [observeGifts], но с tombstone — для движка синка. */
    fun observeGiftsIncludingDeleted(): Flow<List<Gift>>

    suspend fun upsert(gift: Gift)
    suspend fun delete(id: String)
}