package app.vdvoem.core.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Состояние синхронизации для UI (читается из [SyncStatusHolder.state]). */
sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Running : SyncStatus

    /** [serverTime] — серверное время последней успешной синхронизации. */
    data class Success(val serverTime: Long) : SyncStatus
    data class Error(val message: String) : SyncStatus
}

/** Держатель состояния: движок пишет, экран настроек читает. */
class SyncStatusHolder {

    private val _state = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val state: StateFlow<SyncStatus> = _state.asStateFlow()

    internal fun set(state: SyncStatus) {
        _state.value = state
    }
}