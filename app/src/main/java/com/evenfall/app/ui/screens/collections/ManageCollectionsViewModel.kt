package com.evenfall.app.ui.screens.collections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evenfall.app.data.db.dao.CollectionDao
import com.evenfall.app.data.db.entity.CollectionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CollectionItemUi(
    val collection: CollectionEntity,
    val bookCount: Int = 0
)

@HiltViewModel
class ManageCollectionsViewModel @Inject constructor(
    private val collectionDao: CollectionDao
) : ViewModel() {

    val collections: StateFlow<List<CollectionEntity>> = collectionDao.getAllCollections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _bookCounts = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val bookCounts: StateFlow<Map<Long, Int>> = _bookCounts.asStateFlow()

    init {
        loadBookCounts()
    }

    fun loadBookCounts() {
        viewModelScope.launch {
            val allCollections = collectionDao.getAllCollectionsSync()
            val counts = mutableMapOf<Long, Int>()
            for (col in allCollections) {
                val books = collectionDao.getBooksForCollectionSync(col.id)
                counts[col.id] = books.size
            }
            _bookCounts.value = counts
        }
    }

    fun createCollection(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            val existing = collections.value
            val newOrder = existing.size
            collectionDao.insertCollection(
                CollectionEntity(
                    name = trimmed,
                    orderIndex = newOrder
                )
            )
            loadBookCounts()
        }
    }

    fun renameCollection(id: Long, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            val current = collectionDao.getCollectionById(id) ?: return@launch
            collectionDao.updateCollection(current.copy(name = trimmed))
        }
    }

    fun deleteCollection(id: Long) {
        viewModelScope.launch {
            collectionDao.deleteCollectionAndAssociations(id)
            loadBookCounts()
        }
    }

    fun moveUp(index: Int) {
        val currentList = collections.value.toMutableList()
        if (index <= 0 || index >= currentList.size) return
        val temp = currentList[index]
        currentList[index] = currentList[index - 1]
        currentList[index - 1] = temp
        viewModelScope.launch {
            collectionDao.updateCollectionOrders(currentList)
        }
    }

    fun moveDown(index: Int) {
        val currentList = collections.value.toMutableList()
        if (index < 0 || index >= currentList.size - 1) return
        val temp = currentList[index]
        currentList[index] = currentList[index + 1]
        currentList[index + 1] = temp
        viewModelScope.launch {
            collectionDao.updateCollectionOrders(currentList)
        }
    }
}
