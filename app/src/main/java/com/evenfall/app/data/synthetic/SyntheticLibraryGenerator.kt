package com.evenfall.app.data.synthetic

import com.evenfall.app.data.db.dao.BookDao
import com.evenfall.app.data.db.dao.BookmarkDao
import com.evenfall.app.data.db.dao.ChapterDao
import com.evenfall.app.data.db.entity.BookEntity
import com.evenfall.app.data.db.entity.BookmarkEntity
import com.evenfall.app.data.db.entity.BookmarkType
import com.evenfall.app.data.db.entity.ChapterEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class SyntheticLibraryGenerator @Inject constructor(
    private val bookDao: BookDao,
    private val chapterDao: ChapterDao,
    private val bookmarkDao: BookmarkDao
) {

    private val authors = listOf(
        "Brandon Sanderson",
        "Frank Herbert",
        "Ursula K. Le Guin",
        "Robert Jordan",
        "J.R.R. Tolkien",
        "Robin Hobb",
        "Isaac Asimov",
        "Arthur C. Clarke",
        "Terry Pratchett",
        "Neil Gaiman",
        "Dan Simmons",
        "Philip K. Dick",
        "Mary Shelley",
        "H.G. Wells",
        "C.S. Lewis",
        "George R.R. Martin",
        "Patrick Rothfuss",
        "Joe Abercrombie",
        "N.K. Jemisin",
        "Ted Chiang"
    )

    private val bookNouns = listOf(
        "Kings", "Dune", "Light", "Shadow", "Stars", "Empire", "Blade", "Silence",
        "Winter", "Storm", "Dawn", "Dragon", "Memory", "Chronicle", "Void", "City",
        "Sun", "Ocean", "Echo", "Labyrinth", "Tower", "Spire", "Prophecy", "Galaxy"
    )

    private val bookModifiers = listOf(
        "Way of", "Children of", "Words of", "Oath of", "Rhythm of", "Fall of",
        "Rise of", "Lord of", "Hand of", "Secret of", "Tale of", "Songs of",
        "Whispers of", "Architects of", "Keepers of", "Remains of"
    )

    suspend fun generateSyntheticLibrary(bookCount: Int = 500): Int = withContext(Dispatchers.IO) {
        val random = Random(42) // Deterministic seed for reproducible testing
        val now = System.currentTimeMillis()

        val booksToInsert = ArrayList<BookEntity>(bookCount)

        for (i in 1..bookCount) {
            val author = authors[i % authors.size]
            val title = "${bookModifiers[(i * 3) % bookModifiers.size]} ${bookNouns[(i * 7) % bookNouns.size]} Vol. $i"

            // Ensure we create 20+ hour M4B files with 100+ chapters
            val isEpicM4B = i <= 20 || (i % 25 == 0)
            val totalDurationMs = if (isEpicM4B) {
                // 20 to 45 hours (72,000,000 to 162,000,000 ms)
                (20L * 3600 * 1000) + (random.nextLong(0, 25L * 3600 * 1000))
            } else {
                // 3 to 18 hours (10,800,000 to 64,800,000 ms)
                random.nextLong(3L * 3600 * 1000, 18L * 3600 * 1000)
            }

            // Book state distribution: 15% finished, 25% currently listening, 60% not started
            val stateRoll = i % 100
            val isFinished = stateRoll < 15
            val isCurrentlyReading = stateRoll in 15..39

            val currentPos = when {
                isFinished -> totalDurationMs
                isCurrentlyReading -> (totalDurationMs * random.nextDouble(0.1, 0.85)).toLong()
                else -> 0L
            }

            val lastPlayed = when {
                isFinished -> now - random.nextLong(86400000L, 30L * 86400000L)
                isCurrentlyReading -> now - random.nextLong(300000L, 7L * 86400000L)
                else -> 0L
            }

            val speed = if (isCurrentlyReading) listOf(1.0f, 1.15f, 1.25f, 1.5f).random(random) else 1.0f

            val book = BookEntity(
                title = title,
                author = author,
                folderUri = "synthetic://audiobooks/book_$i",
                folderIdentifier = "synthetic_book_$i",
                totalDurationMs = totalDurationMs,
                currentPositionMs = currentPos,
                currentChapterIndex = 0,
                chapterPositionMs = 0L,
                playbackSpeed = speed,
                isFinished = isFinished,
                finishedAt = if (isFinished) lastPlayed else null,
                lastPlayedTimestamp = lastPlayed,
                fileCount = if (isEpicM4B) 1 else random.nextInt(1, 12),
                seriesName = if (i % 3 == 0) "The ${bookNouns[(i * 2) % bookNouns.size]} Saga" else null,
                seriesOrder = if (i % 3 == 0) ((i % 5) + 1).toFloat() else null
            )
            booksToInsert.add(book)
        }

        val insertedIds = bookDao.insertAll(booksToInsert)

        // Generate chapters and bookmarks
        val allChapters = ArrayList<ChapterEntity>()
        val allBookmarks = ArrayList<BookmarkEntity>()

        for ((index, bookId) in insertedIds.withIndex()) {
            val book = booksToInsert[index]
            val isEpicM4B = index < 20 || (index % 25 == 0)
            val chapterCount = if (isEpicM4B) {
                // 100 to 140 chapters for epic M4Bs
                random.nextInt(100, 140)
            } else {
                random.nextInt(8, 30)
            }

            val avgChapterDuration = book.totalDurationMs / chapterCount
            var runningOffset = 0L

            var currentChapterIdx = 0
            var chapterPos = 0L

            for (ch in 0 until chapterCount) {
                val chDuration = if (ch == chapterCount - 1) {
                    (book.totalDurationMs - runningOffset).coerceAtLeast(60000L)
                } else {
                    avgChapterDuration
                }

                if (book.currentPositionMs in runningOffset..(runningOffset + chDuration)) {
                    currentChapterIdx = ch
                    chapterPos = book.currentPositionMs - runningOffset
                }

                allChapters.add(
                    ChapterEntity(
                        bookId = bookId,
                        chapterIndex = ch,
                        title = "Chapter ${ch + 1}: ${bookNouns[(ch * 3) % bookNouns.size]}",
                        startMs = runningOffset,
                        durationMs = chDuration,
                        fileUri = "synthetic://audiobooks/book_${index + 1}/ch_${ch + 1}.m4b"
                    )
                )

                // Add sample bookmarks for currently reading or finished books
                if ((book.isFinished || book.currentPositionMs > 0) && ch % 15 == 0) {
                    val bmType = if (ch % 30 == 0) BookmarkType.QUOTE else BookmarkType.NOTE
                    allBookmarks.add(
                        BookmarkEntity(
                            bookId = bookId,
                            chapterIndex = ch,
                            chapterTitle = "Chapter ${ch + 1}",
                            positionMs = runningOffset + (chDuration / 2),
                            bookProgressFraction = (runningOffset.toFloat() / book.totalDurationMs).coerceIn(0f, 1f),
                            title = "Key moment in Chapter ${ch + 1}",
                            note = if (bmType == BookmarkType.QUOTE) "The secret of getting ahead is getting started." else "Important character introduction.",
                            type = bmType
                        )
                    )
                }

                runningOffset += chDuration
            }

            // Update book with exact current chapter index & chapter position
            if (book.currentPositionMs > 0) {
                val updatedBook = book.copy(
                    id = bookId,
                    currentChapterIndex = currentChapterIdx,
                    chapterPositionMs = chapterPos
                )
                bookDao.update(updatedBook)
            }
        }

        // Batch insert chapters in chunks of 500 to keep memory flat
        allChapters.chunked(500).forEach { chunk ->
            chapterDao.insertAll(chunk)
        }

        allBookmarks.forEach { bm ->
            bookmarkDao.insert(bm)
        }

        insertedIds.size
    }

    suspend fun clearSyntheticBooks(): Int = withContext(Dispatchers.IO) {
        val allBooks = bookDao.getAllBooksSync()
        val syntheticBooks = allBooks.filter { it.folderUri.startsWith("synthetic://") }
        for (book in syntheticBooks) {
            chapterDao.deleteChaptersForBook(book.id)
            bookDao.deleteById(book.id)
        }
        syntheticBooks.size
    }
}
