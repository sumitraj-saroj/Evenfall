package com.evenfall.app.playback

import android.content.Context
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.evenfall.app.data.datastore.PreferencesManager
import com.evenfall.app.data.db.dao.BookDao
import com.evenfall.app.data.db.dao.ChapterDao
import com.evenfall.app.data.db.dao.HistoryDao
import com.evenfall.app.data.db.entity.BookEntity
import com.evenfall.app.data.db.entity.ChapterEntity
import com.evenfall.app.data.db.entity.HistoryEntity
import com.evenfall.app.data.db.entity.HistoryEventType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(UnstableApi::class)
@Singleton
class PlaybackManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bookDao: BookDao,
    private val chapterDao: ChapterDao,
    private val historyDao: HistoryDao,
    private val preferencesManager: PreferencesManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true // handle audio focus automatically
        )
    }

    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var positionTickerJob: Job? = null
    private var sleepTimerJob: Job? = null

    private var lastPauseTimestamp: Long = 0L
    private var interruptionStartTimestamp: Long = 0L

    private val shakeDetector = ShakeDetector(context) {
        onDeviceShook()
    }

    init {
        setupPlayerListener()
        startPositionTicker()
        restoreLastPlayedBookIfAvailable()
    }

    private fun restoreLastPlayedBookIfAvailable() {
        scope.launch {
            try {
                val mostRecent = bookDao.getMostRecentlyPlayedBook().first()
                if (mostRecent != null && mostRecent.currentPositionMs > 0 && _playbackState.value.currentBook == null) {
                    loadBook(mostRecent.id, autoPlay = false)
                }
            } catch (_: Exception) {}
        }
    }

    private fun setupPlayerListener() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)
                if (isPlaying) {
                    handlePlaybackResumed()
                } else {
                    handlePlaybackPaused()
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    onChapterEnded()
                }
            }
        })
    }

    fun loadBook(bookId: Long, autoPlay: Boolean = false) {
        scope.launch {
            val book = bookDao.getBookByIdSync(bookId) ?: return@launch
            val chapters = chapterDao.getChaptersForBookSync(bookId)
            val currentChapterIdx = book.currentChapterIndex.coerceIn(0, (chapters.size - 1).coerceAtLeast(0))
            val currentChapter = chapters.getOrNull(currentChapterIdx)

            _playbackState.value = _playbackState.value.copy(
                currentBook = book,
                chapters = chapters,
                currentChapter = currentChapter,
                playbackSpeed = book.playbackSpeed,
                chapterPositionMs = book.chapterPositionMs,
                chapterDurationMs = currentChapter?.durationMs ?: (book.totalDurationMs / chapters.size.coerceAtLeast(1)),
                totalPositionMs = book.currentPositionMs,
                totalDurationMs = book.totalDurationMs,
                isMiniPlayerDismissed = false
            )

            // Setup ExoPlayer MediaItem
            if (currentChapter != null && currentChapter.fileUri.startsWith("content://")) {
                try {
                    val mediaItem = MediaItem.Builder()
                        .setUri(Uri.parse(currentChapter.fileUri))
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(book.title)
                                .setArtist(book.author)
                                .setDisplayTitle(currentChapter.title)
                                .build()
                        )
                        .build()
                    exoPlayer.setMediaItem(mediaItem)
                    exoPlayer.prepare()
                    exoPlayer.seekTo(book.chapterPositionMs)
                } catch (_: Exception) {}
            }

            exoPlayer.setPlaybackSpeed(book.playbackSpeed)

            if (autoPlay) {
                play()
            }
        }
    }

    fun play() {
        val state = _playbackState.value
        val book = state.currentBook ?: return

        // Auto-rewind calculation
        scope.launch {
            val autoRewindEnabled = preferencesManager.autoRewindEnabled.first()
            if (autoRewindEnabled && lastPauseTimestamp > 0) {
                val pausedDurationMs = System.currentTimeMillis() - lastPauseTimestamp
                val rewindMs = calculateAutoRewind(pausedDurationMs)
                if (rewindMs > 0) {
                    seekTo((state.chapterPositionMs - rewindMs).coerceAtLeast(0L), recordUndo = false)
                }
            }
            lastPauseTimestamp = 0L

            exoPlayer.play()
            _playbackState.value = _playbackState.value.copy(isPlaying = true)

            historyDao.insert(
                HistoryEntity(
                    bookId = book.id,
                    eventType = HistoryEventType.PLAYED,
                    timestamp = System.currentTimeMillis(),
                    positionMs = _playbackState.value.chapterPositionMs,
                    chapterIndex = _playbackState.value.currentChapter?.chapterIndex ?: 0,
                    outputDevice = "Device"
                )
            )
        }
    }

    fun pause() {
        val state = _playbackState.value
        val book = state.currentBook ?: return

        lastPauseTimestamp = System.currentTimeMillis()
        exoPlayer.pause()
        _playbackState.value = _playbackState.value.copy(isPlaying = false)
        saveCurrentPosition()

        scope.launch {
            historyDao.insert(
                HistoryEntity(
                    bookId = book.id,
                    eventType = HistoryEventType.PAUSED,
                    timestamp = System.currentTimeMillis(),
                    positionMs = state.chapterPositionMs,
                    chapterIndex = state.currentChapter?.chapterIndex ?: 0
                )
            )
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun dismissMiniPlayer() {
        pause()
        saveCurrentPosition()
        _playbackState.value = _playbackState.value.copy(isMiniPlayerDismissed = true)
    }

    fun skipForward() {
        scope.launch {
            val skipSec = preferencesManager.skipAmountSeconds.first()
            val newPos = _playbackState.value.chapterPositionMs + (skipSec * 1000L)
            seekTo(newPos, recordUndo = true)
        }
    }

    fun skipBackward() {
        scope.launch {
            val skipSec = preferencesManager.skipAmountSeconds.first()
            val newPos = (_playbackState.value.chapterPositionMs - (skipSec * 1000L)).coerceAtLeast(0L)
            seekTo(newPos, recordUndo = true)
        }
    }

    fun seekTo(positionMs: Long, recordUndo: Boolean = true) {
        val current = _playbackState.value.chapterPositionMs
        val undoPos = if (recordUndo && kotlin.math.abs(positionMs - current) > 3000) current else _playbackState.value.undoSeekPositionMs

        val duration = _playbackState.value.chapterDurationMs
        val clampedPos = positionMs.coerceIn(0L, duration)

        if (exoPlayer.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)) {
            exoPlayer.seekTo(clampedPos)
        }

        val chapterStartMs = _playbackState.value.currentChapter?.startMs ?: 0L
        _playbackState.value = _playbackState.value.copy(
            chapterPositionMs = clampedPos,
            totalPositionMs = chapterStartMs + clampedPos,
            undoSeekPositionMs = undoPos
        )
        saveCurrentPosition()
    }

    fun undoSeek() {
        val undoPos = _playbackState.value.undoSeekPositionMs ?: return
        _playbackState.value = _playbackState.value.copy(undoSeekPositionMs = null)
        seekTo(undoPos, recordUndo = false)
    }

    fun nextChapter() {
        val state = _playbackState.value
        val currentIndex = state.currentChapter?.chapterIndex ?: 0
        if (currentIndex < state.chapters.size - 1) {
            switchToChapter(currentIndex + 1)
        }
    }

    fun previousChapter() {
        val state = _playbackState.value
        val currentIndex = state.currentChapter?.chapterIndex ?: 0
        // If played more than 3 seconds, restart current chapter, otherwise go to previous
        if (state.chapterPositionMs > 3000L) {
            seekTo(0L, recordUndo = false)
        } else if (currentIndex > 0) {
            switchToChapter(currentIndex - 1)
        }
    }

    fun switchToChapter(index: Int) {
        val state = _playbackState.value
        val book = state.currentBook ?: return
        val target = state.chapters.getOrNull(index) ?: return

        _playbackState.value = state.copy(
            currentChapter = target,
            chapterPositionMs = 0L,
            chapterDurationMs = target.durationMs,
            totalPositionMs = target.startMs,
            undoSeekPositionMs = null
        )

        try {
            if (target.fileUri.startsWith("content://")) {
                val mediaItem = MediaItem.fromUri(Uri.parse(target.fileUri))
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.seekTo(0L)
            }
        } catch (_: Exception) {}

        saveCurrentPosition()
    }

    private fun onChapterEnded() {
        val state = _playbackState.value
        val currentIndex = state.currentChapter?.chapterIndex ?: 0
        if (currentIndex < state.chapters.size - 1) {
            nextChapter()
            exoPlayer.play()
        } else {
            // End of book!
            pause()
            scope.launch {
                state.currentBook?.let { book ->
                    bookDao.update(book.copy(isFinished = true, currentPositionMs = book.totalDurationMs))
                }
            }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = (speed.coerceIn(0.5f, 3.0f) * 20).toInt() / 20f // 0.05 step
        exoPlayer.setPlaybackSpeed(clamped)
        _playbackState.value = _playbackState.value.copy(playbackSpeed = clamped)
        scope.launch {
            _playbackState.value.currentBook?.let { book ->
                bookDao.updatePlaybackSpeed(book.id, clamped)
            }
        }
    }

    fun setSilenceSkipping(enabled: Boolean) {
        exoPlayer.skipSilenceEnabled = enabled
        _playbackState.value = _playbackState.value.copy(silenceSkippingEnabled = enabled)
        scope.launch {
            preferencesManager.setSilenceSkippingEnabled(enabled)
        }
    }

    fun setVolumeBoost(level: Float) {
        // level: 0.0f .. 1.0f (maps to 0 to 1000 mB)
        _playbackState.value = _playbackState.value.copy(volumeBoostLevel = level)
        try {
            val audioSessionId = exoPlayer.audioSessionId
            if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                if (loudnessEnhancer == null) {
                    loudnessEnhancer = LoudnessEnhancer(audioSessionId)
                }
                loudnessEnhancer?.setTargetGain((level * 1000).toInt())
                loudnessEnhancer?.enabled = level > 0f
            }
        } catch (_: Exception) {}
        scope.launch {
            preferencesManager.setVolumeBoostLevel(level)
        }
    }

    // Sleep Timer
    fun startSleepTimer(durationMinutes: Int) {
        sleepTimerJob?.cancel()
        val durationMs = durationMinutes * 60 * 1000L
        val endTime = System.currentTimeMillis() + durationMs

        _playbackState.value = _playbackState.value.copy(
            sleepTimerRemainingMs = durationMs,
            sleepTimerIsActive = true
        )

        scope.launch {
            preferencesManager.shakeToExtendEnabled.first().let { enabled ->
                if (enabled) shakeDetector.start()
            }
            _playbackState.value.currentBook?.let { book ->
                historyDao.insert(
                    HistoryEntity(
                        bookId = book.id,
                        eventType = HistoryEventType.SLEEP_TIMER_ON,
                        timestamp = System.currentTimeMillis(),
                        positionMs = _playbackState.value.chapterPositionMs,
                        chapterIndex = _playbackState.value.currentChapter?.chapterIndex ?: 0
                    )
                )
            }
        }

        sleepTimerJob = scope.launch {
            while (isActive) {
                val remaining = endTime - System.currentTimeMillis()
                if (remaining <= 0) {
                    onSleepTimerExpired()
                    break
                }
                // Handle volume fade-out over last N seconds
                val fadeOutSec = preferencesManager.fadeOutDurationSeconds.first()
                val fadeOutMs = fadeOutSec * 1000L
                if (remaining <= fadeOutMs) {
                    val volumeRatio = (remaining.toFloat() / fadeOutMs).coerceIn(0.1f, 1f)
                    exoPlayer.volume = volumeRatio
                } else {
                    exoPlayer.volume = 1f
                }

                _playbackState.value = _playbackState.value.copy(sleepTimerRemainingMs = remaining)
                delay(1000L)
            }
        }
    }

    fun startSleepTimerEndOfChapter() {
        val state = _playbackState.value
        val remainingChapterMs = state.chapterDurationMs - state.chapterPositionMs
        val minutes = (remainingChapterMs / (60 * 1000L)).toInt().coerceAtLeast(1)
        startSleepTimer(minutes)
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        shakeDetector.stop()
        exoPlayer.volume = 1f
        _playbackState.value = _playbackState.value.copy(
            sleepTimerRemainingMs = null,
            sleepTimerIsActive = false
        )
    }

    private fun onSleepTimerExpired() {
        pause()
        exoPlayer.volume = 1f
        shakeDetector.stop()
        val state = _playbackState.value
        _playbackState.value = state.copy(
            sleepTimerRemainingMs = null,
            sleepTimerIsActive = false
        )

        scope.launch {
            state.currentBook?.let { book ->
                historyDao.insert(
                    HistoryEntity(
                        bookId = book.id,
                        eventType = HistoryEventType.SLEEP_TIMER_ENDED,
                        timestamp = System.currentTimeMillis(),
                        positionMs = state.chapterPositionMs,
                        chapterIndex = state.currentChapter?.chapterIndex ?: 0
                    )
                )
            }
        }
    }

    private fun onDeviceShook() {
        // Shook to keep listening: extend sleep timer by 15 minutes!
        val state = _playbackState.value
        if (state.sleepTimerIsActive) {
            val extendedMinutes = 15
            startSleepTimer(extendedMinutes)

            scope.launch {
                state.currentBook?.let { book ->
                    historyDao.insert(
                        HistoryEntity(
                            bookId = book.id,
                            eventType = HistoryEventType.SHOOK_TO_EXTEND,
                            timestamp = System.currentTimeMillis(),
                            positionMs = state.chapterPositionMs,
                            chapterIndex = state.currentChapter?.chapterIndex ?: 0
                        )
                    )
                }
            }
        }
    }

    private fun calculateAutoRewind(pausedDurationMs: Long): Long {
        return when {
            pausedDurationMs < 5_000L -> 0L // instantaneous unpause
            pausedDurationMs < 60_000L -> 3_000L // < 1 min: 3s
            pausedDurationMs < 600_000L -> 8_000L // < 10 min: 8s
            pausedDurationMs < 3_600_000L -> 15_000L // < 1 hr: 15s
            else -> 25_000L // long pause: 25s
        }
    }

    private fun handlePlaybackResumed() {
        // Clear undo chip after 10s of steady playback
        scope.launch {
            delay(10000L)
            if (_playbackState.value.isPlaying) {
                _playbackState.value = _playbackState.value.copy(undoSeekPositionMs = null)
            }
        }
    }

    private fun handlePlaybackPaused() {
        saveCurrentPosition()
    }

    private fun startPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = scope.launch {
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    val currentPos = exoPlayer.currentPosition
                    val duration = if (exoPlayer.duration > 0) exoPlayer.duration else _playbackState.value.chapterDurationMs
                    val startOffset = _playbackState.value.currentChapter?.startMs ?: 0L

                    _playbackState.value = _playbackState.value.copy(
                        chapterPositionMs = currentPos,
                        chapterDurationMs = duration,
                        totalPositionMs = startOffset + currentPos
                    )
                    saveCurrentPosition()
                }
                delay(1000L)
            }
        }
    }

    private fun saveCurrentPosition() {
        val state = _playbackState.value
        val book = state.currentBook ?: return
        val currentChapter = state.currentChapter ?: return

        scope.launch(Dispatchers.IO) {
            bookDao.updatePlaybackPosition(
                id = book.id,
                totalPosMs = state.totalPositionMs,
                chapterIdx = currentChapter.chapterIndex,
                chapterPosMs = state.chapterPositionMs,
                lastPlayed = System.currentTimeMillis()
            )
        }
    }

    fun release() {
        positionTickerJob?.cancel()
        sleepTimerJob?.cancel()
        shakeDetector.stop()
        loudnessEnhancer?.release()
        exoPlayer.release()
    }
}
