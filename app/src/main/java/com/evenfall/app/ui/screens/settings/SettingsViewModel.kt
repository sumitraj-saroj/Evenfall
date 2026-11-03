package com.evenfall.app.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evenfall.app.data.datastore.PreferencesManager
import com.evenfall.app.data.datastore.ThemeMode
import com.evenfall.app.data.db.dao.BookmarkDao
import com.evenfall.app.data.db.dao.BookDao
import com.evenfall.app.data.repository.LibraryRepository
import com.evenfall.app.ui.theme.ColorSchemeOption
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
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
    val backupStatusMessage: String? = null,
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

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val libraryRepository: LibraryRepository,
    private val bookDao: BookDao,
    private val bookmarkDao: BookmarkDao
) : ViewModel() {

    private val _backupStatus = MutableStateFlow<String?>(null)

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

    val uiState: StateFlow<SettingsUiState> = combine(
        appearanceFlow,
        listeningFlow,
        preferencesManager.libraryFolders
    ) { appearance, listening, folders ->
        val (theme, black, scheme) = appearance
        SettingsUiState(
            themeMode = theme,
            pureBlack = black,
            colorSchemeOption = scheme,
            skipAmountSeconds = listening.skip,
            autoRewindEnabled = listening.autoRewind,
            shakeToExtendEnabled = listening.shake,
            compactPlayer = listening.compact,
            autoMarkFinished = listening.autoMarkFinished,
            libraryFolders = folders
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

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            try {
                val books = bookDao.getAllBooks().first()
                val jsonRoot = JSONObject()
                jsonRoot.put("version", 1)
                jsonRoot.put("app", "Evenfall")
                jsonRoot.put("timestamp", System.currentTimeMillis())

                val booksArray = JSONArray()
                for (book in books) {
                    val bookObj = JSONObject()
                    bookObj.put("title", book.title)
                    bookObj.put("author", book.author)
                    bookObj.put("folderIdentifier", book.folderIdentifier)
                    bookObj.put("currentPositionMs", book.currentPositionMs)
                    bookObj.put("chapterIndex", book.currentChapterIndex)
                    bookObj.put("playbackSpeed", book.playbackSpeed.toDouble())
                    bookObj.put("isFinished", book.isFinished)

                    val bookmarks = bookmarkDao.getBookmarksForBook(book.id).first()
                    val bookmarksArray = JSONArray()
                    for (bm in bookmarks) {
                        val bmObj = JSONObject()
                        bmObj.put("title", bm.title)
                        bmObj.put("chapterIndex", bm.chapterIndex)
                        bmObj.put("positionMs", bm.positionMs)
                        bmObj.put("type", bm.type.name)
                        bmObj.put("note", bm.note ?: "")
                        bookmarksArray.put(bmObj)
                    }
                    bookObj.put("bookmarks", bookmarksArray)
                    booksArray.put(bookObj)
                }
                jsonRoot.put("books", booksArray)

                context.contentResolver.openOutputStream(uri)?.use { os ->
                    OutputStreamWriter(os).use { writer ->
                        writer.write(jsonRoot.toString(2))
                    }
                }
                _backupStatus.value = "Backup exported successfully!"
            } catch (e: Exception) {
                _backupStatus.value = "Export failed: ${e.message}"
            }
        }
    }
}
