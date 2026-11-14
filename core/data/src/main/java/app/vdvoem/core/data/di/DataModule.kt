package app.vdvoem.core.data.di

import app.vdvoem.core.data.settings.DataStoreModuleSettingsRepository
import app.vdvoem.core.domain.module.ModuleRegistry
import app.vdvoem.core.domain.repository.ModuleSettingsRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single<ModuleSettingsRepository> {
        DataStoreModuleSettingsRepository(
            context = androidContext(),
            defaultEnabledIds = get<ModuleRegistry>().defaultEnabledIds
        )
    }
}
