package app.vdvoem.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json

class TaskDtoTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `legacy changes without tasks decode to an empty list`() {
        val changes = json.decodeFromString<ChangesDto>("""{"events":[]}""")

        assertEquals(emptyList(), changes.tasks)
    }

    @Test
    fun `task keeps optional due date and explicit clear`() {
        val withDue = json.decodeFromString<TaskDto>(
            """{"id":"t1","title":"Купить продукты","dueAt":1762086400000,"isCompleted":true}"""
        )
        val withoutDue = json.decodeFromString<TaskDto>(
            """{"id":"t2","title":"Позвонить","dueAt":null}"""
        )

        assertEquals(1_762_086_400_000L, withDue.dueAt)
        assertEquals(true, withDue.isCompleted)
        assertNull(withoutDue.dueAt)
    }
}