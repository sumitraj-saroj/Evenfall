package com.evenfall.app.data.backup

import com.evenfall.app.data.db.entity.BookmarkType
import com.evenfall.app.data.db.entity.HistoryEventType

data class EvenfallBackup(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val app: String = APP_IDENTIFIER,
    val createdAt: Long = System.currentTimeMillis(),
    val settings: BackupSettings = BackupSettings(),
    val books: List<BackupBook> = emptyList()
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
        const val APP_IDENTIFIER = "Evenfall"
    }
}

data class BackupSettings(
    val themeMode: String = "FOLLOW_SYSTEM",
    val pureBlack: Boolean = false,
    val colorScheme: String = "EVENFALL_BLUE",
    val skipAmountSeconds: Int = 20,
    val autoRewindEnabled: Boolean = true,
    val shakeToExtendEnabled: Boolean = true,
    val fadeOutDurationSeconds: Int = 30,
    val silenceSkippingEnabled: Boolean = false,
    val volumeBoostLevel: Float = 0f,
    val defaultPlaybackSpeed: Float = 1.0f,
    val scanMode: String = "SUBFOLDER_IS_BOOK",
    val compactPlayer: Boolean = true,
    val autoMarkFinished: Boolean = true
)

data class BackupBook(
    val title: String,
    val author: String,
    val folderUri: String,
    val folderIdentifier: String,
    val totalDurationMs: Long,
    val fileCount: Int,
    val currentPositionMs: Long = 0L,
    val currentChapterIndex: Int = 0,
    val chapterPositionMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isFinished: Boolean = false,
    val finishedAt: Long? = null,
    val lastPlayedTimestamp: Long = 0L,
    val titleOverride: String? = null,
    val authorOverride: String? = null,
    val narrator: String? = null,
    val narratorOverride: String? = null,
    val seriesName: String? = null,
    val seriesOrder: Float? = null,
    val coverOverrideUri: String? = null,
    val chapters: List<BackupChapter> = emptyList(),
    val bookmarks: List<BackupBookmark> = emptyList(),
    val history: List<BackupHistory> = emptyList()
)

data class BackupChapter(
    val chapterIndex: Int,
    val title: String,
    val titleOverride: String? = null,
    val startMs: Long = 0L,
    val durationMs: Long = 0L,
    val fileUri: String = ""
)

data class BackupBookmark(
    val chapterIndex: Int,
    val chapterTitle: String? = null,
    val positionMs: Long,
    val title: String,
    val type: BookmarkType = BookmarkType.BOOKMARK,
    val note: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class BackupHistory(
    val eventType: HistoryEventType,
    val timestamp: Long,
    val positionMs: Long,
    val chapterIndex: Int,
    val durationInterruptedMs: Long = 0L
)

class CorruptBackupException(message: String, cause: Throwable? = null) : Exception(message, cause)
class UnsupportedSchemaVersionException(val version: Int, message: String) : Exception(message)
