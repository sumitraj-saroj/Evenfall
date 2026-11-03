package com.evenfall.app.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "listening_sessions",
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
data class ListeningSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val listenedDurationMs: Long,
    val contentDurationMs: Long
)
