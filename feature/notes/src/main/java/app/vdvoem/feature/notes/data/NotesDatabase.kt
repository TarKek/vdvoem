package app.vdvoem.feature.notes.data

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Отдельная база модуля заметок: один файл, одна таблица, никакой связи с `vdvoem.db`.
 * Так личные записи физически не попадают в общую схему и в бэкап пары.
 */
@Database(
    entities = [NoteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NotesDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        const val NAME = "vdvoem_notes.db"
    }
}