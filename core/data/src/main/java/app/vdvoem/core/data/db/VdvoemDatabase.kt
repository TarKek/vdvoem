package app.vdvoem.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PairEntity::class,
        EventEntity::class,
        WishEntity::class,
        GiftEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class VdvoemDatabase : RoomDatabase() {
    abstract fun pairDao(): PairDao
    abstract fun eventDao(): EventDao
    abstract fun wishDao(): WishDao
    abstract fun giftDao(): GiftDao

    companion object {
        const val NAME = "vdvoem.db"

        /** v1 → v2: желания и подарки. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `wishes` (
                        `id` TEXT NOT NULL,
                        `pairId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `priceEstimate` INTEGER,
                        `status` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `deletedAt` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `gifts` (
                        `id` TEXT NOT NULL,
                        `pairId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `budget` INTEGER,
                        `status` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `deletedAt` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        /** v2 → v3: дни рождения партнёров. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `pair` ADD COLUMN `birthdayA` INTEGER")
                db.execSQL("ALTER TABLE `pair` ADD COLUMN `birthdayB` INTEGER")
            }
        }
    }
}
