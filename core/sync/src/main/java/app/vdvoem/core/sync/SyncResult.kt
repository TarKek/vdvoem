package app.vdvoem.core.sync

/** Итог одного прохода синхронизации. */
sealed interface SyncResult {

    /** [pulled] — сколько сущностей пришло с сервера, [applied] — сколько записали локально. */
    data class Success(
        val pulled: Int,
        val applied: Int,
        val serverTime: Long
    ) : SyncResult

    /** Нет адреса сервера или токена устройства — сеть не трогаем. */
    data object NotConfigured : SyncResult

    data class Error(val message: String, val cause: Throwable? = null) : SyncResult
}