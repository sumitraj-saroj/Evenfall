package com.evenfall.app.ui.screens.onboarding

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evenfall.app.data.datastore.PreferencesManager
import com.evenfall.app.data.datastore.ScanMode
import com.evenfall.app.data.scanner.LibraryScanner
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
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Idle)
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _scanMode = MutableStateFlow(ScanMode.SUBFOLDER_IS_BOOK)
    val scanMode: StateFlow<ScanMode> = _scanMode.asStateFlow()

    private var currentFolderUri: Uri? = null

    init {
        viewModelScope.launch {
            _scanMode.value = preferencesManager.scanMode.first()
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

    private fun scanFolder(uri: Uri, mode: ScanMode) {
        viewModelScope.launch {
            libraryScanner.scanFolderPreview(uri, mode).collect { progress ->
                when (progress) {
                    is ScanProgress.Scanning -> _uiState.value = OnboardingUiState.Scanning(progress.message)
                    is ScanProgress.Found -> {
                        val folderName = uri.lastPathSegment?.substringAfterLast(":") ?: "Audiobooks"
                        _uiState.value = OnboardingUiState.Found(progress.books, folderName)
                    }
                    is ScanProgress.Error -> _uiState.value = OnboardingUiState.Error(progress.message)
                }
            }
        }
    }

    fun confirmAddBooks(onComplete: () -> Unit) {
        val state = _uiState.value
        if (state is OnboardingUiState.Found) {
            viewModelScope.launch {
                libraryScanner.saveScannedBooks(state.books)
                preferencesManager.setOnboardingCompleted(true)
                onComplete()
            }
        }
    }
}
