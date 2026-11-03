package com.evenfall.app.data.backup

import com.evenfall.app.data.db.entity.BookmarkType
import com.evenfall.app.data.db.entity.HistoryEventType
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object BackupSerializer {
    const val CURRENT_SCHEMA_VERSION = 1
    const val APP_IDENTIFIER = "Evenfall"

    fun serialize(backup: EvenfallBackup): String {
        val root = JSONObject()
        root.put("schemaVersion", backup.schemaVersion)
        root.put("app", backup.app)
        root.put("createdAt", backup.createdAt)

        // Settings
        val settingsObj = JSONObject().apply {
            put("themeMode", backup.settings.themeMode)
            put("pureBlack", backup.settings.pureBlack)
            put("colorScheme", backup.settings.colorScheme)
            put("skipAmountSeconds", backup.settings.skipAmountSeconds)
            put("autoRewindEnabled", backup.settings.autoRewindEnabled)
            put("shakeToExtendEnabled", backup.settings.shakeToExtendEnabled)
            put("fadeOutDurationSeconds", backup.settings.fadeOutDurationSeconds)
            put("silenceSkippingEnabled", backup.settings.silenceSkippingEnabled)
            put("volumeBoostLevel", backup.settings.volumeBoostLevel.toDouble())
            put("defaultPlaybackSpeed", backup.settings.defaultPlaybackSpeed.toDouble())
            put("scanMode", backup.settings.scanMode)
            put("compactPlayer", backup.settings.compactPlayer)
            put("autoMarkFinished", backup.settings.autoMarkFinished)
        }
        root.put("settings", settingsObj)

        // Books
        val booksArray = JSONArray()
        for (book in backup.books) {
            val bookObj = JSONObject().apply {
                put("title", book.title)
                put("author", book.author)
                put("folderUri", book.folderUri)
                put("folderIdentifier", book.folderIdentifier)
                put("totalDurationMs", book.totalDurationMs)
                put("fileCount", book.fileCount)
                put("currentPositionMs", book.currentPositionMs)
                put("currentChapterIndex", book.currentChapterIndex)
                put("chapterPositionMs", book.chapterPositionMs)
                put("playbackSpeed", book.playbackSpeed.toDouble())
                put("isFinished", book.isFinished)
                if (book.finishedAt != null) put("finishedAt", book.finishedAt)
                put("lastPlayedTimestamp", book.lastPlayedTimestamp)

                // Overrides
                if (book.titleOverride != null) put("titleOverride", book.titleOverride)
                if (book.authorOverride != null) put("authorOverride", book.authorOverride)
                if (book.narrator != null) put("narrator", book.narrator)
                if (book.narratorOverride != null) put("narratorOverride", book.narratorOverride)
                if (book.seriesName != null) put("seriesName", book.seriesName)
                if (book.seriesOrder != null) put("seriesOrder", book.seriesOrder.toDouble())
                if (book.coverOverrideUri != null) put("coverOverrideUri", book.coverOverrideUri)

                // Chapters
                val chaptersArray = JSONArray()
                for (ch in book.chapters) {
                    val chObj = JSONObject().apply {
                        put("chapterIndex", ch.chapterIndex)
                        put("title", ch.title)
                        if (ch.titleOverride != null) put("titleOverride", ch.titleOverride)
                        put("startMs", ch.startMs)
                        put("durationMs", ch.durationMs)
                        put("fileUri", ch.fileUri)
                    }
                    chaptersArray.put(chObj)
                }
                put("chapters", chaptersArray)

                // Bookmarks
                val bookmarksArray = JSONArray()
                for (bm in book.bookmarks) {
                    val bmObj = JSONObject().apply {
                        put("chapterIndex", bm.chapterIndex)
                        if (bm.chapterTitle != null) put("chapterTitle", bm.chapterTitle)
                        put("positionMs", bm.positionMs)
                        put("title", bm.title)
                        put("type", bm.type.name)
                        if (bm.note != null) put("note", bm.note)
                        put("timestamp", bm.timestamp)
                    }
                    bookmarksArray.put(bmObj)
                }
                put("bookmarks", bookmarksArray)

                // History
                val historyArray = JSONArray()
                for (h in book.history) {
                    val hObj = JSONObject().apply {
                        put("eventType", h.eventType.name)
                        put("timestamp", h.timestamp)
                        put("positionMs", h.positionMs)
                        put("chapterIndex", h.chapterIndex)
                        put("durationInterruptedMs", h.durationInterruptedMs)
                    }
                    historyArray.put(hObj)
                }
                put("history", historyArray)
            }
            booksArray.put(bookObj)
        }
        root.put("books", booksArray)

        return root.toString(2)
    }

    fun deserialize(jsonString: String): EvenfallBackup {
        if (jsonString.isBlank()) {
            throw CorruptBackupException("Backup file is empty")
        }

        val root: JSONObject
        try {
            root = JSONObject(jsonString)
        } catch (e: JSONException) {
            throw CorruptBackupException("Invalid JSON syntax: ${e.message}", e)
        }

        val app = root.optString("app", "")
        if (app != APP_IDENTIFIER) {
            throw CorruptBackupException("File is not an Evenfall backup (found app: '$app')")
        }

        val schemaVersion = if (root.has("schemaVersion")) {
            root.optInt("schemaVersion", -1)
        } else if (root.has("version")) {
            root.optInt("version", -1)
        } else {
            -1
        }

        if (schemaVersion < 1) {
            throw CorruptBackupException("Backup file is missing valid schema version")
        }

        if (schemaVersion > CURRENT_SCHEMA_VERSION) {
            throw UnsupportedSchemaVersionException(
                schemaVersion,
                "Backup was created with schema version $schemaVersion, but this app only supports version $CURRENT_SCHEMA_VERSION. Please update Evenfall."
            )
        }

        val createdAt = root.optLong("createdAt", root.optLong("timestamp", System.currentTimeMillis()))

        // Settings
        val settingsObj = root.optJSONObject("settings")
        val settings = if (settingsObj != null) {
            BackupSettings(
                themeMode = settingsObj.optString("themeMode", "FOLLOW_SYSTEM"),
                pureBlack = settingsObj.optBoolean("pureBlack", false),
                colorScheme = settingsObj.optString("colorScheme", "EVENFALL_BLUE"),
                skipAmountSeconds = settingsObj.optInt("skipAmountSeconds", 20),
                autoRewindEnabled = settingsObj.optBoolean("autoRewindEnabled", true),
                shakeToExtendEnabled = settingsObj.optBoolean("shakeToExtendEnabled", true),
                fadeOutDurationSeconds = settingsObj.optInt("fadeOutDurationSeconds", 30),
                silenceSkippingEnabled = settingsObj.optBoolean("silenceSkippingEnabled", false),
                volumeBoostLevel = settingsObj.optDouble("volumeBoostLevel", 0.0).toFloat(),
                defaultPlaybackSpeed = settingsObj.optDouble("defaultPlaybackSpeed", 1.0).toFloat(),
                scanMode = settingsObj.optString("scanMode", "SUBFOLDER_IS_BOOK"),
                compactPlayer = settingsObj.optBoolean("compactPlayer", true),
                autoMarkFinished = settingsObj.optBoolean("autoMarkFinished", true)
            )
        } else {
            BackupSettings()
        }

        // Books
        val booksArray = root.optJSONArray("books") ?: JSONArray()
        val booksList = mutableListOf<BackupBook>()

        for (i in 0 until booksArray.length()) {
            val bookObj = booksArray.optJSONObject(i) ?: continue
            val title = bookObj.optString("title", "Unknown Title")
            val author = bookObj.optString("author", "Unknown Author")
            val folderUri = bookObj.optString("folderUri", "")
            val folderIdentifier = bookObj.optString("folderIdentifier", folderUri)
            val totalDurationMs = bookObj.optLong("totalDurationMs", 0L)
            val fileCount = bookObj.optInt("fileCount", 1)
            val currentPositionMs = bookObj.optLong("currentPositionMs", 0L)
            val currentChapterIndex = bookObj.optInt("currentChapterIndex", 0)
            val chapterPositionMs = bookObj.optLong("chapterPositionMs", 0L)
            val playbackSpeed = bookObj.optDouble("playbackSpeed", 1.0).toFloat()
            val isFinished = bookObj.optBoolean("isFinished", false)
            val finishedAt = if (bookObj.has("finishedAt") && !bookObj.isNull("finishedAt")) bookObj.optLong("finishedAt") else null
            val lastPlayedTimestamp = bookObj.optLong("lastPlayedTimestamp", 0L)

            val titleOverride = if (bookObj.has("titleOverride") && !bookObj.isNull("titleOverride")) bookObj.optString("titleOverride") else null
            val authorOverride = if (bookObj.has("authorOverride") && !bookObj.isNull("authorOverride")) bookObj.optString("authorOverride") else null
            val narrator = if (bookObj.has("narrator") && !bookObj.isNull("narrator")) bookObj.optString("narrator") else null
            val narratorOverride = if (bookObj.has("narratorOverride") && !bookObj.isNull("narratorOverride")) bookObj.optString("narratorOverride") else null
            val seriesName = if (bookObj.has("seriesName") && !bookObj.isNull("seriesName")) bookObj.optString("seriesName") else null
            val seriesOrder = if (bookObj.has("seriesOrder") && !bookObj.isNull("seriesOrder")) bookObj.optDouble("seriesOrder").toFloat() else null
            val coverOverrideUri = if (bookObj.has("coverOverrideUri") && !bookObj.isNull("coverOverrideUri")) bookObj.optString("coverOverrideUri") else null

            // Chapters
            val chaptersArray = bookObj.optJSONArray("chapters") ?: JSONArray()
            val chapters = mutableListOf<BackupChapter>()
            for (c in 0 until chaptersArray.length()) {
                val chObj = chaptersArray.optJSONObject(c) ?: continue
                chapters.add(
                    BackupChapter(
                        chapterIndex = chObj.optInt("chapterIndex", c),
                        title = chObj.optString("title", "Chapter ${c + 1}"),
                        titleOverride = if (chObj.has("titleOverride") && !chObj.isNull("titleOverride")) chObj.optString("titleOverride") else null,
                        startMs = chObj.optLong("startMs", 0L),
                        durationMs = chObj.optLong("durationMs", 0L),
                        fileUri = chObj.optString("fileUri", "")
                    )
                )
            }

            // Bookmarks
            val bookmarksArray = bookObj.optJSONArray("bookmarks") ?: JSONArray()
            val bookmarks = mutableListOf<BackupBookmark>()
            for (b in 0 until bookmarksArray.length()) {
                val bmObj = bookmarksArray.optJSONObject(b) ?: continue
                val rawType = bmObj.optString("type", BookmarkType.BOOKMARK.name)
                val type = try { BookmarkType.valueOf(rawType) } catch (_: Exception) { BookmarkType.BOOKMARK }
                val timestamp = if (bmObj.has("timestamp")) bmObj.optLong("timestamp", System.currentTimeMillis()) else bmObj.optLong("createdAt", System.currentTimeMillis())
                bookmarks.add(
                    BackupBookmark(
                        chapterIndex = bmObj.optInt("chapterIndex", 0),
                        chapterTitle = if (bmObj.has("chapterTitle") && !bmObj.isNull("chapterTitle")) bmObj.optString("chapterTitle") else null,
                        positionMs = bmObj.optLong("positionMs", 0L),
                        title = bmObj.optString("title", "Bookmark"),
                        type = type,
                        note = if (bmObj.has("note") && !bmObj.isNull("note")) bmObj.optString("note") else null,
                        timestamp = timestamp
                    )
                )
            }

            // History
            val historyArray = bookObj.optJSONArray("history") ?: JSONArray()
            val history = mutableListOf<BackupHistory>()
            for (h in 0 until historyArray.length()) {
                val hObj = historyArray.optJSONObject(h) ?: continue
                val rawEvent = hObj.optString("eventType", HistoryEventType.PLAYED.name)
                val eventType = try { HistoryEventType.valueOf(rawEvent) } catch (_: Exception) { HistoryEventType.PLAYED }
                val duration = if (hObj.has("durationInterruptedMs")) {
                    hObj.optLong("durationInterruptedMs", 0L)
                } else if (hObj.has("interruptionDurationMs") && !hObj.isNull("interruptionDurationMs")) {
                    hObj.optLong("interruptionDurationMs", 0L)
                } else {
                    0L
                }
                history.add(
                    BackupHistory(
                        eventType = eventType,
                        timestamp = hObj.optLong("timestamp", 0L),
                        positionMs = hObj.optLong("positionMs", 0L),
                        chapterIndex = hObj.optInt("chapterIndex", 0),
                        durationInterruptedMs = duration
                    )
                )
            }

            booksList.add(
                BackupBook(
                    title = title,
                    author = author,
                    folderUri = folderUri,
                    folderIdentifier = folderIdentifier,
                    totalDurationMs = totalDurationMs,
                    fileCount = fileCount,
                    currentPositionMs = currentPositionMs,
                    currentChapterIndex = currentChapterIndex,
                    chapterPositionMs = chapterPositionMs,
                    playbackSpeed = playbackSpeed,
                    isFinished = isFinished,
                    finishedAt = finishedAt,
                    lastPlayedTimestamp = lastPlayedTimestamp,
                    titleOverride = titleOverride,
                    authorOverride = authorOverride,
                    narrator = narrator,
                    narratorOverride = narratorOverride,
                    seriesName = seriesName,
                    seriesOrder = seriesOrder,
                    coverOverrideUri = coverOverrideUri,
                    chapters = chapters,
                    bookmarks = bookmarks,
                    history = history
                )
            )
        }

        return EvenfallBackup(
            schemaVersion = schemaVersion,
            app = app,
            createdAt = createdAt,
            settings = settings,
            books = booksList
        )
    }
}
