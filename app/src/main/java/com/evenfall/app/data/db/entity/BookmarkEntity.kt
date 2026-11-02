package com.evenfall.app.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class BookmarkType {
    ALL,
    NOTE,
    FAVORITE,
    QUOTE,
    BOOKMARK
}

@Entity(
    tableName = "bookmarks",
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
        Index(value = ["bookId", "chapterIndex"]),
        Index(value = ["type"])
    ]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Long,
    val chapterIndex: Int,
    val chapterTitle: String,
    val positionMs: Long,
    val bookProgressFraction: Float,
    val title: String,
    val note: String? = null,
    val type: BookmarkType = BookmarkType.BOOKMARK,
    val timestamp: Long = System.currentTimeMillis()
)
