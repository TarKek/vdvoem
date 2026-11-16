package app.vdvoem.core.data.di

import androidx.room.Room
import app.vdvoem.core.data.db.VdvoemDatabase
import app.vdvoem.core.data.repository.RoomCalendarRepository
import app.vdvoem.core.data.repository.RoomGiftRepository
import app.vdvoem.core.data.repository.RoomPairRepository
import app.vdvoem.core.data.repository.RoomPokeRepository
import app.vdvoem.core.data.repository.RoomQuizScoreRepository
import app.vdvoem.core.data.repository.RoomLessonRepository
import app.vdvoem.core.data.repository.RoomWishRepository
import app.vdvoem.core.data.settings.DataStoreAppSettingsRepository
import app.vdvoem.core.data.settings.DataStoreModuleSettingsRepository
import app.vdvoem.core.domain.module.ModuleRegistry
import app.vdvoem.core.domain.repository.CalendarRepository
import app.vdvoem.core.domain.repository.GiftRepository
import app.vdvoem.core.domain.repository.LessonRepository
import app.vdvoem.core.domain.repository.ModuleSettingsRepository
import app.vdvoem.core.domain.repository.PairRepository
import app.vdvoem.core.domain.repository.PokeRepository
import app.vdvoem.core.domain.repository.QuizScoreRepository
import app.vdvoem.core.domain.repository.WishRepository
import app.vdvoem.core.domain.settings.AppSettingsRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single<ModuleSettingsRepository> {
        DataStoreModuleSettingsRepository(
            context = androidContext(),
            defaultEnabledIds = get<ModuleRegistry>().defaultEnabledIds
        )
    }

    single {
        Room.databaseBuilder(androidContext(), VdvoemDatabase::class.java, VdvoemDatabase.NAME)
            .addMigrations(
                VdvoemDatabase.MIGRATION_1_2,
                VdvoemDatabase.MIGRATION_2_3,
                VdvoemDatabase.MIGRATION_3_4,
                VdvoemDatabase.MIGRATION_4_5,
                VdvoemDatabase.MIGRATION_5_6,
                VdvoemDatabase.MIGRATION_6_7
            )
            .build()
    }
    single { get<VdvoemDatabase>().pairDao() }
    single { get<VdvoemDatabase>().eventDao() }
    single { get<VdvoemDatabase>().wishDao() }
    single { get<VdvoemDatabase>().giftDao() }
    single { get<VdvoemDatabase>().pokeDao() }
    single { get<VdvoemDatabase>().quizScoreDao() }
    single { get<VdvoemDatabase>().lessonDao() }

    single<PairRepository> { RoomPairRepository(get()) }
    single<CalendarRepository> { RoomCalendarRepository(get()) }
    single<WishRepository> { RoomWishRepository(get()) }
    single<GiftRepository> { RoomGiftRepository(get()) }
    single<PokeRepository> { RoomPokeRepository(get()) }
    single<QuizScoreRepository> { RoomQuizScoreRepository(get()) }
    single<LessonRepository> { RoomLessonRepository(get()) }
    single<AppSettingsRepository> { DataStoreAppSettingsRepository(androidContext()) }
}
