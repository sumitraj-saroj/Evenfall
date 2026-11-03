package com.evenfall.app

import com.evenfall.app.playback.AutoMarkFinishedEvaluator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoMarkFinishedTest {

    @Test
    fun testProgressBelowThresholdDoesNotTrigger() {
        // 97.9% progress
        val triggers979 = AutoMarkFinishedEvaluator.shouldAutoMarkFinished(
            totalPositionMs = 979L,
            totalDurationMs = 1000L,
            isLastChapterEnded = false,
            isAutoMarkEnabled = true,
            isAlreadyFinished = false
        )
        assertFalse(triggers979)

        // 50% progress
        val triggers500 = AutoMarkFinishedEvaluator.shouldAutoMarkFinished(
            totalPositionMs = 500L,
            totalDurationMs = 1000L,
            isLastChapterEnded = false,
            isAutoMarkEnabled = true,
            isAlreadyFinished = false
        )
        assertFalse(triggers500)
    }

    @Test
    fun testProgressAtOrAbove98PercentTriggers() {
        // Exactly 98.0%
        val triggers980 = AutoMarkFinishedEvaluator.shouldAutoMarkFinished(
            totalPositionMs = 980L,
            totalDurationMs = 1000L,
            isLastChapterEnded = false,
            isAutoMarkEnabled = true,
            isAlreadyFinished = false
        )
        assertTrue(triggers980)

        // 98.5%
        val triggers985 = AutoMarkFinishedEvaluator.shouldAutoMarkFinished(
            totalPositionMs = 985L,
            totalDurationMs = 1000L,
            isLastChapterEnded = false,
            isAutoMarkEnabled = true,
            isAlreadyFinished = false
        )
        assertTrue(triggers985)

        // 99.9%
        val triggers999 = AutoMarkFinishedEvaluator.shouldAutoMarkFinished(
            totalPositionMs = 999L,
            totalDurationMs = 1000L,
            isLastChapterEnded = false,
            isAutoMarkEnabled = true,
            isAlreadyFinished = false
        )
        assertTrue(triggers999)

        // 100%
        val triggers1000 = AutoMarkFinishedEvaluator.shouldAutoMarkFinished(
            totalPositionMs = 1000L,
            totalDurationMs = 1000L,
            isLastChapterEnded = false,
            isAutoMarkEnabled = true,
            isAlreadyFinished = false
        )
        assertTrue(triggers1000)
    }

    @Test
    fun testEndOfLastChapterTriggersEvenBelow98Percent() {
        val triggersEndOfLastChapter = AutoMarkFinishedEvaluator.shouldAutoMarkFinished(
            totalPositionMs = 950L,
            totalDurationMs = 1000L,
            isLastChapterEnded = true,
            isAutoMarkEnabled = true,
            isAlreadyFinished = false
        )
        assertTrue(triggersEndOfLastChapter)
    }

    @Test
    fun testEndOfIntermediateChapterDoesNotTrigger() {
        val triggersIntermediate = AutoMarkFinishedEvaluator.shouldAutoMarkFinished(
            totalPositionMs = 450L,
            totalDurationMs = 1000L,
            isLastChapterEnded = false,
            isAutoMarkEnabled = true,
            isAlreadyFinished = false
        )
        assertFalse(triggersIntermediate)
    }

    @Test
    fun testSettingDisabledSuppressesTrigger() {
        // Disabled at 99%
        val suppressedBySetting99 = AutoMarkFinishedEvaluator.shouldAutoMarkFinished(
            totalPositionMs = 990L,
            totalDurationMs = 1000L,
            isLastChapterEnded = false,
            isAutoMarkEnabled = false,
            isAlreadyFinished = false
        )
        assertFalse(suppressedBySetting99)

        // Disabled at end of last chapter
        val suppressedBySettingEnd = AutoMarkFinishedEvaluator.shouldAutoMarkFinished(
            totalPositionMs = 1000L,
            totalDurationMs = 1000L,
            isLastChapterEnded = true,
            isAutoMarkEnabled = false,
            isAlreadyFinished = false
        )
        assertFalse(suppressedBySettingEnd)
    }

    @Test
    fun testAlreadyFinishedDoesNotReTrigger() {
        val alreadyFinished = AutoMarkFinishedEvaluator.shouldAutoMarkFinished(
            totalPositionMs = 1000L,
            totalDurationMs = 1000L,
            isLastChapterEnded = true,
            isAutoMarkEnabled = true,
            isAlreadyFinished = true
        )
        assertFalse(alreadyFinished)
    }

    @Test
    fun testZeroOrNegativeDurationDoesNotTrigger() {
        val zeroDuration = AutoMarkFinishedEvaluator.shouldAutoMarkFinished(
            totalPositionMs = 100L,
            totalDurationMs = 0L,
            isLastChapterEnded = false,
            isAutoMarkEnabled = true,
            isAlreadyFinished = false
        )
        assertFalse(zeroDuration)
    }
}
