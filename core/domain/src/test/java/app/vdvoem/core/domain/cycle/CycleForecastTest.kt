package app.vdvoem.core.domain.cycle

import app.vdvoem.core.domain.model.CycleDay
import app.vdvoem.core.domain.model.CycleSettings
import app.vdvoem.core.domain.time.Dates
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Правила прогноза цикла: начало цикла, средняя длина, овуляция, задержка.
 * Даты передаются явно, поэтому тест не зависит от системных часов.
 */
class CycleForecastTest {

    private val pairId = "pair-1"

    private fun day(
        date: LocalDate,
        period: Boolean = false,
        symptoms: List<String> = emptyList(),
        mood: String = "",
        deletedAt: Long? = null
    ) = CycleDay(
        id = date.toString(),
        pairId = pairId,
        dateMillis = Dates.startOfDay(date),
        period = period,
        symptoms = symptoms,
        mood = mood,
        deletedAt = deletedAt
    )

    private fun period(from: LocalDate, days: Int): List<CycleDay> =
        (0 until days).map { day(from.plusDays(it.toLong()), period = true) }

    private fun settings(cycleLength: Int, periodLength: Int = 5) = CycleSettings(
        id = pairId,
        pairId = pairId,
        cycleLength = cycleLength,
        periodLength = periodLength
    )

    @Test
    fun `empty data returns neutral forecast`() {
        val stats = buildForecast(emptyList(), null, LocalDate.of(2026, 3, 10))

        assertEquals(1, stats.currentDay)
        assertEquals(CyclePhase.FOLLICULAR, stats.phase)
        assertNull(stats.lastPeriodStart)
        assertNull(stats.nextPeriodStart)
        assertNull(stats.daysUntilNextPeriod)
        assertNull(stats.ovulationDate)
        assertNull(stats.fertileStart)
        assertNull(stats.fertileEnd)
        assertEquals(emptyList(), stats.lengths)
        assertEquals(DEFAULT_CYCLE_LENGTH, stats.averageLength)
        assertEquals(DEFAULT_CYCLE_LENGTH, stats.shortestLength)
        assertEquals(DEFAULT_CYCLE_LENGTH, stats.longestLength)
        assertEquals(DEFAULT_PERIOD_LENGTH, stats.periodLength)
        assertFalse(stats.isLate)
    }

    @Test
    fun `cycle starts where the previous day was not a period day`() {
        val days = period(LocalDate.of(2026, 8, 1), 4) + period(LocalDate.of(2026, 8, 30), 3)

        val stats = buildForecast(days, settings(cycleLength = 28), LocalDate.of(2026, 9, 10))

        assertEquals(LocalDate.of(2026, 8, 30), stats.lastPeriodStart)
        assertEquals(listOf(29), stats.lengths)
        assertEquals(12, stats.currentDay) // 30 августа — 1-й день, 10 сентября — 12-й
    }

    @Test
    fun `continuous period run gives exactly one start`() {
        val days = period(LocalDate.of(2026, 5, 3), 6)

        val stats = buildForecast(days, null, LocalDate.of(2026, 5, 6))

        assertEquals(LocalDate.of(2026, 5, 3), stats.lastPeriodStart)
        assertEquals(emptyList(), stats.lengths)
        assertEquals(4, stats.currentDay)
        assertEquals(CyclePhase.PERIOD, stats.phase)
    }

    @Test
    fun `gap of two days splits cycles`() {
        val days = period(LocalDate.of(2026, 4, 1), 2) + period(LocalDate.of(2026, 4, 4), 2)

        val stats = buildForecast(days, settings(cycleLength = 20), LocalDate.of(2026, 4, 5))

        assertEquals(LocalDate.of(2026, 4, 4), stats.lastPeriodStart)
        assertEquals(listOf(3), stats.lengths)
    }

    @Test
    fun `average is taken over two or more completed cycles`() {
        // Циклы: 1 января → 31 января (30 дней), 31 января → 26 февраля (26 дней).
        val days = period(LocalDate.of(2026, 1, 1), 3) +
            period(LocalDate.of(2026, 1, 31), 3) +
            period(LocalDate.of(2026, 2, 26), 3)

        val stats = buildForecast(days, settings(cycleLength = 35), LocalDate.of(2026, 3, 5))

        assertEquals(listOf(30, 26), stats.lengths)
        assertEquals(28, stats.averageLength)
        assertEquals(26, stats.shortestLength)
        assertEquals(30, stats.longestLength)
        assertEquals(LocalDate.of(2026, 2, 26), stats.lastPeriodStart)
        assertEquals(LocalDate.of(2026, 3, 26), stats.nextPeriodStart)
        assertEquals(21L, stats.daysUntilNextPeriod)
        assertEquals(8, stats.currentDay)
    }

    @Test
    fun `single cycle falls back to settings length`() {
        val days = period(LocalDate.of(2026, 3, 1), 4)

        val stats = buildForecast(days, settings(cycleLength = 26, periodLength = 4), LocalDate.of(2026, 3, 10))

        assertEquals(emptyList(), stats.lengths)
        assertEquals(26, stats.averageLength)
        assertEquals(26, stats.shortestLength)
        assertEquals(26, stats.longestLength)
        assertEquals(4, stats.periodLength)
        assertEquals(LocalDate.of(2026, 3, 27), stats.nextPeriodStart)
        assertEquals(10, stats.currentDay)
    }

