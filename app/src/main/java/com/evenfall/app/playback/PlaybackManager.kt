package com.evenfall.app.playback

import android.content.Context
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
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
import java.util.Calendar
import javax.inject.Inject
import com.evenfall.app.data.db.dao.ListeningSessionDao
import com.evenfall.app.data.db.entity.ListeningSessionEntity
import com.evenfall.app.service.EvenfallTileService
import com.evenfall.app.shortcuts.EvenfallShortcutManager
import javax.inject.Singleton

@OptIn(UnstableApi::class)
@Singleton
class PlaybackManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bookDao: BookDao,
    private val chapterDao: ChapterDao,
    private val historyDao: HistoryDao,
    private val listeningSessionDao: ListeningSessionDao,
    private val preferencesManager: PreferencesManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    val monoAudioProcessor = MonoDownmixAudioProcessor()
    val sessionTracker = ListeningSessionTracker()

    private val renderersFactory = object : DefaultRenderersFactory(context) {
        override fun buildAudioSink(
            context: Context,
            enableFloatOutput: Boolean,
            enableAudioTrackPlaybackParams: Boolean
        ): AudioSink {
            return DefaultAudioSink.Builder(context)
                .setAudioProcessors(arrayOf(monoAudioProcessor))
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .build()
        }
    }

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context, renderersFactory).build().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true // handle audio focus automatically
        )
    }

    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var equalizer: Equalizer? = null
    private var lastAudioSessionId: Int = C.AUDIO_SESSION_ID_UNSET

    private var positionTickerJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var autoMarkFinishedEnabled: Boolean = true
    private var autoPlayNextInSeriesEnabled: Boolean = false

    private val _nextInSeriesPrompt = MutableStateFlow<BookEntity?>(null)
    val nextInSeriesPrompt = _nextInSeriesPrompt.asStateFlow()

    private var lastPauseTimestamp: Long = 0L
    private var lastBedtimeTriggerDate: String = ""

    private val shakeDetector = ShakeDetector(context) {
        onDeviceShook()
    }

    init {
        setupPlayerListener()
        startPositionTicker()
        restoreLastPlayedBookIfAvailable()
        observeAutoMarkPreference()
        observeAutoPlayNextPreference()
        observeGlobalSettings()
        observeStatsPreference()
    }

    private fun observeStatsPreference() {
        scope.launch {
            preferencesManager.statsEnabled.collect { enabled ->
                sessionTracker.setStatsEnabled(enabled)
            }
        }
    }

    private fun saveListeningSession(record: SessionRecord) {
        scope.launch(Dispatchers.IO) {
            listeningSessionDao.insert(
                ListeningSessionEntity(
                    bookId = record.bookId,
                    timestamp = record.timestamp,
                    listenedDurationMs = record.listenedDurationMs,
                    contentDurationMs = record.contentDurationMs
                )
            )
        }
    }

    private fun observeAutoMarkPreference() {
        scope.launch {
            preferencesManager.autoMarkFinished.collect { enabled ->
                autoMarkFinishedEnabled = enabled
            }
        }
    }

    private fun observeAutoPlayNextPreference() {
        scope.launch {
            preferencesManager.autoPlayNextInSeries.collect { enabled ->
                autoPlayNextInSeriesEnabled = enabled
            }
        }
    }

    private fun observeGlobalSettings() {
        // When global settings change, re-apply for current book without overwriting overrides
        scope.launch {
            preferencesManager.defaultPlaybackSpeed.collect {
                _playbackState.value.currentBook?.let { applyEffectiveSettingsForBook(it) }
            }
        }
        scope.launch {
            preferencesManager.skipAmountSeconds.collect {
                _playbackState.value.currentBook?.let { applyEffectiveSettingsForBook(it) }
            }
        }
        scope.launch {
            preferencesManager.silenceSkippingEnabled.collect {
                _playbackState.value.currentBook?.let { applyEffectiveSettingsForBook(it) }
            }
        }
        scope.launch {
            preferencesManager.volumeBoostLevel.collect {
                _playbackState.value.currentBook?.let { applyEffectiveSettingsForBook(it) }
            }
        }
        scope.launch {
            preferencesManager.globalMonoDownmix.collect {
                _playbackState.value.currentBook?.let { applyEffectiveSettingsForBook(it) }
            }
        }
        scope.launch {
            preferencesManager.globalLoudnessNormalization.collect {
                _playbackState.value.currentBook?.let { applyEffectiveSettingsForBook(it) }
            }
        }
        scope.launch {
            preferencesManager.globalVoiceBoostPreset.collect {
                _playbackState.value.currentBook?.let { applyEffectiveSettingsForBook(it) }
            }
        }
    }

    fun dismissNextInSeriesPrompt() {
        _nextInSeriesPrompt.value = null
    }

    fun playNextInSeries(nextBook: BookEntity) {
        _nextInSeriesPrompt.value = null
        loadBook(nextBook.id)
        play()
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
                    setupAudioEffects()
                    handlePlaybackResumed()
                } else {
                    handlePlaybackPaused()
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    setupAudioEffects()
                } else if (state == Player.STATE_ENDED) {
                    onChapterEnded()
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                try {
                    exoPlayer.pause()
                } catch (_: Exception) {}
                saveCurrentPosition()
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = false,
                    isPlaybackError = true,
                    playbackErrorMessage = context.getString(com.evenfall.app.R.string.error_playback_file_missing_desc)
                )
            }
        })
    }

    fun dismissPlaybackError() {
        _playbackState.value = _playbackState.value.copy(
            isPlaybackError = false,
            playbackErrorMessage = null
        )
    }

    fun retryPlayback() {
        val currentBook = _playbackState.value.currentBook ?: return
        dismissPlaybackError()
        loadBook(currentBook.id, autoPlay = true)
    }

    fun loadBook(book: BookEntity, autoPlay: Boolean = false) {
        loadBook(book.id, autoPlay)
    }

    fun loadBook(bookId: Long, autoPlay: Boolean = false) {
        scope.launch {
            val book = bookDao.getBookByIdSync(bookId) ?: return@launch
            val chapters = chapterDao.getChaptersForBookSync(bookId)
            val currentChapterIdx = book.currentChapterIndex.coerceIn(0, (chapters.size - 1).coerceAtLeast(0))
            val currentChapter = chapters.getOrNull(currentChapterIdx)

            val isLongGap = book.lastPlayedTimestamp > 0L &&
                    SmartResumeTiers.isLongGap(System.currentTimeMillis() - book.lastPlayedTimestamp)

            _playbackState.value = _playbackState.value.copy(
                currentBook = book,
                chapters = chapters,
                currentChapter = currentChapter,
                chapterPositionMs = book.chapterPositionMs,
                chapterDurationMs = currentChapter?.durationMs ?: (book.totalDurationMs / chapters.size.coerceAtLeast(1)),
                totalPositionMs = book.currentPositionMs,
                totalDurationMs = book.totalDurationMs,
                isMiniPlayerDismissed = false,
                showWelcomeBackCard = isLongGap,
                welcomeBackChapterTitle = if (isLongGap) currentChapter?.title else null
            )

            // Setup ExoPlayer MediaItem
            if (currentChapter != null && currentChapter.fileUri.startsWith("content://")) {
                try {
                    val mediaItem = MediaItem.Builder()
                        .setUri(Uri.parse(currentChapter.fileUri))
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(book.displayTitle)
                                .setArtist(book.displayAuthor)
                                .setDisplayTitle(currentChapter.title)
                                .build()
                        )
                        .build()
                    exoPlayer.setMediaItem(mediaItem)
                    exoPlayer.prepare()
                    exoPlayer.seekTo(book.chapterPositionMs)
                } catch (_: Exception) {}
            }

            applyEffectiveSettingsForBook(book)
            EvenfallShortcutManager.updateShortcuts(context, book)
            EvenfallTileService.requestTileUpdate(context)

            if (autoPlay) {
                play()
            }
        }
    }

    fun loadBookAndSeek(bookId: Long, chapterIndex: Int, positionMs: Long, autoPlay: Boolean = false) {
        scope.launch {
            val book = bookDao.getBookByIdSync(bookId) ?: return@launch
            val chapters = chapterDao.getChaptersForBookSync(bookId)
            val validChapterIdx = chapterIndex.coerceIn(0, (chapters.size - 1).coerceAtLeast(0))
            val targetChapter = chapters.getOrNull(validChapterIdx)

            val clampedChapterPos = if (targetChapter != null && targetChapter.durationMs > 0L) {
                positionMs.coerceIn(0L, targetChapter.durationMs)
            } else {
                positionMs.coerceAtLeast(0L)
            }

            val totalPos = (targetChapter?.startMs ?: 0L) + clampedChapterPos
            val isLongGap = book.lastPlayedTimestamp > 0L &&
                    SmartResumeTiers.isLongGap(System.currentTimeMillis() - book.lastPlayedTimestamp)

            _playbackState.value = _playbackState.value.copy(
                currentBook = book,
                chapters = chapters,
                currentChapter = targetChapter,
                chapterPositionMs = clampedChapterPos,
                chapterDurationMs = targetChapter?.durationMs ?: (book.totalDurationMs / chapters.size.coerceAtLeast(1)),
                totalPositionMs = totalPos,
                totalDurationMs = book.totalDurationMs,
                isMiniPlayerDismissed = false,
                showWelcomeBackCard = isLongGap,
                welcomeBackChapterTitle = if (isLongGap) targetChapter?.title else null
            )

            // Setup ExoPlayer MediaItem
            if (targetChapter != null && targetChapter.fileUri.startsWith("content://")) {
                try {
                    val mediaItem = MediaItem.Builder()
                        .setUri(Uri.parse(targetChapter.fileUri))
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(book.displayTitle)
                                .setArtist(book.displayAuthor)
                                .setDisplayTitle(targetChapter.title)
                                .build()
                        )
                        .build()
                    exoPlayer.setMediaItem(mediaItem)
                    exoPlayer.prepare()
                    exoPlayer.seekTo(clampedChapterPos)
                } catch (_: Exception) {}
            }

            applyEffectiveSettingsForBook(book)
            EvenfallShortcutManager.updateShortcuts(context, book)
            EvenfallTileService.requestTileUpdate(context)
            saveCurrentPosition()

            if (autoPlay) {
                play()
            }
        }
    }

    private fun applyEffectiveSettingsForBook(book: BookEntity) {
        scope.launch {
            val globalDefaultSpeed = preferencesManager.defaultPlaybackSpeed.first()
            val globalSkipSec = preferencesManager.skipAmountSeconds.first()
            val globalSilenceSkipping = preferencesManager.silenceSkippingEnabled.first()
            val globalVolumeBoost = preferencesManager.volumeBoostLevel.first()
            val globalMono = preferencesManager.globalMonoDownmix.first()
            val globalLoudness = preferencesManager.globalLoudnessNormalization.first()
            val globalVoiceBoost = preferencesManager.globalVoiceBoostPreset.first()

            val effectiveSpeed = book.speedOverride ?: book.playbackSpeed.takeIf { it > 0f } ?: globalDefaultSpeed
            val effectiveSkipBack = book.skipBackSecondsOverride ?: globalSkipSec
            val effectiveSkipForward = book.skipForwardSecondsOverride ?: globalSkipSec
            val effectiveSilenceSkipping = book.silenceSkippingOverride ?: globalSilenceSkipping
            val effectiveVolumeBoost = book.volumeBoostOverride ?: globalVolumeBoost
            val effectiveMono = book.monoDownmixOverride ?: globalMono
            val effectiveLoudness = book.loudnessNormalizationOverride ?: globalLoudness
            val effectiveVoiceBoost = book.voiceBoostPresetOverride ?: globalVoiceBoost

            exoPlayer.setPlaybackSpeed(effectiveSpeed)
            exoPlayer.skipSilenceEnabled = effectiveSilenceSkipping
            monoAudioProcessor.isEnabled = effectiveMono

            _playbackState.value = _playbackState.value.copy(
                playbackSpeed = effectiveSpeed,
                skipBackSeconds = effectiveSkipBack,
                skipForwardSeconds = effectiveSkipForward,
                silenceSkippingEnabled = effectiveSilenceSkipping,
                volumeBoostLevel = effectiveVolumeBoost,
                monoDownmixEnabled = effectiveMono,
                loudnessNormalizationEnabled = effectiveLoudness,
                voiceBoostPreset = effectiveVoiceBoost
            )

            setupAudioEffects()
        }
    }

    fun play() {
        val state = _playbackState.value
        val book = state.currentBook ?: return

        scope.launch {
            // Tiered smart resume auto-rewind
            val autoRewindEnabled = preferencesManager.autoRewindEnabled.first()
            if (autoRewindEnabled && lastPauseTimestamp > 0) {
                val pausedDurationMs = System.currentTimeMillis() - lastPauseTimestamp
                val smartConfig = SmartResumeConfig(
                    enabled = true,
                    tier1Sec = preferencesManager.smartResumeTier1Sec.first(),
                    tier2Sec = preferencesManager.smartResumeTier2Sec.first(),
                    tier3Sec = preferencesManager.smartResumeTier3Sec.first(),
                    tier4Sec = preferencesManager.smartResumeTier4Sec.first(),
                    tier5Sec = preferencesManager.smartResumeTier5Sec.first()
                )
                val rewindMs = SmartResumeTiers.calculateRewindMs(pausedDurationMs, smartConfig)
                if (rewindMs > 0) {
                    seekTo((state.chapterPositionMs - rewindMs).coerceAtLeast(0L), recordUndo = false)
                }
            }
            lastPauseTimestamp = 0L

            // Check bedtime sleep timer schedule
            checkBedtimeSchedule()

            exoPlayer.play()
            _playbackState.value = _playbackState.value.copy(isPlaying = true)

            EvenfallTileService.requestTileUpdate(context)
            EvenfallShortcutManager.updateShortcuts(context, book)

            val sessionRecord = sessionTracker.onPlay(
                nowMs = System.currentTimeMillis(),
                bookId = book.id,
                positionMs = _playbackState.value.chapterPositionMs
            )
            sessionRecord?.let { saveListeningSession(it) }

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

    private suspend fun checkBedtimeSchedule() {
        val enabled = preferencesManager.bedtimeEnabled.first()
        if (!enabled) return

        val days = preferencesManager.bedtimeDays.first().mapNotNull { it.toIntOrNull() }.toSet()
        val startHour = preferencesManager.bedtimeStartHour.first()
        val startMinute = preferencesManager.bedtimeStartMinute.first()
        val duration = preferencesManager.bedtimeDurationMinutes.first()
        val autoStart = preferencesManager.bedtimeAutoStart.first()

        val config = BedtimeConfig(
            enabled = true,
            startHour = startHour,
            startMinute = startMinute,
            durationMinutes = duration,
            daysOfWeek = if (days.isEmpty()) setOf(1, 2, 3, 4, 5, 6, 7) else days,
            autoStart = autoStart
        )

        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val todayStr = "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"

        if (todayStr != lastBedtimeTriggerDate && BedtimeScheduleEvaluator.isWithinBedtimeWindow(now, config)) {
            lastBedtimeTriggerDate = todayStr
            if (autoStart) {
                if (duration == -1) {
                    startSleepTimerEndOfChapter()
                } else {
                    startSleepTimer(duration)
                }
            } else {
                _playbackState.value = _playbackState.value.copy(
                    showBedtimePrompt = true,
                    bedtimePromptDurationMinutes = duration
                )
            }
        }
    }

    fun dismissBedtimePrompt() {
        _playbackState.value = _playbackState.value.copy(showBedtimePrompt = false)
    }

    fun acceptBedtimePrompt() {
        val duration = _playbackState.value.bedtimePromptDurationMinutes
        if (duration == -1) {
            startSleepTimerEndOfChapter()
        } else {
            startSleepTimer(duration)
        }
        dismissBedtimePrompt()
    }

    fun dismissWelcomeBack() {
        _playbackState.value = _playbackState.value.copy(showWelcomeBackCard = false)
    }

    fun rewindToChapterStart() {
        seekTo(0L, recordUndo = true)
        dismissWelcomeBack()
    }

    fun pause() {
        val state = _playbackState.value
        val book = state.currentBook ?: return

        lastPauseTimestamp = System.currentTimeMillis()
        exoPlayer.pause()
        _playbackState.value = _playbackState.value.copy(isPlaying = false)
        saveCurrentPosition()
        EvenfallTileService.requestTileUpdate(context)

        val sessionRecord = sessionTracker.onPause(
            nowMs = System.currentTimeMillis(),
            positionMs = state.chapterPositionMs
        )
        sessionRecord?.let { saveListeningSession(it) }

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
        val skipSec = _playbackState.value.skipForwardSeconds
        val newPos = _playbackState.value.chapterPositionMs + (skipSec * 1000L)
        seekTo(newPos, recordUndo = true)
    }

    fun skipBackward() {
        val skipSec = _playbackState.value.skipBackSeconds
        val newPos = (_playbackState.value.chapterPositionMs - (skipSec * 1000L)).coerceAtLeast(0L)
        seekTo(newPos, recordUndo = true)
    }

    fun seekTo(positionMs: Long, recordUndo: Boolean = true) {
        val current = _playbackState.value.chapterPositionMs
        val undoPos = if (recordUndo && kotlin.math.abs(positionMs - current) > 3000) current else _playbackState.value.undoSeekPositionMs

        val duration = _playbackState.value.chapterDurationMs
        val clampedPos = positionMs.coerceIn(0L, duration)

        if (exoPlayer.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)) {
            exoPlayer.seekTo(clampedPos)
        }

        sessionTracker.onSeek(
            nowMs = System.currentTimeMillis(),
            newPositionMs = clampedPos
        )

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

        sessionTracker.onSeek(
            nowMs = System.currentTimeMillis(),
            newPositionMs = 0L
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
                    val shouldFinish = AutoMarkFinishedEvaluator.shouldAutoMarkFinished(
                        totalPositionMs = book.totalDurationMs,
                        totalDurationMs = book.totalDurationMs,
                        isLastChapterEnded = true,
                        isAutoMarkEnabled = autoMarkFinishedEnabled,
                        isAlreadyFinished = book.isFinished
                    )
                    if (shouldFinish) {
                        val now = System.currentTimeMillis()
                        val finishedBook = book.copy(
                            isFinished = true,
                            finishedAt = now,
                            currentPositionMs = book.totalDurationMs
                        )
                        _playbackState.value = _playbackState.value.copy(currentBook = finishedBook)
                        bookDao.update(finishedBook)
                        checkNextInSeries(book)
                    } else {
                        bookDao.update(book.copy(currentPositionMs = book.totalDurationMs))
                    }
                }
            }
        }
    }

    private suspend fun checkNextInSeries(book: BookEntity) {
        val seriesName = book.displaySeriesName
        if (!seriesName.isNullOrBlank()) {
            val allBooks = bookDao.getAllBooksSync()
            val seriesBooks = allBooks
                .filter { it.displaySeriesName.equals(seriesName, ignoreCase = true) }
                .sortedWith(compareBy({ it.displaySeriesOrder ?: Float.MAX_VALUE }, { it.id }))

            val currentOrder = book.displaySeriesOrder
            val nextBook = if (currentOrder != null) {
                seriesBooks.firstOrNull { (it.displaySeriesOrder ?: 0f) > currentOrder && it.id != book.id }
            } else {
                val idx = seriesBooks.indexOfFirst { it.id == book.id }
                if (idx != -1 && idx < seriesBooks.size - 1) seriesBooks[idx + 1] else null
            }

            if (nextBook != null && !nextBook.isFinished) {
                if (autoPlayNextInSeriesEnabled) {
                    _nextInSeriesPrompt.value = nextBook
                    loadBook(nextBook.id)
                    play()
                } else {
                    _nextInSeriesPrompt.value = nextBook
                }
            }
        }
    }

    // Playback Speed
    fun setPlaybackSpeed(speed: Float) {
        val clamped = (speed.coerceIn(0.5f, 3.0f) * 20).toInt() / 20f
        val book = _playbackState.value.currentBook
        if (book != null && book.speedOverride != null) {
            setBookSpeedOverride(clamped)
        } else {
            exoPlayer.setPlaybackSpeed(clamped)
            _playbackState.value = _playbackState.value.copy(playbackSpeed = clamped)
            scope.launch {
                book?.let { bookDao.updatePlaybackSpeed(it.id, clamped) }
            }
        }
    }

    fun setSilenceSkipping(enabled: Boolean) {
        val book = _playbackState.value.currentBook
        if (book != null && book.silenceSkippingOverride != null) {
            setBookSilenceSkippingOverride(enabled)
        } else {
            exoPlayer.skipSilenceEnabled = enabled
            _playbackState.value = _playbackState.value.copy(silenceSkippingEnabled = enabled)
            scope.launch {
                preferencesManager.setSilenceSkippingEnabled(enabled)
            }
        }
    }

    fun setVolumeBoost(level: Float) {
        val clamped = level.coerceIn(0f, 1f)
        val book = _playbackState.value.currentBook
        if (book != null && book.volumeBoostOverride != null) {
            setBookVolumeBoostOverride(clamped)
        } else {
            _playbackState.value = _playbackState.value.copy(volumeBoostLevel = clamped)
            setupAudioEffects()
            scope.launch {
                preferencesManager.setVolumeBoostLevel(clamped)
            }
        }
    }

    fun setMonoDownmix(enabled: Boolean) {
        val book = _playbackState.value.currentBook
        if (book != null && book.monoDownmixOverride != null) {
            setBookMonoDownmixOverride(enabled)
        } else {
            monoAudioProcessor.isEnabled = enabled
            _playbackState.value = _playbackState.value.copy(monoDownmixEnabled = enabled)
            scope.launch {
                preferencesManager.setGlobalMonoDownmix(enabled)
            }
        }
    }

    fun setLoudnessNormalization(enabled: Boolean) {
        val book = _playbackState.value.currentBook
        if (book != null && book.loudnessNormalizationOverride != null) {
            setBookLoudnessNormalizationOverride(enabled)
        } else {
            _playbackState.value = _playbackState.value.copy(loudnessNormalizationEnabled = enabled)
            setupAudioEffects()
            scope.launch {
                preferencesManager.setGlobalLoudnessNormalization(enabled)
            }
        }
    }

    fun setVoiceBoostPreset(preset: String) {
        val book = _playbackState.value.currentBook
        if (book != null && book.voiceBoostPresetOverride != null) {
            setBookVoiceBoostPresetOverride(preset)
        } else {
            _playbackState.value = _playbackState.value.copy(voiceBoostPreset = preset)
            setupAudioEffects()
            scope.launch {
                preferencesManager.setGlobalVoiceBoostPreset(preset)
            }
        }
    }

    // Per-Book Overrides Methods
    fun setBookSpeedOverride(speed: Float?) {
        val book = _playbackState.value.currentBook ?: return
        val clamped = speed?.let { (it.coerceIn(0.5f, 3.0f) * 20).toInt() / 20f }
        scope.launch {
            bookDao.updateSpeedOverride(book.id, clamped)
            val updated = book.copy(speedOverride = clamped)
            _playbackState.value = _playbackState.value.copy(currentBook = updated)
            applyEffectiveSettingsForBook(updated)
        }
    }

    fun setBookSkipBackOverride(seconds: Int?) {
        val book = _playbackState.value.currentBook ?: return
        val clamped = seconds?.coerceIn(5, 120)
        scope.launch {
            bookDao.updateSkipBackOverride(book.id, clamped)
            val updated = book.copy(skipBackSecondsOverride = clamped)
            _playbackState.value = _playbackState.value.copy(currentBook = updated)
            applyEffectiveSettingsForBook(updated)
        }
    }

    fun setBookSkipForwardOverride(seconds: Int?) {
        val book = _playbackState.value.currentBook ?: return
        val clamped = seconds?.coerceIn(5, 120)
        scope.launch {
            bookDao.updateSkipForwardOverride(book.id, clamped)
            val updated = book.copy(skipForwardSecondsOverride = clamped)
            _playbackState.value = _playbackState.value.copy(currentBook = updated)
            applyEffectiveSettingsForBook(updated)
        }
    }

    fun setBookSilenceSkippingOverride(enabled: Boolean?) {
        val book = _playbackState.value.currentBook ?: return
        scope.launch {
            bookDao.updateSilenceSkippingOverride(book.id, enabled)
            val updated = book.copy(silenceSkippingOverride = enabled)
            _playbackState.value = _playbackState.value.copy(currentBook = updated)
            applyEffectiveSettingsForBook(updated)
        }
    }

    fun setBookVolumeBoostOverride(level: Float?) {
        val book = _playbackState.value.currentBook ?: return
        val clamped = level?.coerceIn(0f, 1f)
        scope.launch {
            bookDao.updateVolumeBoostOverride(book.id, clamped)
            val updated = book.copy(volumeBoostOverride = clamped)
            _playbackState.value = _playbackState.value.copy(currentBook = updated)
            applyEffectiveSettingsForBook(updated)
        }
    }

    fun setBookVoiceBoostPresetOverride(preset: String?) {
        val book = _playbackState.value.currentBook ?: return
        scope.launch {
            bookDao.updateVoiceBoostPresetOverride(book.id, preset)
            val updated = book.copy(voiceBoostPresetOverride = preset)
            _playbackState.value = _playbackState.value.copy(currentBook = updated)
            applyEffectiveSettingsForBook(updated)
        }
    }

    fun setBookMonoDownmixOverride(enabled: Boolean?) {
        val book = _playbackState.value.currentBook ?: return
        scope.launch {
            bookDao.updateMonoDownmixOverride(book.id, enabled)
            val updated = book.copy(monoDownmixOverride = enabled)
            _playbackState.value = _playbackState.value.copy(currentBook = updated)
            applyEffectiveSettingsForBook(updated)
        }
    }

    fun setBookLoudnessNormalizationOverride(enabled: Boolean?) {
        val book = _playbackState.value.currentBook ?: return
        scope.launch {
            bookDao.updateLoudnessNormalizationOverride(book.id, enabled)
            val updated = book.copy(loudnessNormalizationOverride = enabled)
            _playbackState.value = _playbackState.value.copy(currentBook = updated)
            applyEffectiveSettingsForBook(updated)
        }
    }

    fun resetBookPlaybackOverrides() {
        val book = _playbackState.value.currentBook ?: return
        scope.launch {
            bookDao.resetPlaybackOverrides(book.id)
            val resetBook = book.copy(
                speedOverride = null,
                skipBackSecondsOverride = null,
                skipForwardSecondsOverride = null,
                silenceSkippingOverride = null,
                volumeBoostOverride = null,
                voiceBoostPresetOverride = null,
                monoDownmixOverride = null,
                loudnessNormalizationOverride = null
            )
            _playbackState.value = _playbackState.value.copy(currentBook = resetBook)
            applyEffectiveSettingsForBook(resetBook)
        }
    }

    private fun setupAudioEffects() {
        try {
            val audioSessionId = exoPlayer.audioSessionId
            if (audioSessionId != C.AUDIO_SESSION_ID_UNSET && audioSessionId != lastAudioSessionId) {
                lastAudioSessionId = audioSessionId
                loudnessEnhancer?.release()
                equalizer?.release()
                loudnessEnhancer = LoudnessEnhancer(audioSessionId)
                equalizer = Equalizer(0, audioSessionId)
            }
            applyAudioEffects()
        } catch (_: Exception) {}
    }

    private fun applyAudioEffects() {
        val state = _playbackState.value
        try {
            val gainMb = ((state.volumeBoostLevel * 1000) + (if (state.loudnessNormalizationEnabled) 400 else 0)).toInt()
            if (gainMb > 0) {
                loudnessEnhancer?.setTargetGain(gainMb)
                loudnessEnhancer?.enabled = true
            } else {
                loudnessEnhancer?.enabled = false
            }

            val eq = equalizer
            if (eq != null) {
                when (state.voiceBoostPreset) {
                    "Light" -> {
                        eq.enabled = true
                        val numBands = eq.numberOfBands
                        for (i in 0 until numBands) {
                            val freq = eq.getCenterFreq(i.toShort()) // in mHz
                            val level: Short = when {
                                freq < 300_000 -> (-250).toShort()
                                freq in 1_000_000..4_000_000 -> 350.toShort()
                                freq > 8_000_000 -> (-100).toShort()
                                else -> 0.toShort()
                            }
                            eq.setBandLevel(i.toShort(), level)
                        }
                    }
                    "Strong" -> {
                        eq.enabled = true
                        val numBands = eq.numberOfBands
                        for (i in 0 until numBands) {
                            val freq = eq.getCenterFreq(i.toShort()) // in mHz
                            val level: Short = when {
                                freq < 300_000 -> (-500).toShort()
                                freq in 1_000_000..4_000_000 -> 600.toShort()
                                freq > 8_000_000 -> (-200).toShort()
                                else -> 0.toShort()
                            }
                            eq.setBandLevel(i.toShort(), level)
                        }
                    }
                    else -> {
                        eq.enabled = false
                    }
                }
            }
        } catch (_: Exception) {}
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

    private fun handlePlaybackResumed() {
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
                    val totalPos = startOffset + currentPos

                    _playbackState.value = _playbackState.value.copy(
                        chapterPositionMs = currentPos,
                        chapterDurationMs = duration,
                        totalPositionMs = totalPos
                    )
                    saveCurrentPosition()

                    val sessionRecord = sessionTracker.onTick(
                        nowMs = System.currentTimeMillis(),
                        positionMs = currentPos
                    )
                    sessionRecord?.let { saveListeningSession(it) }

                    // Auto-mark finished when reaching >= 98%
                    val book = _playbackState.value.currentBook
                    if (book != null && !book.isFinished) {
                        val shouldFinish = AutoMarkFinishedEvaluator.shouldAutoMarkFinished(
                            totalPositionMs = totalPos,
                            totalDurationMs = book.totalDurationMs,
                            isLastChapterEnded = false,
                            isAutoMarkEnabled = autoMarkFinishedEnabled,
                            isAlreadyFinished = book.isFinished
                        )
                        if (shouldFinish) {
                            val now = System.currentTimeMillis()
                            val finishedBook = book.copy(isFinished = true, finishedAt = now)
                            _playbackState.value = _playbackState.value.copy(currentBook = finishedBook)
                            scope.launch(Dispatchers.IO) {
                                bookDao.updateFinishedStatus(book.id, finished = true, finishedAt = now)
                            }
                        }
                    }
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
        val finalRecord = sessionTracker.flush()
        finalRecord?.let { saveListeningSession(it) }
        loudnessEnhancer?.release()
        equalizer?.release()
        exoPlayer.release()
    }
}
