package app.vdvoem.core.domain.settings

import kotlinx.coroutines.flow.Flow

/**
 * Срок, за который показывать будущие события на «Главной».
 *
 * Ядро знает только про число дней, а откуда оно берётся (настройки напоминаний
 * календаря) — дело фичи. Так «Главная» не зависит от модуля календаря.
 */
interface ReminderLeadSettings {

    /** Сколько дней вперёд показывать события: 0 — только сегодняшние. */
    val leadDays: Flow<Int>

    companion object {
        /** Если настройки недоступны, показываем неделю — как по умолчанию в календаре. */
        const val DEFAULT_LEAD_DAYS = 7
    }
}