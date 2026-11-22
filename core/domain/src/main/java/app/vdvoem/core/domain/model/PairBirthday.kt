package app.vdvoem.core.domain.model

import app.vdvoem.core.domain.time.Dates
import java.time.LocalDate
import java.time.MonthDay

/**
 * День рождения из профиля пары как дата календаря.
 *
 * Отдельной строкой в базе не живёт: дата выводится из [Pair], поэтому правка
 * в профиле сразу видна в календаре и не расходится с ним после синхронизации.
 */
data class PairBirthday(
    val owner: Owner,
    /** Имя из профиля пары; подпись «День рождения» добавляет интерфейс. */
    val name: String,
    val dateMillis: Long
) {
    enum class Owner { ME, PARTNER }

    /**
     * Идентификатор для списков. Настоящие события используют UUID, поэтому
     * префикс исключает совпадение и позволяет отличить день рождения от события.
     */
    val eventId: String get() = "birthday:${owner.name.lowercase()}"
}

/** Дни рождения обоих партнёров; чего нет в профиле — того нет и в календаре. */
fun Pair.birthdays(): List<PairBirthday> = buildList {
    birthdayA?.let { add(PairBirthday(PairBirthday.Owner.ME, nameA, it)) }
    birthdayB?.let { add(PairBirthday(PairBirthday.Owner.PARTNER, nameB, it)) }
}

/**
 * Дни рождения без дублей с ручными ежегодными событиями.
 *
 * Пользователь мог завести день рождения в календаре до того, как профиль пары
 * научился показывать его сам. Сравниваем только месяц и день и только с
 * ежегодными неудалёнными событиями: разовые встречи в тот же день не должны
 * скрывать день рождения.
 */
fun Pair.birthdaysWithoutManualDuplicates(
    events: List<CalendarEvent>
): List<PairBirthday> {
    val manualAnnualDates = events.asSequence()
        .filter { it.pairId == id && it.deletedAt == null && it.annual }
        .map { MonthDay.from(Dates.toLocalDate(it.dateMillis)) }
        .toSet()
    return birthdays().filterNot { birthday ->
        MonthDay.from(Dates.toLocalDate(birthday.dateMillis)) in manualAnnualDates
    }
}

/** Ближайшее наступление дня рождения: в этом году или в следующем. */
fun PairBirthday.upcomingDate(today: LocalDate = Dates.today()): LocalDate =
    Dates.nextOccurrence(dateMillis, annual = true, today = today)