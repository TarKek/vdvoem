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
}