package app.vdvoem.widget

import app.vdvoem.core.domain.model.CalendarEvent
import app.vdvoem.core.domain.model.Wish
import app.vdvoem.core.domain.model.WishStatus
import app.vdvoem.core.domain.time.Dates
import java.time.LocalDate

/** Сколько событий показываем в виджете календаря. */
internal const val CALENDAR_WIDGET_LIMIT = 3

/** Событие, приведённое к ближайшему наступлению (годовщины учитывают год). */
internal data class UpcomingEvent(
    val title: String,
    val date: LocalDate,
    val daysUntil: Long
)

/**
 * Ближайшие события пары: сегодня и позже, без удалённых.
 *
 * [extra] — даты, которых нет в базе (например, дни рождения из профиля пары):
 * их уже привели к ближайшему наступлению и подписали снаружи.
 *
 * Прошедшие не-годовщины не показываем: в виджете нужен «что впереди», а не архив.
 */
internal fun upcomingEvents(
    events: List<CalendarEvent>,
    today: LocalDate,
    extra: List<UpcomingEvent> = emptyList(),
    limit: Int = CALENDAR_WIDGET_LIMIT
): List<UpcomingEvent> {
    val fromEvents = events.asSequence()
        .filter { it.deletedAt == null }
        .map { event ->
            val date = Dates.nextOccurrence(event.dateMillis, event.annual, today)
            UpcomingEvent(title = event.title, date = date, daysUntil = Dates.daysUntil(date, today))
        }
    return (fromEvents + extra.asSequence())
        .filter { it.daysUntil >= 0L }
        .sortedWith(compareBy<UpcomingEvent> { it.daysUntil }.thenBy { it.title })
        .take(limit)
        .toList()
}

/**
 * «Желание дня»: одно желание, которое меняется раз в сутки.
 *
 * Порядок не зависит от порядка в базе — сортируем по id, а день выбирает индекс.
 * Чужие сюрпризы и исполненные не показываем: виджет виден на домашнем экране.
 */
internal fun wishOfDay(wishes: List<Wish>, today: LocalDate, myDeviceId: String = ""): Wish? {
    val visible = wishes.asSequence()
        .filter { it.deletedAt == null && it.status != WishStatus.DONE }
        .filterNot { wish ->
            wish.surprise && wish.ownerDeviceId.isNotEmpty() && wish.ownerDeviceId != myDeviceId
        }
        .sortedBy { it.id }
        .toList()
    if (visible.isEmpty()) return null
    val index = Math.floorMod(today.toEpochDay(), visible.size.toLong()).toInt()
    return visible[index]
}