    @Test
    fun `ovulation is fourteen days before the next start`() {
        val days = period(LocalDate.of(2026, 3, 1), 5)
        val settings = settings(cycleLength = 28, periodLength = 5)

        val ovulationDay = buildForecast(days, settings, LocalDate.of(2026, 3, 15))
        val beforeWindow = buildForecast(days, settings, LocalDate.of(2026, 3, 12))
        val afterWindow = buildForecast(days, settings, LocalDate.of(2026, 3, 18))

        val nextStart = LocalDate.of(2026, 3, 29)
        assertEquals(nextStart, ovulationDay.nextPeriodStart)
        assertEquals(nextStart.minusDays(14), ovulationDay.ovulationDate)
        assertEquals(LocalDate.of(2026, 3, 15), ovulationDay.ovulationDate)
        assertEquals(LocalDate.of(2026, 3, 13), ovulationDay.fertileStart)
        assertEquals(LocalDate.of(2026, 3, 17), ovulationDay.fertileEnd)

        assertEquals(CyclePhase.OVULATION, ovulationDay.phase)
        assertEquals(CyclePhase.FOLLICULAR, beforeWindow.phase)
        assertEquals(CyclePhase.LUTEAL, afterWindow.phase)
    }

    @Test
    fun `period phase wins over forecast phase`() {
        val days = period(LocalDate.of(2026, 3, 1), 5)

        val stats = buildForecast(days, settings(cycleLength = 28), LocalDate.of(2026, 3, 3))

        assertEquals(CyclePhase.PERIOD, stats.phase)
        assertEquals(3, stats.currentDay)
    }

    @Test
    fun `isLate turns on after average plus three days`() {
        val days = period(LocalDate.of(2026, 1, 1), 3) +
            period(LocalDate.of(2026, 1, 29), 3) +
            period(LocalDate.of(2026, 2, 26), 3)
        val settings = settings(cycleLength = 28)

        val onTime = buildForecast(days, settings, LocalDate.of(2026, 3, 28)) // 31-й день
        val late = buildForecast(days, settings, LocalDate.of(2026, 3, 29)) // 32-й день

        assertEquals(28, onTime.averageLength)
        assertEquals(31, onTime.currentDay)
        assertFalse(onTime.isLate)
        assertEquals(32, late.currentDay)
        assertTrue(late.isLate)
    }

    @Test
    fun `new cycle resets lateness`() {
        val days = period(LocalDate.of(2026, 1, 1), 3) +
            period(LocalDate.of(2026, 1, 29), 3) +
            period(LocalDate.of(2026, 2, 26), 3) +
            period(LocalDate.of(2026, 4, 2), 3)

        val stats = buildForecast(days, settings(cycleLength = 28), LocalDate.of(2026, 4, 3))

        assertEquals(2, stats.currentDay)
        assertFalse(stats.isLate)
    }

    @Test
    fun `deleted days are ignored`() {
        val now = 1_700_000_000_000L
        val days = listOf(
            day(LocalDate.of(2026, 3, 1), period = true, deletedAt = now),
            day(LocalDate.of(2026, 3, 10), symptoms = listOf("cramps"))
        )

        val stats = buildForecast(days, settings(cycleLength = 30), LocalDate.of(2026, 3, 12))

        assertNull(stats.lastPeriodStart)
        assertNull(stats.nextPeriodStart)
        assertEquals(emptyList(), stats.lengths)
        assertEquals(30, stats.averageLength)
        assertFalse(stats.isLate)
    }

    @Test
    fun `days without period do not start a cycle`() {
        val days = listOf(
            day(LocalDate.of(2026, 3, 1), mood = "happy"),
            day(LocalDate.of(2026, 3, 2), symptoms = listOf("headache")),
            day(LocalDate.of(2026, 3, 3))
        )

        val stats = buildForecast(days, null, LocalDate.of(2026, 3, 4))

        assertNull(stats.lastPeriodStart)
        assertEquals(emptyList(), stats.lengths)
        assertEquals(CyclePhase.FOLLICULAR, stats.phase)
    }

    @Test
    fun `future start keeps the current day at one`() {
        val days = period(LocalDate.of(2026, 8, 30), 3)

        val stats = buildForecast(days, settings(cycleLength = 28), LocalDate.of(2026, 8, 20))

        assertEquals(1, stats.currentDay)
        assertNull(stats.lastPeriodStart)
        assertEquals(LocalDate.of(2026, 8, 30), stats.nextPeriodStart)
        assertEquals(10L, stats.daysUntilNextPeriod)
    }

    @Test
    fun `duplicate records for the same day count once`() {
        val date = LocalDate.of(2026, 3, 1)
        val days = listOf(day(date, period = true), day(date, period = true, mood = "calm"))

        val stats = buildForecast(days, null, LocalDate.of(2026, 3, 2))

        assertEquals(LocalDate.of(2026, 3, 1), stats.lastPeriodStart)
        assertEquals(emptyList(), stats.lengths)
        assertEquals(2, stats.currentDay)
    }
}