package com.evenfall.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.evenfall.app.data.db.entity.BookCollectionCrossRef
import com.evenfall.app.data.db.entity.BookEntity
import com.evenfall.app.data.db.entity.CollectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {
    @Query("SELECT * FROM collections ORDER BY orderIndex ASC, name ASC")
    fun getAllCollections(): Flow<List<CollectionEntity>>

    @Query("SELECT * FROM collections ORDER BY orderIndex ASC, name ASC")
    suspend fun getAllCollectionsSync(): List<CollectionEntity>

    @Query("SELECT * FROM collections WHERE id = :id LIMIT 1")
    suspend fun getCollectionById(id: Long): CollectionEntity?

    @Query("SELECT * FROM collections WHERE name = :name LIMIT 1")
    suspend fun getCollectionByName(name: String): CollectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: CollectionEntity): Long

    @Update
    suspend fun updateCollection(collection: CollectionEntity)

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteCollection(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(crossRef: BookCollectionCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(crossRefs: List<BookCollectionCrossRef>)

    @Query("DELETE FROM book_collection_cross_ref WHERE bookId = :bookId AND collectionId = :collectionId")
    suspend fun deleteCrossRef(bookId: Long, collectionId: Long)

    @Query("DELETE FROM book_collection_cross_ref WHERE collectionId = :collectionId")
    suspend fun deleteCrossRefsForCollection(collectionId: Long)

    @Query("DELETE FROM book_collection_cross_ref WHERE bookId = :bookId")
    suspend fun deleteCrossRefsForBook(bookId: Long)

    @Query("""
        SELECT c.* FROM collections c
        INNER JOIN book_collection_cross_ref x ON c.id = x.collectionId
        WHERE x.bookId = :bookId
        ORDER BY c.orderIndex ASC, c.name ASC
    """)
    fun getCollectionsForBook(bookId: Long): Flow<List<CollectionEntity>>

    @Query("""
        SELECT c.* FROM collections c
        INNER JOIN book_collection_cross_ref x ON c.id = x.collectionId
        WHERE x.bookId = :bookId
        ORDER BY c.orderIndex ASC, c.name ASC
    """)
    suspend fun getCollectionsForBookSync(bookId: Long): List<CollectionEntity>

    @Query("""
        SELECT b.* FROM books b
        INNER JOIN book_collection_cross_ref x ON b.id = x.bookId
        WHERE x.collectionId = :collectionId
        ORDER BY b.lastPlayedTimestamp DESC, b.dateAdded DESC
    """)
    fun getBooksForCollection(collectionId: Long): Flow<List<BookEntity>>

    @Query("""
        SELECT b.* FROM books b
        INNER JOIN book_collection_cross_ref x ON b.id = x.bookId
        WHERE x.collectionId = :collectionId
    """)
    suspend fun getBooksForCollectionSync(collectionId: Long): List<BookEntity>

    @Query("SELECT COUNT(*) FROM book_collection_cross_ref WHERE collectionId = :collectionId")
    fun getBookCountForCollection(collectionId: Long): Flow<Int>

    @Transaction
    suspend fun setCollectionsForBook(bookId: Long, collectionIds: List<Long>) {
        deleteCrossRefsForBook(bookId)
        for (colId in collectionIds) {
            insertCrossRef(BookCollectionCrossRef(bookId = bookId, collectionId = colId))
        }
    }

    @Transaction
    suspend fun updateCollectionOrders(orderedCollections: List<CollectionEntity>) {
        for ((index, col) in orderedCollections.withIndex()) {
            updateCollection(col.copy(orderIndex = index))
        }
    }

    @Transaction
    suspend fun deleteCollectionAndAssociations(id: Long) {
        deleteCrossRefsForCollection(id)
        deleteCollection(id)
    }
}
