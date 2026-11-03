package com.evenfall.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evenfall.app.R
import com.evenfall.app.data.db.entity.BookEntity
import com.evenfall.app.playback.PlaybackState
import com.evenfall.app.ui.screens.player.PlayerThemeExtractor
import com.evenfall.app.ui.theme.CookieShape
import com.evenfall.app.ui.theme.SerifFont
import kotlin.math.abs

@Composable
fun MiniPlayer(
    playbackState: PlaybackState,
    book: BookEntity,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipBackward: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val bookColors = remember(book.title, isDark) {
        PlayerThemeExtractor.getColorsForBook(book.title, isDark)
    }
    val haptics = com.evenfall.app.ui.util.rememberEvenfallHaptics()

    var totalDragX by remember { mutableFloatStateOf(0f) }
    var totalDragY by remember { mutableFloatStateOf(0f) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = bookColors.cardBackground
        ),
        border = BorderStroke(1.dp, bookColors.onBackground.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("mini_player")
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        totalDragX = 0f
                        totalDragY = 0f
                    },
                    onDragEnd = {
                        if (totalDragY < -40f && abs(totalDragY) > abs(totalDragX)) {
                            // Swipe up -> expand
                            onExpand()
                        } else if (abs(totalDragX) > 60f || totalDragY > 50f) {
                            // Swipe left/right or down -> dismiss
                            onDismiss()
                        }
                        totalDragX = 0f
                        totalDragY = 0f
                    },
                    onDragCancel = {
                        totalDragX = 0f
                        totalDragY = 0f
                    },
                    onDrag = { _, dragAmount ->
                        totalDragX += dragAmount.x
                        totalDragY += dragAmount.y
                    }
                )
            }
            .clickable(onClick = onExpand)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cookie-cropped cover art
                BookCoverArt(
                    title = book.title,
                    author = book.author,
                    coverPath = book.coverUri,
                    shape = CookieShape(lobes = 12, depth = 0.16f),
                    modifier = Modifier.size(46.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Title and Chapter line
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = book.title,
                        fontFamily = SerifFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = bookColors.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val chapterText = playbackState.currentChapter?.title
                        ?: if (book.fileCount > 1) {
                            "Chapter ${(book.currentChapterIndex + 1)} of ${book.fileCount}"
                        } else {
                            book.author
                        }
                    Text(
                        text = chapterText,
                        fontSize = 12.sp,
                        color = bookColors.onBackgroundMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Skip backward (48dp touch target)
                IconButton(
                    onClick = onSkipBackward,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("mini_player_skip_back")
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = stringResource(R.string.cd_skip_backward),
                        tint = bookColors.skipButtonContent,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Play / Pause (48dp touch target)
                IconButton(
                    onClick = {
                        haptics.playPause()
                        onTogglePlayPause()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("mini_player_play_pause")
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = stringResource(
                            if (playbackState.isPlaying) R.string.cd_pause else R.string.cd_play
                        ),
                        tint = bookColors.playButtonContainer,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            // Thin wavy progress line
            WavyProgressLine(
                progress = playbackState.chapterProgressFraction,
                isPlaying = playbackState.isPlaying,
                activeColor = bookColors.activeSliderColor,
                inactiveColor = bookColors.inactiveSliderColor,
                strokeWidth = 2.dp,
                waveAmplitude = 2.dp,
                waveLength = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )
        }
    }
}
