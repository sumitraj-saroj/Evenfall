package com.evenfall.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
