package com.evenfall.app.playback

data class SessionRecord(
    val bookId: Long,
    val timestamp: Long,
    val listenedDurationMs: Long,
    val contentDurationMs: Long
)

class ListeningSessionTracker(
    private var statsEnabled: Boolean = true
) {
    private var currentBookId: Long? = null
    private var isPlaying: Boolean = false
    private var lastWallClockMs: Long = 0L
    private var lastContentPositionMs: Long = 0L

    private var sessionStartTimestamp: Long = 0L
    private var accumulatedListenedMs: Long = 0L
    private var accumulatedContentMs: Long = 0L

    fun setStatsEnabled(enabled: Boolean) {
        statsEnabled = enabled
        if (!enabled) {
            resetAccumulator()
            currentBookId = null
            isPlaying = false
        }
    }

    fun isEnabled(): Boolean = statsEnabled

    fun onPlay(nowMs: Long, bookId: Long, positionMs: Long): SessionRecord? {
        if (!statsEnabled) return null

        var previousFlush: SessionRecord? = null
        if (currentBookId != null && currentBookId != bookId) {
            previousFlush = flush()
        }

        currentBookId = bookId
        isPlaying = true
        lastWallClockMs = nowMs
        lastContentPositionMs = positionMs
        if (sessionStartTimestamp == 0L) {
            sessionStartTimestamp = nowMs
        }

        return previousFlush
    }

    fun onTick(nowMs: Long, positionMs: Long): SessionRecord? {
        if (!statsEnabled || !isPlaying || currentBookId == null) return null

        val deltaWall = nowMs - lastWallClockMs
        if (deltaWall <= 0) return null

        val deltaContent = positionMs - lastContentPositionMs

        // Maximum expected multiplier in 1 tick (e.g. 3.0x speed + silence skip tolerance)
        val maxReasonableMultiplier = 5.0
        val maxExpectedDeltaContent = (deltaWall * maxReasonableMultiplier).toLong() + 500L

        if (deltaContent < 0 || deltaContent > maxExpectedDeltaContent) {
            // Discontinuity / seek occurred between ticks without explicit seek notification
            lastContentPositionMs = positionMs
            lastWallClockMs = nowMs
            return null
        }

        accumulatedListenedMs += deltaWall
        accumulatedContentMs += deltaContent

        lastWallClockMs = nowMs
        lastContentPositionMs = positionMs

        // Auto-checkpoint every 30 seconds of accumulated real-time listening
        if (accumulatedListenedMs >= 30_000L) {
            return flush()
        }

        return null
    }

    fun onSeek(nowMs: Long, newPositionMs: Long) {
        if (!statsEnabled) return

        if (isPlaying && lastWallClockMs > 0) {
            val deltaWall = nowMs - lastWallClockMs
            // If there was a tiny continuous interval before the seek was triggered
            if (deltaWall in 1..2000) {
                accumulatedListenedMs += deltaWall
            }
        }

        lastWallClockMs = nowMs
        lastContentPositionMs = newPositionMs
    }

    fun onPause(nowMs: Long, positionMs: Long): SessionRecord? {
        if (!statsEnabled || !isPlaying || currentBookId == null) {
            isPlaying = false
            return null
        }

        val deltaWall = nowMs - lastWallClockMs
        if (deltaWall in 1..5000) {
            val deltaContent = positionMs - lastContentPositionMs
            if (deltaContent in 0..(deltaWall * 5)) {
                accumulatedListenedMs += deltaWall
                accumulatedContentMs += deltaContent
            }
        }

        isPlaying = false
        lastWallClockMs = nowMs
        lastContentPositionMs = positionMs

        return flush()
    }

    fun flush(minDurationMs: Long = 1000L): SessionRecord? {
        if (!statsEnabled) {
            resetAccumulator()
            return null
        }

        val bookId = currentBookId ?: return null
        if (accumulatedListenedMs < minDurationMs) {
            return null
        }

        val record = SessionRecord(
            bookId = bookId,
            timestamp = if (sessionStartTimestamp > 0) sessionStartTimestamp else System.currentTimeMillis(),
            listenedDurationMs = accumulatedListenedMs,
            contentDurationMs = accumulatedContentMs
        )

        resetAccumulator()
        return record
    }

    private fun resetAccumulator() {
        accumulatedListenedMs = 0L
        accumulatedContentMs = 0L
        sessionStartTimestamp = lastWallClockMs
    }
}
