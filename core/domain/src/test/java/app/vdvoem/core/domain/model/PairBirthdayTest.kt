package app.vdvoem.core.domain.model

import app.vdvoem.core.domain.time.Dates
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Дни рождения из профиля пары становятся датами календаря. Логика чистая:
 * в базе их нет, поэтому проверяем именно вывод из [Pair].
 */
class PairBirthdayTest {

    @Test
    fun `birthdays returns both dates in profile order`() {
        val pair = Pair(
            id = "pair-1",
            nameA = "Варя",
            nameB = "Андрей",
            sinceMillis = 0L,
            birthdayA = 1_000L,
            birthdayB = 2_000L
        )

        val birthdays = pair.birthdays()

        assertEquals(2, birthdays.size)
        assertEquals("Варя", birthdays[0].name)
        assertEquals(PairBirthday.Owner.ME, birthdays[0].owner)
        assertEquals(1_000L, birthdays[0].dateMillis)
        assertEquals("Андрей", birthdays[1].name)
        assertEquals(PairBirthday.Owner.PARTNER, birthdays[1].owner)
        assertEquals(2_000L, birthdays[1].dateMillis)
    }

    @Test
    fun `missing birthdays produce no events`() {
        val pair = Pair(id = "pair-1", nameA = "Варя", nameB = "Андрей", sinceMillis = 0L)

        assertTrue(pair.birthdays().isEmpty())
    }

    @Test
    fun `only the filled birthday becomes an event`() {
        val pair = Pair(
            id = "pair-1",
            nameA = "Варя",
            nameB = "Андрей",
            sinceMillis = 0L,
            birthdayB = 2_000L
        )

        val birthdays = pair.birthdays()

        assertEquals(1, birthdays.size)
        assertEquals(PairBirthday.Owner.PARTNER, birthdays.single().owner)
    }

    /** Настоящие события используют UUID, поэтому id дня рождения не может с ними совпасть. */
    @Test
    fun `event ids are stable and distinct`() {
        val pair = Pair(
            id = "pair-1",
            nameA = "Варя",
            nameB = "Андрей",
            sinceMillis = 0L,
            birthdayA = 1_000L,
            birthdayB = 2_000L
        )

        val ids = pair.birthdays().map { it.eventId }

        assertEquals(listOf("birthday:me", "birthday:partner"), ids)
    }

    /** В календаре и виджете день рождения — ежегодная дата, а не прошедший день. */
    @Test
    fun `upcoming date rolls over to next year after the birthday passed`() {
        val today = LocalDate.of(2026, 11, 20)
        val birthday = PairBirthday(
            owner = PairBirthday.Owner.ME,
            name = "Варя",
            dateMillis = Dates.startOfDay(LocalDate.of(2011, 4, 7))
        )

        assertEquals(LocalDate.of(2027, 4, 7), birthday.upcomingDate(today))
    }

    @Test
    fun `upcoming date keeps this year when the birthday is ahead`() {
        val today = LocalDate.of(2026, 11, 20)
        val birthday = PairBirthday(
            owner = PairBirthday.Owner.PARTNER,
            name = "Андрей",
            dateMillis = Dates.startOfDay(LocalDate.of(2011, 12, 3))
        )

        assertEquals(LocalDate.of(2026, 12, 3), birthday.upcomingDate(today))
    }

    @Test
    fun `manual annual event hides only the matching birthday`() {
        val pair = Pair(
            id = "pair-1",
            nameA = "Варя",
            nameB = "Андрей",
            sinceMillis = 0L,
            birthdayA = Dates.startOfDay(LocalDate.of(2011, 4, 7)),
            birthdayB = Dates.startOfDay(LocalDate.of(2011, 12, 3))
        )
        val manual = CalendarEvent(
            id = "event-1",
            pairId = pair.id,
            title = "День рождения Вари",
            dateMillis = Dates.startOfDay(LocalDate.of(2024, 4, 7)),
            annual = true
        )

        val birthdays = pair.birthdaysWithoutManualDuplicates(listOf(manual))

        assertEquals(listOf(PairBirthday.Owner.PARTNER), birthdays.map { it.owner })
    }

    @Test
    fun `one-time and deleted events do not hide birthdays`() {
        val pair = pairWithBirthday(LocalDate.of(2011, 4, 7))
        val events = listOf(
            CalendarEvent(
                id = "one-time",
                pairId = pair.id,
                title = "Встреча",
                dateMillis = Dates.startOfDay(LocalDate.of(2026, 4, 7)),
                annual = false
            ),
            CalendarEvent(
                id = "deleted",
                pairId = pair.id,
                title = "Старый день рождения",
                dateMillis = Dates.startOfDay(LocalDate.of(2020, 4, 7)),
                annual = true,
                deletedAt = 1L
            )
        )

        assertEquals(1, pair.birthdaysWithoutManualDuplicates(events).size)
    }

    @Test
    fun `leap day duplicate is matched by month and day`() {
        val pair = pairWithBirthday(LocalDate.of(2012, 2, 29))
        val leapDay = CalendarEvent(
            id = "leap",
            pairId = pair.id,
            title = "День рождения",
            dateMillis = Dates.startOfDay(LocalDate.of(2020, 2, 29)),
            annual = true
        )

        assertTrue(pair.birthdaysWithoutManualDuplicates(listOf(leapDay)).isEmpty())
        assertTrue(
            pair.birthdaysWithoutManualDuplicates(
                listOf(leapDay.copy(dateMillis = Dates.startOfDay(LocalDate.of(2021, 2, 28))))
            ).isNotEmpty()
        )
        assertTrue(
            pair.birthdaysWithoutManualDuplicates(
                listOf(leapDay.copy(pairId = "another-pair"))
            ).isNotEmpty()
        )
    }

    private fun pairWithBirthday(date: LocalDate) = Pair(
        id = "pair-1",
        nameA = "Варя",
        nameB = "Андрей",
        sinceMillis = 0L,
        birthdayA = Dates.startOfDay(date)
    )
}