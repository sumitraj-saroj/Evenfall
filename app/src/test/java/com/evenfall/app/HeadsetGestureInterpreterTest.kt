package com.evenfall.app

import com.evenfall.app.playback.HeadsetGesture
import com.evenfall.app.playback.HeadsetGestureInterpreter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeadsetGestureInterpreterTest {

    @Test
    fun testSinglePress() {
        val detected = mutableListOf<HeadsetGesture>()
        val interpreter = HeadsetGestureInterpreter(scope = null) { gesture ->
            detected.add(gesture)
        }

        // Tap once: down at 1000, up at 1100
        interpreter.onKeyDown(1000L)
        interpreter.onKeyUp(1100L)
        // Timeout passes
        interpreter.dispatchClicks()

        assertEquals(1, detected.size)
        assertEquals(HeadsetGesture.SINGLE_PRESS, detected[0])
    }

    @Test
    fun testDoublePress() {
        val detected = mutableListOf<HeadsetGesture>()
        val interpreter = HeadsetGestureInterpreter(scope = null) { gesture ->
            detected.add(gesture)
        }

        // First click
        interpreter.onKeyDown(1000L)
        interpreter.onKeyUp(1080L)

        // Second click within multi-press window
        interpreter.onKeyDown(1200L)
        interpreter.onKeyUp(1280L)

        // Timeout passes
        interpreter.dispatchClicks()

        assertEquals(1, detected.size)
        assertEquals(HeadsetGesture.DOUBLE_PRESS, detected[0])
    }

    @Test
    fun testTriplePressImmediateDispatch() {
        val detected = mutableListOf<HeadsetGesture>()
        val interpreter = HeadsetGestureInterpreter(scope = null) { gesture ->
            detected.add(gesture)
        }

        // First click
        interpreter.onKeyDown(1000L)
        interpreter.onKeyUp(1080L)

        // Second click
        interpreter.onKeyDown(1150L)
        interpreter.onKeyUp(1220L)

        // Third click
        interpreter.onKeyDown(1300L)
        interpreter.onKeyUp(1370L)

        // Triple press is dispatched immediately on 3rd key-up
        assertEquals(1, detected.size)
        assertEquals(HeadsetGesture.TRIPLE_PRESS, detected[0])
    }

    @Test
    fun testLongPress() {
        val detected = mutableListOf<HeadsetGesture>()
        val interpreter = HeadsetGestureInterpreter(scope = null, longPressThresholdMs = 500L) { gesture ->
            detected.add(gesture)
        }

        // Hold button for 600ms
        interpreter.onKeyDown(1000L)
        interpreter.onKeyUp(1600L)

        assertEquals(1, detected.size)
        assertEquals(HeadsetGesture.LONG_PRESS, detected[0])
        assertTrue(interpreter.isLongPressDispatched)
    }

    @Test
    fun testResetClearsPendingState() {
        val detected = mutableListOf<HeadsetGesture>()
        val interpreter = HeadsetGestureInterpreter(scope = null) { gesture ->
            detected.add(gesture)
        }

        interpreter.onKeyDown(1000L)
        interpreter.onKeyUp(1080L)
        interpreter.reset()
        interpreter.dispatchClicks()

        assertEquals(0, detected.size)
    }
}
