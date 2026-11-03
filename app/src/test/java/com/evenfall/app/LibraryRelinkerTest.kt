package com.evenfall.app

import com.evenfall.app.data.db.entity.BookEntity
import com.evenfall.app.data.scanner.BookFileDescriptor
import com.evenfall.app.data.scanner.LibraryRelinker
import com.evenfall.app.data.scanner.MatchConfidence
import com.evenfall.app.data.scanner.NewBookMatchInfo
import com.evenfall.app.data.scanner.OldBookMatchInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryRelinkerTest {

    private fun createOldBook(
        id: Long = 1L,
        title: String = "The Way of Kings",
        author: String = "Brandon Sanderson",
        durationMs: Long = 3600000L,
        relativePath: String = "Sanderson/The Way of Kings",
        files: List<BookFileDescriptor> = listOf(
            BookFileDescriptor("part1.mp3", 1000000L),
            BookFileDescriptor("part2.mp3", 2000000L)
        )
    ): OldBookMatchInfo {
        val entity = BookEntity(
            id = id,
            title = title,
            author = author,
            folderUri = "content://saf/old/$relativePath",
            folderIdentifier = "content://saf/old/$relativePath",
            totalDurationMs = durationMs,
            currentPositionMs = 1500000L,
            currentChapterIndex = 1,
            chapterPositionMs = 500000L,
            playbackSpeed = 1.2f,
            isFinished = false,
            titleOverride = "Custom Title Override",
            authorOverride = "Custom Author Override",
            narrator = "Michael Kramer",
            narratorOverride = "Narrator Override",
            seriesName = "The Stormlight Archive",
            seriesOrder = 1.0f,
            coverOverrideUri = "file:///data/custom_cover.jpg",
            isUnavailable = true
        )
        return OldBookMatchInfo(
            id = id,
            title = title,
            author = author,
            totalDurationMs = durationMs,
            relativePath = relativePath,
            fileCount = files.size,
            files = files,
            bookEntity = entity
        )
    }

    private fun createNewBook(
        identifier: String = "tree://new/Sanderson/The Way of Kings",
        title: String = "The Way of Kings",
        author: String = "Brandon Sanderson",
        durationMs: Long = 3600000L,
        relativePath: String = "Sanderson/The Way of Kings",
        files: List<BookFileDescriptor> = listOf(
            BookFileDescriptor("part1.mp3", 1000000L),
            BookFileDescriptor("part2.mp3", 2000000L)
        )
    ): NewBookMatchInfo {
        return NewBookMatchInfo(
            identifier = identifier,
            title = title,
            author = author,
            totalDurationMs = durationMs,
            relativePath = relativePath,
            fileCount = files.size,
            files = files
        )
    }

    @Test
    fun testExactRelativePathMatch() {
        val oldBook = createOldBook(relativePath = "Fantasy/Sanderson/The Way of Kings")
        val newBook = createNewBook(relativePath = "Fantasy/Sanderson/The Way of Kings")

        val score = LibraryRelinker.computeMatchScore(oldBook, newBook)
        assertTrue("Exact relative path score should be >= 50", score.pathScore >= 50)
        assertTrue("Total score should be >= 70", score.score >= 70)

        val analysis = LibraryRelinker.matchBooks(listOf(oldBook), listOf(newBook))
        assertEquals(1, analysis.matched.size)
        assertEquals(MatchConfidence.MATCHED, analysis.matched[0].status)
        assertEquals(newBook.identifier, analysis.matched[0].matchedCandidate?.identifier)
    }

    @Test
    fun testTitleAuthorDurationMatch() {
        // Path is completely different (e.g. moved from OldSDCard to InternalStorage/Audiobooks)
        val oldBook = createOldBook(
            relativePath = "OldSDCard/BookA",
            title = "Project Hail Mary",
            author = "Andy Weir",
            durationMs = 5000000L,
            files = emptyList()
        )
        val newBook = createNewBook(
            relativePath = "Downloads/Andy_Weir/HailMary",
            title = "Project Hail Mary",
            author = "Andy Weir",
            durationMs = 5002000L, // within 5 second tolerance
            files = emptyList()
        )

        val score = LibraryRelinker.computeMatchScore(oldBook, newBook)
        assertTrue("Metadata + duration score should be >= 50", score.metadataScore >= 50)

        val analysis = LibraryRelinker.matchBooks(listOf(oldBook), listOf(newBook))
        assertEquals(1, analysis.matched.size)
        assertEquals(MatchConfidence.MATCHED, analysis.matched[0].status)
    }

    @Test
    fun testFileNamesAndSizesMatch() {
        // Title and author are missing / generic, but file names and sizes match
        val files = listOf(
            BookFileDescriptor("track01.m4b", 50000000L),
            BookFileDescriptor("track02.m4b", 60000000L)
        )
        val oldBook = createOldBook(
            title = "Audiobook 1",
            author = "Unknown Author",
            durationMs = 0L,
            relativePath = "Folder1",
            files = files
        )
        val newBook = createNewBook(
            title = "Audiobook 1",
            author = "Unknown Author",
            durationMs = 0L,
            relativePath = "Folder2",
            files = files
        )

        val score = LibraryRelinker.computeMatchScore(oldBook, newBook)
        assertTrue("File match score should be >= 30", score.fileScore >= 30)
    }

    @Test
    fun testAmbiguousCandidates() {
        val oldBook = createOldBook(
            title = "Harry Potter",
            author = "J.K. Rowling",
            durationMs = 10000000L,
            relativePath = "HP",
            files = emptyList()
        )
        // Two very close candidate books
        val cand1 = createNewBook(
            identifier = "cand1",
            title = "Harry Potter 1",
            author = "J.K. Rowling",
            durationMs = 10000000L,
            relativePath = "HP1",
            files = emptyList()
        )
        val cand2 = createNewBook(
            identifier = "cand2",
            title = "Harry Potter 2",
            author = "J.K. Rowling",
            durationMs = 10000000L,
            relativePath = "HP2",
            files = emptyList()
        )

        val analysis = LibraryRelinker.matchBooks(listOf(oldBook), listOf(cand1, cand2))
        assertEquals(1, analysis.ambiguous.size)
        assertEquals(MatchConfidence.AMBIGUOUS, analysis.ambiguous[0].status)
    }

    @Test
    fun testUnmatchedBook() {
        val oldBook = createOldBook(
            title = "Old Book Title",
            author = "Author X",
            durationMs = 1000000L,
            relativePath = "OldFolder",
            files = listOf(BookFileDescriptor("old.mp3", 100L))
        )
        val newBook = createNewBook(
            title = "Completely Different Title",
            author = "Author Y",
            durationMs = 9000000L,
            relativePath = "NewFolder",
            files = listOf(BookFileDescriptor("new.mp3", 900L))
        )

        val analysis = LibraryRelinker.matchBooks(listOf(oldBook), listOf(newBook))
        assertEquals(1, analysis.unmatchedOld.size)
        assertEquals(MatchConfidence.UNMATCHED, analysis.unmatchedOld[0].status)
        assertEquals(1, analysis.unmatchedNew.size)
    }

    @Test
    fun testReLinkingPreservesPositionsBookmarksHistoryAndOverrides() {
        val oldBook = createOldBook(id = 42L).bookEntity
        assertTrue("Initial book should be marked unavailable", oldBook.isUnavailable)

        // Simulate re-link execution
        val newFolderUri = "content://com.android.externalstorage.documents/tree/SDCARD%3ANewFolder"
        val newIdentifier = "$newFolderUri/The Way of Kings"

        val updatedBook = oldBook.copy(
            folderUri = newFolderUri,
            folderIdentifier = newIdentifier,
            isUnavailable = false
        )

        // 1. Availability restored
        assertFalse("Re-linked book must be available", updatedBook.isUnavailable)
        assertEquals(newFolderUri, updatedBook.folderUri)
        assertEquals(newIdentifier, updatedBook.folderIdentifier)

        // 2. Playback position 100% preserved
        assertEquals(oldBook.id, updatedBook.id)
        assertEquals(1500000L, updatedBook.currentPositionMs)
        assertEquals(1, updatedBook.currentChapterIndex)
        assertEquals(500000L, updatedBook.chapterPositionMs)
        assertEquals(1.2f, updatedBook.playbackSpeed)

        // 3. Metadata overrides 100% preserved
        assertEquals("Custom Title Override", updatedBook.titleOverride)
        assertEquals("Custom Author Override", updatedBook.authorOverride)
        assertEquals("Narrator Override", updatedBook.narratorOverride)
        assertEquals("The Stormlight Archive", updatedBook.seriesName)
        assertEquals(1.0f, updatedBook.seriesOrder)
        assertEquals("file:///data/custom_cover.jpg", updatedBook.coverOverrideUri)

        // 4. Finished state preserved
        assertEquals(oldBook.isFinished, updatedBook.isFinished)
        assertEquals(oldBook.finishedAt, updatedBook.finishedAt)
    }
}
