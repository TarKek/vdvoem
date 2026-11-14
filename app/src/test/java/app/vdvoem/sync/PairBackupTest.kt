package app.vdvoem.sync

import app.vdvoem.core.domain.model.CalendarEvent
import app.vdvoem.core.domain.model.Gift
import app.vdvoem.core.domain.model.GiftStatus
import app.vdvoem.core.domain.model.Pair
import app.vdvoem.core.domain.model.Wish
import app.vdvoem.core.domain.model.WishStatus
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Резервная копия пары: экспорт в JSON и разбор обратно. Проверяем, что файл,
 * выгруженный на одном устройстве, восстанавливается на другом без потерь.
 */
class PairBackupTest {

    private fun snapshot(
        pair: Pair? = null,
        events: List<CalendarEvent> = emptyList(),
        wishes: List<Wish> = emptyList(),
        gifts: List<Gift> = emptyList()
    ) = PairBackup.Snapshot(pair = pair, events = events, wishes = wishes, gifts = gifts)

    @Test
    fun `empty snapshot survives round trip`() {
        val original = snapshot()

        val restored = PairBackup.parse(PairBackup.export(original))

        assertEquals(original, restored)
        assertNull(restored?.pair)
        assertEquals(emptyList(), restored?.events)
        assertEquals(emptyList(), restored?.wishes)
        assertEquals(emptyList(), restored?.gifts)
    }

    @Test
    fun `full snapshot survives round trip`() {
        val original = snapshot(
            pair = Pair(
                id = "pair-1",
                nameA = "Аня",
                nameB = "Борис",
                sinceMillis = 1_600_000_000_000L,
                updatedAt = 1_700_000_000_000L
            ),
            events = listOf(
                CalendarEvent(
                    id = "event-1",
                    pairId = "pair-1",
                    title = "Годовщина",
                    dateMillis = 1_650_000_000_000L,
                    annual = true,
                    updatedAt = 1_700_000_000_001L
                ),
                CalendarEvent(
                    id = "event-2",
                    pairId = "pair-1",
                    title = "Поездка",
                    dateMillis = 1_660_000_000_000L,
                    annual = false,
                    updatedAt = 1_700_000_000_002L
                )
            ),
            wishes = listOf(
                Wish(
                    id = "wish-1",
                    pairId = "pair-1",
                    title = "Наушники",
                    note = "чёрные",
                    priceEstimate = 15_000L,
                    status = WishStatus.RESERVED,
                    updatedAt = 1_700_000_000_003L
                ),
                Wish(
                    id = "wish-2",
                    pairId = "pair-1",
                    title = "Книга",
                    note = "",
                    priceEstimate = null,
                    status = WishStatus.DONE,
                    updatedAt = 1_700_000_000_004L
                )
            ),
            gifts = listOf(
                Gift(
                    id = "gift-1",
                    pairId = "pair-1",
                    title = "Свитер",
                    note = "размер M",
                    budget = 5_000L,
                    status = GiftStatus.BOUGHT,
                    updatedAt = 1_700_000_000_005L
                ),
                Gift(
                    id = "gift-2",
                    pairId = "pair-1",
                    title = "Цветы",
                    note = "",
                    budget = null,
                    status = GiftStatus.IDEA,
                    updatedAt = 1_700_000_000_006L
                )
            )
        )

        val raw = PairBackup.export(original)
        val restored = PairBackup.parse(raw)

        assertTrue(raw.contains("\"version\""))
        assertEquals(original, restored)
        assertEquals(2, restored?.events?.size)
        assertEquals(2, restored?.wishes?.size)
        assertEquals(2, restored?.gifts?.size)
        assertEquals(WishStatus.RESERVED, restored?.wishes?.first()?.status)
        assertEquals(GiftStatus.BOUGHT, restored?.gifts?.first()?.status)
    }

    @Test
    fun `null price estimate and budget stay null after round trip`() {
        val original = snapshot(
            wishes = listOf(Wish(id = "w", pairId = "p", title = "Без цены")),
            gifts = listOf(Gift(id = "g", pairId = "p", title = "Без бюджета"))
        )

        val restored = PairBackup.parse(PairBackup.export(original))

        assertNull(restored?.wishes?.first()?.priceEstimate)
        assertNull(restored?.gifts?.first()?.budget)
        assertEquals(original, restored)
    }

    @Test
    fun `exported json contains pair and lists`() {
        val raw = PairBackup.export(
            snapshot(
                pair = Pair(id = "p", nameA = "A", nameB = "B", sinceMillis = 1L, updatedAt = 2L),
                events = listOf(
                    CalendarEvent(id = "e", pairId = "p", title = "t", dateMillis = 3L, updatedAt = 4L)
                )
            )
        )

        assertTrue(raw.contains("\"pair\""))
        assertTrue(raw.contains("\"events\""))
        assertTrue(raw.contains("\"wishes\""))
        assertTrue(raw.contains("\"gifts\""))
        assertTrue(raw.contains("\"A\""))
    }

    @Test
    fun `corrupted json returns null`() {
        assertNull(PairBackup.parse(""))
        assertNull(PairBackup.parse("   "))
        assertNull(PairBackup.parse("это не json"))
        assertNull(PairBackup.parse("{"))
        assertNull(PairBackup.parse("{\"version\":1,"))
    }

    @Test
    fun `foreign version returns null`() {
        assertNull(PairBackup.parse("{\"version\":99,\"events\":[]}"))
        assertNull(PairBackup.parse("{\"version\":0}"))
        assertNull(PairBackup.parse("{\"events\":[]}"))
    }

    @Test
    fun `missing optional fields fall back to defaults`() {
        val raw = """
            {
              "version": 1,
              "wishes": [
                {"id": "w1", "pairId": "p1", "title": "Мечта", "status": "НЕТ_ТАКОГО"}
              ],
              "gifts": [
                {"id": "g1", "pairId": "p1", "title": "Идея", "status": "???"}
              ]
            }
        """.trimIndent()

        val restored = PairBackup.parse(raw)

        assertEquals(emptyList(), restored?.events)
        assertEquals(1, restored?.wishes?.size)
        assertEquals(1, restored?.gifts?.size)

        val wish = restored?.wishes?.first()
        assertEquals("", wish?.note)
        assertNull(wish?.priceEstimate)
        assertEquals(WishStatus.WANTED, wish?.status)
        assertEquals(0L, wish?.updatedAt)

        val gift = restored?.gifts?.first()
        assertEquals("", gift?.note)
        assertNull(gift?.budget)
        assertEquals(GiftStatus.IDEA, gift?.status)
        assertEquals(0L, gift?.updatedAt)
    }
}