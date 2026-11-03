package com.evenfall.app.playback

import com.evenfall.app.data.db.entity.ListeningSessionEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class StreakResult(
    val currentStreak: Int,
    val longestStreak: Int
)

object ListeningStatsCalculator {

    /**
     * Minimum listening duration on a calendar day to qualify as an active streak day (1 minute).
     */
    const val MIN_DAILY_DURATION_FOR_STREAK_MS = 60_000L

    /**
     * Calculates current and longest streaks based on calendar days in the given timezone.
     */
    fun calculateStreaks(
        sessions: List<ListeningSessionEntity>,
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): StreakResult {
        if (sessions.isEmpty()) {
            return StreakResult(currentStreak = 0, longestStreak = 0)
        }

        // Aggregate duration per local calendar date
        val dailyDurations = mutableMapOf<LocalDate, Long>()
        for (session in sessions) {
            val date = Instant.ofEpochMilli(session.timestamp).atZone(zoneId).toLocalDate()
            val existing = dailyDurations.getOrDefault(date, 0L)
            dailyDurations[date] = existing + session.listenedDurationMs
        }

        // Filter dates with at least the minimum required listening time
        val activeDates = dailyDurations
            .filter { it.value >= MIN_DAILY_DURATION_FOR_STREAK_MS }
            .keys
            .sorted()

        if (activeDates.isEmpty()) {
            return StreakResult(currentStreak = 0, longestStreak = 0)
        }

        // Calculate longest streak across history
        var maxStreak = 1
        var runningStreak = 1
        for (i in 1 until activeDates.size) {
            val prev = activeDates[i - 1]
            val curr = activeDates[i]
            if (ChronoUnit.DAYS.between(prev, curr) == 1L) {
                runningStreak++
                if (runningStreak > maxStreak) {
                    maxStreak = runningStreak
                }
            } else {
                runningStreak = 1
            }
        }

        // Calculate current streak
        val lastActiveDate = activeDates.last()
        val daysSinceLastActive = ChronoUnit.DAYS.between(lastActiveDate, today)

        val currentStreak = when {
            // User listened today
            daysSinceLastActive == 0L -> {
                var streak = 1
                for (i in activeDates.size - 2 downTo 0) {
                    val expected = activeDates[i + 1].minusDays(1)
                    if (activeDates[i] == expected) {
                        streak++
                    } else {
                        break
                    }
                }
                streak
            }
            // User listened yesterday (streak is still alive today)
            daysSinceLastActive == 1L -> {
                var streak = 1
                for (i in activeDates.size - 2 downTo 0) {
                    val expected = activeDates[i + 1].minusDays(1)
                    if (activeDates[i] == expected) {
                        streak++
                    } else {
                        break
                    }
                }
                streak
            }
            // Gap of 2 or more days
            else -> 0
        }

        return StreakResult(
            currentStreak = currentStreak,
            longestStreak = maxStreak
        )
    }

    /**
     * Aggregates listening duration for a specific list of calendar dates (e.g. past 7 days).
     */
    fun aggregateDurationsForDates(
        sessions: List<ListeningSessionEntity>,
        dates: List<LocalDate>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Map<LocalDate, Long> {
        val result = dates.associateWith { 0L }.toMutableMap()
        for (session in sessions) {
            val date = Instant.ofEpochMilli(session.timestamp).atZone(zoneId).toLocalDate()
            if (result.containsKey(date)) {
                result[date] = (result[date] ?: 0L) + session.listenedDurationMs
            }
        }
        return result
    }

    /**
     * Calculates time saved from speed and silence skipping.
     */
    fun calculateTimeSavedMs(listenedDurationMs: Long, contentDurationMs: Long): Long {
        return (contentDurationMs - listenedDurationMs).coerceAtLeast(0L)
    }
}
