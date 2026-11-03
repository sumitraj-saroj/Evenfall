package com.evenfall.app.data.backup

import com.evenfall.app.data.db.entity.BookEntity
import com.evenfall.app.data.db.entity.BookmarkEntity
import com.evenfall.app.data.db.entity.HistoryEntity
import kotlin.math.abs

enum class RestoreMergeMode {
    MERGE,       // Safe union: keeps newer position, unions bookmarks & history, preserves custom overrides
    KEEP_NEWER,  // Prioritizes on-device state if played more recently
    KEEP_BACKUP  // Fully restores backup position and overrides
}

data class MergedBookResult(
    val updatedBook: BookEntity,
    val newBookmarksToInsert: List<BookmarkEntity>,
    val newHistoryToInsert: List<HistoryEntity>,
    val isPositionFromBackup: Boolean
)

object BackupMergeEngine {

    /**
     * Merges a current on-device book and its bookmarks/history with a backup book,
     * according to the specified RestoreMergeMode.
     */
    fun mergeBook(
        currentBook: BookEntity,
        currentBookmarks: List<BookmarkEntity>,
        currentHistory: List<HistoryEntity>,
        backupBook: BackupBook,
        mode: RestoreMergeMode
    ): MergedBookResult {
        val isBackupNewer = backupBook.lastPlayedTimestamp > currentBook.lastPlayedTimestamp
        val adoptBackupPosition = when (mode) {
            RestoreMergeMode.KEEP_BACKUP -> true
            RestoreMergeMode.KEEP_NEWER -> isBackupNewer || (currentBook.lastPlayedTimestamp == 0L && backupBook.currentPositionMs > 0)
            RestoreMergeMode.MERGE -> isBackupNewer || (currentBook.lastPlayedTimestamp == 0L && backupBook.currentPositionMs > 0)
        }

        val finalCurrentPositionMs = if (adoptBackupPosition) backupBook.currentPositionMs else currentBook.currentPositionMs
        val finalCurrentChapterIndex = if (adoptBackupPosition) backupBook.currentChapterIndex else currentBook.currentChapterIndex
        val finalChapterPositionMs = if (adoptBackupPosition) backupBook.chapterPositionMs else currentBook.chapterPositionMs
        val finalLastPlayed = if (adoptBackupPosition) backupBook.lastPlayedTimestamp else currentBook.lastPlayedTimestamp
        val finalPlaybackSpeed = if (adoptBackupPosition) backupBook.playbackSpeed else currentBook.playbackSpeed

        // Finished state
        val finalIsFinished: Boolean
        val finalFinishedAt: Long?
        when (mode) {
            RestoreMergeMode.KEEP_BACKUP -> {
                finalIsFinished = backupBook.isFinished
                finalFinishedAt = backupBook.finishedAt
            }
            RestoreMergeMode.KEEP_NEWER -> {
                if (currentBook.lastPlayedTimestamp >= backupBook.lastPlayedTimestamp) {
                    finalIsFinished = currentBook.isFinished
                    finalFinishedAt = currentBook.finishedAt
                } else {
                    finalIsFinished = backupBook.isFinished
                    finalFinishedAt = backupBook.finishedAt
                }
            }
            RestoreMergeMode.MERGE -> {
                finalIsFinished = currentBook.isFinished || backupBook.isFinished
                finalFinishedAt = currentBook.finishedAt ?: backupBook.finishedAt
            }
        }

        // Overrides
        val finalTitleOverride = when (mode) {
            RestoreMergeMode.KEEP_BACKUP -> backupBook.titleOverride ?: currentBook.titleOverride
            RestoreMergeMode.KEEP_NEWER -> currentBook.titleOverride ?: backupBook.titleOverride
            RestoreMergeMode.MERGE -> currentBook.titleOverride ?: backupBook.titleOverride
        }
        val finalAuthorOverride = when (mode) {
            RestoreMergeMode.KEEP_BACKUP -> backupBook.authorOverride ?: currentBook.authorOverride
            RestoreMergeMode.KEEP_NEWER -> currentBook.authorOverride ?: backupBook.authorOverride
            RestoreMergeMode.MERGE -> currentBook.authorOverride ?: backupBook.authorOverride
        }
        val finalNarratorOverride = when (mode) {
            RestoreMergeMode.KEEP_BACKUP -> backupBook.narratorOverride ?: currentBook.narratorOverride
            RestoreMergeMode.KEEP_NEWER -> currentBook.narratorOverride ?: backupBook.narratorOverride
            RestoreMergeMode.MERGE -> currentBook.narratorOverride ?: backupBook.narratorOverride
        }
        val finalSeriesName = when (mode) {
            RestoreMergeMode.KEEP_BACKUP -> backupBook.seriesName ?: currentBook.seriesName
            RestoreMergeMode.KEEP_NEWER -> currentBook.seriesName ?: backupBook.seriesName
            RestoreMergeMode.MERGE -> currentBook.seriesName ?: backupBook.seriesName
        }
        val finalSeriesOrder = when (mode) {
            RestoreMergeMode.KEEP_BACKUP -> backupBook.seriesOrder ?: currentBook.seriesOrder
            RestoreMergeMode.KEEP_NEWER -> currentBook.seriesOrder ?: backupBook.seriesOrder
            RestoreMergeMode.MERGE -> currentBook.seriesOrder ?: backupBook.seriesOrder
        }
        val finalCoverOverride = when (mode) {
            RestoreMergeMode.KEEP_BACKUP -> backupBook.coverOverrideUri ?: currentBook.coverOverrideUri
            RestoreMergeMode.KEEP_NEWER -> currentBook.coverOverrideUri ?: backupBook.coverOverrideUri
            RestoreMergeMode.MERGE -> currentBook.coverOverrideUri ?: backupBook.coverOverrideUri
        }

        val updatedBook = currentBook.copy(
            currentPositionMs = finalCurrentPositionMs,
            currentChapterIndex = finalCurrentChapterIndex,
            chapterPositionMs = finalChapterPositionMs,
            lastPlayedTimestamp = finalLastPlayed,
            playbackSpeed = finalPlaybackSpeed,
            isFinished = finalIsFinished,
            finishedAt = finalFinishedAt,
            titleOverride = finalTitleOverride,
            authorOverride = finalAuthorOverride,
            narratorOverride = finalNarratorOverride,
            seriesName = finalSeriesName,
            seriesOrder = finalSeriesOrder,
            coverOverrideUri = finalCoverOverride
        )

        // Bookmarks union: insert backup bookmarks that don't already exist on device
        val newBookmarks = mutableListOf<BookmarkEntity>()
        for (bb in backupBook.bookmarks) {
            val exists = currentBookmarks.any { cb ->
                cb.chapterIndex == bb.chapterIndex && abs(cb.positionMs - bb.positionMs) <= 1000L && cb.type == bb.type
            }
            if (!exists) {
                newBookmarks.add(
                    BookmarkEntity(
                        bookId = currentBook.id,
                        chapterIndex = bb.chapterIndex,
                        chapterTitle = bb.chapterTitle ?: "Chapter ${bb.chapterIndex + 1}",
                        positionMs = bb.positionMs,
                        bookProgressFraction = if (currentBook.totalDurationMs > 0) (bb.positionMs.toFloat() / currentBook.totalDurationMs).coerceIn(0f, 1f) else 0f,
                        title = bb.title,
                        type = bb.type,
                        note = bb.note,
                        timestamp = bb.timestamp
                    )
                )
            }
        }

        // History union: insert backup history events that don't already exist on device
        val newHistory = mutableListOf<HistoryEntity>()
        for (bh in backupBook.history) {
            val exists = currentHistory.any { ch ->
                ch.timestamp == bh.timestamp && ch.eventType == bh.eventType
            }
            if (!exists) {
                newHistory.add(
                    HistoryEntity(
                        bookId = currentBook.id,
                        eventType = bh.eventType,
                        timestamp = bh.timestamp,
                        positionMs = bh.positionMs,
                        chapterIndex = bh.chapterIndex,
                        durationInterruptedMs = bh.durationInterruptedMs
                    )
                )
            }
        }

        return MergedBookResult(
            updatedBook = updatedBook,
            newBookmarksToInsert = newBookmarks,
            newHistoryToInsert = newHistory,
            isPositionFromBackup = adoptBackupPosition
        )
    }
}
