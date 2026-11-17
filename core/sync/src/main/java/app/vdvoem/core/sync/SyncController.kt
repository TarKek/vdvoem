package app.vdvoem.core.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Фасад для UI: настройки, вход в пару, разовая синхронизация.
 * Экрану не нужно знать ни про Ktor, ни про WorkManager.
 */
class SyncController(
    private val settingsStore: SyncSettingsStore,
    private val api: SyncApi,
    private val engine: SyncEngine,
    private val scheduler: SyncScheduler,
    private val statusHolder: SyncStatusHolder,
    private val pendingChanges: PendingChanges
) {

    val settings: Flow<SyncSettings> = settingsStore.settings

    val status: Flow<SyncStatus> = statusHolder.state

    /** «Не отправлено N»: локальные правки, которые ещё не уехали на сервер. */
    val pendingCount: StateFlow<Int> = pendingChanges.pending

    /** Пересчитать число неотправленных записей (вызывается вручную, если нужно сразу). */
    suspend fun refreshPending(): Int = pendingChanges.refresh()

    suspend fun currentSettings(): SyncSettings = settingsStore.current()

    suspend fun setServerUrl(url: String) {
        settingsStore.setServerUrl(url)
        if (settingsStore.current().isConfigured) scheduler.schedulePeriodic()
    }

    suspend fun setEnabled(enabled: Boolean) {
        settingsStore.setEnabled(enabled)
        if (enabled && settingsStore.current().isConfigured) {
            scheduler.schedulePeriodic()
            scheduler.syncNow()
        } else if (!enabled) {
            scheduler.cancelPeriodic()
        }
    }

    /** Создать пару: сохраняет токен и включает фоновую синхронизацию. */
    suspend fun createPair(
        nameA: String,
        nameB: String,
        sinceMillis: Long = 0L,
        birthdayA: Long? = null,
        birthdayB: Long? = null
    ): SyncResult {
        val credentials = try {
            api.createPair(
                nameA = nameA,
                nameB = nameB,
                sinceMillis = sinceMillis,
                birthdayA = birthdayA,
                birthdayB = birthdayB
            )
        } catch (error: Exception) {
            return SyncResult.Error(error.message ?: "Не удалось создать пару", error)
        }
        engine.awaitIdle()
        settingsStore.saveCredentials(
            pairId = credentials.pairId,
            deviceToken = credentials.deviceToken,
            inviteCode = credentials.inviteCode
        )
        scheduler.schedulePeriodic()
        return engine.syncOnce()
    }

    /** Вступить в пару по коду приглашения. */
    suspend fun joinPair(code: String): SyncResult {
        val credentials = try {
            api.joinPair(code)
        } catch (error: Exception) {
            return SyncResult.Error(error.message ?: "Не удалось вступить в пару", error)
        }
        engine.awaitIdle()
        settingsStore.saveCredentials(
            pairId = credentials.pairId,
            deviceToken = credentials.deviceToken,
            inviteCode = credentials.inviteCode
        )
        scheduler.schedulePeriodic()
        return engine.syncOnce()
    }

    /** Выход из пары на этом устройстве: задачи сняты, пара забыта, адрес сервера остаётся. */
    suspend fun leavePair() {
        scheduler.cancelPeriodic()
        engine.awaitIdle()
        val credentials = settingsStore.current()
        try {
            if (credentials.isConfigured) api.leavePair(credentials)
        } finally {
            engine.awaitIdle()
            settingsStore.clearCredentials()
        }
    }

    /** Запустить синхронизацию в фоне (не дожидаясь результата). */
    fun syncInBackground() {
        scheduler.syncNow()
    }

    /** Синхронизировать прямо сейчас и дождаться итога. */
    suspend fun syncNow(): SyncResult {
        val result = engine.syncOnce()
        pendingChanges.refresh()
        return result
    }
}