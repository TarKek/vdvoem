package app.vdvoem.ui.dashboard

import app.vdvoem.core.domain.model.CalendarEvent
import app.vdvoem.core.domain.model.Pair
import app.vdvoem.core.domain.time.Dates
import app.vdvoem.data.CountdownConfig
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Результат отсчёта. [days] — знаковая разница между сегодня и датой цели:
 * больше нуля — впереди, ноль — сегодня, меньше нуля — уже прошло.
 */
data class CountdownResult(
    val days: Long,
    val date: LocalDate,
    val eventTitle: String? = null
)

/**
 * Считает значение виджета-отсчёта по его настройкам.
 * Возвращает null, если цель не настроена или недоступна (например, нет пары).
 */
fun resolveCountdown(
    config: CountdownConfig,
    events: List<CalendarEvent>,
    pair: Pair?,
    today: LocalDate = Dates.today()
): CountdownResult? {
    val since = config.isSince
    return when (config.target) {
        CountdownConfig.TARGET_EVENT -> {
            val event = events.firstOrNull { it.id == config.eventId } ?: return null
            at(event.title, event.dateMillis, event.annual, since, today)
        }

        CountdownConfig.TARGET_PAIR -> {
            val sinceMillis = pair?.sinceMillis ?: return null
            if (since) {
                // «Сколько прошло вместе» — это весь срок пары, а не с последней годовщины.
                val start = Dates.toLocalDate(sinceMillis)
                if (start.isAfter(today)) return null
                CountdownResult(
                    days = ChronoUnit.DAYS.between(start, today),
                    date = start
                )
            } else {
                at(null, sinceMillis, annual = true, since = false, today = today)
            }
        }

        CountdownConfig.TARGET_BIRTHDAY_A -> {
            val birthday = pair?.birthdayA ?: return null
            at(null, birthday, annual = true, since = since, today = today)
        }

        CountdownConfig.TARGET_BIRTHDAY_B -> {
            val birthday = pair?.birthdayB ?: return null
            at(null, birthday, annual = true, since = since, today = today)
        }

        else -> nearest(events, since, today)
    }
}

private fun at(
    title: String?,
    epochMillis: Long,
    annual: Boolean,
    since: Boolean,
    today: LocalDate
): CountdownResult? {
    if (since && !annual) {
        val original = Dates.toLocalDate(epochMillis)
        if (original.isAfter(today)) return null
    }
    val date = if (since) {
        Dates.previousOccurrence(epochMillis, annual, today)
    } else {
        Dates.nextOccurrence(epochMillis, annual, today)
    }
    return CountdownResult(
        days = ChronoUnit.DAYS.between(today, date),
        date = date,
        eventTitle = title
    )
}

/**
 * «Ближайшее» событие: сначала ближайшее будущее, если такого нет — самое свежее
 * прошедшее, чтобы не показывать минус. Для режима «прошло» — наоборот, самое
 * свежее прошедшее.
 */
private fun nearest(
    events: List<CalendarEvent>,
    since: Boolean,
    today: LocalDate
): CountdownResult? {
    if (events.isEmpty()) return null
    val candidates = events.mapNotNull { event ->
        at(
            title = event.title,
            epochMillis = event.dateMillis,
            annual = event.annual,
            since = since,
            today = today
        )
    }
    return if (since) {
        candidates.filter { it.days <= 0 }.maxByOrNull { it.days }
    } else {
        candidates.filter { it.days >= 0 }.minByOrNull { it.days } ?: candidates.maxByOrNull { it.days }
    }
}