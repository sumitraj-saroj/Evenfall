package com.evenfall.app

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackLogicTest {

    @Test
    fun testAutoRewindThresholds() {
        // Under 5s: no rewind
        assertEquals(0L, calculateAutoRewind(3000L))

        // 30s pause: 3s rewind
        assertEquals(3000L, calculateAutoRewind(30_000L))

        // 5 min pause: 8s rewind
        assertEquals(8000L, calculateAutoRewind(300_000L))

        // 30 min pause: 15s rewind
        assertEquals(15_000L, calculateAutoRewind(1800_000L))

        // 2 hour pause: 25s rewind
        assertEquals(25_000L, calculateAutoRewind(7200_000L))
    }

    private fun calculateAutoRewind(pausedDurationMs: Long): Long {
        return when {
            pausedDurationMs < 5_000L -> 0L
            pausedDurationMs < 60_000L -> 3_000L
            pausedDurationMs < 600_000L -> 8_000L
            pausedDurationMs < 3_600_000L -> 15_000L
            else -> 25_000L
        }
    }
}
