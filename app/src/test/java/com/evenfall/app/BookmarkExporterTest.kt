package com.evenfall.app

import com.evenfall.app.data.db.entity.BookEntity
import com.evenfall.app.data.db.entity.BookmarkEntity
import com.evenfall.app.data.db.entity.BookmarkType
import com.evenfall.app.data.db.entity.ChapterEntity
import com.evenfall.app.util.BookExportData
import com.evenfall.app.util.BookmarkExporter
import com.evenfall.app.util.ExportFormat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookmarkExporterTest {

    private val sampleBook = BookEntity(
        id = 1L,
        title = "The Way of Kings",
        author = "Brandon Sanderson",
        folderUri = "content://audiobooks/wok",
        folderIdentifier = "wok_id",
        totalDurationMs = 150000000L,
        fileCount = 2
    )

    private val sampleChapters = listOf(
        ChapterEntity(
            id = 1L,
            bookId = 1L,
            chapterIndex = 0,
            title = "Prelude to the Stormlight Archive",
            startMs = 0L,
            durationMs = 4000000L,
            fileUri = "content://audiobooks/wok/01.mp3"
        ),
        ChapterEntity(
            id = 2L,
            bookId = 1L,
            chapterIndex = 1,
            title = "Stormblessed",
            startMs = 4000000L,
            durationMs = 5000000L,
            fileUri = "content://audiobooks/wok/02.mp3"
        )
    )

    private val sampleBookmarks = listOf(
        BookmarkEntity(
            id = 1L,
            bookId = 1L,
            chapterIndex = 0,
            chapterTitle = "Prelude",
            positionMs = 125000L,
            bookProgressFraction = 0.001f,
            title = "Herald Oath",
            note = "Jezrien departs, leaving honorblades behind.",
            type = BookmarkType.NOTE
        ),
        BookmarkEntity(
            id = 2L,
            bookId = 1L,
            chapterIndex = 1,
            chapterTitle = "Stormblessed",
            positionMs = 345000L,
            bookProgressFraction = 0.03f,
            title = "Immortal Words",
            note = "Life before death.\nStrength before weakness.\nJourney before destination.",
            type = BookmarkType.QUOTE
        ),
        BookmarkEntity(
            id = 3L,
            bookId = 1L,
            chapterIndex = 1,
            chapterTitle = "Stormblessed",
            positionMs = 600000L,
            bookProgressFraction = 0.04f,
            title = "Chasm run begins",
            note = null,
            type = BookmarkType.BOOKMARK
        )
    )

    @Test
    fun testMarkdownExportFormat() {
        val markdown = BookmarkExporter.exportBook(
            book = sampleBook,
            bookmarks = sampleBookmarks,
            chapters = sampleChapters,
            format = ExportFormat.MARKDOWN
        )

        // Header
        assertTrue(markdown.contains("# The Way of Kings"))
        assertTrue(markdown.contains("**Author:** Brandon Sanderson"))

        // Chapter groupings
        assertTrue(markdown.contains("## Prelude to the Stormlight Archive"))
        assertTrue(markdown.contains("## Stormblessed"))

        // Note with timestamp
        assertTrue(markdown.contains("Herald Oath"))
        assertTrue(markdown.contains("(Note)"))
        assertTrue(markdown.contains("Jezrien departs, leaving honorblades behind."))

        // Quote with Markdown blockquote formatting
        assertTrue(markdown.contains("Immortal Words"))
        assertTrue(markdown.contains("(Quote)"))
        assertTrue(markdown.contains("  > Life before death."))
        assertTrue(markdown.contains("  > Strength before weakness."))
        assertTrue(markdown.contains("  > Journey before destination."))

        // Standard bookmark
        assertTrue(markdown.contains("Chasm run begins"))
        assertTrue(markdown.contains("(Bookmark)"))
    }

    @Test
    fun testPlainTextExportFormat() {
        val text = BookmarkExporter.exportBook(
            book = sampleBook,
            bookmarks = sampleBookmarks,
            chapters = sampleChapters,
            format = ExportFormat.TEXT
        )

        // Header
        assertTrue(text.contains("========================================"))
        assertTrue(text.contains("The Way of Kings"))
        assertTrue(text.contains("Author: Brandon Sanderson"))

        // Chapters
        assertTrue(text.contains("--- Prelude to the Stormlight Archive ---"))
        assertTrue(text.contains("--- Stormblessed ---"))

        // Note
        assertTrue(text.contains("Herald Oath (Note)"))
        assertTrue(text.contains("Jezrien departs, leaving honorblades behind."))

        // Quotes in quotation marks
        assertTrue(text.contains("Immortal Words (Quote)"))
        assertTrue(text.contains("\"Life before death."))
    }

    @Test
    fun testExportAllBooks() {
        val book2 = BookEntity(
            id = 2L,
            title = "Words of Radiance",
            author = "Brandon Sanderson",
            folderUri = "content://audiobooks/wor",
            folderIdentifier = "wor_id",
            totalDurationMs = 160000000L,
            fileCount = 1
        )
        val bm2 = BookmarkEntity(
            id = 4L,
            bookId = 2L,
            chapterIndex = 0,
            chapterTitle = "Chapter 1",
            positionMs = 50000L,
            bookProgressFraction = 0.01f,
            title = "Shallan arrives",
            type = BookmarkType.BOOKMARK
        )

        val booksData = listOf(
            BookExportData(sampleBook, sampleBookmarks, sampleChapters),
            BookExportData(book2, listOf(bm2), emptyList())
        )

        val markdownAll = BookmarkExporter.exportAll(booksData, ExportFormat.MARKDOWN)
        assertTrue(markdownAll.contains("# Evenfall Audiobooks — All Bookmarks & Notes"))
        assertTrue(markdownAll.contains("# The Way of Kings"))
        assertTrue(markdownAll.contains("# Words of Radiance"))
        assertTrue(markdownAll.contains("Shallan arrives"))

        val textAll = BookmarkExporter.exportAll(booksData, ExportFormat.TEXT)
        assertTrue(textAll.contains("Evenfall Audiobooks — All Bookmarks & Notes"))
        assertTrue(textAll.contains("The Way of Kings"))
        assertTrue(textAll.contains("Words of Radiance"))
    }

    @Test
    fun testEmptyBookmarksExport() {
        val emptyMarkdown = BookmarkExporter.exportBook(
            book = sampleBook,
            bookmarks = emptyList(),
            chapters = sampleChapters,
            format = ExportFormat.MARKDOWN
        )
        assertTrue(emptyMarkdown.contains("*No bookmarks or notes recorded for this book.*"))

        val emptyText = BookmarkExporter.exportBook(
            book = sampleBook,
            bookmarks = emptyList(),
            chapters = sampleChapters,
            format = ExportFormat.TEXT
        )
        assertTrue(emptyText.contains("No bookmarks or notes recorded for this book."))
    }
}
