package com.evenfall.app

import com.evenfall.app.playback.ListeningSessionTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ListeningSessionTrackerTest {

    @Test
    fun testNormalPlaybackAtNormalSpeed() {
        val tracker = ListeningSessionTracker(statsEnabled = true)
        val bookId = 42L

        // Start playback at T=0, pos=10_000ms
        tracker.onPlay(nowMs = 0L, bookId = bookId, positionMs = 10_000L)

        // 5 ticks of 1 second each
        for (i in 1..5) {
            val flush = tracker.onTick(nowMs = i * 1000L, positionMs = 10_000L + (i * 1000L))
            assertNull(flush) // Does not auto-flush until 30s
        }

        // Pause at T=5_000ms
        val record = tracker.onPause(nowMs = 5_000L, positionMs = 15_000L)
        assertNotNull(record)
        assertEquals(bookId, record?.bookId)
        assertEquals(5000L, record?.listenedDurationMs)
        assertEquals(5000L, record?.contentDurationMs)
        val timeSaved = record!!.contentDurationMs - record.listenedDurationMs
        assertEquals(0L, timeSaved)
    }

    @Test
    fun testFastPlaybackAtTwoXSpeedComputesTimeSaved() {
        val tracker = ListeningSessionTracker(statsEnabled = true)
        val bookId = 100L

        // Start at T=1000, pos=0
        tracker.onPlay(nowMs = 1000L, bookId = bookId, positionMs = 0L)

        // In 10 wall-clock seconds at 2.0x speed, 20s of audio content is covered
        for (i in 1..10) {
            tracker.onTick(nowMs = 1000L + (i * 1000L), positionMs = i * 2000L)
        }

        val record = tracker.onPause(nowMs = 11_000L, positionMs = 20_000L)
        assertNotNull(record)
        assertEquals(10_000L, record?.listenedDurationMs)
        assertEquals(20_000L, record?.contentDurationMs)
        val timeSaved = record!!.contentDurationMs - record.listenedDurationMs
        assertEquals(10_000L, timeSaved)
    }

    @Test
    fun testSeekForwardDoesNotCountAsContentListened() {
        val tracker = ListeningSessionTracker(statsEnabled = true)
        val bookId = 7L

        // Play for 5 seconds: 0 to 5000ms
        tracker.onPlay(nowMs = 0L, bookId = bookId, positionMs = 0L)
        for (i in 1..5) {
            tracker.onTick(nowMs = i * 1000L, positionMs = i * 1000L)
        }

        // User skips forward 30 seconds to pos 35_000ms at T=5000ms
        tracker.onSeek(nowMs = 5000L, newPositionMs = 35_000L)

        // Play for another 5 seconds from 35_000ms to 40_000ms
        for (i in 1..5) {
            tracker.onTick(nowMs = 5000L + (i * 1000L), positionMs = 35_000L + (i * 1000L))
        }

        val record = tracker.onPause(nowMs = 10_000L, positionMs = 40_000L)
        assertNotNull(record)
        // Listened wall-clock: 10s. Content covered: 5s + 5s = 10s. The 30s jump is NOT included!
        assertEquals(10_000L, record?.listenedDurationMs)
        assertEquals(10_000L, record?.contentDurationMs)
    }

    @Test
    fun testSeekBackwardRewindDoesNotCauseNegativeOrLostTime() {
        val tracker = ListeningSessionTracker(statsEnabled = true)
        val bookId = 7L

        // Play 5 seconds
        tracker.onPlay(nowMs = 0L, bookId = bookId, positionMs = 20_000L)
        for (i in 1..5) {
            tracker.onTick(nowMs = i * 1000L, positionMs = 20_000L + (i * 1000L))
        }

        // Rewind 15 seconds to 10_000ms at T=5000ms
        tracker.onSeek(nowMs = 5000L, newPositionMs = 10_000L)

        // Play another 5 seconds
        for (i in 1..5) {
            tracker.onTick(nowMs = 5000L + (i * 1000L), positionMs = 10_000L + (i * 1000L))
        }

        val record = tracker.onPause(nowMs = 10_000L, positionMs = 15_000L)
        assertNotNull(record)
        assertEquals(10_000L, record?.listenedDurationMs)
        assertEquals(10_000L, record?.contentDurationMs)
    }

    @Test
    fun testSilenceSkippingIncreasesContentWithoutExtraWallClock() {
        val tracker = ListeningSessionTracker(statsEnabled = true)
        val bookId = 12L

        tracker.onPlay(nowMs = 0L, bookId = bookId, positionMs = 0L)
        // 5 seconds wall clock, but silence skipping skipped 2.5s of silent passages
        // so content advanced by 7.5s (1.5x effective)
        for (i in 1..5) {
            tracker.onTick(nowMs = i * 1000L, positionMs = (i * 1500L))
        }

        val record = tracker.onPause(nowMs = 5000L, positionMs = 7500L)
        assertNotNull(record)
        assertEquals(5000L, record?.listenedDurationMs)
        assertEquals(7500L, record?.contentDurationMs)
        assertEquals(2500L, record!!.contentDurationMs - record.listenedDurationMs)
    }

    @Test
    fun testPauseFreezesWallClock() {
        val tracker = ListeningSessionTracker(statsEnabled = true)
        val bookId = 99L

        tracker.onPlay(nowMs = 0L, bookId = bookId, positionMs = 1000L)
        tracker.onTick(nowMs = 2000L, positionMs = 3000L)
        val rec1 = tracker.onPause(nowMs = 2000L, positionMs = 3000L)
        assertEquals(2000L, rec1?.listenedDurationMs)

        // Paused for 10 minutes (600_000ms)
        // Resume at T = 602_000ms
        tracker.onPlay(nowMs = 602_000L, bookId = bookId, positionMs = 3000L)
        tracker.onTick(nowMs = 605_000L, positionMs = 6000L)
        val rec2 = tracker.onPause(nowMs = 605_000L, positionMs = 6000L)

        // Only the 3 seconds after resuming are recorded
        assertEquals(3000L, rec2?.listenedDurationMs)
        assertEquals(3000L, rec2?.contentDurationMs)
    }

    @Test
    fun testDisablingStatsStopsRecordingImmediately() {
        val tracker = ListeningSessionTracker(statsEnabled = true)
        val bookId = 1L

        tracker.onPlay(nowMs = 0L, bookId = bookId, positionMs = 0L)
        tracker.onTick(nowMs = 2000L, positionMs = 2000L)

        // Disable stats
        tracker.setStatsEnabled(false)

        assertNull(tracker.onTick(nowMs = 3000L, positionMs = 3000L))
        assertNull(tracker.onPause(nowMs = 4000L, positionMs = 4000L))
        assertNull(tracker.flush())
        assertNull(tracker.onPlay(nowMs = 5000L, bookId = bookId, positionMs = 5000L))
    }
}
