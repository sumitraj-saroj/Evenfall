package com.evenfall.app

import com.evenfall.app.data.db.entity.BookmarkType
import com.evenfall.app.util.TimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookmarksHistoryTest {

    @Test
    fun testTimeGroupFormatting() {
        val now = System.currentTimeMillis()
        val formatted = TimeUtils.formatHistoryTimeGroup(now)
        assertTrue(formatted.isNotEmpty())
    }

    @Test
    fun testRelativeDateFormatting() {
        val now = System.currentTimeMillis()
        assertEquals("Today", TimeUtils.formatRelativeDate(now))

        val yesterday = now - (24 * 3600 * 1000L)
        assertEquals("Yesterday", TimeUtils.formatRelativeDate(yesterday))
    }
}
