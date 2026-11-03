package com.evenfall.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.evenfall.app.data.db.entity.ChapterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY chapterIndex ASC")
    fun getChaptersForBook(bookId: Long): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY chapterIndex ASC")
    suspend fun getChaptersForBookSync(bookId: Long): List<ChapterEntity>

    @Query("SELECT * FROM chapters")
    suspend fun getAllChaptersSync(): List<ChapterEntity>

    @Query("SELECT * FROM chapters WHERE bookId = :bookId AND chapterIndex = :index LIMIT 1")
    suspend fun getChapter(bookId: Long, index: Int): ChapterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chapters: List<ChapterEntity>)

    @Query("DELETE FROM chapters WHERE bookId = :bookId")
    suspend fun deleteChaptersForBook(bookId: Long)

    @Query("UPDATE chapters SET titleOverride = :titleOverride WHERE id = :chapterId")
    suspend fun updateChapterTitleOverride(chapterId: Long, titleOverride: String?)

    @Query("UPDATE chapters SET titleOverride = NULL WHERE bookId = :bookId")
    suspend fun resetChapterOverridesForBook(bookId: Long)
}
