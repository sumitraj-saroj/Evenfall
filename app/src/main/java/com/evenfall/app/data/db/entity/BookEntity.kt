package com.evenfall.app.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "books",
    indices = [
        Index(value = ["folderIdentifier"], unique = true),
        Index(value = ["lastPlayedTimestamp"])
    ]
)
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String,
    val coverUri: String? = null,
    val folderUri: String,
    val folderIdentifier: String,
    val totalDurationMs: Long,
    val currentPositionMs: Long = 0L,
    val currentChapterIndex: Int = 0,
    val chapterPositionMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isFinished: Boolean = false,
    val fileCount: Int = 1,
    val dateAdded: Long = System.currentTimeMillis(),
    val lastPlayedTimestamp: Long = 0L,
    val titleOverride: String? = null,
    val authorOverride: String? = null,
    val narrator: String? = null,
    val narratorOverride: String? = null,
    val seriesName: String? = null,
    val seriesOrder: Float? = null,
    val seriesNameOverride: String? = null,
    val seriesOrderOverride: Float? = null,
    val coverOverrideUri: String? = null,
    val finishedAt: Long? = null,
    val isUnavailable: Boolean = false
) {
    val displayTitle: String
        get() = titleOverride?.takeIf { it.isNotBlank() } ?: title

    val displayAuthor: String
        get() = authorOverride?.takeIf { it.isNotBlank() } ?: author

    val displayNarrator: String?
        get() = narratorOverride?.takeIf { it.isNotBlank() } ?: narrator

    val displaySeriesName: String?
        get() = seriesNameOverride?.takeIf { it.isNotBlank() } ?: seriesName

    val displaySeriesOrder: Float?
        get() = seriesOrderOverride ?: seriesOrder

    val displayCoverUri: String?
        get() = coverOverrideUri ?: coverUri
}
