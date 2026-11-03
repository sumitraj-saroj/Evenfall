package com.evenfall.app

import com.evenfall.app.data.backup.BackupBook
import com.evenfall.app.data.backup.BackupChapter
import com.evenfall.app.data.backup.BackupSerializer
import com.evenfall.app.data.backup.EvenfallBackup
import com.evenfall.app.data.db.dao.BookDao
import com.evenfall.app.data.db.dao.BookmarkDao
import com.evenfall.app.data.db.dao.ChapterDao
import com.evenfall.app.data.db.entity.BookEntity
import com.evenfall.app.data.db.entity.BookmarkEntity
import com.evenfall.app.data.db.entity.BookmarkType
import com.evenfall.app.data.db.entity.ChapterEntity
import com.evenfall.app.data.synthetic.SyntheticLibraryGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureTimeMillis

class PerformanceStressTest {

    private class FakeBookDao : BookDao {
        val booksMap = LinkedHashMap<Long, BookEntity>()
        private var nextId = 1L

        override suspend fun insert(book: BookEntity): Long {
            val id = if (book.id == 0L) nextId++ else book.id
            booksMap[id] = book.copy(id = id)
            return id
        }

        override suspend fun insertAll(books: List<BookEntity>): List<Long> {
            return books.map { insert(it) }
        }

        override suspend fun update(book: BookEntity) {
            booksMap[book.id] = book
        }

        override suspend fun deleteById(id: Long) {
            booksMap.remove(id)
        }

        override fun getAllBooks(): Flow<List<BookEntity>> = MutableStateFlow(booksMap.values.toList())
        override suspend fun getAllBooksSync(): List<BookEntity> = booksMap.values.toList()
        override fun getBookById(id: Long): Flow<BookEntity?> = MutableStateFlow(booksMap[id])
        override suspend fun getBookByIdSync(id: Long): BookEntity? = booksMap[id]
        override suspend fun getBookByFolderIdentifier(folderIdentifier: String): BookEntity? =
            booksMap.values.firstOrNull { it.folderIdentifier == folderIdentifier }

        override fun getCurrentBooks(): Flow<List<BookEntity>> =
            MutableStateFlow(booksMap.values.filter { it.currentPositionMs > 0 && !it.isFinished })

        override fun getNotStartedBooks(): Flow<List<BookEntity>> =
            MutableStateFlow(booksMap.values.filter { it.currentPositionMs == 0L && !it.isFinished })

        override fun getFinishedBooks(): Flow<List<BookEntity>> =
            MutableStateFlow(booksMap.values.filter { it.isFinished })

        override fun getUnavailableBooks(): Flow<List<BookEntity>> =
            MutableStateFlow(booksMap.values.filter { it.isUnavailable })

        override fun getMostRecentlyPlayedBook(): Flow<BookEntity?> =
            MutableStateFlow(booksMap.values.maxByOrNull { it.lastPlayedTimestamp })

        override suspend fun getMostRecentlyPlayedBookSync(): BookEntity? =
            booksMap.values.maxByOrNull { it.lastPlayedTimestamp }

        override suspend fun updateUnavailableStatus(id: Long, isUnavailable: Boolean) {
            val book = booksMap[id] ?: return
            booksMap[id] = book.copy(isUnavailable = isUnavailable)
        }

        override suspend fun updatePlaybackPosition(
            id: Long,
            totalPosMs: Long,
            chapterIdx: Int,
            chapterPosMs: Long,
            lastPlayed: Long
        ) {
            val book = booksMap[id] ?: return
            val isFinished = if (totalPosMs >= book.totalDurationMs - 5000) true else book.isFinished
            booksMap[id] = book.copy(
                currentPositionMs = totalPosMs,
                currentChapterIndex = chapterIdx,
                chapterPositionMs = chapterPosMs,
                lastPlayedTimestamp = lastPlayed,
                isFinished = isFinished
            )
        }

        override suspend fun updatePlaybackSpeed(id: Long, speed: Float) {
            val book = booksMap[id] ?: return
            booksMap[id] = book.copy(playbackSpeed = speed)
        }

