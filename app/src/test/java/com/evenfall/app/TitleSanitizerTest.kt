package com.evenfall.app

import com.evenfall.app.data.parser.TitleSanitizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TitleSanitizerTest {

    @Test
    fun testIsGenericTitleDetection() {
        // Generic patterns must return true
        assertTrue(TitleSanitizer.isGenericTitle(null))
        assertTrue(TitleSanitizer.isGenericTitle(""))
        assertTrue(TitleSanitizer.isGenericTitle("   "))
        assertTrue(TitleSanitizer.isGenericTitle("Audiobook"))
        assertTrue(TitleSanitizer.isGenericTitle("audiobook"))
        assertTrue(TitleSanitizer.isGenericTitle("audiobooks"))
        assertTrue(TitleSanitizer.isGenericTitle("Audiobook 1"))
        assertTrue(TitleSanitizer.isGenericTitle("Audiobook_02"))
        assertTrue(TitleSanitizer.isGenericTitle("Unknown"))
        assertTrue(TitleSanitizer.isGenericTitle("Unknown Title"))
        assertTrue(TitleSanitizer.isGenericTitle("Unknown Track"))
        assertTrue(TitleSanitizer.isGenericTitle("Untitled"))
        assertTrue(TitleSanitizer.isGenericTitle("Track 1"))
        assertTrue(TitleSanitizer.isGenericTitle("Track 01"))
        assertTrue(TitleSanitizer.isGenericTitle("track-12"))
        assertTrue(TitleSanitizer.isGenericTitle("track_3"))
        assertTrue(TitleSanitizer.isGenericTitle("1"))
        assertTrue(TitleSanitizer.isGenericTitle("01"))
        assertTrue(TitleSanitizer.isGenericTitle("42"))
        assertTrue(TitleSanitizer.isGenericTitle("Chapter 1"))
        assertTrue(TitleSanitizer.isGenericTitle("Disc 1"))

        // Genuine book titles must return false
        assertFalse(TitleSanitizer.isGenericTitle("Echoes of Tomorrow"))
        assertFalse(TitleSanitizer.isGenericTitle("The Bookshop on Maple Street"))
        assertFalse(TitleSanitizer.isGenericTitle("Beyond the Horizon"))
    }

    @Test
    fun testResolveTitlePriorityOrder() {
        // 1. Valid title tag is used directly
        val title1 = TitleSanitizer.resolveTitle(
            titleTag = "Echoes of Tomorrow",
            albumTag = "Some Album",
            folderName = "Sci-Fi Books",
            fileName = "part1.mp3"
        )
        assertEquals("Echoes of Tomorrow", title1)

        // 2. Generic title tag falls back to album tag
        val title2 = TitleSanitizer.resolveTitle(
            titleTag = "Audiobook",
            albumTag = "Dune",
            folderName = "Frank Herbert",
            fileName = "track01.mp3"
        )
        assertEquals("Dune", title2)

        // 3. Generic title tag + generic album tag falls back to folder name
        val title3 = TitleSanitizer.resolveTitle(
            titleTag = "Track 1",
            albumTag = "Unknown Album",
            folderName = "Project Hail Mary",
            fileName = "01.mp3"
        )
        assertEquals("Project Hail Mary", title3)

        // 4. Generic title + album + generic root folder ("Audiobooks") falls back to file name
        val title4 = TitleSanitizer.resolveTitle(
            titleTag = "01",
            albumTag = "Audiobook",
            folderName = "Audiobooks",
            fileName = "The_Hobbit.m4b"
        )
        assertEquals("The Hobbit", title4)

        // 5. Bare number title tag falls back to album tag
        val title5 = TitleSanitizer.resolveTitle(
            titleTag = "1",
            albumTag = "Neuromancer",
            folderName = "Audiobooks",
            fileName = "1.mp3"
        )
        assertEquals("Neuromancer", title5)
    }

    @Test
    fun testCleanFileName() {
        assertEquals("The Great Gatsby", TitleSanitizer.cleanFileName("The_Great_Gatsby.mp3"))
        assertEquals("Sherlock Holmes", TitleSanitizer.cleanFileName("Sherlock-Holmes.m4b"))
        assertEquals("War and Peace", TitleSanitizer.cleanFileName("War and Peace.opus"))
    }
}
