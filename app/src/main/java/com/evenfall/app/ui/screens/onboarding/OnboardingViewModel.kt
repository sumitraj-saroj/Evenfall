package com.evenfall.app.ui.screens.onboarding

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evenfall.app.data.backup.BackupBook
import com.evenfall.app.data.backup.BackupManager
import com.evenfall.app.data.backup.EvenfallBackup
import com.evenfall.app.data.backup.RestoreMergeMode
import com.evenfall.app.data.datastore.PreferencesManager
import com.evenfall.app.data.datastore.ScanMode
import com.evenfall.app.data.db.dao.BookDao
import com.evenfall.app.data.db.entity.BookEntity
import com.evenfall.app.data.scanner.BookFileDescriptor
import com.evenfall.app.data.scanner.LibraryRelinker
import com.evenfall.app.data.scanner.LibraryScanner
import com.evenfall.app.data.scanner.NewBookMatchInfo
import com.evenfall.app.data.scanner.OldBookMatchInfo
import com.evenfall.app.data.scanner.RelinkAnalysis
import com.evenfall.app.data.scanner.ScanProgress
import com.evenfall.app.data.scanner.ScannedBookPreview
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class OnboardingUiState {
    data object Idle : OnboardingUiState()
    data class Scanning(val message: String) : OnboardingUiState()
    data class Found(val books: List<ScannedBookPreview>, val folderName: String) : OnboardingUiState()
    data class Error(val message: String) : OnboardingUiState()
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val libraryScanner: LibraryScanner,
    private val preferencesManager: PreferencesManager,
    private val backupManager: BackupManager,
    private val bookDao: BookDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Idle)
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _scanMode = MutableStateFlow(ScanMode.SUBFOLDER_IS_BOOK)
    val scanMode: StateFlow<ScanMode> = _scanMode.asStateFlow()

    private val _pendingRestoreBackup = MutableStateFlow<EvenfallBackup?>(null)
    val pendingRestoreBackup: StateFlow<EvenfallBackup?> = _pendingRestoreBackup.asStateFlow()

    private val _restoreAnalysis = MutableStateFlow<RelinkAnalysis?>(null)
    val restoreAnalysis: StateFlow<RelinkAnalysis?> = _restoreAnalysis.asStateFlow()

    private val _restoreMergeMode = MutableStateFlow(RestoreMergeMode.MERGE)
    val restoreMergeMode: StateFlow<RestoreMergeMode> = _restoreMergeMode.asStateFlow()

    private val _restoreMessage = MutableStateFlow<String?>(null)
    val restoreMessage: StateFlow<String?> = _restoreMessage.asStateFlow()

    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

    private var currentFolderUri: Uri? = null

    init {
        viewModelScope.launch {
            _scanMode.value = preferencesManager.scanMode.first()
        }
    }

    fun onRestoreBackupSelected(uri: Uri) {
        viewModelScope.launch {
            _restoreMessage.value = null
            val result = backupManager.readBackupFromUri(uri)
            if (result.isFailure) {
                _restoreMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to read backup"
                return@launch
            }

            val backup = result.getOrThrow()
            _pendingRestoreBackup.value = backup
            _restoreMessage.value = "Backup loaded (${backup.books.size} books). Now select your Audiobooks folder."

            val state = _uiState.value
            if (state is OnboardingUiState.Found) {
                computeRestoreAnalysis(backup, state.books)
            }
        }
    }

    fun onFolderSelected(uri: Uri) {
        currentFolderUri = uri
        try {
            // Persist SAF permission
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (_: Exception) {}

        viewModelScope.launch {
            preferencesManager.addLibraryFolder(uri.toString())
            scanFolder(uri, _scanMode.value)
        }
    }

    fun setScanMode(mode: ScanMode) {
        _scanMode.value = mode
        viewModelScope.launch {
            preferencesManager.setScanMode(mode)
            currentFolderUri?.let { scanFolder(it, mode) }
        }
    }

    fun setRestoreMergeMode(mode: RestoreMergeMode) {
        _restoreMergeMode.value = mode
    }

    fun dismissRestoreReview() {
        _pendingRestoreBackup.value = null
        _restoreAnalysis.value = null
    }

    fun clearRestoreMessage() {
        _restoreMessage.value = null
    }

    private fun scanFolder(uri: Uri, mode: ScanMode) {
        viewModelScope.launch {
            libraryScanner.scanFolderPreview(uri, mode).collect { progress ->
                when (progress) {
                    is ScanProgress.Scanning -> _uiState.value = OnboardingUiState.Scanning(progress.message)
                    is ScanProgress.Found -> {
                        val folderName = uri.lastPathSegment?.substringAfterLast(":") ?: "Audiobooks"
                        _uiState.value = OnboardingUiState.Found(progress.books, folderName)
                        val backup = _pendingRestoreBackup.value
                        if (backup != null) {
                            computeRestoreAnalysis(backup, progress.books)
                        }
                    }
                    is ScanProgress.Error -> _uiState.value = OnboardingUiState.Error(progress.message)
                    is ScanProgress.Cancelled -> _uiState.value = OnboardingUiState.Idle
                }
            }
        }
    }

    private fun computeRestoreAnalysis(backup: EvenfallBackup, scannedBooks: List<ScannedBookPreview>) {
        val oldMatchInfos = backup.books.mapIndexed { idx, bb ->
            val files = bb.chapters.map { ch ->
                BookFileDescriptor(ch.fileUri.substringAfterLast('/'), ch.durationMs)
            }
            OldBookMatchInfo(
                id = idx.toLong(),
                title = bb.title,
                author = bb.author,
                totalDurationMs = bb.totalDurationMs,
                relativePath = bb.folderUri.substringAfterLast('/'),
                fileCount = bb.fileCount,
                files = files,
                bookEntity = BookEntity(
                    id = idx.toLong(),
                    title = bb.title,
                    author = bb.author,
                    folderUri = bb.folderUri,
                    folderIdentifier = bb.folderIdentifier,
                    totalDurationMs = bb.totalDurationMs
                )
            )
        }

        val newMatchInfos = scannedBooks.map { sb ->
            val files = sb.files.map { f ->
                BookFileDescriptor(f.name, f.durationMs)
            }
            NewBookMatchInfo(
                identifier = sb.identifier,
                title = sb.title,
                author = sb.author,
                totalDurationMs = sb.durationMs,
                relativePath = sb.folderUri.substringAfterLast('/'),
                fileCount = sb.fileCount,
                files = files
            )
        }

        _restoreAnalysis.value = LibraryRelinker.matchBooks(oldMatchInfos, newMatchInfos)
    }

    fun confirmAddBooks(onComplete: () -> Unit) {
        val state = _uiState.value
        if (state is OnboardingUiState.Found) {
            val backup = _pendingRestoreBackup.value
            val analysis = _restoreAnalysis.value
            val mode = _restoreMergeMode.value

            viewModelScope.launch {
                _isRestoring.value = true
                try {
                    libraryScanner.saveScannedBooks(state.books)
                    if (backup != null && analysis != null) {
                        val currentBooks = bookDao.getAllBooksSync()
                        val matchedPairs = mutableListOf<Pair<BookEntity, BackupBook>>()
                        for (match in (analysis.matched + analysis.ambiguous)) {
                            val candidate = match.matchedCandidate ?: continue
                            val deviceBook = currentBooks.firstOrNull { it.folderIdentifier == candidate.identifier } ?: continue
                            val backupBook = backup.books.getOrNull(match.oldBook.id.toInt()) ?: continue
                            matchedPairs.add(deviceBook to backupBook)
                        }
                        backupManager.applyRestore(backup, matchedPairs, mode)
                    }
                    preferencesManager.setOnboardingCompleted(true)
                    onComplete()
                } finally {
                    _isRestoring.value = false
                }
            }
        }
    }
}
