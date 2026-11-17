package app.vdvoem.core.domain.time

import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Чистая логика дат: без Android, на JVM. Date-only значения не зависят от системной зоны.
 */
class DatesTest {

    private val zone: ZoneId = ZoneOffset.UTC

    private fun millis(date: LocalDate): Long = Dates.startOfDay(date)

    private fun utcMidnight(date: LocalDate): Long =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    // --- nextOccurrence: годовщина ---

    @Test
    fun `nextOccurrence annual keeps today when anniversary is today`() {
        val anniversary = LocalDate.of(2020, 5, 20)
        val today = LocalDate.of(2025, 5, 20)

        val next = Dates.nextOccurrence(millis(anniversary), annual = true, today = today)

        assertEquals(today, next)
        assertEquals(0L, Dates.daysUntil(next, today))
    }

    @Test
    fun `nextOccurrence annual moves to this year when date is ahead`() {
        val anniversary = LocalDate.of(2020, 12, 31)
        val today = LocalDate.of(2026, 11, 14)

        val next = Dates.nextOccurrence(millis(anniversary), annual = true, today = today)

        assertEquals(LocalDate.of(2026, 12, 31), next)
        assertEquals(47L, Dates.daysUntil(next, today))
    }

    @Test
    fun `nextOccurrence annual moves to next year when this year has passed`() {
        val anniversary = LocalDate.of(2020, 3, 15)
        val today = LocalDate.of(2026, 11, 14)

        val next = Dates.nextOccurrence(millis(anniversary), annual = true, today = today)

        assertEquals(LocalDate.of(2027, 3, 15), next)
        assertTrue(next.isAfter(today))
    }

    @Test
    fun `nextOccurrence annual handles anniversary created in a past year`() {
        val anniversary = LocalDate.of(2019, 11, 14)
        val today = LocalDate.of(2026, 11, 14)

        val next = Dates.nextOccurrence(millis(anniversary), annual = true, today = today)

        assertEquals(today, next)
    }

    @Test
    fun `nextOccurrence non annual keeps stored date even if it is in the past`() {
        val stored = LocalDate.of(2026, 1, 10)
        val today = LocalDate.of(2026, 11, 14)

        val next = Dates.nextOccurrence(millis(stored), annual = false, today = today)

        assertEquals(stored, next)
    }

    @Test
    fun `nextOccurrence leap day anniversary stays in February`() {
        val anniversary = LocalDate.of(2020, 2, 29)
        val today = LocalDate.of(2025, 6, 1)

        val next = Dates.nextOccurrence(millis(anniversary), annual = true, today = today)

        assertEquals(2, next.monthValue)
        assertTrue(!next.isBefore(today))
    }

    // --- daysUntil ---

    @Test
    fun `daysUntil is zero for the same day`() {
        val today = LocalDate.of(2026, 11, 14)

        assertEquals(0L, Dates.daysUntil(today, today))
    }

    @Test
    fun `daysUntil is one for tomorrow`() {
        val today = LocalDate.of(2026, 11, 14)

        assertEquals(1L, Dates.daysUntil(today.plusDays(1), today))
    }

    @Test
    fun `daysUntil is negative for a past date`() {
        val today = LocalDate.of(2026, 11, 14)

        assertEquals(-5L, Dates.daysUntil(today.minusDays(5), today))
        assertTrue(Dates.daysUntil(today.minusDays(5), today) < 0)
    }

    // --- startOfDay ---

    @Test
    fun `startOfDay returns UTC midnight of the same date`() {
        val date = LocalDate.of(2026, 3, 1)

        val start = Dates.startOfDay(date)

        assertEquals(LocalTime.MIDNIGHT, Instant.ofEpochMilli(start).atZone(zone).toLocalTime())
        assertEquals(date, Dates.toLocalDate(start))
    }

    @Test
    fun `startOfDay of consecutive days differs by exactly one calendar day`() {
        val date = LocalDate.of(2026, 3, 1)

        val first = Dates.toLocalDate(Dates.startOfDay(date))
        val second = Dates.toLocalDate(Dates.startOfDay(date.plusDays(1)))

        assertEquals(1L, ChronoUnit.DAYS.between(first, second))
        assertTrue(Dates.startOfDay(date.plusDays(1)) > Dates.startOfDay(date))
    }

    // --- DatePicker и хранение используют одну UTC-полночь ---

    @Test
    fun `fromPickerMillis keeps utc midnight`() {
        val date = LocalDate.of(2026, 7, 4)

        val local = Dates.fromPickerMillis(utcMidnight(date))

        assertEquals(Dates.startOfDay(date), local)
        assertEquals(date, Dates.toLocalDate(local))
        assertEquals(LocalTime.MIDNIGHT, Instant.ofEpochMilli(local).atZone(zone).toLocalTime())
    }

    @Test
    fun `toPickerMillis converts local midnight to utc midnight`() {
        val date = LocalDate.of(2026, 7, 4)

        val picker = Dates.toPickerMillis(Dates.startOfDay(date))

        assertEquals(utcMidnight(date), picker)
        assertEquals(date, Instant.ofEpochMilli(picker).atZone(ZoneOffset.UTC).toLocalDate())
    }

    @Test
    fun `picker millis survive the full round trip`() {
        val date = LocalDate.of(2026, 12, 31)
        val picker = utcMidnight(date)

        val stored = Dates.fromPickerMillis(picker)

        assertEquals(picker, Dates.toPickerMillis(stored))
        assertEquals(date, Dates.toLocalDate(stored))
    }

    // --- format ---

    @Test
    fun `format prints russian day month and year`() {
        assertEquals("15 марта 2020", Dates.format(LocalDate.of(2020, 3, 15)))
        assertEquals("1 января 2026", Dates.format(LocalDate.of(2026, 1, 1)))
    }
}