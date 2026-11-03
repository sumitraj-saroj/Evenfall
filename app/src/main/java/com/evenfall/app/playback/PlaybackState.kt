package com.evenfall.app.playback

import com.evenfall.app.data.db.entity.BookEntity
import com.evenfall.app.data.db.entity.ChapterEntity

data class PlaybackState(
    val currentBook: BookEntity? = null,
    val currentChapter: ChapterEntity? = null,
    val chapters: List<ChapterEntity> = emptyList(),
    val isPlaying: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val chapterPositionMs: Long = 0L,
    val chapterDurationMs: Long = 1L,
    val totalPositionMs: Long = 0L,
    val totalDurationMs: Long = 1L,
    val sleepTimerRemainingMs: Long? = null,
    val sleepTimerIsActive: Boolean = false,
    val silenceSkippingEnabled: Boolean = false,
    val volumeBoostLevel: Float = 0f,
    val undoSeekPositionMs: Long? = null,
    val isMiniPlayerDismissed: Boolean = false,
    val skipBackSeconds: Int = 20,
    val skipForwardSeconds: Int = 20,
    val monoDownmixEnabled: Boolean = false,
    val loudnessNormalizationEnabled: Boolean = false,
    val voiceBoostPreset: String = "Off",
    val showWelcomeBackCard: Boolean = false,
    val welcomeBackChapterTitle: String? = null,
    val showBedtimePrompt: Boolean = false,
    val bedtimePromptDurationMinutes: Int = 30
) {
    val chapterProgressFraction: Float
        get() = if (chapterDurationMs > 0) {
            (chapterPositionMs.toFloat() / chapterDurationMs).coerceIn(0f, 1f)
        } else 0f

    val totalProgressFraction: Float
        get() = if (totalDurationMs > 0) {
            (totalPositionMs.toFloat() / totalDurationMs).coerceIn(0f, 1f)
        } else 0f

    val totalProgressPercent: Int
        get() = (totalProgressFraction * 100).toInt()

    val totalTimeRemainingMs: Long
        get() = (totalDurationMs - totalPositionMs).coerceAtLeast(0L)

    val chapterTimeRemainingMs: Long
        get() = (chapterDurationMs - chapterPositionMs).coerceAtLeast(0L)

    val hasCustomPlaybackOverrides: Boolean
        get() = currentBook?.hasPlaybackOverrides == true
}
