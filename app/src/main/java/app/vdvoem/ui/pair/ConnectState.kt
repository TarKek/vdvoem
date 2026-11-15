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
        val busy: Boolean = false,
        val error: String? = null,
        /** Кто ты и как зовут партнёра — уезжает на сервер при создании пары. */
        val nameA: String = "",
        val nameB: String = "",
        /** Дата начала отношений; 0 — «сегодня», подставляется при создании. */
        val sinceMillis: Long = Dates.startOfDay(Dates.today())
    ) : ConnectState

    /** Пара создана: показываем код, который нужно передать партнёру. */
    data class Created(
        val pairId: String,
        val inviteCode: String
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