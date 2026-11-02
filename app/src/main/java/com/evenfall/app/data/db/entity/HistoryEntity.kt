package com.evenfall.app.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class HistoryEventType {
    PLAYED,
    PAUSED,
    INTERRUPTED,
    SLEEP_TIMER_ON,
    SLEEP_TIMER_ENDED,
    SHOOK_TO_EXTEND
}

@Entity(
    tableName = "history",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["bookId"]),
        Index(value = ["timestamp"])
    ]
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Long,
    val eventType: HistoryEventType,
    val timestamp: Long = System.currentTimeMillis(),
    val positionMs: Long = 0L,
    val chapterIndex: Int = 0,
    val durationInterruptedMs: Long = 0L,
    val outputDevice: String? = null
)
