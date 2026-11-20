package app.vdvoem.widget

import app.vdvoem.core.domain.model.CalendarEvent
import app.vdvoem.core.domain.model.Wish
import app.vdvoem.core.domain.model.WishStatus
import app.vdvoem.core.domain.time.Dates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WidgetDataTest {

    private val today = LocalDate.of(2026, 11, 19)

    private fun event(
        id: String,
        title: String,
        date: LocalDate,
        annual: Boolean = false,
        deleted: Boolean = false
    ) = CalendarEvent(
        id = id,
        pairId = "pair",
        title = title,
        dateMillis = Dates.startOfDay(date),
        annual = annual,
        deletedAt = if (deleted) 1L else null
    )

    private fun wish(
        id: String,
        status: WishStatus = WishStatus.WANTED,
        surprise: Boolean = false,
        deleted: Boolean = false
    ) = Wish(
        id = id,
        pairId = "pair",
        title = "wish-$id",
        status = status,
        surprise = surprise,
        ownerDeviceId = if (surprise) "partner-device" else "",
        deletedAt = if (deleted) 1L else null
    )

    @Test
    fun `upcoming events keep future dates sorted by proximity`() {
        val events = listOf(
            event("far", "Поездка", today.plusDays(30)),
            event("near", "Ужин", today.plusDays(2)),
            event("today", "Встреча", today)
        )

        val result = upcomingEvents(events, today)

        assertEquals(listOf("Встреча", "Ужин", "Поездка"), result.map { it.title })
        assertEquals(listOf(0L, 2L, 30L), result.map { it.daysUntil })
    }

    @Test
    fun `upcoming events skip past non annual and deleted`() {
        val events = listOf(
            event("past", "Прошлое", today.minusDays(3)),
            event("deleted", "Удалённое", today.plusDays(1), deleted = true),
            event("ok", "Завтра", today.plusDays(1))
        )

        val result = upcomingEvents(events, today)

        assertEquals(listOf("Завтра"), result.map { it.title })
    }

    @Test
    fun `annual event moves to the next occurrence this year`() {
        val events = listOf(
            event("anniversary", "Годовщина", LocalDate.of(2020, 12, 1), annual = true)
        )

        val result = upcomingEvents(events, today)

        assertEquals(LocalDate.of(2026, 12, 1), result.single().date)
        assertEquals(12L, result.single().daysUntil)
    }

    @Test
    fun `annual event rolls over to next year when this year passed`() {
        val events = listOf(
            event("anniversary", "Годовщина", LocalDate.of(2020, 3, 1), annual = true)
        )

        val result = upcomingEvents(events, today)

        assertEquals(LocalDate.of(2027, 3, 1), result.single().date)
    }

    @Test
    fun `upcoming events are limited`() {
        val events = (1..5).map { event("e$it", "Событие $it", today.plusDays(it.toLong())) }

        assertEquals(CALENDAR_WIDGET_LIMIT, upcomingEvents(events, today).size)
    }

    /** Дни рождения приходят не из базы, а из профиля пары — они идут в тот же список. */
    @Test
    fun `extra dates are merged into the same sorted list`() {
        val events = listOf(event("e1", "Ужин", today.plusDays(5)))
        val birthdays = listOf(
            UpcomingEvent(title = "День рождения: Варя", date = today.plusDays(2), daysUntil = 2L)
        )

        val result = upcomingEvents(events, today, extra = birthdays)

        assertEquals(listOf("День рождения: Варя", "Ужин"), result.map { it.title })
    }

    @Test
    fun `extra dates respect the limit after merging`() {
        val events = (1..3).map { event("e$it", "Событие $it", today.plusDays(it.toLong())) }
        val birthdays = listOf(
            UpcomingEvent(title = "День рождения: Варя", date = today, daysUntil = 0L)
        )

        val result = upcomingEvents(events, today, extra = birthdays)

        assertEquals(CALENDAR_WIDGET_LIMIT, result.size)
        assertEquals("День рождения: Варя", result.first().title)
    }

    @Test
    fun `wish of day hides done surprised and deleted wishes`() {
        val wishes = listOf(
            wish("a", status = WishStatus.DONE),
            wish("b", surprise = true),
            wish("c", deleted = true),
            wish("d")
        )

        assertEquals("d", wishOfDay(wishes, today)?.id)
    }

    @Test
    fun `wish of day is null when nothing is visible`() {
        assertNull(wishOfDay(listOf(wish("a", status = WishStatus.DONE)), today))
    }

    @Test
    fun `wish of day is stable for a date and changes with the next date`() {
        val wishes = listOf(wish("b"), wish("a"), wish("c"))
        val day = LocalDate.of(2026, 11, 19)

        val first = wishOfDay(wishes, day)
        val second = wishOfDay(wishes.shuffled(), day)
        val nextDay = wishOfDay(wishes, day.plusDays(1))

        assertEquals(first, second)
        assertTrue(first?.id != nextDay?.id)
    }
}