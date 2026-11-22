package app.vdvoem.di

import app.vdvoem.core.domain.module.ModuleRegistry
import app.vdvoem.core.sync.IncomingPokeHandler
import app.vdvoem.feature.calendar.CalendarModule
import app.vdvoem.feature.calendar.CalendarViewModel
import app.vdvoem.feature.gifts.GiftsViewModel
import app.vdvoem.feature.quizzes.QuizzesModule
import app.vdvoem.feature.settings.SettingsModule
import app.vdvoem.feature.settings.SettingsViewModel
import app.vdvoem.feature.store.StoreModule
import app.vdvoem.feature.timetable.TimetableModule
import app.vdvoem.feature.tasks.TasksModule
import app.vdvoem.feature.notes.NotesModule
import app.vdvoem.feature.cycle.CycleModule
import app.vdvoem.feature.dates.DatesModule
import app.vdvoem.feature.voice.VoiceModule
import app.vdvoem.feature.wishes.WishesViewModel
import app.vdvoem.data.HomePreferences
import app.vdvoem.notify.PokeNotifier
import app.vdvoem.ui.ModulesViewModel
import app.vdvoem.ui.dashboard.DashboardViewModel
import app.vdvoem.ui.presents.PresentsModule
import app.vdvoem.ui.presents.PresentsTabController
import app.vdvoem.ui.pair.ConnectPairViewModel
import app.vdvoem.ui.sync.BackupViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.android.ext.koin.androidContext

/**
 * Точка сборки приложения: здесь перечислены все модули, которые попадут в реестр.
 * Список явный, чтобы не зависеть от порядка разрешения DI и сразу видеть состав APK.
 */
val appKoinModule = module {
    single { PresentsModule() }
    single { PresentsTabController() }
    single {
        ModuleRegistry(
            modules = listOf(
                get<CalendarModule>(),
                get<PresentsModule>(),
                get<SettingsModule>(),
                get<QuizzesModule>(),
                get<TimetableModule>(),
                get<TasksModule>(),
                get<NotesModule>(),
                get<CycleModule>(),
                get<DatesModule>(),
                get<VoiceModule>(),
                get<StoreModule>()
            )
        )
    }
    viewModel { ModulesViewModel(registry = get(), repository = get()) }
    viewModel {
        DashboardViewModel(
            pairRepository = get(),
            calendarRepository = get(),
            wishRepository = get(),
            giftRepository = get(),
            pokeRepository = get(),
            cycleRepository = get(),
            lessonRepository = get(),
            reminderLeadSettings = get(),
            deviceIdentity = get()
        )
    }
    viewModel { CalendarViewModel(calendarRepository = get(), pairRepository = get()) }
    viewModel { WishesViewModel(wishRepository = get(), pairRepository = get(), wishGiftConverter = get(), deviceIdentity = get()) }
    viewModel { GiftsViewModel(giftRepository = get(), pairRepository = get()) }
    viewModel { SettingsViewModel(repository = get()) }
    single { HomePreferences(context = androidContext()) }
    single<IncomingPokeHandler> { PokeNotifier(context = androidContext()) }
    viewModel { ConnectPairViewModel(controller = get(), pairRepository = get()) }
    viewModel {
        BackupViewModel(
            pairRepository = get(),
            calendarRepository = get(),
            wishRepository = get(),
            giftRepository = get(),
            pokeRepository = get(),
            quizScoreRepository = get(),
            lessonRepository = get(),
            taskRepository = get(),
            cycleRepository = get(),
            deviceIdentity = get(),
            database = get()
        )
    }
}
