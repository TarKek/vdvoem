package app.vdvoem.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json

class LessonDtoTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `missing textbookBy means legacy client`() {
        val lesson = json.decodeFromString<LessonDto>("""{"id":"lesson-1"}""")

        assertNull(lesson.textbookBy)
    }

    @Test
    fun `empty textbookBy means explicit clear`() {
        val lesson = json.decodeFromString<LessonDto>("""{"id":"lesson-1","textbookBy":""}""")

        assertEquals("", lesson.textbookBy)
    }
}
