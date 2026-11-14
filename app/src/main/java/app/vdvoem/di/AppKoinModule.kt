package app.vdvoem.di

import app.vdvoem.core.domain.module.AppModule
import app.vdvoem.core.domain.module.ModuleRegistry
import app.vdvoem.ui.ModulesViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appKoinModule = module {
    single { ModuleRegistry(getAll<AppModule>()) }
    viewModel { ModulesViewModel(registry = get(), repository = get()) }
}
