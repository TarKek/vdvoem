package app.vdvoem.di

import app.vdvoem.core.domain.module.ModuleRegistry
import app.vdvoem.feature.calendar.CalendarModule
import app.vdvoem.feature.calendar.CalendarViewModel
import app.vdvoem.feature.gifts.GiftsModule
import app.vdvoem.feature.store.StoreModule
import app.vdvoem.feature.wishes.WishesModule
import app.vdvoem.ui.ModulesViewModel
import app.vdvoem.ui.dashboard.DashboardViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Точка сборки приложения: здесь перечислены все модули, которые попадут в реестр.
 * Список явный, чтобы не зависеть от порядка разрешения DI и сразу видеть состав APK.
 */
val appKoinModule = module {
    single {
        ModuleRegistry(
            modules = listOf(
                get<CalendarModule>(),
                get<WishesModule>(),
                get<GiftsModule>(),
                get<StoreModule>()
            )
        )
    }
    viewModel { ModulesViewModel(registry = get(), repository = get()) }
    viewModel { DashboardViewModel(pairRepository = get(), calendarRepository = get()) }
    viewModel { CalendarViewModel(calendarRepository = get(), pairRepository = get()) }
}
