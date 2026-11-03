package com.evenfall.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.evenfall.app.data.db.entity.ListeningSessionEntity
import kotlinx.coroutines.flow.Flow

data class BookListeningStat(
    val bookId: Long,
    val totalListenedMs: Long,
    val totalContentMs: Long
)

@Dao
interface ListeningSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: ListeningSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<ListeningSessionEntity>): List<Long>

    @Query("SELECT * FROM listening_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<ListeningSessionEntity>>

    @Query("SELECT * FROM listening_sessions ORDER BY timestamp DESC")
    suspend fun getAllSessionsSync(): List<ListeningSessionEntity>

    @Query("SELECT * FROM listening_sessions WHERE timestamp >= :startMs AND timestamp <= :endMs ORDER BY timestamp ASC")
    fun getSessionsInRange(startMs: Long, endMs: Long): Flow<List<ListeningSessionEntity>>

    @Query("SELECT * FROM listening_sessions WHERE timestamp >= :startMs AND timestamp <= :endMs ORDER BY timestamp ASC")
    suspend fun getSessionsInRangeSync(startMs: Long, endMs: Long): List<ListeningSessionEntity>

    @Query("SELECT SUM(listenedDurationMs) FROM listening_sessions")
    fun getTotalListenedDuration(): Flow<Long?>

    @Query("SELECT SUM(contentDurationMs) FROM listening_sessions")
    fun getTotalContentDuration(): Flow<Long?>

    @Query("SELECT SUM(listenedDurationMs) FROM listening_sessions WHERE timestamp >= :startMs AND timestamp <= :endMs")
    fun getListenedDurationInRange(startMs: Long, endMs: Long): Flow<Long?>

    @Query("SELECT SUM(contentDurationMs) FROM listening_sessions WHERE timestamp >= :startMs AND timestamp <= :endMs")
    fun getContentDurationInRange(startMs: Long, endMs: Long): Flow<Long?>

    @Query("""
        SELECT bookId, SUM(listenedDurationMs) AS totalListenedMs, SUM(contentDurationMs) AS totalContentMs 
        FROM listening_sessions 
        GROUP BY bookId 
        ORDER BY totalListenedMs DESC 
        LIMIT :limit
    """)
    fun getTopBooksStats(limit: Int): Flow<List<BookListeningStat>>

    @Query("SELECT COUNT(*) FROM listening_sessions")
    fun getSessionCount(): Flow<Int>

    @Query("DELETE FROM listening_sessions")
    suspend fun deleteAll()
}
