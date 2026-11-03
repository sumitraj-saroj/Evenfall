package com.evenfall.app.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HeadsetGestureInterpreter(
    private val scope: CoroutineScope? = null,
    val multiPressTimeoutMs: Long = 350L,
    val longPressThresholdMs: Long = 500L,
    private val onGesture: (HeadsetGesture) -> Unit
) {
    private var downTimestamp: Long = 0L
    private var clickCount: Int = 0
    private var pendingClickJob: Job? = null
    var isLongPressDispatched: Boolean = false
        private set

    fun onKeyDown(eventTimeMs: Long) {
        downTimestamp = eventTimeMs
        isLongPressDispatched = false
        pendingClickJob?.cancel()
    }

    fun onKeyUp(eventTimeMs: Long) {
        val pressDuration = eventTimeMs - downTimestamp
        if (pressDuration >= longPressThresholdMs) {
            isLongPressDispatched = true
            clickCount = 0
            pendingClickJob?.cancel()
            onGesture(HeadsetGesture.LONG_PRESS)
            return
        }

        clickCount++
        if (clickCount >= 3) {
            clickCount = 0
            pendingClickJob?.cancel()
            onGesture(HeadsetGesture.TRIPLE_PRESS)
            return
        }

        if (scope != null) {
            pendingClickJob?.cancel()
            pendingClickJob = scope.launch {
                delay(multiPressTimeoutMs)
                dispatchClicks()
            }
        }
    }

    fun dispatchClicks() {
        if (clickCount == 0) return
        val countToDispatch = clickCount
        clickCount = 0
        pendingClickJob?.cancel()
        when (countToDispatch) {
            1 -> onGesture(HeadsetGesture.SINGLE_PRESS)
            2 -> onGesture(HeadsetGesture.DOUBLE_PRESS)
            else -> onGesture(HeadsetGesture.TRIPLE_PRESS)
        }
    }

    fun reset() {
        downTimestamp = 0L
        clickCount = 0
        isLongPressDispatched = false
        pendingClickJob?.cancel()
    }
}
