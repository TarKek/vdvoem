package app.vdvoem.core.sync.di

import app.vdvoem.core.domain.device.DeviceIdentity
import app.vdvoem.core.sync.IncomingPokeHandler
import app.vdvoem.core.sync.RealtimeSync
import app.vdvoem.core.sync.SyncApi
import app.vdvoem.core.sync.SyncController
import app.vdvoem.core.sync.SyncDeviceIdentity
import app.vdvoem.core.sync.SyncEngine
import app.vdvoem.core.sync.SyncScheduler
import app.vdvoem.core.sync.SyncSettingsStore
import app.vdvoem.core.sync.SyncStatusHolder
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * DI-модуль синхронизации. Подключается в `app` рядом с `dataModule`:
 * `modules(dataModule, syncModule, ...)`.
 *
 * Worker берёт зависимости через KoinComponent, поэтому `workerFactory` настраивать не нужно.
 */
val syncModule: Module = module {

    single { SyncSettingsStore(androidContext()) }

    single<DeviceIdentity> { SyncDeviceIdentity(settingsStore = get()) }

    single { SyncApi(settings = get()) }

    single { SyncStatusHolder() }

    single {
        SyncEngine(
            settingsStore = get(),
            api = get(),
            pairRepository = get(),
            calendarRepository = get(),
            wishRepository = get(),
            giftRepository = get(),
            pokeRepository = get(),
            quizScoreRepository = get(),
            lessonRepository = get(),
            cycleRepository = get(),
            statusHolder = get(),
            pokeReceiver = { pokes -> get<IncomingPokeHandler>().onIncoming(pokes) }
        )
    }

    single { SyncScheduler(context = androidContext()) }

    single {
        SyncController(
            settingsStore = get(),
            api = get(),
            engine = get(),
            scheduler = get(),
            statusHolder = get()
        )
    }

    // «Живой» опрос, пока приложение на экране: активити дёргает start()/stop().
    single { RealtimeSync(controller = get()) }
}