package com.evenfall.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.evenfall.app.data.db.entity.BookmarkEntity
import com.evenfall.app.data.db.entity.BookmarkType
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY chapterIndex ASC, positionMs ASC")
    fun getBookmarksForBook(bookId: Long): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId AND type = :type ORDER BY chapterIndex ASC, positionMs ASC")
    fun getBookmarksForBookByType(bookId: Long, type: BookmarkType): Flow<List<BookmarkEntity>>

    @Query("SELECT COUNT(*) FROM bookmarks WHERE bookId = :bookId")
    fun getBookmarkCountForBook(bookId: Long): Flow<Int>

    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId")
    suspend fun getBookmarksForBookSync(bookId: Long): List<BookmarkEntity>

    @Query("UPDATE bookmarks SET bookId = :newBookId WHERE bookId = :oldBookId")
    suspend fun transferBookmarksToBook(oldBookId: Long, newBookId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Update
    suspend fun update(bookmark: BookmarkEntity)

    @Delete
    suspend fun delete(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteById(id: Long)
}
