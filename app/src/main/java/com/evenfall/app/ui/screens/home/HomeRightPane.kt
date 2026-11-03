package com.evenfall.app.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evenfall.app.R
import com.evenfall.app.data.db.entity.BookEntity
import com.evenfall.app.playback.PlaybackState
import com.evenfall.app.ui.components.BookCoverArt
import com.evenfall.app.ui.components.CircularSkipButton
import com.evenfall.app.ui.components.WavyProgressLine
import com.evenfall.app.ui.theme.CookieShape
import com.evenfall.app.ui.theme.SerifFont
import com.evenfall.app.util.TimeUtils

/**
 * Tablet and foldable right-pane detail and embedded player.
 * Shown on medium and expanded screen widths (>= 600dp).
 */
@Composable
fun HomeRightPane(
    playbackState: PlaybackState,
    selectedBook: BookEntity?,
    onPlayPause: () -> Unit,
    onSkipBackward: () -> Unit,
    onSkipForward: () -> Unit,
    onSeek: (Float) -> Unit,
    onOpenFullPlayer: (Long) -> Unit,
    onPlayBook: (BookEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeBook = if (playbackState.currentBook != null && (selectedBook == null || selectedBook.id == playbackState.currentBook.id)) {
        playbackState.currentBook
    } else {
        selectedBook ?: playbackState.currentBook
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        if (activeBook != null) {
            val isCurrentPlaying = playbackState.currentBook?.id == activeBook.id
            val currentPositionMs = if (isCurrentPlaying) playbackState.totalPositionMs else activeBook.currentPositionMs
            val totalDurationMs = activeBook.totalDurationMs

            val progressFraction = if (totalDurationMs > 0) {
                (currentPositionMs.toFloat() / totalDurationMs).coerceIn(0f, 1f)
            } else 0f
            val timeLeftMs = (totalDurationMs - currentPositionMs).coerceAtLeast(0L)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Large Cookie Cover Art
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .clip(CookieShape(16))
                ) {
                    BookCoverArt(
                        title = activeBook.displayTitle,
                        author = activeBook.displayAuthor,
                        coverPath = activeBook.displayCoverUri,
                        shape = CookieShape(16),
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Title
                Text(
                    text = activeBook.displayTitle,
                    fontFamily = SerifFont,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Author
                Text(
                    text = activeBook.displayAuthor,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!activeBook.displayNarrator.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Narrated by ${activeBook.displayNarrator}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Wavy progress line
                WavyProgressLine(
                    progress = progressFraction,
                    isPlaying = playbackState.isPlaying && isCurrentPlaying,
                    onSeek = onSeek,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = TimeUtils.formatTimestamp(currentPositionMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = TimeUtils.formatTimeLeft(timeLeftMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Transport Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onSkipBackward,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = stringResource(R.string.cd_skip_backward),
                            modifier = Modifier.size(26.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Surface(
                        shape = CookieShape(12),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(60.dp)
                            .clickable {
                                if (isCurrentPlaying) {
                                    onPlayPause()
                                } else {
                                    onPlayBook(activeBook)
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (playbackState.isPlaying && isCurrentPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playbackState.isPlaying && isCurrentPlaying) stringResource(R.string.cd_pause) else stringResource(R.string.cd_play),
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    CircularSkipButton(
                        seconds = playbackState.skipForwardSeconds,
                        isForward = true,
                        onClick = onSkipForward,
                        size = 48.dp,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // "Open player" button
                Button(
                    onClick = { onOpenFullPlayer(activeBook.id) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    Icon(Icons.Default.OpenInFull, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.app_name) + " Player", fontSize = 13.sp)
                }
            }
        } else {
            // Calm placeholder for right pane when library has no books loaded
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CookieShape(12),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    fontFamily = SerifFont,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.empty_library_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
