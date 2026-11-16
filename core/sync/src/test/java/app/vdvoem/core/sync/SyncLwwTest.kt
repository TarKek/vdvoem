package app.vdvoem.core.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Правила last-write-wins и водяной знак отправки — сердце синхронизации. */
class SyncLwwTest {

    // --- shouldApplyRemote ---

    @Test
    fun `missing local record is always applied`() {
        assertTrue(shouldApplyRemote(localUpdatedAt = null, localDeletedAt = null, remoteUpdatedAt = 10))
    }

    @Test
    fun `newer remote record wins`() {
        assertTrue(shouldApplyRemote(localUpdatedAt = 10, localDeletedAt = null, remoteUpdatedAt = 11))
    }

    @Test
    fun `older remote record is ignored`() {
        assertFalse(shouldApplyRemote(localUpdatedAt = 10, localDeletedAt = null, remoteUpdatedAt = 9))
    }

    @Test
    fun `equal timestamps keep local copy`() {
        assertFalse(shouldApplyRemote(localUpdatedAt = 10, localDeletedAt = null, remoteUpdatedAt = 10))
    }

    @Test
    fun `locally deleted record is not resurrected`() {
        assertFalse(
            shouldApplyRemote(localUpdatedAt = 10, localDeletedAt = 10, remoteUpdatedAt = 99)
        )
    }

    @Test
    fun `remote tombstone is applied as a normal newer record`() {
        // Локальная запись жива, сервер прислал более свежий tombstone — удаление применяется.
        assertTrue(shouldApplyRemote(localUpdatedAt = 10, localDeletedAt = null, remoteUpdatedAt = 20))
    }

    // --- maxUpdatedAt ---

    @Test
    fun `watermark ignores empty changes`() {
        assertEquals(0L, ChangesDto().maxUpdatedAt())
    }

    @Test
    fun `watermark takes the newest record of any type`() {
        val changes = ChangesDto(
            pair = PairDto(id = "pair", updatedAt = 5),
            events = listOf(EventDto(id = "e1", title = "e1", dateMillis = 0, updatedAt = 40)),
            wishes = listOf(WishDto(id = "w1", title = "w1", updatedAt = 12)),
            gifts = listOf(GiftDto(id = "g1", title = "g1", updatedAt = 7)),
            pokes = listOf(PokeDto(id = "p1", text = "hi", updatedAt = 33)),
            quizScores = listOf(QuizScoreDto(id = "q1", quizId = "compatibility", updatedAt = 21)),
            lessons = listOf(LessonDto(id = "l1", updatedAt = 18))
        )
        assertEquals(40L, changes.maxUpdatedAt())
    }

    @Test
    fun `size counts every entity kind`() {
        val empty = ChangesDto()
        assertEquals(0, empty.size)
        assertTrue(empty.isEmpty)

        val full = ChangesDto(
            pair = PairDto(id = "pair"),
            events = listOf(EventDto(id = "e1", title = "e1", dateMillis = 0)),
            wishes = listOf(WishDto(id = "w1", title = "w1")),
            gifts = listOf(GiftDto(id = "g1", title = "g1")),
            pokes = listOf(PokeDto(id = "p1", text = "hi")),
            quizScores = listOf(QuizScoreDto(id = "q1", quizId = "x")),
            lessons = listOf(LessonDto(id = "l1"))
        )
        assertEquals(7, full.size)
        assertFalse(full.isEmpty)
    }
}