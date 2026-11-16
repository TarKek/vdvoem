package app.vdvoem.sync

import app.vdvoem.core.domain.model.CalendarEvent
import app.vdvoem.core.domain.model.CycleDay
import app.vdvoem.core.domain.model.CycleSettings
import app.vdvoem.core.domain.model.Flow
import app.vdvoem.core.domain.model.Gift
import app.vdvoem.core.domain.model.GiftStatus
import app.vdvoem.core.domain.model.Lesson
import app.vdvoem.core.domain.model.Pair
import app.vdvoem.core.domain.model.Poke
import app.vdvoem.core.domain.model.QuizScore
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
        gifts: List<Gift> = emptyList(),
        pokes: List<Poke> = emptyList(),
        quizScores: List<QuizScore> = emptyList(),
        lessons: List<Lesson> = emptyList(),
        cycleDays: List<CycleDay> = emptyList(),
        cycleSettings: CycleSettings? = null
    ) = PairBackup.Snapshot(
        pair = pair,
        events = events,
        wishes = wishes,
        gifts = gifts,
        pokes = pokes,
        quizScores = quizScores,
        lessons = lessons,
        cycleDays = cycleDays,
        cycleSettings = cycleSettings
    )

    @Test
    fun `empty snapshot survives round trip`() {
        val original = snapshot()

        val restored = PairBackup.parse(PairBackup.export(original))

        assertEquals(original, restored)
        assertNull(restored?.pair)
        assertEquals(emptyList(), restored?.events)
        assertEquals(emptyList(), restored?.wishes)
        assertEquals(emptyList(), restored?.gifts)
        assertEquals(emptyList(), restored?.pokes)
        assertEquals(emptyList(), restored?.quizScores)
        assertEquals(emptyList(), restored?.lessons)
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
            ),
            pokes = listOf(
                Poke(
                    id = "poke-1",
                    pairId = "pair-1",
                    text = "Доброе утро",
                    createdAt = 1_700_000_000_007L,
                    updatedAt = 1_700_000_000_007L
                )
            ),
            quizScores = listOf(
                QuizScore(
                    id = "score-1",
                    pairId = "pair-1",
                    quizId = "who-am-i",
                    percent = 87,
                    passedAt = 1_700_000_000_008L,
                    updatedAt = 1_700_000_000_008L
                )
            ),
            lessons = listOf(
                Lesson(
                    id = "lesson-1",
                    pairId = "pair-1",
                    subject = "Математика",
                    weekday = 1,
                    startMinute = 540,
                    endMinute = 585,
                    room = "204",
                    teacher = "Иванова",
                    updatedAt = 1_700_000_000_009L
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
        assertEquals(1, restored?.pokes?.size)
        assertEquals(1, restored?.quizScores?.size)
        assertEquals(1, restored?.lessons?.size)
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
        assertTrue(raw.contains("\"pokes\""))
        assertTrue(raw.contains("\"quizScores\""))
        assertTrue(raw.contains("\"lessons\""))
        assertTrue(raw.contains("\"A\""))
    }

    @Test
    fun `pokes scores and lessons survive round trip in both directions`() {
        val original = snapshot(
            pokes = listOf(Poke(id = "p1", pairId = "pair", text = "Скучаю")),
            quizScores = listOf(QuizScore(id = "s1", pairId = "pair", quizId = "q1", percent = 100)),
            lessons = listOf(
                Lesson(id = "l1", pairId = "pair", subject = "Физика", weekday = 3, startMinute = 600, endMinute = 645)
            )
        )

        val restored = PairBackup.parse(PairBackup.export(original))

        assertEquals(original, restored)
        assertEquals("Скучаю", restored?.pokes?.first()?.text)
        assertEquals(100, restored?.quizScores?.first()?.percent)
        assertEquals(3, restored?.lessons?.first()?.weekday)
        assertEquals("10:00–10:45", restored?.lessons?.first()?.timeRange)
    }

    @Test
    fun `tombstones and optional fields are preserved`() {
        val original = snapshot(
            pokes = listOf(
                Poke(id = "p1", pairId = "pair", text = "Удалено", deletedAt = 1_700_000_000_010L)
            ),
            quizScores = listOf(
                QuizScore(
                    id = "s1",
                    pairId = "pair",
                    quizId = "q1",
                    percent = 40,
                    passedAt = 1_700_000_000_011L,
                    updatedAt = 1_700_000_000_012L,
                    deletedAt = 1_700_000_000_012L
                )
            ),
            lessons = listOf(
                Lesson(
                    id = "l1",
                    pairId = "pair",
                    subject = "Химия",
                    weekday = 5,
                    startMinute = 480,
                    endMinute = 525,
                    updatedAt = 1_700_000_000_013L,
                    deletedAt = 1_700_000_000_013L
                )
            )
        )

        val restored = PairBackup.parse(PairBackup.export(original))

        assertEquals(original, restored)
        assertEquals(1_700_000_000_010L, restored?.pokes?.first()?.deletedAt)
        assertEquals(1_700_000_000_012L, restored?.quizScores?.first()?.deletedAt)
        assertEquals(1_700_000_000_013L, restored?.lessons?.first()?.deletedAt)
    }

    @Test
    fun `old file without new lists still imports`() {
        // Файл, выгруженный до появления сердечек, тестов и уроков.
        val raw = """
            {
              "version": 1,
              "exportedAt": 1700000000000,
              "pair": {
                "id": "p1",
                "nameA": "Аня",
                "nameB": "Борис",
                "sinceMillis": 1600000000000,
                "birthdayA": null,
                "birthdayB": 1610000000000,
                "updatedAt": 1700000000000
              },
              "events": [
                {"id": "e1", "pairId": "p1", "title": "Годовщина", "dateMillis": 1650000000000, "annual": true, "updatedAt": 1700000000001}
              ],
              "wishes": [
                {"id": "w1", "pairId": "p1", "title": "Наушники", "note": "чёрные", "priceEstimate": 15000, "status": "RESERVED", "updatedAt": 1700000000002}
              ],
              "gifts": [
                {"id": "g1", "pairId": "p1", "title": "Свитер", "note": "", "budget": null, "status": "BOUGHT", "updatedAt": 1700000000003}
              ]
            }
        """.trimIndent()

        val restored = PairBackup.parse(raw)

        assertEquals("p1", restored?.pair?.id)
        assertEquals(1, restored?.events?.size)
        assertEquals(1, restored?.wishes?.size)
        assertEquals(1, restored?.gifts?.size)
        assertEquals(emptyList(), restored?.pokes)
        assertEquals(emptyList(), restored?.quizScores)
        assertEquals(emptyList(), restored?.lessons)
    }

    @Test
    fun `new entities tolerate missing optional fields`() {
        val raw = """
            {
              "version": 1,
              "pokes": [
                {"id": "p1"}
              ],
              "quizScores": [
                {"id": "s1"}
              ],
              "lessons": [
                {"id": "l1"}
              ]
            }
        """.trimIndent()

        val restored = PairBackup.parse(raw)

        val poke = restored?.pokes?.single()
        assertEquals("", poke?.pairId)
        assertEquals("", poke?.text)
        assertEquals(0L, poke?.createdAt)
        assertEquals(0L, poke?.updatedAt)
        assertNull(poke?.deletedAt)

        val score = restored?.quizScores?.single()
        assertEquals("", score?.quizId)
        assertEquals(0, score?.percent)
        assertNull(score?.deletedAt)

        val lesson = restored?.lessons?.single()
        assertEquals("", lesson?.subject)
        assertEquals(1, lesson?.weekday)
        assertEquals(0, lesson?.startMinute)
        assertEquals(0, lesson?.endMinute)
        assertNull(lesson?.deletedAt)
    }

    @Test
    fun `wish extras and gift link survive round trip`() {
        val original = snapshot(
            wishes = listOf(
                Wish(
                    id = "w1",
                    pairId = "pair",
                    title = "Сюрприз",
                    note = "не подглядывать",
                    priceEstimate = 9_000L,
                    status = WishStatus.WANTED,
                    updatedAt = 1_700_000_000_020L,
                    priority = Wish.PRIORITY_HIGH,
                    url = "https://example.com/gift",
                    surprise = true,
                    ownerDeviceId = "device-1"
                )
            ),
            gifts = listOf(
                Gift(
                    id = "g1",
                    pairId = "pair",
                    title = "Из желания",
                    note = "Из желаний",
                    budget = 9_000L,
                    status = GiftStatus.IDEA,
                    updatedAt = 1_700_000_000_021L,
                    wishId = "w1"
                )
            )
        )

        val restored = PairBackup.parse(PairBackup.export(original))

        assertEquals(original, restored)
        val wish = restored?.wishes?.single()
        assertEquals(Wish.PRIORITY_HIGH, wish?.priority)
        assertEquals("https://example.com/gift", wish?.url)
        assertEquals(true, wish?.surprise)
        assertEquals("device-1", wish?.ownerDeviceId)
        assertEquals("w1", restored?.gifts?.single()?.wishId)
    }

    @Test
    fun `wish and gift tombstones survive round trip`() {
        val original = snapshot(
            wishes = listOf(
                Wish(
                    id = "w1",
                    pairId = "pair",
                    title = "Удалённое желание",
                    updatedAt = 1_700_000_000_030L,
                    deletedAt = 1_700_000_000_030L
                )
            ),
            gifts = listOf(
                Gift(
                    id = "g1",
                    pairId = "pair",
                    title = "Удалённый подарок",
                    updatedAt = 1_700_000_000_031L,
                    deletedAt = 1_700_000_000_031L
                )
            )
        )

        val restored = PairBackup.parse(PairBackup.export(original))

        assertEquals(original, restored)
        assertEquals(1_700_000_000_030L, restored?.wishes?.single()?.deletedAt)
        assertEquals(1_700_000_000_031L, restored?.gifts?.single()?.deletedAt)
    }

    @Test
    fun `cycle days and settings survive round trip`() {
        val original = snapshot(
            cycleDays = listOf(
                CycleDay(
                    id = "day-1",
                    pairId = "pair",
                    dateMillis = 1_762_000_000_000L,
                    period = true,
                    flow = Flow.MEDIUM,
                    symptoms = listOf("cramps", "headache"),
                    mood = "irritable",
                    weight = 61.4f,
                    note = "тяжело",
                    updatedAt = 1_762_000_000_100L
                ),
                CycleDay(
                    id = "day-2",
                    pairId = "pair",
                    dateMillis = 1_762_086_400_000L,
                    updatedAt = 1_762_000_000_200L,
                    deletedAt = 1_762_000_000_200L
                )
            ),
            cycleSettings = CycleSettings(
                id = "pair",
                pairId = "pair",
                cycleLength = 30,
                periodLength = 6,
                updatedAt = 1_762_000_000_300L
            )
        )

        val restored = PairBackup.parse(PairBackup.export(original))

        assertEquals(original, restored)
        val day = restored?.cycleDays?.first()
        assertEquals(Flow.MEDIUM, day?.flow)
        assertEquals(listOf("cramps", "headache"), day?.symptoms)
        assertEquals("irritable", day?.mood)
        assertEquals(61.4f, day?.weight)
        assertEquals("тяжело", day?.note)
        assertEquals(1_762_000_000_200L, restored?.cycleDays?.last()?.deletedAt)
        assertEquals(30, restored?.cycleSettings?.cycleLength)
        assertEquals(6, restored?.cycleSettings?.periodLength)
    }

    @Test
    fun `cycle data counts towards the import total`() {
        val original = snapshot(
            cycleDays = listOf(CycleDay(id = "d1", pairId = "pair", dateMillis = 1L)),
            cycleSettings = CycleSettings(id = "pair", pairId = "pair")
        )

        assertEquals(2, original.total)
        assertEquals(2, PairBackup.parse(PairBackup.export(original))?.total)
    }

    @Test
    fun `old file without cycle data imports with empty defaults`() {
        val raw = """
            {
              "version": 1,
              "events": [
                {"id": "e1", "pairId": "p1", "title": "Годовщина", "dateMillis": 1650000000000, "annual": true, "updatedAt": 1700000000001}
              ]
            }
        """.trimIndent()

        val restored = PairBackup.parse(raw)

        assertEquals(emptyList(), restored?.cycleDays)
        assertNull(restored?.cycleSettings)
        assertEquals(1, restored?.events?.size)
    }

    @Test
    fun `cycle records tolerate missing fields`() {
        val raw = """
            {
              "version": 1,
              "cycleDays": [
                {"id": "d1"}
              ],
              "cycleSettings": {
                "id": "pair"
              }
            }
        """.trimIndent()

        val restored = PairBackup.parse(raw)

        val day = restored?.cycleDays?.single()
        assertEquals("", day?.pairId)
        assertEquals(0L, day?.dateMillis)
        assertEquals(false, day?.period)
        assertEquals(Flow.NONE, day?.flow)
        assertEquals(emptyList(), day?.symptoms)
        assertEquals("", day?.mood)
        assertNull(day?.weight)
        assertNull(day?.deletedAt)

        val settings = restored?.cycleSettings
        assertEquals(28, settings?.cycleLength)
        assertEquals(5, settings?.periodLength)
        assertEquals("", settings?.pairId)
        assertNull(settings?.deletedAt)
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