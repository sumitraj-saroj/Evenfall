package com.evenfall.app

import com.evenfall.app.data.db.entity.BookEntity
import com.evenfall.app.data.db.entity.ChapterEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MetadataOverrideTest {

    @Test
    fun testBookEntityDisplayHelpersRespectOverrides() {
        val original = BookEntity(
            id = 1L,
            title = "Original Title",
            author = "Original Author",
            narrator = "Original Narrator",
            coverUri = "file:///covers/orig.jpg",
            folderUri = "content://folder",
            folderIdentifier = "id_1",
            totalDurationMs = 10000L
        )

        // Without overrides
        assertEquals("Original Title", original.displayTitle)
        assertEquals("Original Author", original.displayAuthor)
        assertEquals("Original Narrator", original.displayNarrator)
        assertEquals("file:///covers/orig.jpg", original.displayCoverUri)

        // With overrides
        val overridden = original.copy(
            titleOverride = "Custom Title",
            authorOverride = "Custom Author",
            narratorOverride = "Custom Narrator",
            seriesName = "Epic Saga",
            seriesOrder = 2.5f,
            coverOverrideUri = "file:///custom_covers/custom.jpg"
        )
        assertEquals("Custom Title", overridden.displayTitle)
        assertEquals("Custom Author", overridden.displayAuthor)
        assertEquals("Custom Narrator", overridden.displayNarrator)
        assertEquals("file:///custom_covers/custom.jpg", overridden.displayCoverUri)
        assertEquals("Epic Saga", overridden.seriesName)
        assertEquals(2.5f, overridden.seriesOrder)

        // Blank overrides fall back to original
        val blankOverride = original.copy(
            titleOverride = "   ",
            authorOverride = ""
        )
        assertEquals("Original Title", blankOverride.displayTitle)
        assertEquals("Original Author", blankOverride.displayAuthor)
    }

    @Test
    fun testChapterEntityDisplayHelpersRespectOverrides() {
        val ch = ChapterEntity(
            id = 10L,
            bookId = 1L,
            chapterIndex = 0,
            title = "Chapter 1: The Beginning",
            startMs = 0L,
            durationMs = 5000L,
            fileUri = "content://ch1.mp3"
        )
        assertEquals("Chapter 1: The Beginning", ch.displayTitle)

        val chOverridden = ch.copy(titleOverride = "Prologue")
        assertEquals("Prologue", chOverridden.displayTitle)

        val chBlank = ch.copy(titleOverride = "   ")
        assertEquals("Chapter 1: The Beginning", chBlank.displayTitle)
    }

    @Test
    fun testResetRestoresOriginalMetadata() {
        val book = BookEntity(
            id = 1L,
            title = "Dune",
            author = "Frank Herbert",
            coverUri = "file:///orig.jpg",
            folderUri = "content://dune",
            folderIdentifier = "dune_id",
            totalDurationMs = 20000L,
            titleOverride = "My Dune",
            authorOverride = "Herbert",
            coverOverrideUri = "file:///custom.jpg"
        )

        val resetBook = book.copy(
            titleOverride = null,
            authorOverride = null,
            narratorOverride = null,
            seriesName = null,
            seriesOrder = null,
            coverOverrideUri = null
        )

        assertEquals("Dune", resetBook.displayTitle)
        assertEquals("Frank Herbert", resetBook.displayAuthor)
        assertEquals("file:///orig.jpg", resetBook.displayCoverUri)
        assertNull(resetBook.titleOverride)
        assertNull(resetBook.authorOverride)
        assertNull(resetBook.coverOverrideUri)
    }
}
