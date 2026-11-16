package app.vdvoem.feature.notes.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Заметка. Живёт только в локальной базе модуля — без `pairId`, `deletedAt` и версии,
 * потому что личные заметки никогда не синхронизируются.
 */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val createdAt: Long,
    val updatedAt: Long
)