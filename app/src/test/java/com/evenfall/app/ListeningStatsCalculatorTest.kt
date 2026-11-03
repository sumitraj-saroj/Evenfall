package com.evenfall.app

import com.evenfall.app.data.db.entity.ListeningSessionEntity
import com.evenfall.app.playback.ListeningStatsCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ListeningStatsCalculatorTest {

    private val zoneId = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 11, 3)

    private fun createSession(date: LocalDate, durationMs: Long): ListeningSessionEntity {
        val timestamp = date.atStartOfDay(zoneId).toInstant().toEpochMilli() + 3600_000L // 1:00 AM UTC
        return ListeningSessionEntity(
            bookId = 1L,
            timestamp = timestamp,
            listenedDurationMs = durationMs,
            contentDurationMs = durationMs
        )
    }

    @Test
    fun testEmptySessionsGivesZeroStreaks() {
        val result = ListeningStatsCalculator.calculateStreaks(emptyList(), today, zoneId)
        assertEquals(0, result.currentStreak)
        assertEquals(0, result.longestStreak)
    }

    @Test
    fun testActiveStreakEndingToday() {
        val sessions = listOf(
            createSession(LocalDate.of(2026, 11, 1), 120_000L),
            createSession(LocalDate.of(2026, 11, 2), 120_000L),
            createSession(LocalDate.of(2026, 11, 3), 120_000L)
        )
        val result = ListeningStatsCalculator.calculateStreaks(sessions, today, zoneId)
        assertEquals(3, result.currentStreak)
        assertEquals(3, result.longestStreak)
    }

    @Test
    fun testActiveStreakEndingYesterdayStillActiveToday() {
        // User listened on Oct 31, Nov 1, Nov 2, but hasn't listened yet today (Nov 3)
        val sessions = listOf(
            createSession(LocalDate.of(2026, 10, 31), 120_000L),
            createSession(LocalDate.of(2026, 11, 1), 120_000L),
            createSession(LocalDate.of(2026, 11, 2), 120_000L)
        )
        val result = ListeningStatsCalculator.calculateStreaks(sessions, today, zoneId)
        assertEquals(3, result.currentStreak)
        assertEquals(3, result.longestStreak)
    }

    @Test
    fun testBrokenStreakReturnsZeroCurrentStreakAndRetainsLongest() {
        // User listened 4 consecutive days, then stopped for 3 days
        val sessions = listOf(
            createSession(LocalDate.of(2026, 10, 20), 120_000L),
            createSession(LocalDate.of(2026, 10, 21), 120_000L),
            createSession(LocalDate.of(2026, 10, 22), 120_000L),
            createSession(LocalDate.of(2026, 10, 23), 120_000L)
        )
        val result = ListeningStatsCalculator.calculateStreaks(sessions, today, zoneId)
        assertEquals(0, result.currentStreak)
        assertEquals(4, result.longestStreak)
    }

    @Test
    fun testSubMinuteSessionsDoNotCountTowardsStreak() {
        // Less than 60s
        val sessions = listOf(
            createSession(LocalDate.of(2026, 11, 3), 45_000L)
        )
        val result = ListeningStatsCalculator.calculateStreaks(sessions, today, zoneId)
        assertEquals(0, result.currentStreak)
        assertEquals(0, result.longestStreak)
    }

    @Test
    fun testTimeSavedCalculation() {
        val saved = ListeningStatsCalculator.calculateTimeSavedMs(
            listenedDurationMs = 3600_000L, // 1 hour wall-clock
            contentDurationMs = 5400_000L   // 1.5 hours content (1.5x speed)
        )
        assertEquals(1800_000L, saved) // 30 minutes saved
    }
}
