package com.evenfall.app.data.repository

import com.evenfall.app.data.db.dao.BookDao
import com.evenfall.app.data.db.dao.BookmarkDao
import com.evenfall.app.data.db.dao.ChapterDao
import com.evenfall.app.data.db.dao.SearchDao
import com.evenfall.app.data.db.entity.BookEntity
import com.evenfall.app.data.db.entity.BookmarkEntity
import com.evenfall.app.data.db.entity.ChapterEntity
import com.evenfall.app.data.db.entity.RecentSearchEntity
import com.evenfall.app.data.db.entity.SearchIndexFtsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.measureTimeMillis

data class BookmarkSearchResult(
    val bookmark: BookmarkEntity,
    val book: BookEntity?,
    val displayTitle: String,
    val displaySubtitle: String,
    val snippet: String,
    val positionMs: Long,
    val chapterIndex: Int
)

data class ChapterSearchResult(
    val chapter: ChapterEntity,
    val book: BookEntity?,
    val displayTitle: String,
    val displaySubtitle: String,
    val chapterIndex: Int,
    val startMs: Long
)

data class SearchResults(
    val books: List<BookEntity> = emptyList(),
    val bookmarks: List<BookmarkSearchResult> = emptyList(),
    val chapters: List<ChapterSearchResult> = emptyList(),
    val totalCount: Int = 0,
    val searchDurationMs: Long = 0L
)

