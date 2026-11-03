package com.evenfall.app.data.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.evenfall.app.data.datastore.PreferencesManager
import com.evenfall.app.data.db.dao.BookDao
import com.evenfall.app.data.db.dao.BookmarkDao
import com.evenfall.app.data.db.dao.ChapterDao
import com.evenfall.app.data.db.dao.HistoryDao
import com.evenfall.app.data.db.entity.BookEntity
import com.evenfall.app.data.db.entity.ChapterEntity
import com.evenfall.app.data.scanner.LibraryScanner
import com.evenfall.app.data.scanner.ScanProgress
import com.evenfall.app.data.scanner.ScannedBookPreview
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val bookDao: BookDao,
    private val chapterDao: ChapterDao,
    private val bookmarkDao: BookmarkDao,
    private val historyDao: HistoryDao,
    private val libraryScanner: LibraryScanner,
    private val preferencesManager: PreferencesManager
) {
    fun getAllBooks(): Flow<List<BookEntity>> = bookDao.getAllBooks()

    fun getCurrentBooks(): Flow<List<BookEntity>> = bookDao.getCurrentBooks()

    fun getNotStartedBooks(): Flow<List<BookEntity>> = bookDao.getNotStartedBooks()

    fun getFinishedBooks(): Flow<List<BookEntity>> = bookDao.getFinishedBooks()

    fun getUnavailableBooks(): Flow<List<BookEntity>> = bookDao.getUnavailableBooks()

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

    fun isFolderUriReachable(folderUriStr: String): Boolean {
        return try {
            val uri = Uri.parse(folderUriStr)
            val persisted = context.contentResolver.persistedUriPermissions.any {
                it.uri == uri && it.isReadPermission
            }
            if (!persisted) return false
            val doc = DocumentFile.fromTreeUri(context, uri)
            doc != null && doc.canRead() && doc.exists()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun checkFolderReachability(): List<String> = withContext(Dispatchers.IO) {
        val folders = preferencesManager.libraryFolders.first()
        val unreachableFolders = mutableListOf<String>()
        val allBooks = bookDao.getAllBooksSync()

        for (folderUriStr in folders) {
            if (!isFolderUriReachable(folderUriStr)) {
                unreachableFolders.add(folderUriStr)
            }
        }

        for (book in allBooks) {
            val isUnreachable = unreachableFolders.any { book.folderUri.startsWith(it) } || !isFolderUriReachable(book.folderUri)
            if (book.isUnavailable != isUnreachable) {
                bookDao.updateUnavailableStatus(book.id, isUnreachable)
            }
        }

        unreachableFolders
    }

    suspend fun executeRelink(
        matches: List<Pair<BookEntity, ScannedBookPreview>>,
        oldFolderUri: String? = null,
        newFolderUri: String? = null
    ) = withContext(Dispatchers.IO) {
        for ((oldBook, newPreview) in matches) {
            // Update book with new folder location, resetting isUnavailable to false.
            // Preserves 100% of positions, bookmarks, history, overrides, and finished state!
            val updatedBook = oldBook.copy(
                folderUri = newPreview.folderUri,
                folderIdentifier = newPreview.identifier,
                totalDurationMs = newPreview.durationMs,
                fileCount = newPreview.fileCount,
                isUnavailable = false
            )
            bookDao.update(updatedBook)

            val existingChapters = chapterDao.getChaptersForBookSync(oldBook.id).associateBy { it.chapterIndex }
            chapterDao.deleteChaptersForBook(oldBook.id)

            val newChapters = mutableListOf<ChapterEntity>()
            var runningOffset = 0L
            newPreview.files.forEachIndexed { index, file ->
                val prevOverride = existingChapters[index]?.titleOverride
                newChapters.add(
                    ChapterEntity(
                        bookId = oldBook.id,
                        chapterIndex = index,
                        title = file.title.ifEmpty { "Chapter ${index + 1}" },
                        titleOverride = prevOverride,
                        startMs = runningOffset,
                        durationMs = file.durationMs,
                        fileUri = file.uri.toString()
                    )
                )
                runningOffset += file.durationMs
            }
            chapterDao.insertAll(newChapters)
        }

        if (!oldFolderUri.isNullOrBlank() && !newFolderUri.isNullOrBlank()) {
            preferencesManager.removeLibraryFolder(oldFolderUri)
            preferencesManager.addLibraryFolder(newFolderUri)
        }
    }

    suspend fun transferBookData(sourceBookId: Long, targetBookId: Long) = withContext(Dispatchers.IO) {
        val source = bookDao.getBookByIdSync(sourceBookId) ?: return@withContext
        val target = bookDao.getBookByIdSync(targetBookId) ?: return@withContext

        val updatedTarget = target.copy(
            currentPositionMs = source.currentPositionMs,
            currentChapterIndex = source.currentChapterIndex,
            chapterPositionMs = source.chapterPositionMs,
            playbackSpeed = source.playbackSpeed,
            lastPlayedTimestamp = source.lastPlayedTimestamp,
            isFinished = source.isFinished,
            finishedAt = source.finishedAt,
            titleOverride = source.titleOverride,
            authorOverride = source.authorOverride,
            narratorOverride = source.narratorOverride,
            seriesName = source.seriesName,
            seriesOrder = source.seriesOrder,
            coverOverrideUri = source.coverOverrideUri,
            isUnavailable = false
        )
        bookDao.update(updatedTarget)

        bookmarkDao.transferBookmarksToBook(sourceBookId, targetBookId)
        historyDao.transferHistoryToBook(sourceBookId, targetBookId)
        bookDao.deleteById(sourceBookId)
    }

    private val _scanProgress = MutableStateFlow<ScanProgress?>(null)
    val scanProgress: StateFlow<ScanProgress?> = _scanProgress.asStateFlow()

    fun cancelScan() {
        libraryScanner.cancelScan()
    }

    suspend fun rescanLibrary(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            libraryScanner.resetCancellation()
            val folders = preferencesManager.libraryFolders.first()
            val mode = preferencesManager.scanMode.first()
            var totalBooksFound = 0
            for (folderUriStr in folders) {
                val uri = Uri.parse(folderUriStr)
                libraryScanner.scanFolderPreview(uri, mode).collect { progress ->
                    _scanProgress.value = progress
                    if (progress is ScanProgress.Found) {
                        libraryScanner.saveScannedBooks(progress.books)
                        totalBooksFound += progress.books.size
                    }
                }
            }
            _scanProgress.value = null
            Result.success(totalBooksFound)
        } catch (e: Exception) {
            _scanProgress.value = null
            Result.failure(e)
        }
    }
}
