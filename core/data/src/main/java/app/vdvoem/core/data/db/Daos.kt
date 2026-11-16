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

@Dao
interface WishDao {
    @Query("SELECT * FROM wishes WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<WishEntity>>

    @Upsert
    suspend fun upsert(wish: WishEntity)

    @Query("UPDATE wishes SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)
}

@Dao
interface GiftDao {
    @Query("SELECT * FROM gifts WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<GiftEntity>>

    @Upsert
    suspend fun upsert(gift: GiftEntity)

    @Query("UPDATE gifts SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)
}

@Dao
interface PokeDao {
    @Query("SELECT * FROM pokes WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PokeEntity>>

    @Upsert
    suspend fun upsert(poke: PokeEntity)
}

@Dao
interface QuizScoreDao {
    @Query("SELECT * FROM quiz_scores WHERE deletedAt IS NULL ORDER BY passedAt DESC")
    fun observeAll(): Flow<List<QuizScoreEntity>>

    @Upsert
    suspend fun upsert(score: QuizScoreEntity)
}

@Dao
interface LessonDao {
    @Query("SELECT * FROM lessons WHERE deletedAt IS NULL ORDER BY weekday, startMinute")
    fun observeAll(): Flow<List<LessonEntity>>

    @Upsert
    suspend fun upsert(lesson: LessonEntity)

    @Query("UPDATE lessons SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)
}
