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
        GiftEntity::class,
        PokeEntity::class,
        QuizScoreEntity::class,
        LessonEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class VdvoemDatabase : RoomDatabase() {
    abstract fun pairDao(): PairDao
    abstract fun eventDao(): EventDao
    abstract fun wishDao(): WishDao
    abstract fun giftDao(): GiftDao
    abstract fun pokeDao(): PokeDao
    abstract fun quizScoreDao(): QuizScoreDao
    abstract fun lessonDao(): LessonDao

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

        /** v3 → v4: сердечки, которые теперь синхронизируются. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `pokes` (
                        `id` TEXT NOT NULL,
                        `pairId` TEXT NOT NULL,
                        `text` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `deletedAt` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        /** v4 → v5: результаты тестов, чтобы партнёр видел их тоже. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `quiz_scores` (
                        `id` TEXT NOT NULL,
                        `pairId` TEXT NOT NULL,
                        `quizId` TEXT NOT NULL,
                        `percent` INTEGER NOT NULL,
                        `passedAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `deletedAt` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        /** v5 → v6: расписание уроков. */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `lessons` (
                        `id` TEXT NOT NULL,
                        `pairId` TEXT NOT NULL,
                        `subject` TEXT NOT NULL,
                        `weekday` INTEGER NOT NULL,
                        `startMinute` INTEGER NOT NULL,
                        `endMinute` INTEGER NOT NULL,
                        `room` TEXT NOT NULL,
                        `teacher` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `deletedAt` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * v6 → v7: приоритет, ссылка и «сюрприз» у желаний, ссылка на желание у подарка.
         *
         * Дефолты в ALTER обязаны совпадать с `@ColumnInfo(defaultValue = …)` в сущностях,
         * иначе Room не примет миграцию при проверке схемы.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `wishes` ADD COLUMN `priority` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `wishes` ADD COLUMN `url` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `wishes` ADD COLUMN `surprise` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `wishes` ADD COLUMN `owner_device_id` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `gifts` ADD COLUMN `wish_id` TEXT")
            }
        }
    }
}
