package com.evenfall.app

import com.evenfall.app.playback.SmartResumeConfig
import com.evenfall.app.playback.SmartResumeTiers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartResumeTest {

    @Test
    fun testDefaultTiersCalculation() {
        val config = SmartResumeConfig()

        // < 1 min: 0s
        assertEquals(0L, SmartResumeTiers.calculateRewindMs(30_000L, config))
        assertEquals(0L, SmartResumeTiers.calculateRewindMs(59_999L, config))

        // 1 - 10 min: 5s
        assertEquals(5_000L, SmartResumeTiers.calculateRewindMs(60_000L, config))
        assertEquals(5_000L, SmartResumeTiers.calculateRewindMs(599_999L, config))

        // 10 min - 1 hour: 15s
        assertEquals(15_000L, SmartResumeTiers.calculateRewindMs(600_000L, config))
        assertEquals(15_000L, SmartResumeTiers.calculateRewindMs(3_599_999L, config))

        // 1 hour - 24 hours: 30s
        assertEquals(30_000L, SmartResumeTiers.calculateRewindMs(3_600_000L, config))
        assertEquals(30_000L, SmartResumeTiers.calculateRewindMs(86_399_999L, config))

        // > 24 hours: 60s
        assertEquals(60_000L, SmartResumeTiers.calculateRewindMs(86_400_000L, config))
        assertEquals(60_000L, SmartResumeTiers.calculateRewindMs(100_000_000L, config))
    }

    @Test
    fun testDisabledSmartResume() {
        val config = SmartResumeConfig(enabled = false)
        assertEquals(0L, SmartResumeTiers.calculateRewindMs(500_000L, config))
        assertEquals(0L, SmartResumeTiers.calculateRewindMs(90_000_000L, config))
    }

    @Test
    fun testCustomTiers() {
        val customConfig = SmartResumeConfig(
            enabled = true,
            tier1Sec = 2,
            tier2Sec = 8,
            tier3Sec = 20,
            tier4Sec = 45,
            tier5Sec = 90
        )
        assertEquals(2_000L, SmartResumeTiers.calculateRewindMs(10_000L, customConfig))
        assertEquals(8_000L, SmartResumeTiers.calculateRewindMs(120_000L, customConfig))
        assertEquals(20_000L, SmartResumeTiers.calculateRewindMs(1_200_000L, customConfig))
        assertEquals(45_000L, SmartResumeTiers.calculateRewindMs(7_200_000L, customConfig))
        assertEquals(90_000L, SmartResumeTiers.calculateRewindMs(90_000_000L, customConfig))
    }

    @Test
    fun testIsLongGap() {
        assertFalse(SmartResumeTiers.isLongGap(10_000L))
        assertFalse(SmartResumeTiers.isLongGap(86_399_999L))
        assertTrue(SmartResumeTiers.isLongGap(86_400_000L))
        assertTrue(SmartResumeTiers.isLongGap(100_000_000L))
    }
}
