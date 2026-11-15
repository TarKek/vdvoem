package app.vdvoem.ui.pair

import app.vdvoem.core.domain.time.Dates

/**
 * Состояние экрана подключения пары.
 *
 * UI ничего не знает про сеть: адрес, код и текст ошибки приходят из ViewModel,
 * а действия — обычные колбэки. Так экран остаётся чистым и предсказуемым.
 */
sealed interface ConnectState {

    /** Ввод адреса сервера и кода приглашения. */
    data class Form(
        val serverUrl: String = "",
        val inviteCode: String = "",
        val nameA: String = "",
        val nameB: String = "",
        val sinceMillis: Long = Dates.startOfDay(Dates.today()),
        val busy: Boolean = false,
        val error: String? = null
    ) : ConnectState

    /** Пара создана: показываем код, который нужно передать партнёру. */
    data class Created(
        val pairId: String,
        val inviteCode: String
    ) : ConnectState

    /**
     * Пара уже подключена: дальше настраивать нечего, но нужен вход в партнёра по коду,
     * ручная синхронизация и выход из пары.
     */
    data class Connected(
        val serverUrl: String,
        val pairId: String,
        val inviteCode: String,
        val nameA: String = "",
        val nameB: String = "",
        val sinceMillis: Long = 0L,
        val syncing: Boolean = false,
        val lastSyncMillis: Long = 0L,
        val error: String? = null
    ) : ConnectState

    companion object {
        /** Длина кода приглашения. */
        const val INVITE_CODE_LENGTH: Int = 8

        /** Код всегда из 8 заглавных латинских букв и цифр. */
        fun normalizeInviteCode(raw: String): String = raw
            .uppercase()
            .filter { it in 'A'..'Z' || it in '0'..'9' }
            .take(INVITE_CODE_LENGTH)
    }
}