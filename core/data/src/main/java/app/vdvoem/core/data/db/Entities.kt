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
    @ColumnInfo(name = "wish_id") val wishId: String? = null,
    @ColumnInfo(defaultValue = "0") val surprise: Boolean = false,
    @ColumnInfo(name = "owner_device_id", defaultValue = "''") val ownerDeviceId: String = "",
    @ColumnInfo(name = "bought_at") val boughtAt: Long? = null,
    @ColumnInfo(name = "given_at") val givenAt: Long? = null
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

/**
 * День трекера цикла. `symptoms` — список ключей через `|` (см. [CycleConverters]),
 * `flow` — имя [app.vdvoem.core.domain.model.Flow].
 *
 * Дефолты обязаны совпадать с `MIGRATION_7_8`: Room сверяет схему при открытии базы.
 */
@Entity(tableName = "cycle_days")
data class CycleDayEntity(
    @PrimaryKey val id: String,
    val pairId: String,
    val dateMillis: Long,
    @ColumnInfo(defaultValue = "0") val period: Boolean = false,
    @ColumnInfo(defaultValue = "NONE") val flow: String = "NONE",
    @ColumnInfo(defaultValue = "''") val symptoms: List<String> = emptyList(),
    @ColumnInfo(defaultValue = "''") val mood: String = "",
    val weight: Float? = null,
    @ColumnInfo(defaultValue = "''") val note: String = "",
    val updatedAt: Long,
    val deletedAt: Long? = null
)

/** Ожидания пары по циклу: одна строка на пару, `id` = `pairId`. */
@Entity(tableName = "cycle_settings")
data class CycleSettingsEntity(
    @PrimaryKey val id: String,
    val pairId: String,
    @ColumnInfo(defaultValue = "28") val cycleLength: Int = 28,
    @ColumnInfo(defaultValue = "5") val periodLength: Int = 5,
    val updatedAt: Long,
    val deletedAt: Long? = null
)
