package com.evenfall.app

import com.evenfall.app.data.db.dao.CollectionDao
import com.evenfall.app.data.db.entity.BookCollectionCrossRef
import com.evenfall.app.data.db.entity.BookEntity
import com.evenfall.app.data.db.entity.CollectionEntity
import com.evenfall.app.ui.screens.collections.ManageCollectionsViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CollectionsTest {

    private class FakeCollectionDao : CollectionDao {
        val collections = LinkedHashMap<Long, CollectionEntity>()
        val crossRefs = mutableListOf<BookCollectionCrossRef>()
        val booksMap = LinkedHashMap<Long, BookEntity>()
        private var nextId = 1L

        override fun getAllCollections(): Flow<List<CollectionEntity>> =
            MutableStateFlow(collections.values.sortedWith(compareBy({ it.orderIndex }, { it.name })))

        override suspend fun getAllCollectionsSync(): List<CollectionEntity> =
            collections.values.sortedWith(compareBy({ it.orderIndex }, { it.name }))

        override suspend fun getCollectionById(id: Long): CollectionEntity? = collections[id]

        override suspend fun getCollectionByName(name: String): CollectionEntity? =
            collections.values.firstOrNull { it.name.equals(name, ignoreCase = true) }

        override suspend fun insertCollection(collection: CollectionEntity): Long {
            val id = if (collection.id == 0L) nextId++ else collection.id
            collections[id] = collection.copy(id = id)
            return id
        }

        override suspend fun updateCollection(collection: CollectionEntity) {
            collections[collection.id] = collection
        }

        override suspend fun deleteCollection(id: Long) {
            collections.remove(id)
        }

        override suspend fun insertCrossRef(crossRef: BookCollectionCrossRef) {
            if (!crossRefs.contains(crossRef)) {
                crossRefs.add(crossRef)
            }
        }

        override suspend fun insertCrossRefs(crossRefs: List<BookCollectionCrossRef>) {
            for (cr in crossRefs) {
                insertCrossRef(cr)
            }
        }

        override suspend fun deleteCrossRef(bookId: Long, collectionId: Long) {
            crossRefs.removeAll { it.bookId == bookId && it.collectionId == collectionId }
        }

        override suspend fun deleteCrossRefsForCollection(collectionId: Long) {
            crossRefs.removeAll { it.collectionId == collectionId }
        }

        override suspend fun deleteCrossRefsForBook(bookId: Long) {
            crossRefs.removeAll { it.bookId == bookId }
        }

        override fun getCollectionsForBook(bookId: Long): Flow<List<CollectionEntity>> {
            val colIds = crossRefs.filter { it.bookId == bookId }.map { it.collectionId }.toSet()
            return MutableStateFlow(collections.values.filter { colIds.contains(it.id) }.sortedBy { it.orderIndex })
        }

        override suspend fun getCollectionsForBookSync(bookId: Long): List<CollectionEntity> {
            val colIds = crossRefs.filter { it.bookId == bookId }.map { it.collectionId }.toSet()
            return collections.values.filter { colIds.contains(it.id) }.sortedBy { it.orderIndex }
        }

        override fun getBooksForCollection(collectionId: Long): Flow<List<BookEntity>> {
            val bookIds = crossRefs.filter { it.collectionId == collectionId }.map { it.bookId }.toSet()
            return MutableStateFlow(booksMap.values.filter { bookIds.contains(it.id) })
        }

        override suspend fun getBooksForCollectionSync(collectionId: Long): List<BookEntity> {
            val bookIds = crossRefs.filter { it.collectionId == collectionId }.map { it.bookId }.toSet()
            return booksMap.values.filter { bookIds.contains(it.id) }
        }

        override fun getBookCountForCollection(collectionId: Long): Flow<Int> =
            MutableStateFlow(crossRefs.count { it.collectionId == collectionId })
    }

    private lateinit var collectionDao: FakeCollectionDao
    private lateinit var viewModel: ManageCollectionsViewModel

    @Before
    fun setup() {
        collectionDao = FakeCollectionDao()
        viewModel = ManageCollectionsViewModel(collectionDao)
    }

    @Test
    fun testCollectionCreationAndRenaming() = runTest {
        val sciFiId = collectionDao.insertCollection(CollectionEntity(name = "Sci-Fi", orderIndex = 0))
        val fantasyId = collectionDao.insertCollection(CollectionEntity(name = "Fantasy", orderIndex = 1))

        assertEquals(2, collectionDao.getAllCollectionsSync().size)

        val retrieved = collectionDao.getCollectionById(sciFiId)
        assertNotNull(retrieved)
        assertEquals("Sci-Fi", retrieved?.name)

        // Rename
        collectionDao.updateCollection(retrieved!!.copy(name = "Science Fiction"))
        val updated = collectionDao.getCollectionById(sciFiId)
        assertEquals("Science Fiction", updated?.name)
    }

    @Test
    fun testBookAssignmentToMultipleCollections() = runTest {
        val fictionId = collectionDao.insertCollection(CollectionEntity(name = "Fiction", orderIndex = 0))
        val favoritesId = collectionDao.insertCollection(CollectionEntity(name = "Favorites", orderIndex = 1))
        val bookId = 101L

        // Assign book to both collections
        collectionDao.setCollectionsForBook(bookId, listOf(fictionId, favoritesId))

        val cols = collectionDao.getCollectionsForBookSync(bookId)
        assertEquals(2, cols.size)
        assertTrue(cols.any { it.id == fictionId })
        assertTrue(cols.any { it.id == favoritesId })

        // Update: remove Favorites, keep Fiction
        collectionDao.setCollectionsForBook(bookId, listOf(fictionId))
        val updatedCols = collectionDao.getCollectionsForBookSync(bookId)
        assertEquals(1, updatedCols.size)
        assertEquals(fictionId, updatedCols[0].id)
    }

    @Test
    fun testDeleteCollectionPreservesBooks() = runTest {
        val colId = collectionDao.insertCollection(CollectionEntity(name = "Temporary", orderIndex = 0))
        val book1 = BookEntity(
            id = 1,
            title = "Dune",
            author = "Frank Herbert",
            folderIdentifier = "dune",
            folderUri = "content://dune",
            totalDurationMs = 50000L,
            dateAdded = 1000L
        )
        collectionDao.booksMap[1] = book1
        collectionDao.insertCrossRef(BookCollectionCrossRef(bookId = 1, collectionId = colId))

        assertEquals(1, collectionDao.crossRefs.size)

        // Delete collection and associations
        collectionDao.deleteCollectionAndAssociations(colId)

        // Collection is deleted
        assertNull(collectionDao.getCollectionById(colId))
        // Cross references are deleted
        assertTrue(collectionDao.crossRefs.isEmpty())
        // Book remains intact in library
        assertNotNull(collectionDao.booksMap[1])
        assertEquals("Dune", collectionDao.booksMap[1]?.title)
    }

    @Test
    fun testCollectionReordering() = runTest {
        val col1 = CollectionEntity(id = 1, name = "First", orderIndex = 0)
        val col2 = CollectionEntity(id = 2, name = "Second", orderIndex = 1)
        val col3 = CollectionEntity(id = 3, name = "Third", orderIndex = 2)

        collectionDao.insertCollection(col1)
        collectionDao.insertCollection(col2)
        collectionDao.insertCollection(col3)

        // Swap 1 and 2 (moving Second up to index 0)
        val reordered = listOf(col2, col1, col3)
        collectionDao.updateCollectionOrders(reordered)

        val result = collectionDao.getAllCollectionsSync()
        assertEquals("Second", result[0].name)
        assertEquals(0, result[0].orderIndex)
        assertEquals("First", result[1].name)
        assertEquals(1, result[1].orderIndex)
        assertEquals("Third", result[2].name)
        assertEquals(2, result[2].orderIndex)
    }
}
