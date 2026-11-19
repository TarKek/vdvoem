package app.vdvoem

import android.app.Application
import app.vdvoem.core.data.di.dataModule
import app.vdvoem.core.domain.repository.CycleRepository
import app.vdvoem.core.sync.SyncController
import app.vdvoem.core.sync.SyncSettingsStore
import app.vdvoem.core.sync.di.syncModule
import app.vdvoem.di.appKoinModule
import app.vdvoem.notify.PokeNotifier
import app.vdvoem.push.PushService
import app.vdvoem.widget.WidgetRefresher
import app.vdvoem.feature.calendar.calendarFeatureModule
import app.vdvoem.feature.calendar.CalendarReminderObserver
import app.vdvoem.feature.gifts.giftsFeatureModule
import app.vdvoem.feature.quizzes.quizzesFeatureModule
import app.vdvoem.feature.settings.settingsFeatureModule
import app.vdvoem.feature.store.storeFeatureModule
import app.vdvoem.feature.timetable.timetableFeatureModule
import app.vdvoem.feature.notes.notesFeatureModule
import app.vdvoem.feature.cycle.cycleFeatureModule
import app.vdvoem.feature.dates.datesFeatureModule
import app.vdvoem.feature.voice.voiceFeatureModule
import app.vdvoem.feature.wishes.wishesFeatureModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

class VdvoemApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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
                timetableFeatureModule,
                notesFeatureModule,
                cycleFeatureModule,
                datesFeatureModule,
                voiceFeatureModule,
                storeFeatureModule
            )
        }

        // Канал сердечек создаём заранее: уведомление может прийти из фонового воркера.
        runCatching { PokeNotifier.ensureChannel(this) }

        val koin = GlobalContext.get()
        koin.get<CalendarReminderObserver>().start()
        applicationScope.launch {
            val cycle = koin.get<CycleRepository>()
            merge(cycle.observeDays(), cycle.observeSettings()).collect {
                WidgetRefresher.requestRefresh(this@VdvoemApplication)
            }
        }

        // Запуск после create/join/enable, остановка после leave/disable — без reopen Activity.
        applicationScope.launch {
            koin.get<SyncSettingsStore>().settings
                .map { it.isConfigured && it.enabled }
                .distinctUntilChanged()
                .collect { active ->
                    runCatching { if (active) PushService.start(this@VdvoemApplication) else PushService.stop(this@VdvoemApplication) }
                }
        }

        // Первый синк сразу после старта: local-first, сеть может быть недоступна — WorkManager повторит.
        runCatching {
            GlobalContext.get().get<SyncController>().syncInBackground()
        }
    }
}
