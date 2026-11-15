package app.vdvoem

import android.app.Application
import app.vdvoem.core.data.di.dataModule
import app.vdvoem.core.sync.SyncController
import app.vdvoem.core.sync.di.syncModule
import app.vdvoem.di.appKoinModule
import app.vdvoem.feature.calendar.calendarFeatureModule
import app.vdvoem.feature.gifts.giftsFeatureModule
import app.vdvoem.feature.quizzes.quizzesFeatureModule
import app.vdvoem.feature.settings.settingsFeatureModule
import app.vdvoem.feature.store.storeFeatureModule
import app.vdvoem.feature.wishes.wishesFeatureModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class VdvoemApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.ERROR else Level.NONE)
            androidContext(this@VdvoemApplication)
            modules(
                dataModule,
                syncModule,
                appKoinModule,
                calendarFeatureModule,
                wishesFeatureModule,
                giftsFeatureModule,
                settingsFeatureModule,
                quizzesFeatureModule,
                storeFeatureModule
            )
        }

        // Первый синк сразу после старта: local-first, сеть может быть недоступна — WorkManager повторит.
        runCatching {
            GlobalContext.get().get<SyncController>().syncInBackground()
        }
    }
}
