package com.evenfall.app

import com.evenfall.app.data.db.dao.BookDao
import com.evenfall.app.data.db.dao.BookmarkDao
import com.evenfall.app.data.db.dao.ChapterDao
import com.evenfall.app.data.db.dao.SearchDao
import com.evenfall.app.data.db.entity.BookEntity
import com.evenfall.app.data.db.entity.BookmarkEntity
import com.evenfall.app.data.db.entity.BookmarkType
import com.evenfall.app.data.db.entity.ChapterEntity
import com.evenfall.app.data.db.entity.RecentSearchEntity
import com.evenfall.app.data.db.entity.SearchIndexFtsEntity
import com.evenfall.app.data.repository.SearchRepository
import com.evenfall.app.data.synthetic.SyntheticLibraryGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.system.measureTimeMillis

class SearchFtsTest {

    private class FakeSearchDao : SearchDao {
        val index = mutableListOf<SearchIndexFtsEntity>()
        val recentSearches = LinkedHashMap<String, RecentSearchEntity>()
        private var nextRowId = 1L
        private var mockTimestamp = 1000L

        override fun getRecentSearches(): Flow<List<RecentSearchEntity>> =
            MutableStateFlow(recentSearches.values.sortedByDescending { it.timestamp }.take(20))

        override suspend fun insertRecentSearch(search: RecentSearchEntity) {
            recentSearches[search.query] = search.copy(timestamp = mockTimestamp++)
        }

        override suspend fun deleteRecentSearch(query: String) {
            recentSearches.remove(query)
        }

        override suspend fun clearRecentSearches() {
            recentSearches.clear()
        }

        override suspend fun searchFts(query: String): List<SearchIndexFtsEntity> {
            val terms = query.split(" ").filter { it.isNotBlank() }.map { it.removeSuffix("*").lowercase() }
            if (terms.isEmpty()) return emptyList()

            return index.filter { entry ->
                val fullText = "${entry.title} ${entry.subtitle} ${entry.content}".lowercase()
                terms.all { term -> fullText.contains(term) }
            }
        }

        override suspend fun insertSearchIndex(item: SearchIndexFtsEntity) {
            index.add(item.copy(rowid = nextRowId++))
        }

        override suspend fun insertSearchIndices(items: List<SearchIndexFtsEntity>) {
            for (item in items) {
                index.add(item.copy(rowid = nextRowId++))
            }
        }

        override suspend fun deleteByBookId(bookId: Long) {
            index.removeAll { it.bookId == bookId }
        }

        override suspend fun clearSearchIndex() {
            index.clear()
        }

        override suspend fun getIndexCount(): Int = index.size
    }

    private class FakeBookDao : BookDao {
        val booksMap = LinkedHashMap<Long, BookEntity>()
        private var nextId = 1L

        override suspend fun insert(book: BookEntity): Long {
            val id = if (book.id == 0L) nextId++ else book.id
            booksMap[id] = book.copy(id = id)
            return id
        }
        override suspend fun insertAll(books: List<BookEntity>): List<Long> = books.map { insert(it) }
        override suspend fun update(book: BookEntity) { booksMap[book.id] = book }
        override suspend fun deleteById(id: Long) { booksMap.remove(id) }
        override fun getAllBooks(): Flow<List<BookEntity>> = MutableStateFlow(booksMap.values.toList())
        override suspend fun getAllBooksSync(): List<BookEntity> = booksMap.values.toList()
        override fun getBookById(id: Long): Flow<BookEntity?> = MutableStateFlow(booksMap[id])
        override suspend fun getBookByIdSync(id: Long): BookEntity? = booksMap[id]
        override suspend fun getBookByFolderIdentifier(folderIdentifier: String): BookEntity? =
            booksMap.values.firstOrNull { it.folderIdentifier == folderIdentifier }
        override fun getCurrentBooks(): Flow<List<BookEntity>> = MutableStateFlow(emptyList())
        override fun getNotStartedBooks(): Flow<List<BookEntity>> = MutableStateFlow(emptyList())
        override fun getFinishedBooks(): Flow<List<BookEntity>> = MutableStateFlow(emptyList())
        override fun getUnavailableBooks(): Flow<List<BookEntity>> = MutableStateFlow(emptyList())
        override fun getMostRecentlyPlayedBook(): Flow<BookEntity?> = MutableStateFlow(null)
        override suspend fun updateUnavailableStatus(id: Long, isUnavailable: Boolean) {}
        override suspend fun updateUnavailableStatusForFolder(folderUri: String, isUnavailable: Boolean) {}
        override suspend fun getUnavailableBooksSync(): List<BookEntity> = booksMap.values.filter { it.isUnavailable }
        override suspend fun updatePlaybackPosition(id: Long, totalPosMs: Long, chapterIdx: Int, chapterPosMs: Long, lastPlayed: Long) {}
        override suspend fun updatePlaybackSpeed(id: Long, speed: Float) {}
        override suspend fun updateMetadataOverrides(id: Long, titleOverride: String?, authorOverride: String?, narratorOverride: String?, seriesName: String?, seriesOrder: Float?, coverOverrideUri: String?) {}
        override suspend fun resetMetadataOverrides(id: Long) {}
        override suspend fun updateFinishedStatus(id: Long, isFinished: Boolean, finishedAt: Long?) {}
        override suspend fun updateSeriesTag(id: Long, seriesName: String?, seriesOrder: Float?) {}
        override suspend fun delete(book: BookEntity) { booksMap.remove(book.id) }
        override suspend fun updatePlaybackOverrides(id: Long, speed: Float?, skipBack: Int?, skipForward: Int?, silenceSkipping: Boolean?, volumeBoost: Float?, voiceBoostPreset: String?, monoDownmix: Boolean?, loudnessNormalization: Boolean?) {}
        override suspend fun resetPlaybackOverrides(id: Long) {}
        override suspend fun updateSpeedOverride(id: Long, speed: Float?) {}
        override suspend fun updateSkipBackOverride(id: Long, seconds: Int?) {}
        override suspend fun updateSkipForwardOverride(id: Long, seconds: Int?) {}
        override suspend fun updateSilenceSkippingOverride(id: Long, enabled: Boolean?) {}
        override suspend fun updateVolumeBoostOverride(id: Long, level: Float?) {}
        override suspend fun updateVoiceBoostPresetOverride(id: Long, preset: String?) {}
        override suspend fun updateMonoDownmixOverride(id: Long, enabled: Boolean?) {}
        override suspend fun updateLoudnessNormalizationOverride(id: Long, enabled: Boolean?) {}
    }

