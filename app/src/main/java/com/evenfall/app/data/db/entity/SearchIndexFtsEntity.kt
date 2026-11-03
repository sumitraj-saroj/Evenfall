package com.evenfall.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Entity(tableName = "search_index_fts")
@Fts4
data class SearchIndexFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowid: Long = 0,
    val entityType: String, // "BOOK", "BOOKMARK", "CHAPTER"
    val bookId: Long,
    val targetId: Long, // bookId, bookmarkId, or chapterId
    val title: String,
    val subtitle: String, // author, book title, etc.
    val content: String, // note, series, etc.
    val positionMs: Long = 0L,
    val chapterIndex: Int = 0
)
