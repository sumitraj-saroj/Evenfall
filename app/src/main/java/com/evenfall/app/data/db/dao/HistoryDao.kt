package com.evenfall.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.evenfall.app.data.db.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history WHERE bookId = :bookId ORDER BY timestamp DESC")
    fun getHistoryForBook(bookId: Long): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: HistoryEntity): Long

    @Query("DELETE FROM history WHERE bookId = :bookId AND timestamp < :cutoffTimestamp")
    suspend fun pruneOldHistory(bookId: Long, cutoffTimestamp: Long)

    @Query("DELETE FROM history WHERE id NOT IN (SELECT id FROM history WHERE bookId = :bookId ORDER BY timestamp DESC LIMIT :maxEvents) AND bookId = :bookId")
    suspend fun pruneOldEvents(bookId: Long, maxEvents: Int = 100)
}