    private class FakeChapterDao : ChapterDao {
        val chaptersMap = LinkedHashMap<Long, MutableList<ChapterEntity>>()
        override suspend fun insertAll(chapters: List<ChapterEntity>) {
            for (ch in chapters) {
                chaptersMap.getOrPut(ch.bookId) { mutableListOf() }.add(ch)
            }
        }
        override fun getChaptersForBook(bookId: Long): Flow<List<ChapterEntity>> =
            MutableStateFlow(chaptersMap[bookId] ?: emptyList())
        override suspend fun getChaptersForBookSync(bookId: Long): List<ChapterEntity> =
            chaptersMap[bookId] ?: emptyList()
        override suspend fun getAllChaptersSync(): List<ChapterEntity> =
            chaptersMap.values.flatten()
        override suspend fun getChapter(bookId: Long, index: Int): ChapterEntity? =
            chaptersMap[bookId]?.firstOrNull { it.chapterIndex == index }
        override suspend fun deleteChaptersForBook(bookId: Long) { chaptersMap.remove(bookId) }
        override suspend fun updateChapterTitleOverride(chapterId: Long, titleOverride: String?) {}
        override suspend fun resetChapterOverridesForBook(bookId: Long) {}
    }

    private class FakeBookmarkDao : BookmarkDao {
        val bookmarks = mutableListOf<BookmarkEntity>()
        private var nextId = 1L
        override suspend fun insert(bookmark: BookmarkEntity): Long {
            val id = nextId++
            bookmarks.add(bookmark.copy(id = id))
            return id
        }
        override suspend fun delete(bookmark: BookmarkEntity) { bookmarks.removeAll { it.id == bookmark.id } }
        override suspend fun deleteById(id: Long) { bookmarks.removeAll { it.id == id } }
        override suspend fun update(bookmark: BookmarkEntity) {}
        override fun getBookmarksForBook(bookId: Long): Flow<List<BookmarkEntity>> =
            MutableStateFlow(bookmarks.filter { it.bookId == bookId })
        override fun getBookmarksForBookByType(bookId: Long, type: BookmarkType): Flow<List<BookmarkEntity>> =
            MutableStateFlow(bookmarks.filter { it.bookId == bookId && it.type == type })
        override fun getBookmarkCountForBook(bookId: Long): Flow<Int> =
            MutableStateFlow(bookmarks.count { it.bookId == bookId })
        override suspend fun getBookmarksForBookSync(bookId: Long): List<BookmarkEntity> =
            bookmarks.filter { it.bookId == bookId }
        override suspend fun getAllBookmarksSync(): List<BookmarkEntity> =
            bookmarks.toList()
        override suspend fun transferBookmarksToBook(oldBookId: Long, newBookId: Long) {}
    }

    private lateinit var searchDao: FakeSearchDao
    private lateinit var bookDao: FakeBookDao
    private lateinit var chapterDao: FakeChapterDao
    private lateinit var bookmarkDao: FakeBookmarkDao
    private lateinit var searchRepository: SearchRepository
    private lateinit var generator: SyntheticLibraryGenerator

    @Before
    fun setup() {
        searchDao = FakeSearchDao()
        bookDao = FakeBookDao()
        chapterDao = FakeChapterDao()
        bookmarkDao = FakeBookmarkDao()
        searchRepository = SearchRepository(searchDao, bookDao, chapterDao, bookmarkDao)
        generator = SyntheticLibraryGenerator(bookDao, chapterDao, bookmarkDao)
    }

