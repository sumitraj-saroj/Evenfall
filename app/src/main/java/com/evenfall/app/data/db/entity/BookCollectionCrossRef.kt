package com.evenfall.app.data.db.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "book_collection_cross_ref",
    primaryKeys = ["bookId", "collectionId"],
    indices = [
        Index(value = ["collectionId"]),
        Index(value = ["bookId"])
    ]
)
data class BookCollectionCrossRef(
    val bookId: Long,
    val collectionId: Long
)
