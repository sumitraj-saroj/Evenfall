package com.evenfall.app.data.repository

import android.net.Uri
import com.evenfall.app.data.datastore.PreferencesManager
import com.evenfall.app.data.db.dao.BookDao
import com.evenfall.app.data.db.entity.BookEntity
import com.evenfall.app.data.scanner.LibraryScanner
import com.evenfall.app.data.scanner.ScanProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepository @Inject constructor(
    private val bookDao: BookDao,
    private val libraryScanner: LibraryScanner,
    private val preferencesManager: PreferencesManager
) {
    fun getAllBooks(): Flow<List<BookEntity>> = bookDao.getAllBooks()

    fun getCurrentBooks(): Flow<List<BookEntity>> = bookDao.getCurrentBooks()

    fun getNotStartedBooks(): Flow<List<BookEntity>> = bookDao.getNotStartedBooks()

    fun getFinishedBooks(): Flow<List<BookEntity>> = bookDao.getFinishedBooks()

    fun getMostRecentBook(): Flow<BookEntity?> = bookDao.getMostRecentlyPlayedBook()

    fun searchBooks(query: String): Flow<List<BookEntity>> {
        val trimmed = query.trim().lowercase()
        return bookDao.getAllBooks().map { list ->
            if (trimmed.isEmpty()) list
            else list.filter {
                it.title.lowercase().contains(trimmed) || it.author.lowercase().contains(trimmed)
            }
        }.flowOn(Dispatchers.Default)
    }

    suspend fun rescanLibrary(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val folders = preferencesManager.libraryFolders.first()
            val mode = preferencesManager.scanMode.first()
            var totalBooksFound = 0
            for (folderUriStr in folders) {
                val uri = Uri.parse(folderUriStr)
                libraryScanner.scanFolderPreview(uri, mode).collect { progress ->
                    if (progress is ScanProgress.Found) {
                        libraryScanner.saveScannedBooks(progress.books)
                        totalBooksFound += progress.books.size
                    }
                }
            }
            Result.success(totalBooksFound)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
