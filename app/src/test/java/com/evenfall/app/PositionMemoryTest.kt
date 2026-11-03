package com.evenfall.app

import com.evenfall.app.playback.PlaybackState
import org.junit.Assert.assertEquals
import org.junit.Test

class PositionMemoryTest {

    @Test
    fun testProgressFractionCalculation() {
        val state = PlaybackState(
            totalPositionMs = 7200_000L,
            totalDurationMs = 10000_000L,
            chapterPositionMs = 600_000L,
            chapterDurationMs = 1200_000L
        )

        assertEquals(0.72f, state.totalProgressFraction, 0.001f)
        assertEquals(72, state.totalProgressPercent)
        assertEquals(0.5f, state.chapterProgressFraction, 0.001f)
        assertEquals(2800_000L, state.totalTimeRemainingMs)
        assertEquals(600_000L, state.chapterTimeRemainingMs)
    }

    @Test
    fun testZeroDurationSafelyHandled() {
        val state = PlaybackState(
            totalPositionMs = 0L,
            totalDurationMs = 0L,
            chapterPositionMs = 0L,
            chapterDurationMs = 0L
        )

        assertEquals(0f, state.totalProgressFraction, 0.001f)
        assertEquals(0, state.totalProgressPercent)
        assertEquals(0f, state.chapterProgressFraction, 0.001f)
        assertEquals(0L, state.totalTimeRemainingMs)
        assertEquals(0L, state.chapterTimeRemainingMs)
    }

    @Test
    fun testMiniPlayerDismissedState() {
        val state = PlaybackState(isMiniPlayerDismissed = false)
        org.junit.Assert.assertFalse(state.isMiniPlayerDismissed)

        val dismissedState = state.copy(isMiniPlayerDismissed = true)
        org.junit.Assert.assertTrue(dismissedState.isMiniPlayerDismissed)
    }
}
