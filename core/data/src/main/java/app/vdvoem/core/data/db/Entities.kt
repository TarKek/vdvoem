package app.vdvoem.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pair")
data class PairEntity(
    @PrimaryKey val id: String,
    val pairId: String,
    val nameA: String,
    val nameB: String,
    val sinceMillis: Long,
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
