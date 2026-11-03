package com.evenfall.app

import com.evenfall.app.data.backup.BackupBook
import com.evenfall.app.data.backup.BackupBookmark
import com.evenfall.app.data.backup.BackupChapter
import com.evenfall.app.data.backup.BackupFileInfo
import com.evenfall.app.data.backup.BackupHistory
import com.evenfall.app.data.backup.BackupMergeEngine
import com.evenfall.app.data.backup.BackupRotator
import com.evenfall.app.data.backup.BackupSerializer
import com.evenfall.app.data.backup.BackupSettings
import com.evenfall.app.data.backup.CorruptBackupException
import com.evenfall.app.data.backup.EvenfallBackup
import com.evenfall.app.data.backup.RestoreMergeMode
import com.evenfall.app.data.backup.UnsupportedSchemaVersionException
import com.evenfall.app.data.db.entity.BookEntity
import com.evenfall.app.data.db.entity.BookmarkEntity
import com.evenfall.app.data.db.entity.BookmarkType
import com.evenfall.app.data.db.entity.HistoryEntity
import com.evenfall.app.data.db.entity.HistoryEventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class BackupTest {

    @Test
    fun testBackupSerializationAndDeserialization() {
        val originalBackup = EvenfallBackup(
            schemaVersion = 1,
            app = "Evenfall",
            createdAt = 1700000000000L,
            settings = BackupSettings(
                themeMode = "DARK",
                pureBlack = true,
                colorScheme = "TEAL",
                skipAmountSeconds = 30,
                autoRewindEnabled = true,
                shakeToExtendEnabled = false,
                fadeOutDurationSeconds = 45,
                silenceSkippingEnabled = true,
                volumeBoostLevel = 1.5f,
                defaultPlaybackSpeed = 1.25f,
                scanMode = "SUBFOLDER_IS_BOOK",
                compactPlayer = true,
                autoMarkFinished = true
            ),
            books = listOf(
                BackupBook(
                    title = "The Way of Kings",
                    author = "Brandon Sanderson",
                    folderUri = "content://tree/primary:Audiobooks/TheWayOfKings",
                    folderIdentifier = "primary:Audiobooks/TheWayOfKings",
                    totalDurationMs = 150000000L,
                    fileCount = 2,
                    currentPositionMs = 4500000L,
                    currentChapterIndex = 1,
                    chapterPositionMs = 500000L,
                    playbackSpeed = 1.2f,
                    isFinished = false,
                    finishedAt = null,
                    lastPlayedTimestamp = 1700005000000L,
                    titleOverride = "The Way of Kings (Edh)",
                    authorOverride = "B. Sanderson",
                    narrator = "Michael Kramer & Kate Reading",
                    narratorOverride = "Michael Kramer",
                    seriesName = "The Stormlight Archive",
                    seriesOrder = 1.0f,
                    coverOverrideUri = "file:///data/user/0/app/covers/custom.jpg",
                    chapters = listOf(
                        BackupChapter(
                            chapterIndex = 0,
                            title = "Prelude to the Stormlight Archive",
                            titleOverride = "Prelude",
                            startMs = 0L,
                            durationMs = 4000000L,
                            fileUri = "content://tree/primary:Audiobooks/TheWayOfKings/01.mp3"
                        ),
                        BackupChapter(
                            chapterIndex = 1,
                            title = "Chapter 1: Stormblessed",
                            titleOverride = null,
                            startMs = 4000000L,
                            durationMs = 4500000L,
                            fileUri = "content://tree/primary:Audiobooks/TheWayOfKings/02.mp3"
                        )
                    ),
                    bookmarks = listOf(
                        BackupBookmark(
                            chapterIndex = 1,
                            chapterTitle = "Chapter 1: Stormblessed",
                            positionMs = 4200000L,
                            title = "Kaladin intro",
                            type = BookmarkType.QUOTE,
                            note = "Life before death, strength before weakness, journey before destination.",
                            timestamp = 1700001000000L
                        )
                    ),
                    history = listOf(
                        BackupHistory(
                            eventType = HistoryEventType.PAUSED,
                            timestamp = 1700005000000L,
                            positionMs = 4500000L,
                            chapterIndex = 1,
                            durationInterruptedMs = 300000L
                        )
                    )
                )
            )
        )

        val json = BackupSerializer.serialize(originalBackup)
        val deserialized = BackupSerializer.deserialize(json)

        assertEquals(1, deserialized.schemaVersion)
        assertEquals("Evenfall", deserialized.app)
        assertEquals(1700000000000L, deserialized.createdAt)

        // Verify settings
        assertEquals("DARK", deserialized.settings.themeMode)
        assertTrue(deserialized.settings.pureBlack)
        assertEquals("TEAL", deserialized.settings.colorScheme)
        assertEquals(30, deserialized.settings.skipAmountSeconds)
        assertTrue(deserialized.settings.autoRewindEnabled)
        assertFalse(deserialized.settings.shakeToExtendEnabled)
        assertEquals(45, deserialized.settings.fadeOutDurationSeconds)
        assertTrue(deserialized.settings.silenceSkippingEnabled)
        assertEquals(1.5f, deserialized.settings.volumeBoostLevel, 0.001f)
        assertEquals(1.25f, deserialized.settings.defaultPlaybackSpeed, 0.001f)
        assertTrue(deserialized.settings.compactPlayer)
        assertTrue(deserialized.settings.autoMarkFinished)

        // Verify books
        assertEquals(1, deserialized.books.size)
        val book = deserialized.books[0]
        assertEquals("The Way of Kings", book.title)
        assertEquals("Brandon Sanderson", book.author)
        assertEquals("The Way of Kings (Edh)", book.titleOverride)
        assertEquals("B. Sanderson", book.authorOverride)
        assertEquals("Michael Kramer", book.narratorOverride)
        assertEquals("The Stormlight Archive", book.seriesName)
        assertEquals(1.0f, book.seriesOrder ?: 0f, 0.001f)
        assertEquals("file:///data/user/0/app/covers/custom.jpg", book.coverOverrideUri)
        assertEquals(4500000L, book.currentPositionMs)
        assertEquals(1, book.currentChapterIndex)
        assertEquals(500000L, book.chapterPositionMs)
        assertEquals(1.2f, book.playbackSpeed, 0.001f)
        assertFalse(book.isFinished)

        // Chapters
        assertEquals(2, book.chapters.size)
        assertEquals("Prelude", book.chapters[0].titleOverride)
        assertEquals(4000000L, book.chapters[0].durationMs)
        assertEquals("Chapter 1: Stormblessed", book.chapters[1].title)

        // Bookmarks
        assertEquals(1, book.bookmarks.size)
        val bm = book.bookmarks[0]
        assertEquals("Kaladin intro", bm.title)
        assertEquals(BookmarkType.QUOTE, bm.type)
        assertEquals("Life before death, strength before weakness, journey before destination.", bm.note)

        // History
        assertEquals(1, book.history.size)
        val h = book.history[0]
        assertEquals(HistoryEventType.PAUSED, h.eventType)
        assertEquals(300000L, h.durationInterruptedMs)
    }

    @Test
    fun testSchemaVersioningAndCorruptHandling() {
        // Newer schema version should throw UnsupportedSchemaVersionException
        val futureJson = """
            {
                "schemaVersion": 99,
                "app": "Evenfall",
                "createdAt": 1700000000000,
                "settings": {},
                "books": []
            }
        """.trimIndent()
        val versionEx = assertThrows(UnsupportedSchemaVersionException::class.java) {
            BackupSerializer.deserialize(futureJson)
        }
        assertEquals(99, versionEx.version)

        // Invalid JSON syntax throws CorruptBackupException
        assertThrows(CorruptBackupException::class.java) {
            BackupSerializer.deserialize("{ invalid json syntax ...")
        }

        // Empty JSON throws CorruptBackupException
        assertThrows(CorruptBackupException::class.java) {
            BackupSerializer.deserialize("")
        }

        // Wrong app identifier throws CorruptBackupException
        val alienAppJson = """
            {
                "schemaVersion": 1,
                "app": "OtherApp",
                "books": []
            }
        """.trimIndent()
        assertThrows(CorruptBackupException::class.java) {
            BackupSerializer.deserialize(alienAppJson)
        }

        // Missing schema version throws CorruptBackupException
        val noVersionJson = """
            {
                "app": "Evenfall",
                "books": []
            }
        """.trimIndent()
        assertThrows(CorruptBackupException::class.java) {
            BackupSerializer.deserialize(noVersionJson)
        }
    }

    @Test
    fun testBackupRotation() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 3)
        }
        val filename = BackupRotator.formatBackupFileName(cal.timeInMillis)
        assertEquals("evenfall-backup-2026-11-03.json", filename)

        assertTrue(BackupRotator.isBackupFile("evenfall-backup-2026-11-03.json"))
        assertTrue(BackupRotator.isBackupFile("evenfall-backup-2026-10-31-auto.json"))
        assertFalse(BackupRotator.isBackupFile("other-file.json"))
        assertFalse(BackupRotator.isBackupFile("evenfall-backup.txt"))

        // Create 10 backup file entries
        val files = (1..10).map { day ->
            val dayStr = if (day < 10) "0$day" else "$day"
            BackupFileInfo(
                name = "evenfall-backup-2026-11-$dayStr.json",
                uriString = "content://backup/evenfall-backup-2026-11-$dayStr.json",
                lastModified = 1700000000000L + day * 86400000L
            )
        }

        // Keep 7 -> should delete the 3 oldest (days 01, 02, 03)
        val toDelete = BackupRotator.findFilesToDelete(files, keepCount = 7)
        assertEquals(3, toDelete.size)
        assertEquals("evenfall-backup-2026-11-03.json", toDelete[0].name)
        assertEquals("evenfall-backup-2026-11-02.json", toDelete[1].name)
        assertEquals("evenfall-backup-2026-11-01.json", toDelete[2].name)

        // When files count is <= 7, returns empty list
        val fewFiles = files.take(5)
        val noneToDelete = BackupRotator.findFilesToDelete(fewFiles, keepCount = 7)
        assertTrue(noneToDelete.isEmpty())
    }

    @Test
    fun testMergeRules() {
        val currentBook = BookEntity(
            id = 1L,
            title = "Dune",
            author = "Frank Herbert",
            folderUri = "content://audiobooks/dune",
            folderIdentifier = "dune_folder",
            totalDurationMs = 70000000L,
            fileCount = 1,
            currentPositionMs = 10000000L,
            currentChapterIndex = 2,
            chapterPositionMs = 100000L,
            playbackSpeed = 1.0f,
            isFinished = false,
            lastPlayedTimestamp = 1700000000000L, // older timestamp
            titleOverride = "Dune (1965)"
        )

        val backupBook = BackupBook(
            title = "Dune",
            author = "Frank Herbert",
            folderUri = "content://audiobooks/dune",
            folderIdentifier = "dune_folder",
            totalDurationMs = 70000000L,
            fileCount = 1,
            currentPositionMs = 25000000L,
            currentChapterIndex = 5,
            chapterPositionMs = 300000L,
            playbackSpeed = 1.25f,
            isFinished = false,
            lastPlayedTimestamp = 1700050000000L, // newer timestamp in backup
            titleOverride = "Dune Masterpiece",
            narratorOverride = "George Guidall"
        )

        val deviceBookmark = BookmarkEntity(
            id = 10L,
            bookId = 1L,
            chapterIndex = 2,
            chapterTitle = "Chapter 3",
            positionMs = 9500000L,
            bookProgressFraction = 0.13f,
            title = "Device Bookmark",
            type = BookmarkType.BOOKMARK,
            timestamp = 1700000000000L
        )

        val backupBookmark = BackupBookmark(
            chapterIndex = 5,
            chapterTitle = "Chapter 6",
            positionMs = 24000000L,
            title = "Backup Quote",
            type = BookmarkType.QUOTE,
            note = "Fear is the mind-killer.",
            timestamp = 1700050000000L
        )

        val backupBookWithBookmarks = backupBook.copy(
            bookmarks = listOf(backupBookmark)
        )

        // MERGE mode:
        // Since backup has newer lastPlayedTimestamp, adopts backup position.
        // Overrides: on-device titleOverride is preserved ("Dune (1965)"), but missing narratorOverride is adopted!
        // Bookmarks: unions the backup bookmark (1 new bookmark to insert).
        val mergeResult = BackupMergeEngine.mergeBook(
            currentBook = currentBook,
            currentBookmarks = listOf(deviceBookmark),
            currentHistory = emptyList(),
            backupBook = backupBookWithBookmarks,
            mode = RestoreMergeMode.MERGE
        )

        assertTrue(mergeResult.isPositionFromBackup)
        assertEquals(25000000L, mergeResult.updatedBook.currentPositionMs)
        assertEquals(5, mergeResult.updatedBook.currentChapterIndex)
        assertEquals(1.25f, mergeResult.updatedBook.playbackSpeed, 0.001f)
        assertEquals("Dune (1965)", mergeResult.updatedBook.titleOverride) // preserved device edit
        assertEquals("George Guidall", mergeResult.updatedBook.narratorOverride) // adopted backup narrator
        assertEquals(1, mergeResult.newBookmarksToInsert.size)
        assertEquals("Backup Quote", mergeResult.newBookmarksToInsert[0].title)

        // KEEP_BACKUP mode:
        // Overwrites titleOverride with backup's titleOverride ("Dune Masterpiece")
        val keepBackupResult = BackupMergeEngine.mergeBook(
            currentBook = currentBook,
            currentBookmarks = listOf(deviceBookmark),
            currentHistory = emptyList(),
            backupBook = backupBookWithBookmarks,
            mode = RestoreMergeMode.KEEP_BACKUP
        )
        assertEquals("Dune Masterpiece", keepBackupResult.updatedBook.titleOverride)
        assertEquals(25000000L, keepBackupResult.updatedBook.currentPositionMs)

        // KEEP_NEWER mode when current device book is newer:
        val brandNewDeviceBook = currentBook.copy(
            lastPlayedTimestamp = 1700999000000L, // much newer than backup
            currentPositionMs = 30000000L,
            currentChapterIndex = 6
        )
        val keepNewerResult = BackupMergeEngine.mergeBook(
            currentBook = brandNewDeviceBook,
            currentBookmarks = listOf(deviceBookmark),
            currentHistory = emptyList(),
            backupBook = backupBookWithBookmarks,
            mode = RestoreMergeMode.KEEP_NEWER
        )
        assertFalse(keepNewerResult.isPositionFromBackup)
        assertEquals(30000000L, keepNewerResult.updatedBook.currentPositionMs)
        assertEquals(6, keepNewerResult.updatedBook.currentChapterIndex)
    }
}
