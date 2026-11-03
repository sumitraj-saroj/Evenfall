package com.evenfall.app.ui.screens.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evenfall.app.data.datastore.PreferencesManager
import com.evenfall.app.data.db.dao.BookDao
import com.evenfall.app.data.db.dao.BookmarkDao
import com.evenfall.app.data.db.dao.ChapterDao
import com.evenfall.app.data.db.entity.BookmarkEntity
import com.evenfall.app.data.db.entity.BookmarkType
import com.evenfall.app.playback.PlaybackManager
import com.evenfall.app.playback.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    val playbackManager: PlaybackManager,
    private val bookmarkDao: BookmarkDao,
    private val bookDao: BookDao,
    private val chapterDao: ChapterDao,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = playbackManager.playbackState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PlaybackState()
        )

    val skipSeconds: StateFlow<Int> = preferencesManager.skipAmountSeconds
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 20
        )

    val compactPlayer: StateFlow<Boolean> = preferencesManager.compactPlayer
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    init {
        val bookId: Long? = savedStateHandle["bookId"]
        if (bookId != null && bookId > 0) {
            playbackManager.loadBook(bookId, autoPlay = false)
        }
    }

    fun togglePlayPause() = playbackManager.togglePlayPause()

    fun skipForward() = playbackManager.skipForward()

    fun skipBackward() = playbackManager.skipBackward()

    fun seekToFraction(fraction: Float) {
        val duration = playbackState.value.chapterDurationMs
        val targetMs = (duration * fraction).toLong()
        playbackManager.seekTo(targetMs, recordUndo = true)
    }

    fun undoSeek() = playbackManager.undoSeek()

    fun nextChapter() = playbackManager.nextChapter()

    fun previousChapter() = playbackManager.previousChapter()

    fun selectChapter(index: Int) = playbackManager.switchToChapter(index)

    fun setPlaybackSpeed(speed: Float) = playbackManager.setPlaybackSpeed(speed)

    fun toggleSilenceSkipping() {
        val current = playbackState.value.silenceSkippingEnabled
        playbackManager.setSilenceSkipping(!current)
    }

    fun setVolumeBoost(level: Float) = playbackManager.setVolumeBoost(level)

    fun startSleepTimer(minutes: Int) = playbackManager.startSleepTimer(minutes)

    fun startSleepTimerEndOfChapter() = playbackManager.startSleepTimerEndOfChapter()

    fun cancelSleepTimer() = playbackManager.cancelSleepTimer()

    fun quickSaveBookmark() {
        val state = playbackState.value
        val book = state.currentBook ?: return
        val chapter = state.currentChapter ?: return

        viewModelScope.launch {
            val count = bookmarkDao.getBookmarkCountForBook(book.id).first()
            val defaultTitle = "Bookmark ${count + 1}"
            bookmarkDao.insert(
                BookmarkEntity(
                    bookId = book.id,
                    chapterIndex = chapter.chapterIndex,
                    chapterTitle = chapter.title,
                    positionMs = state.chapterPositionMs,
                    bookProgressFraction = state.totalProgressFraction,
                    title = defaultTitle,
                    note = null,
                    type = BookmarkType.BOOKMARK
                )
            )
        }
    }

    fun updateMetadataOverrides(
        title: String,
        author: String,
        narrator: String?,
        seriesName: String?,
        seriesOrder: Float?,
        coverUri: String?
    ) {
        val book = playbackState.value.currentBook ?: return
        viewModelScope.launch {
            val titleOv = if (title != book.title) title else null
            val authorOv = if (author != book.author) author else null
            val coverOv = if (coverUri != book.coverUri) coverUri else null

            bookDao.updateMetadataOverrides(
                id = book.id,
                titleOverride = titleOv,
                authorOverride = authorOv,
                narratorOverride = narrator,
                seriesName = seriesName,
                seriesOrder = seriesOrder,
                coverOverrideUri = coverOv
            )
            playbackManager.loadBook(book.id, autoPlay = false)
        }
    }

    fun resetMetadataOverrides() {
        val book = playbackState.value.currentBook ?: return
        viewModelScope.launch {
            bookDao.resetMetadataOverrides(book.id)
            chapterDao.resetChapterOverridesForBook(book.id)
            playbackManager.loadBook(book.id, autoPlay = false)
        }
    }

    fun updateChapterTitle(chapterId: Long, newTitle: String) {
        val book = playbackState.value.currentBook ?: return
        viewModelScope.launch {
            chapterDao.updateChapterTitleOverride(chapterId, newTitle.ifEmpty { null })
            playbackManager.loadBook(book.id, autoPlay = false)
        }
    }
}
