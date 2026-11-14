package app.vdvoem.core.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PairDao {
    @Query("SELECT * FROM pair WHERE deletedAt IS NULL LIMIT 1")
    fun observe(): Flow<PairEntity?>

    @Query("SELECT * FROM pair WHERE deletedAt IS NULL LIMIT 1")
    suspend fun current(): PairEntity?

    @Upsert
    suspend fun upsert(pair: PairEntity)
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE deletedAt IS NULL ORDER BY dateMillis ASC")
    fun observeAll(): Flow<List<EventEntity>>

    @Upsert
    suspend fun upsert(event: EventEntity)

    @Query("UPDATE events SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)
}
