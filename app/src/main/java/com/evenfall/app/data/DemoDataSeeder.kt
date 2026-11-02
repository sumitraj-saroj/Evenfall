package com.evenfall.app.data

import com.evenfall.app.data.db.dao.BookDao
import com.evenfall.app.data.db.dao.BookmarkDao
import com.evenfall.app.data.db.dao.ChapterDao
import com.evenfall.app.data.db.dao.HistoryDao
import com.evenfall.app.data.db.entity.BookEntity
import com.evenfall.app.data.db.entity.BookmarkEntity
import com.evenfall.app.data.db.entity.BookmarkType
import com.evenfall.app.data.db.entity.ChapterEntity
import com.evenfall.app.data.db.entity.HistoryEntity
import com.evenfall.app.data.db.entity.HistoryEventType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DemoDataSeeder @Inject constructor(
    private val bookDao: BookDao,
    private val chapterDao: ChapterDao,
    private val bookmarkDao: BookmarkDao,
    private val historyDao: HistoryDao
) {
    suspend fun seedDemoDataIfEmpty() = withContext(Dispatchers.IO) {
        val existing = bookDao.getBookByFolderIdentifier("demo_echoes_of_tomorrow")
        if (existing != null) return@withContext

        val now = System.currentTimeMillis()
        val oneDay = 24 * 3600 * 1000L

        // 1. Echoes of Tomorrow (Daniel Hartwell) - PRIMARY HERO BOOK
        val echoesTotalMs = (14 * 3600 + 27 * 60) * 1000L
        val echoesCurrentPosMs = (echoesTotalMs * 0.72).toLong() // 72%
        val echoesId = bookDao.insert(
            BookEntity(
                title = "Echoes of Tomorrow",
                author = "Daniel Hartwell",
                coverUri = null,
                folderUri = "content://com.evenfall.demo/echoes",
                folderIdentifier = "demo_echoes_of_tomorrow",
                totalDurationMs = echoesTotalMs,
                currentPositionMs = echoesCurrentPosMs,
                currentChapterIndex = 11, // Chapter 12 of 24
                chapterPositionMs = (34 * 60 + 18) * 1000L, // 34:18
                playbackSpeed = 1.0f,
                fileCount = 24,
                lastPlayedTimestamp = now // Most recently played -> Hero Card
            )
        )

        // Echoes Chapters
        val chapterTitles = listOf(
            "Prologue: Departure",
            "The Awakening",
            "Static on the Line",
            "Orbital Descent",
            "Atmosphere",
            "Dust and Shadows",
            "Forgotten Coordinates",
            "Mira",
            "Whispers in the Void",
            "Anomaly Detected",
            "Interference",
            "The Signal",
            "The Deep Trench",
            "Static",
            "Decryption",
            "Echo Chamber",
            "First Contact",
            "The Ark Protocol",
            "Gravitational Pulse",
            "The Beacon",
            "Relay Station 9",
            "Fractured Horizon",
            "Return Vector",
            "Epilogue: The Star Ahead"
        )
        val avgChapterMs = echoesTotalMs / chapterTitles.size
        chapterTitles.forEachIndexed { i, title ->
            chapterDao.insertAll(
                listOf(
                    ChapterEntity(
                        bookId = echoesId,
                        chapterIndex = i,
                        title = title,
                        startMs = i * avgChapterMs,
                        durationMs = avgChapterMs,
                        fileUri = "content://com.evenfall.demo/echoes/ch_$i.mp3"
                    )
                )
            )
        }

        // Echoes Bookmarks (matching Image 4)
        bookmarkDao.insert(
            BookmarkEntity(
                bookId = echoesId,
                chapterIndex = 2,
                chapterTitle = "Static on the Line",
                positionMs = (12 * 60 + 40) * 1000L,
                bookProgressFraction = 0.15f,
                title = "The first signal",
                note = "Mysterious pulse repeating every 12 seconds",
                type = BookmarkType.FAVORITE,
                timestamp = now - (oneDay * 45) // Sep 12
            )
        )
        bookmarkDao.insert(
            BookmarkEntity(
                bookId = echoesId,
                chapterIndex = 7,
                chapterTitle = "Mira",
                positionMs = (24 * 60 + 31) * 1000L,
                bookProgressFraction = 0.46f,
                title = "Mira's theory",
                note = "She argues the signal isn't artificial, but sentient",
                type = BookmarkType.QUOTE,
                timestamp = now - (oneDay * 3) // Thu
            )
        )
        bookmarkDao.insert(
            BookmarkEntity(
                bookId = echoesId,
                chapterIndex = 10,
                chapterTitle = "Interference",
                positionMs = (21 * 60 + 48) * 1000L,
                bookProgressFraction = 0.65f,
                title = "The hidden message",
                note = null,
                type = BookmarkType.NOTE,
                timestamp = now - oneDay // Yesterday
            )
        )
        bookmarkDao.insert(
            BookmarkEntity(
                bookId = echoesId,
                chapterIndex = 13,
                chapterTitle = "Static",
                positionMs = (36 * 60 + 44) * 1000L,
                bookProgressFraction = 0.83f,
                title = "Return to this later",
                note = "Key revelation regarding the origin coordinates",
                type = BookmarkType.BOOKMARK,
                timestamp = now - (oneDay * 2)
            )
        )

        // Echoes History (matching Image 5)
        historyDao.insert(
            HistoryEntity(
                bookId = echoesId,
                eventType = HistoryEventType.PAUSED,
                timestamp = now - (1000 * 60 * 75), // 8:15 AM
                positionMs = (34 * 60 + 18) * 1000L,
                chapterIndex = 11,
                outputDevice = "Car"
            )
        )
        historyDao.insert(
            HistoryEntity(
                bookId = echoesId,
                eventType = HistoryEventType.INTERRUPTED,
                timestamp = now - (1000 * 60 * 89), // 8:01 AM
                positionMs = (28 * 60 + 10) * 1000L,
                chapterIndex = 11,
                durationInterruptedMs = 3 * 60 * 1000L // 3 min
            )
        )
        historyDao.insert(
            HistoryEntity(
                bookId = echoesId,
                eventType = HistoryEventType.PLAYED,
                timestamp = now - (1000 * 60 * 98), // 7:52 AM
                positionMs = (14 * 60 + 40) * 1000L,
                chapterIndex = 11,
                outputDevice = "Car"
            )
        )
        historyDao.insert(
            HistoryEntity(
                bookId = echoesId,
                eventType = HistoryEventType.SLEEP_TIMER_ENDED,
                timestamp = now - (1000 * 60 * 620), // 11:10 PM last night
                positionMs = (14 * 60 + 48) * 1000L,
                chapterIndex = 11
            )
        )
        historyDao.insert(
            HistoryEntity(
                bookId = echoesId,
                eventType = HistoryEventType.SHOOK_TO_EXTEND,
                timestamp = now - (1000 * 60 * 650), // 10:40 PM last night
                positionMs = (31 * 60 + 58) * 1000L,
                chapterIndex = 10
            )
        )
        historyDao.insert(
            HistoryEntity(
                bookId = echoesId,
                eventType = HistoryEventType.SLEEP_TIMER_ENDED,
                timestamp = now - (1000 * 60 * 650),
                positionMs = (32 * 60 + 10) * 1000L,
                chapterIndex = 10
            )
        )
        historyDao.insert(
            HistoryEntity(
                bookId = echoesId,
                eventType = HistoryEventType.SLEEP_TIMER_ON,
                timestamp = now - (1000 * 60 * 680), // 10:10 PM last night
                positionMs = (2 * 60 + 10) * 1000L,
                chapterIndex = 10
            )
        )
        historyDao.insert(
            HistoryEntity(
                bookId = echoesId,
                eventType = HistoryEventType.PLAYED,
                timestamp = now - (1000 * 60 * 680),
                positionMs = 0L,
                chapterIndex = 10
            )
        )

        // 2. The Bookshop on Maple Street (Lena Morgan)
        val bookshopTotalMs = (8 * 3600 + 19 * 60) * 1000L
        val bookshopRemainingMs = (4 * 3600 + 19 * 60 + 55) * 1000L
        val bookshopCurrentPosMs = bookshopTotalMs - bookshopRemainingMs
        val bookshopId = bookDao.insert(
            BookEntity(
                title = "The Bookshop on Maple Street",
                author = "Lena Morgan",
                coverUri = null,
                folderUri = "content://com.evenfall.demo/bookshop",
                folderIdentifier = "demo_bookshop",
                totalDurationMs = bookshopTotalMs,
                currentPositionMs = bookshopCurrentPosMs,
                currentChapterIndex = 6,
                chapterPositionMs = (14 * 60) * 1000L,
                playbackSpeed = 1.0f,
                fileCount = 16,
                lastPlayedTimestamp = now - 1000 * 60 * 60 * 2
            )
        )
        val bookshopAvg = bookshopTotalMs / 16
        for (i in 0 until 16) {
            chapterDao.insertAll(
                listOf(
                    ChapterEntity(
                        bookId = bookshopId,
                        chapterIndex = i,
                        title = "Chapter ${i + 1}",
                        startMs = i * bookshopAvg,
                        durationMs = bookshopAvg,
                        fileUri = "content://com.evenfall.demo/bookshop/ch_$i.mp3"
                    )
                )
            )
        }
        bookmarkDao.insert(
            BookmarkEntity(
                bookId = bookshopId,
                chapterIndex = 2,
                chapterTitle = "Chapter 3",
                positionMs = (10 * 60) * 1000L,
                bookProgressFraction = 0.20f,
                title = "The Old Oak Desk",
                note = "Discovery of the hidden letter behind drawer three",
                type = BookmarkType.FAVORITE,
                timestamp = now - (oneDay * 5)
            )
        )
        bookmarkDao.insert(
            BookmarkEntity(
                bookId = bookshopId,
                chapterIndex = 6,
                chapterTitle = "Chapter 7",
                positionMs = (14 * 60) * 1000L,
                bookProgressFraction = 0.41f,
                title = "A cup of Earl Grey",
                note = "Rainy afternoon conversation",
                type = BookmarkType.NOTE,
                timestamp = now - (oneDay * 1)
            )
        )
        historyDao.insert(
            HistoryEntity(
                bookId = bookshopId,
                eventType = HistoryEventType.PLAYED,
                timestamp = now - (1000 * 60 * 120),
                positionMs = (14 * 60) * 1000L,
                chapterIndex = 6,
                outputDevice = "Headphones"
            )
        )
        historyDao.insert(
            HistoryEntity(
                bookId = bookshopId,
                eventType = HistoryEventType.PAUSED,
                timestamp = now - (1000 * 60 * 110),
                positionMs = (14 * 60 + 41) * 1000L,
                chapterIndex = 6
            )
        )

        // 3. Beyond the Horizon (Alexander Reeves)
        val horizonTotalMs = (11 * 3600 + 5 * 60) * 1000L
        val horizonRemainingMs = (7 * 3600 + 26 * 60 + 5) * 1000L
        val horizonCurrentPosMs = horizonTotalMs - horizonRemainingMs
        val horizonId = bookDao.insert(
            BookEntity(
                title = "Beyond the Horizon",
                author = "Alexander Reeves",
                coverUri = null,
                folderUri = "content://com.evenfall.demo/horizon",
                folderIdentifier = "demo_horizon",
                totalDurationMs = horizonTotalMs,
                currentPositionMs = horizonCurrentPosMs,
                currentChapterIndex = 7, // Chapter 8 of 20: The Compass Turns
                chapterPositionMs = (17 * 60 + 35) * 1000L, // 17:35
                playbackSpeed = 1.0f,
                fileCount = 20,
                lastPlayedTimestamp = now - 1000 * 60 * 60 * 5
            )
        )
        val horizonAvg = horizonTotalMs / 20
        for (i in 0 until 20) {
            val title = if (i == 7) "The Compass Turns" else "Chapter ${i + 1}"
            chapterDao.insertAll(
                listOf(
                    ChapterEntity(
                        bookId = horizonId,
                        chapterIndex = i,
                        title = title,
                        startMs = i * horizonAvg,
                        durationMs = horizonAvg,
                        fileUri = "content://com.evenfall.demo/horizon/ch_$i.mp3"
                    )
                )
            )
        }
        bookmarkDao.insert(
            BookmarkEntity(
                bookId = horizonId,
                chapterIndex = 7,
                chapterTitle = "The Compass Turns",
                positionMs = (6 * 60 + 43) * 1000L,
                bookProgressFraction = 0.38f,
                title = "The compass reading",
                note = "Needle swinging toward the ridge",
                type = BookmarkType.FAVORITE,
                timestamp = now - (oneDay * 2)
            )
        )
        historyDao.insert(
            HistoryEntity(
                bookId = horizonId,
                eventType = HistoryEventType.PLAYED,
                timestamp = now - (1000 * 60 * 300),
                positionMs = (17 * 60 + 35) * 1000L,
                chapterIndex = 7
            )
        )
    }
}
