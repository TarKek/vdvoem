package app.vdvoem.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pair")
data class PairEntity(
    @PrimaryKey val id: String,
    val pairId: String,
    val nameA: String,
    val nameB: String,
    val sinceMillis: Long,
    val birthdayA: Long?,
    val birthdayB: Long?,
    val updatedAt: Long,
    val deletedAt: Long? = null
)

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val pairId: String,
    val title: String,
    val dateMillis: Long,
    val annual: Boolean,
    val updatedAt: Long,
    val deletedAt: Long? = null
)

@Entity(tableName = "wishes")
data class WishEntity(
    @PrimaryKey val id: String,
    val pairId: String,
    val title: String,
    val note: String,
    val priceEstimate: Long?,
    val status: String,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    /** 0 — обычный, 1 — низкий, 2 — высокий. */
    @ColumnInfo(defaultValue = "0") val priority: Int = 0,
    @ColumnInfo(defaultValue = "''") val url: String = "",
    @ColumnInfo(defaultValue = "0") val surprise: Boolean = false,
    @ColumnInfo(name = "owner_device_id", defaultValue = "''") val ownerDeviceId: String = ""
)

@Entity(tableName = "gifts")
data class GiftEntity(
    @PrimaryKey val id: String,
    val pairId: String,
    val title: String,
    val note: String,
    val budget: Long?,
    val status: String,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    /** Желание, из которого вырос подарок; null — подарок добавлен вручную. */
    @ColumnInfo(name = "wish_id") val wishId: String? = null
)

@Entity(tableName = "pokes")
data class PokeEntity(
    @PrimaryKey val id: String,
    val pairId: String,
    val text: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)

@Entity(tableName = "quiz_scores")
data class QuizScoreEntity(
    @PrimaryKey val id: String,
    val pairId: String,
    val quizId: String,
    val percent: Int,
    val passedAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)

@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey val id: String,
    val pairId: String,
    val subject: String,
    val weekday: Int,
    val startMinute: Int,
    val endMinute: Int,
    val room: String,
    val teacher: String,
    val updatedAt: Long,
    val deletedAt: Long? = null
)
