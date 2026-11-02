package com.evenfall.app.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object TimeUtils {

    /**
     * Formats duration to "8 hr, 19 min" or "45 min".
     */
    fun formatDurationWords(millis: Long): String {
        if (millis <= 0) return "0 min"
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        return when {
            hours > 0 && minutes > 0 -> "$hours hr, $minutes min"
            hours > 0 -> "$hours hr"
            else -> "$minutes min"
        }
    }

    /**
     * Formats remaining time to "4:03:15 left" or "24:10 left".
     */
    fun formatTimeLeft(millis: Long): String {
        if (millis <= 0) return "0:00 left"
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d left", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%d:%02d left", minutes, seconds)
        }
    }

    /**
     * Formats time to standard mm:ss or h:mm:ss (e.g. "17:35" or "1:04:12").
     */
    fun formatTimestamp(millis: Long): String {
        val totalSeconds = (millis.coerceAtLeast(0) / 1000).toInt()
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    /**
     * Formats relative date like "Sep 12", "Thu", "Yesterday", "Today".
     */
    fun formatRelativeDate(timestamp: Long): String {
        val now = Calendar.getInstance()
        val then = Calendar.getInstance().apply { timeInMillis = timestamp }

        val isSameDay = now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)

        now.add(Calendar.DAY_OF_YEAR, -1)
        val isYesterday = now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)

        return when {
            isSameDay -> "Today"
            isYesterday -> "Yesterday"
            else -> {
                val diffDays = (System.currentTimeMillis() - timestamp) / (1000 * 60 * 60 * 24)
                if (diffDays < 7) {
                    SimpleDateFormat("EEE", Locale.getDefault()).format(Date(timestamp))
                } else {
                    SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
                }
            }
        }
    }

    /**
     * Formats time of day like "8:15 AM", "10:40 PM".
     */
    fun formatTimeOfDay(timestamp: Long): String {
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
    }

    /**
     * Groups history timestamp into "This morning", "This afternoon", "Last night", etc.
     */
    fun formatHistoryTimeGroup(timestamp: Long): String {
        val now = Calendar.getInstance()
        val then = Calendar.getInstance().apply { timeInMillis = timestamp }

        val isSameDay = now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)

        val hour = then.get(Calendar.HOUR_OF_DAY)
        return if (isSameDay) {
            when (hour) {
                in 4..11 -> "This morning"
                in 12..16 -> "This afternoon"
                else -> "This evening"
            }
        } else {
            val daysAgo = (now.timeInMillis - timestamp) / (1000 * 60 * 60 * 24)
            if (daysAgo <= 1) {
                if (hour >= 20 || hour < 4) "Last night" else "Yesterday"
            } else {
                SimpleDateFormat("MMMM d", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }
}
