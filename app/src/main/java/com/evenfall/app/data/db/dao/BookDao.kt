package com.evenfall.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.evenfall.app.data.db.entity.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY lastPlayedTimestamp DESC, dateAdded DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE currentPositionMs > 0 AND isFinished = 0 ORDER BY lastPlayedTimestamp DESC")
    fun getCurrentBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE currentPositionMs = 0 AND isFinished = 0 ORDER BY dateAdded DESC")
    fun getNotStartedBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE isFinished = 1 ORDER BY lastPlayedTimestamp DESC")
    fun getFinishedBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    fun getBookById(id: Long): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookByIdSync(id: Long): BookEntity?

    @Query("SELECT * FROM books WHERE folderIdentifier = :folderIdentifier LIMIT 1")
    suspend fun getBookByFolderIdentifier(folderIdentifier: String): BookEntity?

    @Query("SELECT * FROM books ORDER BY lastPlayedTimestamp DESC LIMIT 1")
    fun getMostRecentlyPlayedBook(): Flow<BookEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(book: BookEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(books: List<BookEntity>): List<Long>

    @Update
    suspend fun update(book: BookEntity)

    @Query("""
        UPDATE books 
        SET currentPositionMs = :totalPosMs,
            currentChapterIndex = :chapterIdx,
            chapterPositionMs = :chapterPosMs,
            lastPlayedTimestamp = :lastPlayed,
            isFinished = CASE WHEN :totalPosMs >= totalDurationMs - 5000 THEN 1 ELSE isFinished END
        WHERE id = :id
    """)
    suspend fun updatePlaybackPosition(
        id: Long,
        totalPosMs: Long,
        chapterIdx: Int,
        chapterPosMs: Long,
        lastPlayed: Long
    )

    @Query("UPDATE books SET playbackSpeed = :speed WHERE id = :id")
    suspend fun updatePlaybackSpeed(id: Long, speed: Float)

    @Query("""
        UPDATE books 
        SET titleOverride = :titleOverride,
            authorOverride = :authorOverride,
            narratorOverride = :narratorOverride,
            seriesName = :seriesName,
            seriesOrder = :seriesOrder,
            coverOverrideUri = :coverOverrideUri
        WHERE id = :id
    """)
    suspend fun updateMetadataOverrides(
        id: Long,
        titleOverride: String?,
        authorOverride: String?,
        narratorOverride: String?,
        seriesName: String?,
        seriesOrder: Float?,
        coverOverrideUri: String?
    )

    @Query("""
        UPDATE books
        SET titleOverride = NULL,
            authorOverride = NULL,
            narratorOverride = NULL,
            seriesName = NULL,
            seriesOrder = NULL,
            coverOverrideUri = NULL
        WHERE id = :id
    """)
    suspend fun resetMetadataOverrides(id: Long)

    @Query("UPDATE books SET isFinished = :finished, finishedAt = :finishedAt WHERE id = :id")
    suspend fun updateFinishedStatus(id: Long, finished: Boolean, finishedAt: Long?)

    @Query("UPDATE books SET isUnavailable = :isUnavailable WHERE id = :id")
    suspend fun updateUnavailableStatus(id: Long, isUnavailable: Boolean)

    @Query("UPDATE books SET isUnavailable = :isUnavailable WHERE folderUri = :folderUri")
    suspend fun updateUnavailableStatusForFolder(folderUri: String, isUnavailable: Boolean)

    @Delete
    suspend fun delete(book: BookEntity)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteById(id: Long)
}
