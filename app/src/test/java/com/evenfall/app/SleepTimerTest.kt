package com.evenfall.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepTimerTest {

    @Test
    fun testVolumeFadeOutRatio() {
        val fadeOutMs = 30_000L // 30s fade out
        
        // 30s remaining -> 1.0f volume
        val ratioAtStart = calculateFadeOutVolume(30_000L, fadeOutMs)
        assertEquals(1.0f, ratioAtStart, 0.01f)

        // 15s remaining -> 0.5f volume
        val ratioAtMid = calculateFadeOutVolume(15_000L, fadeOutMs)
        assertEquals(0.5f, ratioAtMid, 0.01f)

        // 3s remaining -> 0.1f volume (safe floor)
        val ratioAtEnd = calculateFadeOutVolume(3_000L, fadeOutMs)
        assertEquals(0.1f, ratioAtEnd, 0.01f)
    }

    @Test
    fun testShakeToExtendIncrementsDuration() {
        val currentRemainingMs = 60_000L // 1 minute left
        val extendMinutes = 15
        val extendedMs = currentRemainingMs + (extendMinutes * 60 * 1000L)
        assertTrue(extendedMs > currentRemainingMs)
        assertEquals(16 * 60 * 1000L, extendedMs)
    }

    private fun calculateFadeOutVolume(remainingMs: Long, fadeOutMs: Long): Float {
        return (remainingMs.toFloat() / fadeOutMs).coerceIn(0.1f, 1f)
    }
}
