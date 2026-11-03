package com.evenfall.app

import com.evenfall.app.playback.BedtimeConfig
import com.evenfall.app.playback.BedtimeScheduleEvaluator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class BedtimeScheduleTest {

    @Test
    fun testBedtimeDisabledReturnsFalse() {
        val config = BedtimeConfig(enabled = false)
        val now = System.currentTimeMillis()
        assertFalse(BedtimeScheduleEvaluator.isWithinBedtimeWindow(now, config))
    }

    @Test
    fun testSameDayWindow() {
        // Window from 20:00 (8 PM) to 22:00 (10 PM)
        val config = BedtimeConfig(
            enabled = true,
            startHour = 20,
            startMinute = 0,
            daysOfWeek = setOf(Calendar.MONDAY)
        )

        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 30)
        }

        assertTrue(BedtimeScheduleEvaluator.isWithinBedtimeWindow(cal.timeInMillis, config, windowDurationMinutes = 120))

        // Before window start (19:59)
        cal.set(Calendar.HOUR_OF_DAY, 19)
        cal.set(Calendar.MINUTE, 59)
        assertFalse(BedtimeScheduleEvaluator.isWithinBedtimeWindow(cal.timeInMillis, config, windowDurationMinutes = 120))

        // After window end (22:01)
        cal.set(Calendar.HOUR_OF_DAY, 22)
        cal.set(Calendar.MINUTE, 1)
        assertFalse(BedtimeScheduleEvaluator.isWithinBedtimeWindow(cal.timeInMillis, config, windowDurationMinutes = 120))
    }

    @Test
    fun testMidnightCrossingWindow() {
        // Window from 22:30 (10:30 PM) for 3 hours (until 01:30 AM next day) on Friday
        val config = BedtimeConfig(
            enabled = true,
            startHour = 22,
            startMinute = 30,
            daysOfWeek = setOf(Calendar.FRIDAY)
        )

        // Friday 23:15 -> within window
        val calFridayNight = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 15)
        }
        assertTrue(BedtimeScheduleEvaluator.isWithinBedtimeWindow(calFridayNight.timeInMillis, config, windowDurationMinutes = 180))

        // Saturday 00:45 -> within window belonging to Friday night
        val calSaturdayEarly = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 45)
        }
        assertTrue(BedtimeScheduleEvaluator.isWithinBedtimeWindow(calSaturdayEarly.timeInMillis, config, windowDurationMinutes = 180))

        // Saturday 02:00 -> window ended at 01:30
        val calSaturdayLater = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
            set(Calendar.HOUR_OF_DAY, 2)
            set(Calendar.MINUTE, 0)
        }
        assertFalse(BedtimeScheduleEvaluator.isWithinBedtimeWindow(calSaturdayLater.timeInMillis, config, windowDurationMinutes = 180))
    }

    @Test
    fun testInactiveDayOfWeek() {
        val config = BedtimeConfig(
            enabled = true,
            startHour = 22,
            startMinute = 0,
            daysOfWeek = setOf(Calendar.MONDAY, Calendar.TUESDAY)
        )

        val calWednesday = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY)
            set(Calendar.HOUR_OF_DAY, 22)
            set(Calendar.MINUTE, 30)
        }
        assertFalse(BedtimeScheduleEvaluator.isWithinBedtimeWindow(calWednesday.timeInMillis, config))
    }
}
