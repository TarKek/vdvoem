package app.vdvoem.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class SharedTaskTest {

    @Test
    fun `create trims content and initializes sync metadata`() {
        val task = SharedTask.create(
            id = "task",
            pairId = "pair",
            createdBy = "device",
            title = "  Купить молоко  ",
            note = "  Обезжиренное  ",
            dueAt = 123L,
            now = 100L
        )

        assertEquals("Купить молоко", task.title)
        assertEquals("Обезжиренное", task.note)
        assertEquals(123L, task.dueAt)
        assertEquals(100L, task.createdAt)
        assertEquals(100L, task.updatedAt)
        assertEquals(1L, task.version)
        assertEquals(false, task.isCompleted)
        assertEquals(null, task.deletedAt)
    }

    @Test
    fun `edit increments version and never moves updatedAt backwards`() {
        val original = task()
        val edited = original.edit(title = "Новое название", note = "заметка", dueAt = null, now = 50L)

        assertEquals(original.updatedAt + 1, edited.updatedAt)
        assertEquals(original.version + 1, edited.version)
        assertEquals(original.createdAt, edited.createdAt)
        assertEquals(original.createdBy, edited.createdBy)
    }

    @Test
    fun `unchanged edit and repeated completion are no ops`() {
        val original = task()
        val same = original.edit(original.title, original.note, original.dueAt, now = 200L)
        val completed = original.withCompletion(completed = true, now = 200L)

        assertSame(original, same)
        assertSame(completed, completed.withCompletion(completed = true, now = 300L))
        assertEquals(200L, completed.updatedAt)
        assertEquals(2L, completed.version)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank title is rejected`() {
        SharedTask.create("id", "pair", "device", "   ", "", null, 1L)
    }

    private fun task() = SharedTask(
        id = "task",
        pairId = "pair",
        title = "Дело",
        note = "",
        dueAt = 10L,
        createdBy = "device",
        createdAt = 90L,
        updatedAt = 100L,
        version = 1L
    )
}
