package com.evenfall.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.evenfall.app.data.db.entity.BookEntity
import com.evenfall.app.data.db.entity.BookmarkEntity
import com.evenfall.app.data.db.entity.BookmarkType
import com.evenfall.app.data.db.entity.ChapterEntity
import java.io.OutputStreamWriter

enum class ExportFormat {
    MARKDOWN,
    TEXT
}

data class BookExportData(
    val book: BookEntity,
    val bookmarks: List<BookmarkEntity>,
    val chapters: List<ChapterEntity>
)

object BookmarkExporter {

    fun exportBook(
        book: BookEntity,
        bookmarks: List<BookmarkEntity>,
        chapters: List<ChapterEntity>,
        format: ExportFormat = ExportFormat.MARKDOWN
    ): String {
        return when (format) {
            ExportFormat.MARKDOWN -> generateBookMarkdown(book, bookmarks, chapters)
            ExportFormat.TEXT -> generateBookPlainText(book, bookmarks, chapters)
        }
    }

    fun exportAll(
        booksData: List<BookExportData>,
        format: ExportFormat = ExportFormat.MARKDOWN
    ): String {
        return when (format) {
            ExportFormat.MARKDOWN -> generateAllMarkdown(booksData)
            ExportFormat.TEXT -> generateAllPlainText(booksData)
        }
    }

    private fun generateBookMarkdown(
        book: BookEntity,
        bookmarks: List<BookmarkEntity>,
        chapters: List<ChapterEntity>
    ): String {
        val sb = StringBuilder()
        sb.append("# ").append(book.displayTitle).append("\n")
        sb.append("**Author:** ").append(book.displayAuthor).append("\n\n")

        if (bookmarks.isEmpty()) {
            sb.append("*No bookmarks or notes recorded for this book.*\n")
            return sb.toString()
        }

        val chaptersMap = chapters.associateBy { it.chapterIndex }
        val grouped = bookmarks
            .sortedWith(compareBy({ it.chapterIndex }, { it.positionMs }))
            .groupBy { it.chapterIndex }

        for ((chapterIndex, chapterBookmarks) in grouped) {
            val chapter = chaptersMap[chapterIndex]
            val chapterTitle = chapter?.displayTitle
                ?: chapterBookmarks.firstOrNull()?.chapterTitle
                ?: "Chapter ${chapterIndex + 1}"

            sb.append("## ").append(chapterTitle).append("\n\n")

            for (bm in chapterBookmarks) {
                val timeStr = TimeUtils.formatTimestamp(bm.positionMs)
                val typeStr = formatType(bm.type)

                sb.append("- **[").append(timeStr).append("] ")
                    .append(bm.title).append("** (").append(typeStr).append(")\n")

                if (!bm.note.isNullOrBlank()) {
                    if (bm.type == BookmarkType.QUOTE) {
                        bm.note.lines().forEach { line ->
                            sb.append("  > ").append(line).append("\n")
                        }
                    } else {
                        bm.note.lines().forEach { line ->
                            sb.append("  ").append(line).append("\n")
                        }
                    }
                }
                sb.append("\n")
            }
        }
        return sb.toString()
    }

    private fun generateBookPlainText(
        book: BookEntity,
        bookmarks: List<BookmarkEntity>,
        chapters: List<ChapterEntity>
    ): String {
        val sb = StringBuilder()
        sb.append("========================================\n")
        sb.append(book.displayTitle).append("\n")
        sb.append("Author: ").append(book.displayAuthor).append("\n")
        sb.append("========================================\n\n")

        if (bookmarks.isEmpty()) {
            sb.append("No bookmarks or notes recorded for this book.\n")
            return sb.toString()
        }

        val chaptersMap = chapters.associateBy { it.chapterIndex }
        val grouped = bookmarks
            .sortedWith(compareBy({ it.chapterIndex }, { it.positionMs }))
            .groupBy { it.chapterIndex }

        for ((chapterIndex, chapterBookmarks) in grouped) {
            val chapter = chaptersMap[chapterIndex]
            val chapterTitle = chapter?.displayTitle
                ?: chapterBookmarks.firstOrNull()?.chapterTitle
                ?: "Chapter ${chapterIndex + 1}"

            sb.append("--- ").append(chapterTitle).append(" ---\n")

            for (bm in chapterBookmarks) {
                val timeStr = TimeUtils.formatTimestamp(bm.positionMs)
                val typeStr = formatType(bm.type)

                sb.append("• [").append(timeStr).append("] ")
                    .append(bm.title).append(" (").append(typeStr).append(")\n")

                if (!bm.note.isNullOrBlank()) {
                    if (bm.type == BookmarkType.QUOTE) {
                        sb.append("  \"").append(bm.note.trim()).append("\"\n")
                    } else {
                        bm.note.lines().forEach { line ->
                            sb.append("  ").append(line).append("\n")
                        }
                    }
                }
                sb.append("\n")
            }
        }
        return sb.toString()
    }

    private fun generateAllMarkdown(booksData: List<BookExportData>): String {
        val sb = StringBuilder()
        sb.append("# Evenfall Audiobooks — All Bookmarks & Notes\n\n")

        val booksWithBookmarks = booksData.filter { it.bookmarks.isNotEmpty() }
        if (booksWithBookmarks.isEmpty()) {
            sb.append("*No bookmarks or notes saved in library.*\n")
            return sb.toString()
        }

        for ((index, item) in booksWithBookmarks.withIndex()) {
            if (index > 0) sb.append("\n---\n\n")
            sb.append(generateBookMarkdown(item.book, item.bookmarks, item.chapters))
        }
        return sb.toString()
    }

    private fun generateAllPlainText(booksData: List<BookExportData>): String {
        val sb = StringBuilder()
        sb.append("==================================================\n")
        sb.append("Evenfall Audiobooks — All Bookmarks & Notes\n")
        sb.append("==================================================\n\n")

        val booksWithBookmarks = booksData.filter { it.bookmarks.isNotEmpty() }
        if (booksWithBookmarks.isEmpty()) {
            sb.append("No bookmarks or notes saved in library.\n")
            return sb.toString()
        }

        for (item in booksWithBookmarks) {
            sb.append(generateBookPlainText(item.book, item.bookmarks, item.chapters))
            sb.append("\n")
        }
        return sb.toString()
    }

    private fun formatType(type: BookmarkType): String {
        return when (type) {
            BookmarkType.BOOKMARK -> "Bookmark"
            BookmarkType.NOTE -> "Note"
            BookmarkType.QUOTE -> "Quote"
            BookmarkType.FAVORITE -> "Favorite"
            BookmarkType.ALL -> "Bookmark"
        }
    }

    fun shareExport(context: Context, text: String, title: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_TITLE, title)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(shareIntent)
    }

    fun writeToUri(context: Context, uri: Uri, content: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                OutputStreamWriter(os).use { writer ->
                    writer.write(content)
                    writer.flush()
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
