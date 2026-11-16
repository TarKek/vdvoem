package app.vdvoem.core.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Решение о паузе между тиками «живой» синхронизации — без корутин и Android. */
class RealtimeSyncTest {

    @Test
    fun `successful pass waits base interval`() {
        assertEquals(
            REALTIME_BASE_INTERVAL_MS,
            nextDelayMs(lastResultFailed = false)
        )
    }

    @Test
    fun `failed pass waits failure backoff`() {
        assertEquals(
            REALTIME_FAILURE_BACKOFF_MS,
            nextDelayMs(lastResultFailed = true)
        )
    }

    @Test
    fun `backoff is longer than base interval`() {
        assertTrue(REALTIME_FAILURE_BACKOFF_MS > REALTIME_BASE_INTERVAL_MS)
    }

    @Test
    fun `base interval is realtime-ish but not a busy loop`() {
        assertTrue(REALTIME_BASE_INTERVAL_MS in 10_000L..15_000L)
    }

    @Test
    fun `custom intervals are respected`() {
        assertEquals(1_000L, nextDelayMs(false, baseIntervalMs = 1_000L, failureBackoffMs = 5_000L))
        assertEquals(5_000L, nextDelayMs(true, baseIntervalMs = 1_000L, failureBackoffMs = 5_000L))
    }
}