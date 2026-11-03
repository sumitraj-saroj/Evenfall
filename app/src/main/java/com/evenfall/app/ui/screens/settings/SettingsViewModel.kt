package com.evenfall.app.ui.screens.settings

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
import com.evenfall.app.data.datastore.ThemeMode
import com.evenfall.app.data.db.dao.BookDao
import com.evenfall.app.data.db.dao.BookmarkDao
import com.evenfall.app.data.db.dao.ChapterDao
import com.evenfall.app.data.db.dao.HistoryDao
import com.evenfall.app.data.db.entity.BookEntity
import com.evenfall.app.data.repository.LibraryRepository
import com.evenfall.app.data.scanner.BookFileDescriptor
import com.evenfall.app.data.scanner.LibraryRelinker
import com.evenfall.app.data.scanner.NewBookMatchInfo
import com.evenfall.app.data.scanner.OldBookMatchInfo
import com.evenfall.app.data.scanner.RelinkAnalysis
import com.evenfall.app.ui.theme.ColorSchemeOption
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    val pureBlack: Boolean = false,
    val colorSchemeOption: ColorSchemeOption = ColorSchemeOption.EVENFALL_BLUE,
    val skipAmountSeconds: Int = 20,
    val autoRewindEnabled: Boolean = true,
    val shakeToExtendEnabled: Boolean = true,
    val fadeOutDurationSeconds: Int = 30,
    val silenceSkippingEnabled: Boolean = false,
    val defaultSpeed: Float = 1.0f,
    val libraryFolders: Set<String> = emptySet(),
    val backupFolderUri: String? = null,
    val lastBackupTimestamp: Long? = null,
    val backupStatusMessage: String? = null,
    val isBackingUp: Boolean = false,
    val pendingRestoreBackup: EvenfallBackup? = null,
    val restoreAnalysis: RelinkAnalysis? = null,
    val restoreMergeMode: RestoreMergeMode = RestoreMergeMode.MERGE,
    val isRestoring: Boolean = false,
    val restoreSuccessMessage: String? = null,
    val restoreErrorMessage: String? = null,
    val compactPlayer: Boolean = true,
    val autoMarkFinished: Boolean = true
)

private data class ListeningPrefs(
    val skip: Int,
    val autoRewind: Boolean,
    val shake: Boolean,
    val compact: Boolean,
    val autoMarkFinished: Boolean
)

