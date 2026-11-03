package com.evenfall.app

import com.evenfall.app.ui.util.EvenfallHaptics
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HapticsTest {

    @Test
    fun testHapticsDisabledByUserReturnsFalse() {
        val haptics = EvenfallHaptics(
            context = null,
            view = null,
            isHapticsEnabledByUser = false
        )
        assertFalse(haptics.canVibrate())
    }

    @Test
    fun testHapticsEnabledByUserWhenSystemDefaultIsTrue() {
        val haptics = EvenfallHaptics(
            context = null,
            view = null,
            isHapticsEnabledByUser = true
        )
        assertTrue(haptics.canVibrate())
    }
}
