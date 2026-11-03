package com.evenfall.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.evenfall.app.data.db.entity.RecentSearchEntity
import com.evenfall.app.data.db.entity.SearchIndexFtsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchDao {
    // Recent Searches
    @Query("SELECT * FROM recent_searches ORDER BY timestamp DESC LIMIT 20")
    fun getRecentSearches(): Flow<List<RecentSearchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentSearch(search: RecentSearchEntity)

    @Query("DELETE FROM recent_searches WHERE `query` = :query")
    suspend fun deleteRecentSearch(query: String)

    @Query("DELETE FROM recent_searches")
    suspend fun clearRecentSearches()

    // FTS Search
    @Query("""
        SELECT rowid, * FROM search_index_fts 
        WHERE search_index_fts MATCH :query
    """)
    suspend fun searchFts(query: String): List<SearchIndexFtsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchIndex(item: SearchIndexFtsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchIndices(items: List<SearchIndexFtsEntity>)

    @Query("DELETE FROM search_index_fts WHERE bookId = :bookId")
    suspend fun deleteByBookId(bookId: Long)

    @Query("DELETE FROM search_index_fts")
    suspend fun clearSearchIndex()

    @Query("SELECT COUNT(*) FROM search_index_fts")
    suspend fun getIndexCount(): Int

    @Transaction
    suspend fun rebuildSearchIndex(items: List<SearchIndexFtsEntity>) {
        clearSearchIndex()
        insertSearchIndices(items)
    }
}
