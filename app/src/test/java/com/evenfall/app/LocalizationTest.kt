package com.evenfall.app

import com.evenfall.app.util.TimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Locale

class LocalizationTest {

    @Test
    fun testTimeFormattingWithLocale() {
        Locale.setDefault(Locale.US)
        assertEquals("0:00 left", TimeUtils.formatTimeLeft(0))
        assertEquals("5:30 left", TimeUtils.formatTimeLeft(330_000))
        assertEquals("1:05:30 left", TimeUtils.formatTimeLeft(3930_000))

        assertEquals("00:00", TimeUtils.formatTimestamp(0))
        assertEquals("05:30", TimeUtils.formatTimestamp(330_000))
        assertEquals("1:05:30", TimeUtils.formatTimestamp(3930_000))
    }

    @Test
    fun testLocalesConfigContainsRequiredLocales() {
        val localesConfigFile = File("src/main/res/xml/locales_config.xml")
        assertTrue("locales_config.xml should exist", localesConfigFile.exists())
        val content = localesConfigFile.readText()
        assertTrue("Should support en", content.contains("android:name=\"en\""))
        assertTrue("Should support pseudo-locale en-XA", content.contains("android:name=\"en-XA\""))
        assertTrue("Should support pseudo-locale ar-XB for RTL testing", content.contains("android:name=\"ar-XB\""))
    }

    @Test
    fun testPluralsConfigContainsRequiredEntries() {
        val pluralsFile = File("src/main/res/values/plurals.xml")
        assertTrue("plurals.xml should exist", pluralsFile.exists())
        val content = pluralsFile.readText()
        assertTrue("Should contain books_count", content.contains("name=\"books_count\""))
        assertTrue("Should contain chapters_count", content.contains("name=\"chapters_count\""))
        assertTrue("Should contain days_count", content.contains("name=\"days_count\""))
        assertTrue("Should contain hours_count", content.contains("name=\"hours_count\""))
        assertTrue("Should contain minutes_count", content.contains("name=\"minutes_count\""))
        assertTrue("Should contain seconds_count", content.contains("name=\"seconds_count\""))
    }
}
