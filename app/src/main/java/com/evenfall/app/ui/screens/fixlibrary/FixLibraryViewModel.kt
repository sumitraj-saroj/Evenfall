package com.evenfall.app.ui.screens.fixlibrary

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evenfall.app.data.datastore.PreferencesManager
import com.evenfall.app.data.datastore.ScanMode
import com.evenfall.app.data.db.dao.BookDao
import com.evenfall.app.data.db.dao.ChapterDao
import com.evenfall.app.data.db.entity.BookEntity
import com.evenfall.app.data.repository.LibraryRepository
import com.evenfall.app.data.scanner.BookFileDescriptor
import com.evenfall.app.data.scanner.LibraryRelinker
import com.evenfall.app.data.scanner.LibraryScanner
import com.evenfall.app.data.scanner.NewBookMatchInfo
import com.evenfall.app.data.scanner.OldBookMatchInfo
import com.evenfall.app.data.scanner.RelinkAnalysis
import com.evenfall.app.data.scanner.ScanProgress
import com.evenfall.app.data.scanner.ScannedBookPreview
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FixLibraryUiState(
    val unreachableFolders: List<String> = emptyList(),
    val unavailableBooks: List<BookEntity> = emptyList(),
    val isScanning: Boolean = false,
    val scanMessage: String? = null,
    val analysis: RelinkAnalysis? = null,
    val selectedMatches: Map<Long, NewBookMatchInfo> = emptyMap(),
    val isApplying: Boolean = false,
    val isComplete: Boolean = false,
    val relinkedCount: Int = 0,
    val errorMessage: String? = null
)

@HiltViewModel
class FixLibraryViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val libraryScanner: LibraryScanner,
    private val bookDao: BookDao,
    private val chapterDao: ChapterDao,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(FixLibraryUiState())
    val uiState: StateFlow<FixLibraryUiState> = _uiState.asStateFlow()

    private var selectedTreeUri: Uri? = null

    init {
        refreshStatus()
    }

    fun refreshStatus() {
        viewModelScope.launch {
            val unreachable = libraryRepository.checkFolderReachability()
            val unavailable = bookDao.getUnavailableBooksSync()
            _uiState.value = _uiState.value.copy(
                unreachableFolders = unreachable,
                unavailableBooks = unavailable
            )
        }
    }

    fun onFolderSelected(treeUri: Uri) {
        selectedTreeUri = treeUri
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isScanning = true,
                scanMessage = "Scanning new location...",
                errorMessage = null
            )

            try {
                val mode = preferencesManager.scanMode.first()
                var scannedBooks = emptyList<ScannedBookPreview>()

                libraryScanner.scanFolderPreview(treeUri, mode).collect { progress ->
                    when (progress) {
                        is ScanProgress.Scanning -> {
                            _uiState.value = _uiState.value.copy(scanMessage = progress.message)
                        }
                        is ScanProgress.Found -> {
                            scannedBooks = progress.books
                        }
                        is ScanProgress.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isScanning = false,
                                errorMessage = progress.message
                            )
                        }
                        is ScanProgress.Cancelled -> {
                            _uiState.value = _uiState.value.copy(
                                isScanning = false,
                                scanMessage = "Scan cancelled"
                            )
                        }
                    }
                }

                if (scannedBooks.isNotEmpty()) {
                    performMatching(scannedBooks)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isScanning = false,
                        errorMessage = "No audiobooks found in selected folder."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    errorMessage = e.localizedMessage ?: "Failed to scan folder"
                )
            }
        }
    }

    private suspend fun performMatching(newScanned: List<ScannedBookPreview>) {
        val unavailable = bookDao.getUnavailableBooksSync()

        val oldMatchInfos = unavailable.map { book ->
            val chapters = chapterDao.getChaptersForBookSync(book.id)
            val files = chapters.map { ch ->
                val fileName = ch.fileUri.substringAfterLast('/')
                BookFileDescriptor(fileName, ch.durationMs)
            }
            val relative = book.folderUri.substringAfterLast('/')
            OldBookMatchInfo(
                id = book.id,
                title = book.displayTitle,
                author = book.displayAuthor,
                totalDurationMs = book.totalDurationMs,
                relativePath = relative,
                fileCount = book.fileCount,
                files = files,
                bookEntity = book
            )
        }

        val newMatchInfos = newScanned.map { preview ->
            val files = preview.files.map { f ->
                BookFileDescriptor(f.name, f.durationMs)
            }
            val relative = preview.folderUri.substringAfterLast('/')
            NewBookMatchInfo(
                identifier = preview.identifier,
                title = preview.title,
                author = preview.author,
                totalDurationMs = preview.durationMs,
                relativePath = relative,
                fileCount = preview.fileCount,
                files = files,
                preview = preview
            )
        }

        val analysis = LibraryRelinker.matchBooks(oldMatchInfos, newMatchInfos)

        // Pre-select all high-confidence matched books
        val initialSelected = mutableMapOf<Long, NewBookMatchInfo>()
        analysis.matched.forEach { match ->
            match.matchedCandidate?.let { candidate ->
                initialSelected[match.oldBook.id] = candidate
            }
        }

        _uiState.value = _uiState.value.copy(
            isScanning = false,
            scanMessage = null,
            analysis = analysis,
            selectedMatches = initialSelected
        )
    }

    fun toggleMatch(oldBookId: Long, isChecked: Boolean, candidate: NewBookMatchInfo) {
        val current = _uiState.value.selectedMatches.toMutableMap()
        if (isChecked) {
            current[oldBookId] = candidate
        } else {
            current.remove(oldBookId)
        }
        _uiState.value = _uiState.value.copy(selectedMatches = current)
    }

    fun applyRelink() {
        val treeUri = selectedTreeUri ?: return
        val analysis = _uiState.value.analysis ?: return
        val selected = _uiState.value.selectedMatches

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isApplying = true)
            try {
                val matchesToApply = mutableListOf<Pair<BookEntity, ScannedBookPreview>>()

                for (match in (analysis.matched + analysis.ambiguous)) {
                    val candidate = selected[match.oldBook.id]
                    if (candidate?.preview != null) {
                        matchesToApply.add(match.oldBook.bookEntity to candidate.preview)
                    }
                }

                val oldFolderUri = _uiState.value.unreachableFolders.firstOrNull()
                libraryRepository.executeRelink(
                    matches = matchesToApply,
                    oldFolderUri = oldFolderUri,
                    newFolderUri = treeUri.toString()
                )

                _uiState.value = _uiState.value.copy(
                    isApplying = false,
                    isComplete = true,
                    relinkedCount = matchesToApply.size
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isApplying = false,
                    errorMessage = e.localizedMessage ?: "Failed to re-link books"
                )
            }
        }
    }
}
