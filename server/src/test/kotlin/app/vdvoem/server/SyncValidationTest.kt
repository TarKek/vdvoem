package app.vdvoem.server

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Регрессии по багам, найденным на проде 28.11.2026:
 * битое тело синка отвечало 500 вместо 400, а `tasks` не проходили валидацию
 * (не-UUID id давал 500, а «время из будущего» принималось без ограничения).
 */
class SyncValidationTest {
    private val pairId = UUID.randomUUID().toString()
    private val deviceId = UUID.randomUUID().toString()
    private val now = 1_795_867_000_000L

    private fun changes(block: ChangesDto.() -> ChangesDto) = ChangesDto().block()

    @Test
    fun `broken json body is not parsed instead of throwing`() {
        assertNull(parsePushRequest("{bad"))
        assertNull(parsePushRequest(""))
        assertNull(parsePushRequest("[]"))
    }

    @Test
    fun `body without changes is rejected politely`() {
        // MissingFieldException раньше доходил до StatusPages как 500.
        assertNull(parsePushRequest("{}"))
        assertNull(parsePushRequest("""{"baseTime":0}"""))
    }

    @Test
    fun `valid body is parsed with diary and tasks`() {
        val entryId = UUID.randomUUID().toString()
        val raw = """
            {"baseTime":0,"changes":{
              "diaryEntries":[{"id":"$entryId","title":"день","updatedAt":$now}],
              "tasks":[{"id":"${UUID.randomUUID()}","title":"дело","updatedAt":$now}]
            }}
        """.trimIndent()

        val request = parsePushRequest(raw)

        assertNotNull(request)
        assertEquals(entryId, request.changes.diaryEntries.single().id)
        assertEquals(1, request.changes.tasks.size)
    }

    @Test
    fun `task with non uuid id is a client error not a crash`() {
        val error = validateSyncRequest(
            pairId,
            deviceId,
            changes { copy(tasks = listOf(TaskDto(id = "t1", title = "плохой", updatedAt = now))) }
        )

        assertEquals("invalid entity id", error)
    }

    @Test
    fun `task updated in the far future is rejected`() {
        val error = validateSyncRequest(
            pairId,
            deviceId,
            changes {
                copy(
                    tasks = listOf(
                        TaskDto(id = UUID.randomUUID().toString(), title = "из будущего", updatedAt = 99_999_999_999_999L)
                    )
                )
            }
        )

        assertEquals("invalid updatedAt", error)
    }

    @Test
    fun `task with negative updatedAt is rejected`() {
        val error = validateSyncRequest(
            pairId,
            deviceId,
            changes {
                copy(tasks = listOf(TaskDto(id = UUID.randomUUID().toString(), title = "минус", updatedAt = -1L)))
            }
        )

        assertEquals("invalid updatedAt", error)
    }

    @Test
    fun `task inside the clock skew window is accepted`() {
        val error = validateSyncRequest(
            pairId,
            deviceId,
            changes {
                copy(tasks = listOf(TaskDto(id = UUID.randomUUID().toString(), title = "ок", updatedAt = now)))
            }
        )

        assertNull(error)
    }

    @Test
    fun `task ids count towards the sync payload limit`() {
        val many = (0..MAX_SYNC_CHANGES_TEST).map {
            TaskDto(id = UUID.randomUUID().toString(), title = "t$it", updatedAt = now)
        }

        val error = validateSyncRequest(pairId, deviceId, changes { copy(tasks = many) })

        assertEquals("too many changes", error)
    }

    private companion object {
        /** Тот же предел, что MAX_SYNC_CHANGES в Routes.kt. */
        const val MAX_SYNC_CHANGES_TEST = 5_000
    }
}