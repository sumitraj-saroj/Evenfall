package com.evenfall.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ScanIssueReason {
    UNREADABLE,
    NO_AUDIO_TRACK,
    UNSUPPORTED_CODEC,
    PERMISSION_DENIED,
    TIMEOUT
}

@Entity(tableName = "scan_issues")
data class ScanIssueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filePath: String,
    val fileName: String,
    val reason: ScanIssueReason,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)