@Singleton
class SearchRepository @Inject constructor(
    private val searchDao: SearchDao,
    private val bookDao: BookDao,
    private val chapterDao: ChapterDao,
    private val bookmarkDao: BookmarkDao
) {
    private val indexMutex = Mutex()

    fun getRecentSearches(): Flow<List<RecentSearchEntity>> = searchDao.getRecentSearches()

    suspend fun addRecentSearch(query: String) = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isNotBlank()) {
            searchDao.insertRecentSearch(
                RecentSearchEntity(
                    query = trimmed,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun deleteRecentSearch(query: String) = withContext(Dispatchers.IO) {
        searchDao.deleteRecentSearch(query)
    }

    suspend fun clearRecentSearches() = withContext(Dispatchers.IO) {
        searchDao.clearRecentSearches()
    }

    suspend fun ensureIndexPopulated() = withContext(Dispatchers.IO) {
        indexMutex.withLock {
            val count = searchDao.getIndexCount()
            if (count == 0) {
                rebuildSearchIndexInternal()
            }
        }
    }

    suspend fun rebuildSearchIndex() = withContext(Dispatchers.IO) {
        indexMutex.withLock {
            rebuildSearchIndexInternal()
        }
    }

    private suspend fun rebuildSearchIndexInternal() {
        val allBooks = bookDao.getAllBooksSync()
        val allChapters = chapterDao.getAllChaptersSync()
        val allBookmarks = bookmarkDao.getAllBookmarksSync()

        val bookMap = allBooks.associateBy { it.id }

        val entries = ArrayList<SearchIndexFtsEntity>(allBooks.size + allChapters.size + allBookmarks.size)

        // 1. Books
        for (book in allBooks) {
            val seriesInfo = book.displaySeriesName ?: ""
            val narratorInfo = book.narrator ?: ""
            val content = "$seriesInfo $narratorInfo".trim()

            entries.add(
                SearchIndexFtsEntity(
                    rowid = 0,
                    entityType = "BOOK",
                    bookId = book.id,
                    targetId = book.id,
                    title = book.displayTitle,
                    subtitle = book.displayAuthor,
                    content = content,
                    positionMs = 0L,
                    chapterIndex = 0
                )
            )
        }

        // 2. Chapters
        for (chapter in allChapters) {
            val book = bookMap[chapter.bookId]
            val bookTitle = book?.displayTitle ?: ""

            entries.add(
                SearchIndexFtsEntity(
                    rowid = 0,
                    entityType = "CHAPTER",
                    bookId = chapter.bookId,
                    targetId = chapter.id,
                    title = chapter.displayTitle,
                    subtitle = bookTitle,
                    content = "",
                    positionMs = chapter.startMs,
                    chapterIndex = chapter.chapterIndex
                )
            )
        }

        // 3. Bookmarks & Notes
        for (bookmark in allBookmarks) {
            val book = bookMap[bookmark.bookId]
            val bookTitle = book?.displayTitle ?: ""
            val title = bookmark.title.ifBlank { "Bookmark" }
            val subtitle = if (bookmark.chapterTitle.isNotBlank()) {
                "$bookTitle • ${bookmark.chapterTitle}"
            } else {
                bookTitle
            }

            entries.add(
                SearchIndexFtsEntity(
                    rowid = 0,
                    entityType = "BOOKMARK",
                    bookId = bookmark.bookId,
                    targetId = bookmark.id,
                    title = title,
                    subtitle = subtitle,
                    content = bookmark.note ?: "",
                    positionMs = bookmark.positionMs,
                    chapterIndex = bookmark.chapterIndex
                )
            )
        }

        searchDao.rebuildSearchIndex(entries)
    }

    suspend fun search(rawQuery: String): SearchResults = withContext(Dispatchers.IO) {
        val sanitized = sanitizeFtsQuery(rawQuery)
        if (sanitized.isBlank()) {
            return@withContext SearchResults()
        }

        ensureIndexPopulated()

        var ftsMatches: List<SearchIndexFtsEntity>
        val durationMs = measureTimeMillis {
            ftsMatches = try {
                searchDao.searchFts(sanitized)
            } catch (_: Exception) {
                emptyList()
            }
        }

        val allBooks = bookDao.getAllBooksSync()
        val bookMap = allBooks.associateBy { it.id }
        val allChapters = chapterDao.getAllChaptersSync().associateBy { it.id }
        val allBookmarks = bookmarkDao.getAllBookmarksSync().associateBy { it.id }

        val matchedBooks = mutableListOf<BookEntity>()
        val matchedBookmarks = mutableListOf<BookmarkSearchResult>()
        val matchedChapters = mutableListOf<ChapterSearchResult>()

        val seenBookIds = HashSet<Long>()
        val seenBookmarkIds = HashSet<Long>()
        val seenChapterIds = HashSet<Long>()

        for (match in ftsMatches) {
            when (match.entityType) {
                "BOOK" -> {
                    if (seenBookIds.add(match.targetId)) {
                        bookMap[match.targetId]?.let { matchedBooks.add(it) }
                    }
                }
                "BOOKMARK" -> {
                    if (seenBookmarkIds.add(match.targetId)) {
                        val bookmark = allBookmarks[match.targetId]
                        if (bookmark != null) {
                            val book = bookMap[bookmark.bookId]
                            matchedBookmarks.add(
                                BookmarkSearchResult(
                                    bookmark = bookmark,
                                    book = book,
                                    displayTitle = bookmark.title.ifBlank { "Bookmark" },
                                    displaySubtitle = if (book != null) "${book.displayTitle} • Chapter ${bookmark.chapterIndex + 1}" else "Chapter ${bookmark.chapterIndex + 1}",
                                    snippet = bookmark.note ?: "",
                                    positionMs = bookmark.positionMs,
                                    chapterIndex = bookmark.chapterIndex
                                )
                            )
                        }
                    }
                }
                "CHAPTER" -> {
                    if (seenChapterIds.add(match.targetId)) {
                        val chapter = allChapters[match.targetId]
                        if (chapter != null) {
                            val book = bookMap[chapter.bookId]
                            matchedChapters.add(
                                ChapterSearchResult(
                                    chapter = chapter,
                                    book = book,
                                    displayTitle = chapter.displayTitle,
                                    displaySubtitle = book?.displayTitle ?: "",
                                    chapterIndex = chapter.chapterIndex,
                                    startMs = chapter.startMs
                                )
                            )
                        }
                    }
                }
            }
        }

        val total = matchedBooks.size + matchedBookmarks.size + matchedChapters.size
        SearchResults(
            books = matchedBooks,
            bookmarks = matchedBookmarks,
            chapters = matchedChapters,
            totalCount = total,
            searchDurationMs = durationMs
        )
    }

    companion object {
        fun sanitizeFtsQuery(query: String): String {
            // Keep alphanumeric and unicode letters/numbers, replace other characters with space
            val clean = query.replace(Regex("[^a-zA-Z0-9\\p{L}\\p{Nd}\\s]"), " ")
            val tokens = clean.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            if (tokens.isEmpty()) return ""
            return tokens.joinToString(" ") { "${it.lowercase()}*" }
        }
    }
}
