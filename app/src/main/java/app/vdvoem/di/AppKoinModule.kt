package app.vdvoem.di

import app.vdvoem.core.domain.module.AppModule
import app.vdvoem.core.domain.module.ModuleRegistry
import app.vdvoem.feature.calendar.CalendarViewModel
import app.vdvoem.ui.ModulesViewModel
import app.vdvoem.ui.dashboard.DashboardViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appKoinModule = module {
    single { ModuleRegistry(getAll<AppModule>()) }
    viewModel { ModulesViewModel(registry = get(), repository = get()) }
    viewModel { DashboardViewModel(pairRepository = get(), calendarRepository = get()) }
    viewModel { CalendarViewModel(calendarRepository = get(), pairRepository = get()) }
}
