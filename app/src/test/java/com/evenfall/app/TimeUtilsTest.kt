package com.evenfall.app

import com.evenfall.app.util.TimeUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeUtilsTest {

    @Test
    fun testFormatDurationWords() {
        val millis = (8 * 3600 + 19 * 60) * 1000L
        assertEquals("8 hr, 19 min", TimeUtils.formatDurationWords(millis))
    }

    @Test
    fun testFormatTimeLeft() {
        val millis = (4 * 3600 + 3 * 60 + 15) * 1000L
        assertEquals("4:03:15 left", TimeUtils.formatTimeLeft(millis))

        val shortMillis = (11 * 60 + 24) * 1000L
        assertEquals("11:24 left", TimeUtils.formatTimeLeft(shortMillis))
    }

    @Test
    fun testFormatTimestamp() {
        val millis = (17 * 60 + 35) * 1000L
        assertEquals("17:35", TimeUtils.formatTimestamp(millis))
    }
}