private data class BackupStateHolder(
    val folderUri: String?,
    val lastBackup: Long?,
    val statusMsg: String?,
    val isBackingUp: Boolean,
    val pendingRestore: EvenfallBackup?,
    val restoreAnalysis: RelinkAnalysis?,
    val restoreMergeMode: RestoreMergeMode,
    val isRestoring: Boolean,
    val restoreSuccessMsg: String?,
    val restoreErrorMsg: String?
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val libraryRepository: LibraryRepository,
    private val bookDao: BookDao,
    private val chapterDao: ChapterDao,
    private val bookmarkDao: BookmarkDao,
    private val historyDao: HistoryDao,
    private val backupManager: BackupManager
) : ViewModel() {

    private val _backupStatus = MutableStateFlow<String?>(null)
    private val _isBackingUp = MutableStateFlow(false)
    private val _pendingRestoreBackup = MutableStateFlow<EvenfallBackup?>(null)
    private val _restoreAnalysis = MutableStateFlow<RelinkAnalysis?>(null)
    private val _restoreMergeMode = MutableStateFlow(RestoreMergeMode.MERGE)
    private val _isRestoring = MutableStateFlow(false)
    private val _restoreSuccessMessage = MutableStateFlow<String?>(null)
    private val _restoreErrorMessage = MutableStateFlow<String?>(null)

    private val appearanceFlow = combine(
        preferencesManager.themeMode,
        preferencesManager.pureBlack,
        preferencesManager.colorSchemeOption
    ) { theme, black, scheme ->
        Triple(theme, black, scheme)
    }

    private val listeningFlow = combine(
        preferencesManager.skipAmountSeconds,
        preferencesManager.autoRewindEnabled,
        preferencesManager.shakeToExtendEnabled,
        preferencesManager.compactPlayer,
        preferencesManager.autoMarkFinished
    ) { skip, autoRewind, shake, compact, autoMark ->
        ListeningPrefs(skip, autoRewind, shake, compact, autoMark)
    }

    private val backupStateFlow = combine(
        preferencesManager.backupFolderUri,
        preferencesManager.lastBackupTimestamp,
        _backupStatus,
        _isBackingUp,
        _pendingRestoreBackup
    ) { folder, lastBackup, status, isBackingUp, pendingRestore ->
        Triple(folder, lastBackup, Triple(status, isBackingUp, pendingRestore))
    }

    private val restoreStateFlow = combine(
        _restoreAnalysis,
        _restoreMergeMode,
        _isRestoring,
        _restoreSuccessMessage,
        _restoreErrorMessage
    ) { analysis, mode, isRestoring, success, error ->
        Pair(analysis, mode) to Triple(isRestoring, success, error)
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        appearanceFlow,
        listeningFlow,
        preferencesManager.libraryFolders,
        backupStateFlow,
        restoreStateFlow
    ) { appearance, listening, folders, backupData, restoreData ->
        val (theme, black, scheme) = appearance
        val (folder, lastBackup, backupSub) = backupData
        val (status, isBackingUp, pendingRestore) = backupSub
        val (pair, triple) = restoreData
        val (analysis, mode) = pair
        val (isRestoring, success, error) = triple

        SettingsUiState(
            themeMode = theme,
            pureBlack = black,
            colorSchemeOption = scheme,
            skipAmountSeconds = listening.skip,
            autoRewindEnabled = listening.autoRewind,
            shakeToExtendEnabled = listening.shake,
            compactPlayer = listening.compact,
            autoMarkFinished = listening.autoMarkFinished,
            libraryFolders = folders,
            backupFolderUri = folder,
            lastBackupTimestamp = lastBackup,
            backupStatusMessage = status,
            isBackingUp = isBackingUp,
            pendingRestoreBackup = pendingRestore,
            restoreAnalysis = analysis,
            restoreMergeMode = mode,
            isRestoring = isRestoring,
            restoreSuccessMessage = success,
            restoreErrorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesManager.setThemeMode(mode) }
    }

    fun setPureBlack(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setPureBlack(enabled) }
    }

    fun setColorScheme(scheme: ColorSchemeOption) {
        viewModelScope.launch { preferencesManager.setColorScheme(scheme) }
    }

    fun updateSkipAmount(delta: Int) {
        val current = uiState.value.skipAmountSeconds
        val updated = (current + delta).coerceIn(5, 60)
        viewModelScope.launch { preferencesManager.setSkipAmountSeconds(updated) }
    }

    fun setAutoRewindEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setAutoRewindEnabled(enabled) }
    }

    fun setShakeToExtendEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setShakeToExtendEnabled(enabled) }
    }

    fun setCompactPlayer(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setCompactPlayer(enabled) }
    }

    fun setAutoMarkFinished(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setAutoMarkFinished(enabled) }
    }

    fun rescanLibrary() {
        viewModelScope.launch {
            libraryRepository.rescanLibrary()
        }
    }

    fun removeFolder(uri: String) {
        viewModelScope.launch {
            preferencesManager.removeLibraryFolder(uri)
        }
    }

    fun setBackupFolder(uri: Uri) {
        viewModelScope.launch {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (_: Exception) {}
            preferencesManager.setBackupFolderUri(uri.toString())
            backupManager.scheduleDailyBackup()
            _backupStatus.value = "Backup folder configured"
        }
    }

    fun clearBackupFolder() {
        viewModelScope.launch {
            backupManager.cancelDailyBackup()
            preferencesManager.setBackupFolderUri(null)
            _backupStatus.value = null
        }
    }

    fun backupNow() {
        val folderUriStr = uiState.value.backupFolderUri
        if (folderUriStr.isNullOrBlank()) {
            _backupStatus.value = "Please select a backup folder first"
            return
        }

        viewModelScope.launch {
            _isBackingUp.value = true
            try {
                val backup = backupManager.createBackup()
                val result = backupManager.writeBackupToFolder(Uri.parse(folderUriStr), backup)
                if (result.isSuccess) {
                    _backupStatus.value = "Backup created: ${result.getOrNull()}"
                } else {
                    _backupStatus.value = "Backup failed: ${result.exceptionOrNull()?.localizedMessage}"
                }
            } catch (e: Exception) {
                _backupStatus.value = "Backup failed: ${e.localizedMessage}"
            } finally {
                _isBackingUp.value = false
            }
        }
    }

    fun onRestoreFileSelected(fileUri: Uri) {
        viewModelScope.launch {
            _restoreErrorMessage.value = null
            _restoreSuccessMessage.value = null
            val result = backupManager.readBackupFromUri(fileUri)
            if (result.isFailure) {
                _restoreErrorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to read backup"
                return@launch
            }

            val backup = result.getOrThrow()
            _pendingRestoreBackup.value = backup

            // Match backup books with device books using LibraryRelinker
            val currentBooks = bookDao.getAllBooksSync()
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

            val newMatchInfos = currentBooks.map { cb ->
                val chapters = chapterDao.getChaptersForBookSync(cb.id)
                val files = chapters.map { ch ->
                    BookFileDescriptor(ch.fileUri.substringAfterLast('/'), ch.durationMs)
                }
                NewBookMatchInfo(
                    identifier = cb.folderIdentifier,
                    title = cb.displayTitle,
                    author = cb.displayAuthor,
                    totalDurationMs = cb.totalDurationMs,
                    relativePath = cb.folderUri.substringAfterLast('/'),
                    fileCount = cb.fileCount,
                    files = files
                )
            }

            val analysis = LibraryRelinker.matchBooks(oldMatchInfos, newMatchInfos)
            _restoreAnalysis.value = analysis
        }
    }

    fun setRestoreMergeMode(mode: RestoreMergeMode) {
        _restoreMergeMode.value = mode
    }

    fun confirmRestore() {
        val backup = _pendingRestoreBackup.value ?: return
        val analysis = _restoreAnalysis.value ?: return
        val mode = _restoreMergeMode.value

        viewModelScope.launch {
            _isRestoring.value = true
            try {
                val currentBooks = bookDao.getAllBooksSync()
                val matchedPairs = mutableListOf<Pair<BookEntity, BackupBook>>()

                for (match in (analysis.matched + analysis.ambiguous)) {
                    val candidate = match.matchedCandidate ?: continue
                    val deviceBook = currentBooks.firstOrNull { it.folderIdentifier == candidate.identifier } ?: continue
                    val backupBook = backup.books.getOrNull(match.oldBook.id.toInt()) ?: continue
                    matchedPairs.add(deviceBook to backupBook)
                }

                val result = backupManager.applyRestore(backup, matchedPairs, mode)
                if (result.isSuccess) {
                    _restoreSuccessMessage.value = "Restored ${result.getOrNull() ?: 0} books successfully!"
                    _pendingRestoreBackup.value = null
                    _restoreAnalysis.value = null
                } else {
                    _restoreErrorMessage.value = "Restore failed: ${result.exceptionOrNull()?.localizedMessage}"
                }
            } catch (e: Exception) {
                _restoreErrorMessage.value = "Restore failed: ${e.localizedMessage}"
            } finally {
                _isRestoring.value = false
            }
        }
    }

    fun dismissRestoreReview() {
        _pendingRestoreBackup.value = null
        _restoreAnalysis.value = null
        _restoreErrorMessage.value = null
    }

    fun clearStatusMessage() {
        _backupStatus.value = null
        _restoreSuccessMessage.value = null
        _restoreErrorMessage.value = null
    }
}
