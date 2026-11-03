package com.evenfall.app.playback

object AutoMarkFinishedEvaluator {
    const val FINISH_THRESHOLD_RATIO = 0.98

    /**
     * Determines whether playback has met criteria to automatically mark a book as finished:
     * 1. Setting `isAutoMarkEnabled` must be true.
     * 2. Book must not already be marked finished.
     * 3. Either playback reached >= 98% of total book duration OR the last chapter ended.
     */
    fun shouldAutoMarkFinished(
        totalPositionMs: Long,
        totalDurationMs: Long,
        isLastChapterEnded: Boolean,
        isAutoMarkEnabled: Boolean,
        isAlreadyFinished: Boolean
    ): Boolean {
        if (!isAutoMarkEnabled) return false
        if (isAlreadyFinished) return false
        if (isLastChapterEnded) return true
        if (totalDurationMs <= 0L) return false

        val progressRatio = totalPositionMs.toDouble() / totalDurationMs.toDouble()
        return progressRatio >= (FINISH_THRESHOLD_RATIO - 1e-7)
    }
}
