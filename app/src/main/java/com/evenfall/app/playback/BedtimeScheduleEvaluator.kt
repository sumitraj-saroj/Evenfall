package com.evenfall.app.playback

import java.util.Calendar

data class BedtimeConfig(
    val enabled: Boolean = false,
    val startHour: Int = 22,
    val startMinute: Int = 0,
    val durationMinutes: Int = 30, // -1 for end of chapter
    val daysOfWeek: Set<Int> = setOf(
        Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY,
        Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
    ),
    val autoStart: Boolean = true // false = show prompt
)

object BedtimeScheduleEvaluator {
    /**
     * Determines whether [nowMillis] falls within the active bedtime window.
     * The bedtime window starts at [startHour]:[startMinute] and lasts [windowDurationMinutes]
     * (default 180 min / 3 hours). Handles midnight crossover properly.
     */
    fun isWithinBedtimeWindow(
        nowMillis: Long,
        config: BedtimeConfig,
        windowDurationMinutes: Int = 180
    ): Boolean {
        if (!config.enabled) return false

        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val currentMinutesOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        val windowStart = config.startHour * 60 + config.startMinute
        val windowEnd = windowStart + windowDurationMinutes

        if (windowEnd < 24 * 60) {
            // Window stays on same calendar day
            if (currentDayOfWeek !in config.daysOfWeek) return false
            return currentMinutesOfDay in windowStart..windowEnd
        } else {
            // Window crosses midnight into the next day
            if (currentMinutesOfDay >= windowStart) {
                // Before midnight: must be in active days
                return currentDayOfWeek in config.daysOfWeek
            } else if (currentMinutesOfDay <= (windowEnd % (24 * 60))) {
                // After midnight: must match previous day's active setting
                val prevDay = if (currentDayOfWeek == Calendar.SUNDAY) Calendar.SATURDAY else currentDayOfWeek - 1
                return prevDay in config.daysOfWeek
            }
            return false
        }
    }
}