    @Test
    fun testFtsQuerySanitization() {
        // Special characters removed, multiple whitespace collapsed, trailing * added
        assertEquals("dune*", SearchRepository.sanitizeFtsQuery("dune"))
        assertEquals("brandon* sanderson*", SearchRepository.sanitizeFtsQuery("Brandon Sanderson"))
        assertEquals("stormlight* archive*", SearchRepository.sanitizeFtsQuery("Stormlight-Archive!"))
        assertEquals("chapter*", SearchRepository.sanitizeFtsQuery("  Chapter#12  ".replace("#12", "")))
        assertEquals("", SearchRepository.sanitizeFtsQuery("!@#$%^&*()"))
        assertEquals("", SearchRepository.sanitizeFtsQuery("   "))
    }

    @Test
    fun testSearchLatencyUnder100MsOver500Books() = runTest {
        // Generate 500 books with chapters and bookmarks
        generator.generateSyntheticLibrary(bookCount = 500)
        assertEquals(500, bookDao.booksMap.size)

        // Rebuild FTS search index
        searchRepository.rebuildSearchIndex()
        assertTrue("Index should contain entries", searchDao.getIndexCount() > 500)

        // Perform search and measure latency
        val searchDuration = measureTimeMillis {
            val results = searchRepository.search("Kings")
            assertTrue("Expected results for query", results.totalCount > 0)
        }

        // Acceptance criteria: Search over 500 books returns results in under 100 ms
        assertTrue("Search took ${searchDuration}ms, must be under 100ms", searchDuration < 100)
    }

    @Test
    fun testGroupedResultsBooksChaptersBookmarks() = runTest {
        val bookId = bookDao.insert(
            BookEntity(
                title = "Mistborn: The Final Empire",
                author = "Brandon Sanderson",
                narrator = "Michael Kramer",
                seriesName = "Mistborn",
                seriesOrder = 1f,
                folderIdentifier = "mistborn_1",
                folderUri = "content://books/mistborn_1",
                totalDurationMs = 90_000_000L,
                dateAdded = 1000L
            )
        )

        chapterDao.insertAll(
            listOf(
                ChapterEntity(
                    bookId = bookId,
                    chapterIndex = 0,
                    title = "Prologue: Ash Fell From the Sky",
                    fileUri = "content://books/mistborn_1/prologue.mp3",
                    durationMs = 1_800_000L,
                    startMs = 0L
                ),
                ChapterEntity(
                    bookId = bookId,
                    chapterIndex = 1,
                    title = "Chapter 1: The Survivor of Hathsin",
                    fileUri = "content://books/mistborn_1/ch1.mp3",
                    durationMs = 2_400_000L,
                    startMs = 1_800_000L
                )
            )
        )

        bookmarkDao.insert(
            BookmarkEntity(
                bookId = bookId,
                chapterIndex = 1,
                chapterTitle = "Chapter 1",
                positionMs = 120_000L,
                bookProgressFraction = 0.05f,
                title = "Allomancy Rules",
                note = "Tin burns bright and enhances senses",
                type = BookmarkType.NOTE
            )
        )

        searchRepository.rebuildSearchIndex()

        // 1. Search by title -> matches Book
        val bookResults = searchRepository.search("Mistborn")
        assertEquals(1, bookResults.books.size)
        assertEquals("Mistborn: The Final Empire", bookResults.books[0].title)

        // 2. Search by chapter name -> matches Chapter
        val chapterResults = searchRepository.search("Survivor")
        assertEquals(1, chapterResults.chapters.size)
        assertEquals("Chapter 1: The Survivor of Hathsin", chapterResults.chapters[0].displayTitle)
        assertEquals(bookId, chapterResults.chapters[0].chapter.bookId)

        // 3. Search by bookmark note content -> matches Bookmark
        val bookmarkResults = searchRepository.search("Allomancy")
        assertEquals(1, bookmarkResults.bookmarks.size)
        assertEquals("Allomancy Rules", bookmarkResults.bookmarks[0].displayTitle)
        assertTrue(bookmarkResults.bookmarks[0].snippet.contains("Tin burns"))
        assertEquals(120_000L, bookmarkResults.bookmarks[0].positionMs)
    }

    @Test
    fun testRecentSearchesManagement() = runTest {
        searchRepository.addRecentSearch("sanderson")
        searchRepository.addRecentSearch("dune")
        searchRepository.addRecentSearch("tolkien")

        var recents = searchRepository.getRecentSearches().first()
        assertEquals(3, recents.size)
        assertEquals("tolkien", recents[0].query)

        // Delete single recent search
        searchRepository.deleteRecentSearch("dune")
        recents = searchRepository.getRecentSearches().first()
        assertEquals(2, recents.size)
        assertFalse(recents.any { it.query == "dune" })

        // Clear all recent searches
        searchRepository.clearRecentSearches()
        recents = searchRepository.getRecentSearches().first()
        assertTrue(recents.isEmpty())
    }
}
