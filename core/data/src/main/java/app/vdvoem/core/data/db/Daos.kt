package app.vdvoem.core.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PairDao {
    @Query("SELECT * FROM pair WHERE deletedAt IS NULL ORDER BY updatedAt DESC LIMIT 1")
    fun observe(): Flow<PairEntity?>

    @Query("SELECT * FROM pair WHERE deletedAt IS NULL ORDER BY updatedAt DESC LIMIT 1")
    suspend fun current(): PairEntity?

    @Query("DELETE FROM pair WHERE id <> :id")
    suspend fun deleteOthers(id: String)

    @Upsert
    suspend fun upsert(pair: PairEntity)

    @Transaction
    suspend fun replace(pair: PairEntity) {
        deleteOthers(pair.id)
        upsert(pair)
    }
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE deletedAt IS NULL AND pairId = (SELECT id FROM pair WHERE deletedAt IS NULL ORDER BY updatedAt DESC LIMIT 1) ORDER BY dateMillis ASC")
    fun observeAll(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events ORDER BY dateMillis ASC")
    fun observeAllIncludingDeleted(): Flow<List<EventEntity>>

    @Upsert
    suspend fun upsert(event: EventEntity)

    @Query("UPDATE events SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)
}

@Dao
interface WishDao {
    @Query("SELECT * FROM wishes WHERE deletedAt IS NULL AND pairId = (SELECT id FROM pair WHERE deletedAt IS NULL ORDER BY updatedAt DESC LIMIT 1) ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<WishEntity>>

    /** Включая tombstone: синку нужны удаления, интерфейсу — нет. */
    @Query("SELECT * FROM wishes ORDER BY updatedAt DESC")
    fun observeAllIncludingDeleted(): Flow<List<WishEntity>>

    @Upsert
    suspend fun upsert(wish: WishEntity)

    @Query("UPDATE wishes SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)
}

@Dao
interface GiftDao {
    @Query("SELECT * FROM gifts WHERE deletedAt IS NULL AND pairId = (SELECT id FROM pair WHERE deletedAt IS NULL ORDER BY updatedAt DESC LIMIT 1) ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<GiftEntity>>

    /** Включая tombstone: синку нужны удаления, интерфейсу — нет. */
    @Query("SELECT * FROM gifts ORDER BY updatedAt DESC")
    fun observeAllIncludingDeleted(): Flow<List<GiftEntity>>

    @Upsert
    suspend fun upsert(gift: GiftEntity)

    @Query("UPDATE gifts SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)
}

@Dao
interface PokeDao {
    @Query("SELECT * FROM pokes WHERE deletedAt IS NULL AND pairId = (SELECT id FROM pair WHERE deletedAt IS NULL ORDER BY updatedAt DESC LIMIT 1) ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PokeEntity>>

    @Query("SELECT * FROM pokes ORDER BY createdAt DESC")
    fun observeAllIncludingDeleted(): Flow<List<PokeEntity>>

    @Upsert
    suspend fun upsert(poke: PokeEntity)
}

@Dao
interface QuizScoreDao {
    @Query("SELECT * FROM quiz_scores WHERE deletedAt IS NULL AND pairId = (SELECT id FROM pair WHERE deletedAt IS NULL ORDER BY updatedAt DESC LIMIT 1) ORDER BY passedAt DESC")
    fun observeAll(): Flow<List<QuizScoreEntity>>

    @Query("SELECT * FROM quiz_scores ORDER BY passedAt DESC")
    fun observeAllIncludingDeleted(): Flow<List<QuizScoreEntity>>

    @Upsert
    suspend fun upsert(score: QuizScoreEntity)
}

@Dao
interface LessonDao {
    @Query("SELECT * FROM lessons WHERE deletedAt IS NULL AND pairId = (SELECT id FROM pair WHERE deletedAt IS NULL ORDER BY updatedAt DESC LIMIT 1) ORDER BY weekday, startMinute")
    fun observeAll(): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons ORDER BY weekday, startMinute")
    fun observeAllIncludingDeleted(): Flow<List<LessonEntity>>

    @Upsert
    suspend fun upsert(lesson: LessonEntity)

    @Query("UPDATE lessons SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)
}

@Dao
interface TaskDao {
    @Query(
        "SELECT * FROM tasks WHERE deletedAt IS NULL " +
            "AND pairId = (SELECT id FROM pair WHERE deletedAt IS NULL ORDER BY updatedAt DESC LIMIT 1) " +
            "ORDER BY isCompleted ASC, CASE WHEN dueAt IS NULL THEN 1 ELSE 0 END, dueAt ASC, updatedAt DESC"
    )
    fun observeAll(): Flow<List<TaskEntity>>

    /** Включая tombstone: синку нужны удаления, экрану — нет. */
    @Query("SELECT * FROM tasks ORDER BY updatedAt DESC")
    fun observeAllIncludingDeleted(): Flow<List<TaskEntity>>

    @Upsert
    suspend fun upsert(task: TaskEntity)

    @Query("UPDATE tasks SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)
}

@Dao
interface CycleDayDao {
    @Query("SELECT * FROM cycle_days WHERE deletedAt IS NULL AND pairId = (SELECT id FROM pair WHERE deletedAt IS NULL ORDER BY updatedAt DESC LIMIT 1) ORDER BY dateMillis ASC")
    fun observeAll(): Flow<List<CycleDayEntity>>

    /** Включая tombstone: синку нужны удаления, экрану — нет. */
    @Query("SELECT * FROM cycle_days ORDER BY dateMillis ASC")
    fun observeAllIncludingDeleted(): Flow<List<CycleDayEntity>>

    @Upsert
    suspend fun upsert(day: CycleDayEntity)

    @Query("UPDATE cycle_days SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)
}

@Dao
interface CycleSettingsDao {
    @Query("SELECT * FROM cycle_settings WHERE deletedAt IS NULL AND pairId = (SELECT id FROM pair WHERE deletedAt IS NULL ORDER BY updatedAt DESC LIMIT 1) ORDER BY updatedAt DESC LIMIT 1")
    fun observe(): Flow<CycleSettingsEntity?>

    /** Включая tombstone: синку нужны удаления, экрану — нет. */
    @Query("SELECT * FROM cycle_settings ORDER BY updatedAt DESC")
    fun observeAllIncludingDeleted(): Flow<List<CycleSettingsEntity>>

    @Upsert
    suspend fun upsert(settings: CycleSettingsEntity)
}

@Dao
interface DiaryDao {
    @Query("SELECT * FROM diary_entries WHERE deletedAt IS NULL AND pairId = (SELECT id FROM pair WHERE deletedAt IS NULL ORDER BY updatedAt DESC LIMIT 1) ORDER BY dateMillis DESC, updatedAt DESC")
    fun observeAll(): Flow<List<DiaryEntryEntity>>

    /** Включая tombstone: синку нужны удаления, экрану — нет. */
    @Query("SELECT * FROM diary_entries ORDER BY dateMillis DESC, updatedAt DESC")
    fun observeAllIncludingDeleted(): Flow<List<DiaryEntryEntity>>

    @Upsert
    suspend fun upsert(entry: DiaryEntryEntity)

    @Query("UPDATE diary_entries SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)
}

/**
 * Напоминания дневника.
 *
 * [observeAll] отдаёт только активные (не удалённые и не выполненные) текущей пары:
 * выполненные исчезают с экрана. Синку и копии нужен [observeAllIncludingDeleted].
 */
@Dao
interface DiaryReminderDao {
    @Query("SELECT * FROM diary_reminders WHERE deletedAt IS NULL AND doneAt IS NULL AND pairId = (SELECT id FROM pair WHERE deletedAt IS NULL ORDER BY updatedAt DESC LIMIT 1) ORDER BY remindAt ASC")
    fun observeAll(): Flow<List<DiaryReminderEntity>>

    /** Включая выполненные и tombstone: синку и резервной копии нужны все строки. */
    @Query("SELECT * FROM diary_reminders ORDER BY remindAt ASC")
    fun observeAllIncludingDeleted(): Flow<List<DiaryReminderEntity>>

    @Upsert
    suspend fun upsert(reminder: DiaryReminderEntity)

    @Query("UPDATE diary_reminders SET doneAt = :doneAt, updatedAt = :now WHERE id = :id")
    suspend fun setDone(id: String, doneAt: Long?, now: Long)

    @Query("UPDATE diary_reminders SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)
}
