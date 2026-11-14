package app.vdvoem.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PairEntity::class, EventEntity::class],
    version = 1,
    exportSchema = false
)
abstract class VdvoemDatabase : RoomDatabase() {
    abstract fun pairDao(): PairDao
    abstract fun eventDao(): EventDao

    companion object {
        const val NAME = "vdvoem.db"
    }
}
