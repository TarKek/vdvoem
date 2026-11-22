package app.vdvoem.feature.diary

import app.vdvoem.core.domain.repository.DiaryReminderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Держит уведомления дневника актуальными без открытого экрана: правки приходят
 * из синхронизации, а задачи WorkManager должны следовать за ними.
 */
class DiaryReminderObserver(
    private val repository: DiaryReminderRepository,
    private val scheduler: DiaryReminderScheduler
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        scope.launch {
            repository.observeRemindersIncludingDeleted().collect { reminders ->
                scheduler.sync(reminders)
            }
        }
    }
}