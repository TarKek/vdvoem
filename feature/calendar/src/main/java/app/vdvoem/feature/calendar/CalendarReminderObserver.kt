package app.vdvoem.feature.calendar

import app.vdvoem.core.domain.repository.CalendarRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Держит напоминания актуальными и после фоновой синхронизации, без открытого экрана. */
class CalendarReminderObserver(
    private val repository: CalendarRepository,
    private val preferences: ReminderPreferences,
    private val scheduler: ReminderScheduler
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        scope.launch {
            combine(repository.observeEvents(), preferences.settings) { events, settings ->
                events to settings
            }.collect { (events, settings) ->
                if (settings.enabled) scheduler.sync(events, settings) else scheduler.cancelAll()
            }
        }
    }
}
