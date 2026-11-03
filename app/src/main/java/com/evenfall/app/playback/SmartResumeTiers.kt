package com.evenfall.app.playback

data class SmartResumeConfig(
    val enabled: Boolean = true,
    val tier1Sec: Int = 0,   // < 1 min: 0s
    val tier2Sec: Int = 5,   // 1–10 min: 5s
    val tier3Sec: Int = 15,  // 10 min–1 h: 15s
    val tier4Sec: Int = 30,  // 1 h–24 h: 30s
    val tier5Sec: Int = 60   // > 24 h: 60s
)

object SmartResumeTiers {
    const val ONE_MINUTE_MS = 60_000L
    const val TEN_MINUTES_MS = 600_000L
    const val ONE_HOUR_MS = 3_600_000L
    const val ONE_DAY_MS = 86_400_000L

    fun calculateRewindMs(pausedDurationMs: Long, config: SmartResumeConfig = SmartResumeConfig()): Long {
        if (!config.enabled || pausedDurationMs <= 0L) return 0L

        return when {
            pausedDurationMs < ONE_MINUTE_MS -> config.tier1Sec * 1000L
            pausedDurationMs < TEN_MINUTES_MS -> config.tier2Sec * 1000L
            pausedDurationMs < ONE_HOUR_MS -> config.tier3Sec * 1000L
            pausedDurationMs < ONE_DAY_MS -> config.tier4Sec * 1000L
            else -> config.tier5Sec * 1000L
        }
    }

    fun isLongGap(pausedDurationMs: Long, thresholdMs: Long = ONE_DAY_MS): Boolean {
        return pausedDurationMs >= thresholdMs
    }
}