        override suspend fun updateMetadataOverrides(
            id: Long,
            titleOverride: String?,
            authorOverride: String?,
            narratorOverride: String?,
            seriesName: String?,
            seriesOrder: Float?,
            coverOverrideUri: String?
        ) {
            val book = booksMap[id] ?: return
            booksMap[id] = book.copy(
                titleOverride = titleOverride,
                authorOverride = authorOverride,
                narratorOverride = narratorOverride,
                seriesName = seriesName,
                seriesOrder = seriesOrder,
                coverOverrideUri = coverOverrideUri
            )
        }

        override suspend fun resetMetadataOverrides(id: Long) {
            val book = booksMap[id] ?: return
            booksMap[id] = book.copy(
                titleOverride = null,
                authorOverride = null,
                narratorOverride = null,
                seriesName = null,
                seriesOrder = null,
                coverOverrideUri = null
            )
        }

        override suspend fun updateSeriesTag(id: Long, seriesName: String?, seriesOrder: Float?) {
            val book = booksMap[id] ?: return
            booksMap[id] = book.copy(seriesName = seriesName, seriesOrder = seriesOrder)
        }

        override suspend fun updateFinishedStatus(id: Long, finished: Boolean, finishedAt: Long?) {
            val book = booksMap[id] ?: return
            booksMap[id] = book.copy(isFinished = finished, finishedAt = finishedAt)
        }

        override suspend fun updateUnavailableStatusForFolder(folderUri: String, isUnavailable: Boolean) {
            for ((id, book) in booksMap.entries) {
                if (book.folderUri == folderUri) {
                    booksMap[id] = book.copy(isUnavailable = isUnavailable)
                }
            }
        }

        override suspend fun getUnavailableBooksSync(): List<BookEntity> =
            booksMap.values.filter { it.isUnavailable }

        override suspend fun delete(book: BookEntity) {
            booksMap.remove(book.id)
        }

        override suspend fun updatePlaybackOverrides(
            id: Long,
            speed: Float?,
            skipBack: Int?,
            skipForward: Int?,
            silenceSkipping: Boolean?,
            volumeBoost: Float?,
            voiceBoostPreset: String?,
            monoDownmix: Boolean?,
            loudnessNormalization: Boolean?
        ) {
            val book = booksMap[id] ?: return
            booksMap[id] = book.copy(
                speedOverride = speed,
                skipBackSecondsOverride = skipBack,
                skipForwardSecondsOverride = skipForward,
                silenceSkippingOverride = silenceSkipping,
                volumeBoostOverride = volumeBoost,
                voiceBoostPresetOverride = voiceBoostPreset,
                monoDownmixOverride = monoDownmix,
                loudnessNormalizationOverride = loudnessNormalization
            )
        }

        override suspend fun resetPlaybackOverrides(id: Long) {
            val book = booksMap[id] ?: return
            booksMap[id] = book.copy(
                speedOverride = null,
                skipBackSecondsOverride = null,
                skipForwardSecondsOverride = null,
                silenceSkippingOverride = null,
                volumeBoostOverride = null,
                voiceBoostPresetOverride = null,
                monoDownmixOverride = null,
                loudnessNormalizationOverride = null
            )
        }

        override suspend fun updateSpeedOverride(id: Long, speed: Float?) {
            val book = booksMap[id] ?: return
            booksMap[id] = book.copy(speedOverride = speed)
        }

        override suspend fun updateSkipBackOverride(id: Long, seconds: Int?) {
            val book = booksMap[id] ?: return
            booksMap[id] = book.copy(skipBackSecondsOverride = seconds)
        }

        override suspend fun updateSkipForwardOverride(id: Long, seconds: Int?) {
            val book = booksMap[id] ?: return
            booksMap[id] = book.copy(skipForwardSecondsOverride = seconds)
        }

        override suspend fun updateSilenceSkippingOverride(id: Long, enabled: Boolean?) {
            val book = booksMap[id] ?: return
            booksMap[id] = book.copy(silenceSkippingOverride = enabled)
        }

        override suspend fun updateVolumeBoostOverride(id: Long, level: Float?) {
            val book = booksMap[id] ?: return
            booksMap[id] = book.copy(volumeBoostOverride = level)
        }

        override suspend fun updateVoiceBoostPresetOverride(id: Long, preset: String?) {
            val book = booksMap[id] ?: return
            booksMap[id] = book.copy(voiceBoostPresetOverride = preset)
        }

        override suspend fun updateMonoDownmixOverride(id: Long, enabled: Boolean?) {
            val book = booksMap[id] ?: return
            booksMap[id] = book.copy(monoDownmixOverride = enabled)
        }

        override suspend fun updateLoudnessNormalizationOverride(id: Long, enabled: Boolean?) {
            val book = booksMap[id] ?: return
            booksMap[id] = book.copy(loudnessNormalizationOverride = enabled)
        }
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

        override suspend fun deleteChaptersForBook(bookId: Long) {
            chaptersMap.remove(bookId)
        }

        override suspend fun updateChapterTitleOverride(chapterId: Long, titleOverride: String?) {
            for (list in chaptersMap.values) {
                val idx = list.indexOfFirst { it.id == chapterId }
                if (idx != -1) {
                    list[idx] = list[idx].copy(titleOverride = titleOverride)
                    break
                }
            }
        }

        override suspend fun resetChapterOverridesForBook(bookId: Long) {
            chaptersMap[bookId]?.let { list ->
                for (i in list.indices) {
                    list[i] = list[i].copy(titleOverride = null)
                }
            }
        }
    }

    private class FakeBookmarkDao : BookmarkDao {
        val bookmarks = mutableListOf<BookmarkEntity>()
        private var nextId = 1L

        override suspend fun insert(bookmark: BookmarkEntity): Long {
            val id = nextId++
            bookmarks.add(bookmark.copy(id = id))
            return id
        }

        override suspend fun delete(bookmark: BookmarkEntity) {
            bookmarks.removeAll { it.id == bookmark.id }
        }

        override suspend fun deleteById(id: Long) {
            bookmarks.removeAll { it.id == id }
        }

        override suspend fun update(bookmark: BookmarkEntity) {
            val idx = bookmarks.indexOfFirst { it.id == bookmark.id }
            if (idx != -1) {
                bookmarks[idx] = bookmark
            }
        }

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

        override suspend fun transferBookmarksToBook(oldBookId: Long, newBookId: Long) {
            val transferred = bookmarks.filter { it.bookId == oldBookId }.map { it.copy(bookId = newBookId) }
            bookmarks.removeAll { it.bookId == oldBookId }
            bookmarks.addAll(transferred)
        }
    }

    @Test
    fun testSyntheticLibraryGenerationAndVolume() = runTest {
        val bookDao = FakeBookDao()
        val chapterDao = FakeChapterDao()
        val bookmarkDao = FakeBookmarkDao()
        val generator = SyntheticLibraryGenerator(bookDao, chapterDao, bookmarkDao)

        val generationDuration = measureTimeMillis {
            val count = generator.generateSyntheticLibrary(500)
            assertEquals(500, count)
        }

        // Must generate 500 books in reasonable JVM time (< 2.5 seconds)
        println("Generated 500 synthetic books with chapters in ${generationDuration}ms")
        assertTrue("Generation took too long: ${generationDuration}ms", generationDuration < 2500L)

        val allBooks = bookDao.getAllBooksSync()
        assertEquals(500, allBooks.size)

        // Verify presence of 20+ hour M4B files with 100+ chapters
        val epicM4BBooks = allBooks.filter { it.totalDurationMs >= 20L * 3600 * 1000 }
        assertTrue("Should contain multiple 20+ hour epic books", epicM4BBooks.size >= 10)

        var found100PlusChapters = false
        for (epicBook in epicM4BBooks) {
            val chapters = chapterDao.getChaptersForBookSync(epicBook.id)
            if (chapters.size >= 100) {
                found100PlusChapters = true
                break
            }
        }
        assertTrue("Expected at least one 20+ hour book with 100+ chapters", found100PlusChapters)

        // Verify clearSyntheticBooks cleans up cleanly
        val cleared = generator.clearSyntheticBooks()
        assertEquals(500, cleared)
        assertTrue(bookDao.getAllBooksSync().isEmpty())
    }

    @Test
    fun test500BooksBackupSerializationPerformance() = runTest {
        val bookDao = FakeBookDao()
        val chapterDao = FakeChapterDao()
        val bookmarkDao = FakeBookmarkDao()
        val generator = SyntheticLibraryGenerator(bookDao, chapterDao, bookmarkDao)

        generator.generateSyntheticLibrary(500)
        val allBooks = bookDao.getAllBooksSync()

        val backupBooks = allBooks.map { b ->
            val chs = chapterDao.getChaptersForBookSync(b.id).map { c ->
                BackupChapter(c.chapterIndex, c.title, c.titleOverride, c.startMs, c.durationMs, c.fileUri)
            }
            BackupBook(
                title = b.title,
                author = b.author,
                folderUri = b.folderUri,
                folderIdentifier = b.folderIdentifier,
                totalDurationMs = b.totalDurationMs,
                fileCount = b.fileCount,
                currentPositionMs = b.currentPositionMs,
                currentChapterIndex = b.currentChapterIndex,
                chapterPositionMs = b.chapterPositionMs,
                playbackSpeed = b.playbackSpeed,
                isFinished = b.isFinished,
                finishedAt = b.finishedAt,
                lastPlayedTimestamp = b.lastPlayedTimestamp,
                chapters = chs
            )
        }

        val backup = EvenfallBackup(books = backupBooks)

        var json = ""
        val serializeTime = measureTimeMillis {
            json = BackupSerializer.serialize(backup)
        }
        println("Serialized 500 books to JSON in ${serializeTime}ms (size: ${json.length / 1024} KB)")
        assertTrue("Serialization of 500 books must take < 500ms, took ${serializeTime}ms", serializeTime < 500L)

        var deserialized: EvenfallBackup? = null
        val deserializeTime = measureTimeMillis {
            deserialized = BackupSerializer.deserialize(json)
        }
        println("Deserialized 500 books from JSON in ${deserializeTime}ms")
        assertTrue("Deserialization of 500 books must take < 500ms, took ${deserializeTime}ms", deserializeTime < 500L)

        assertNotNull(deserialized)
        assertEquals(500, deserialized!!.books.size)
    }

    @Test
    fun testPositionPersistenceThroughSimulation() = runTest {
        val bookDao = FakeBookDao()
        val bookId = bookDao.insert(
            BookEntity(
                title = "The Way of Kings",
                author = "Brandon Sanderson",
                folderUri = "synthetic://wok",
                folderIdentifier = "wok",
                totalDurationMs = 150000000L,
                fileCount = 1,
                currentPositionMs = 0L,
                currentChapterIndex = 0,
                chapterPositionMs = 0L,
                playbackSpeed = 1.0f
            )
        )

        // Simulate 1,000 rapid playback progress saves as occurs during playback and Doze transitions
        val updatesCount = 1000
        val updateTime = measureTimeMillis {
            for (step in 1..updatesCount) {
                val currentBook = bookDao.getBookByIdSync(bookId)!!
                val newPosition = step * 1000L
                bookDao.update(
                    currentBook.copy(
                        currentPositionMs = newPosition,
                        chapterPositionMs = newPosition % 600000L,
                        currentChapterIndex = (newPosition / 600000L).toInt(),
                        lastPlayedTimestamp = System.currentTimeMillis()
                    )
                )
            }
        }

        println("Executed $updatesCount position persistence updates in ${updateTime}ms")
        val finalBook = bookDao.getBookByIdSync(bookId)!!
        assertEquals(1000000L, finalBook.currentPositionMs)
        assertEquals(1, finalBook.currentChapterIndex)
        assertEquals(400000L, finalBook.chapterPositionMs)
    }

    @Test
    fun testMemoryStabilityDuringLargeLibraryOperations() = runTest {
        val runtime = Runtime.getRuntime()
        System.gc()
        val memBefore = runtime.totalMemory() - runtime.freeMemory()

        val bookDao = FakeBookDao()
        val chapterDao = FakeChapterDao()
        val bookmarkDao = FakeBookmarkDao()
        val generator = SyntheticLibraryGenerator(bookDao, chapterDao, bookmarkDao)

        generator.generateSyntheticLibrary(500)

        System.gc()
        val memAfter = runtime.totalMemory() - runtime.freeMemory()
        val diffMb = (memAfter - memBefore) / (1024 * 1024)
        println("Memory delta after 500 books + chapters loaded: ${diffMb}MB")

        // 500 books with tens of thousands of chapters should consume minimal memory (< 50MB delta)
        assertTrue("Memory delta too high: ${diffMb}MB", diffMb < 50L)
    }
}
