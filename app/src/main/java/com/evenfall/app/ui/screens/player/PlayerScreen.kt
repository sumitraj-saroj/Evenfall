package com.evenfall.app.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.evenfall.app.ui.components.BookCoverArt
import com.evenfall.app.ui.theme.CookieShape
import com.evenfall.app.ui.components.DashedSegmentedBar
import com.evenfall.app.ui.components.MorphingPlayPauseButton
import com.evenfall.app.ui.components.SparkleDecorations
import com.evenfall.app.ui.components.WavyProgressLine
import com.evenfall.app.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onCollapse: () -> Unit,
    onOpenBookmarks: (Long) -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val state by viewModel.playbackState.collectAsState()
    val skipSeconds by viewModel.skipSeconds.collectAsState()
    val book = state.currentBook
    val chapter = state.currentChapter

    val isDark = isSystemInDarkTheme()
    val bookColors = PlayerThemeExtractor.getColorsForBook(book?.title ?: "Evenfall", isDark)

    var showChapterSheet by remember { mutableStateOf(false) }
    var showSleepSheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showVolumeBoostSheet by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bookColors.backgroundBrush)
    ) {
        SparkleDecorations(tint = bookColors.onBackground.copy(alpha = 0.15f))

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                // Top bar: Collapse chevron, Chapter list, Overflow menu
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Collapse chevron
                    Surface(
                        shape = CircleShape,
                        color = bookColors.cardBackground,
                        modifier = Modifier.size(44.dp)
                    ) {
                        IconButton(onClick = onCollapse) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Collapse player",
                                tint = bookColors.onBackground
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Chapter list button
                        Surface(
                            shape = CircleShape,
                            color = bookColors.cardBackground,
                            modifier = Modifier.size(44.dp)
                        ) {
                            IconButton(onClick = { showChapterSheet = true }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                                    contentDescription = "Chapter list",
                                    tint = bookColors.onBackground
                                )
                            }
                        }

                        // Overflow menu
                        Box {
                            Surface(
                                shape = CircleShape,
                                color = bookColors.cardBackground,
                                modifier = Modifier.size(44.dp)
                            ) {
                                IconButton(onClick = { showOverflowMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "More options",
                                        tint = bookColors.onBackground
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Save bookmark") },
                                    leadingIcon = { Icon(Icons.Default.BookmarkBorder, contentDescription = null) },
                                    onClick = {
                                        viewModel.quickSaveBookmark()
                                        showOverflowMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Bookmarks & History") },
                                    leadingIcon = { Icon(Icons.Default.Bookmark, contentDescription = null) },
                                    onClick = {
                                        book?.let { onOpenBookmarks(it.id) }
                                        showOverflowMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Playback speed") },
                                    leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null) },
                                    onClick = {
                                        showSpeedSheet = true
                                        showOverflowMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Cover Art (~270dp)
                BookCoverArt(
                    title = book?.title ?: "Evenfall",
                    author = book?.author ?: "",
                    coverPath = book?.coverUri,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .size(265.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Title & Author (matching Images 2 & 3 in Serif)
                Text(
                    text = book?.title ?: "Untitled",
                    fontFamily = FontFamily.Serif,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = bookColors.onBackground,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = book?.author ?: "Unknown Author",
                    fontSize = 15.sp,
                    color = bookColors.onBackgroundMuted,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Book Progress: Segmented Dashed Bar (one segment per chapter)
                val totalChapters = state.chapters.size.coerceAtLeast(1)
                val currentChapterIdx = state.currentChapter?.chapterIndex ?: 0
                DashedSegmentedBar(
                    totalSegments = totalChapters,
                    currentSegment = currentChapterIdx,
                    progressWithinCurrent = state.chapterProgressFraction,
                    activeColor = bookColors.activeSliderColor,
                    inactiveColor = bookColors.inactiveSliderColor,
                    barHeight = 5.dp,
                    gapWidth = 4.dp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // "% of book" on left, "time left" on right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${state.totalProgressPercent}% of book",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = bookColors.onBackgroundMuted
                    )
                    Text(
                        text = TimeUtils.formatTimeLeft(state.totalTimeRemainingMs),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = bookColors.onBackgroundMuted
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Chapter Selector Pill
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = bookColors.cardBackground,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = viewModel::previousChapter,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous Chapter",
                                tint = bookColors.onBackground
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showChapterSheet = true },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Chapter ${currentChapterIdx + 1} of $totalChapters",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = bookColors.onBackgroundMuted
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = chapter?.title ?: "Chapter ${currentChapterIdx + 1}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = bookColors.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = bookColors.onBackground,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = viewModel::nextChapter,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next Chapter",
                                tint = bookColors.onBackground
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Chapter Seek Slider: Wavy when playing, Flat when paused!
                WavyProgressLine(
                    progress = state.chapterProgressFraction,
                    onSeek = viewModel::seekToFraction,
                    isPlaying = state.isPlaying,
                    activeColor = bookColors.activeSliderColor,
                    inactiveColor = bookColors.inactiveSliderColor,
                    thumbColor = bookColors.activeSliderColor
                )

                // Elapsed time, "Back to mm:ss" undo chip, and remaining time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = TimeUtils.formatTimestamp(state.chapterPositionMs),
                        fontSize = 12.sp,
                        color = bookColors.onBackgroundMuted
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Undo seek chip (matching Image 2: "Back to 06:43")
                    if (state.undoSeekPositionMs != null) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = bookColors.undoChipBackground,
                            modifier = Modifier.clickable(onClick = viewModel::undoSeek)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = "Undo Seek",
                                    tint = bookColors.undoChipContent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Back to ${TimeUtils.formatTimestamp(state.undoSeekPositionMs ?: 0L)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = bookColors.undoChipContent
                                )
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Text(
                        text = "-${TimeUtils.formatTimestamp(state.chapterTimeRemainingMs)}",
                        fontSize = 12.sp,
                        color = bookColors.onBackgroundMuted
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Main Controls: Skip Back, Morphing Play/Pause, Skip Forward
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Skip back
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(bookColors.skipButtonBackground)
                            .clickable(onClick = viewModel::skipBackward),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = "Rewind $skipSeconds seconds",
                            tint = bookColors.skipButtonContent,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(28.dp))

                    // Large Morphing Play/Pause Button
                    MorphingPlayPauseButton(
                        isPlaying = state.isPlaying,
                        onClick = viewModel::togglePlayPause,
                        size = 86.dp,
                        iconSize = 42.dp,
                        containerColor = bookColors.playButtonContainer,
                        contentColor = bookColors.playButtonContent
                    )

                    Spacer(modifier = Modifier.width(28.dp))

                    // Skip forward
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(bookColors.skipButtonBackground)
                            .clickable(onClick = viewModel::skipForward),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "Forward $skipSeconds seconds",
                            tint = bookColors.skipButtonContent,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Bottom Pill Toolbar (Sleep timer, Speed, Bookmarks, Silence skipping, Volume boost)
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = bookColors.bottomToolbarContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Sleep Timer (shows live countdown chip e.g. "29:57" when active)
                        if (state.sleepTimerIsActive && state.sleepTimerRemainingMs != null) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White.copy(alpha = 0.25f),
                                modifier = Modifier.clickable { showSleepSheet = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bedtime,
                                        contentDescription = "Sleep Timer Active",
                                        tint = bookColors.bottomToolbarContent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = TimeUtils.formatTimestamp(state.sleepTimerRemainingMs ?: 0L),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = bookColors.bottomToolbarContent
                                    )
                                }
                            }
                        } else {
                            IconButton(onClick = { showSleepSheet = true }) {
                                Icon(
                                    imageVector = Icons.Default.Bedtime,
                                    contentDescription = "Sleep Timer",
                                    tint = bookColors.bottomToolbarContent
                                )
                            }
                        }

                        // 2. Playback Speed
                        IconButton(onClick = { showSpeedSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Playback Speed: ${state.playbackSpeed}x",
                                tint = bookColors.bottomToolbarContent
                            )
                        }

                        // 3. Bookmarks & History
                        IconButton(onClick = { book?.let { onOpenBookmarks(it.id) } }) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = "Bookmarks and History",
                                tint = bookColors.bottomToolbarContent
                            )
                        }

                        // 4. Silence Skipping (Scissors icon)
                        IconButton(onClick = viewModel::toggleSilenceSkipping) {
                            Icon(
                                imageVector = Icons.Default.ContentCut,
                                contentDescription = "Silence Skipping",
                                tint = if (state.silenceSkippingEnabled) bookColors.bottomToolbarContent else bookColors.bottomToolbarContent.copy(alpha = 0.5f)
                            )
                        }

                        // 5. Volume Boost
                        IconButton(onClick = { showVolumeBoostSheet = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Volume Boost",
                                tint = if (state.volumeBoostLevel > 0f) bookColors.bottomToolbarContent else bookColors.bottomToolbarContent.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Chapter List Bottom Sheet
        if (showChapterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showChapterSheet = false },
                sheetState = rememberModalBottomSheetState()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Chapters",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                    ) {
                        itemsIndexed(state.chapters) { index, ch ->
                            val isCurrent = index == state.currentChapter?.chapterIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        viewModel.selectChapter(index)
                                        showChapterSheet = false
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 13.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(28.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ch.title,
                                        fontSize = 14.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = TimeUtils.formatDurationWords(ch.durationMs),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sleep Timer Bottom Sheet
        if (showSleepSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSleepSheet = false },
                sheetState = rememberModalBottomSheetState()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text("Sleep Timer", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))

                    val presets = listOf(5, 15, 30, 45, 60)
                    presets.forEach { min ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.startSleepTimer(min)
                                    showSleepSheet = false
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "$min minutes",
                                modifier = Modifier.padding(14.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.startSleepTimerEndOfChapter()
                                showSleepSheet = false
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "End of chapter",
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    if (state.sleepTimerIsActive) {
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(
                            onClick = {
                                viewModel.cancelSleepTimer()
                                showSleepSheet = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Turn off sleep timer", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Playback Speed Bottom Sheet
        if (showSpeedSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSpeedSheet = false },
                sheetState = rememberModalBottomSheetState()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Playback Speed", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${String.format("%.2f", state.playbackSpeed)}x",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Slider(
                        value = state.playbackSpeed,
                        onValueChange = viewModel::setPlaybackSpeed,
                        valueRange = 0.5f..3.0f,
                        steps = 49 // 0.05 increments
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f).forEach { preset ->
                            TextButton(onClick = { viewModel.setPlaybackSpeed(preset) }) {
                                Text("${preset}x")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Volume Boost Bottom Sheet
        if (showVolumeBoostSheet) {
            ModalBottomSheet(
                onDismissRequest = { showVolumeBoostSheet = false },
                sheetState = rememberModalBottomSheetState()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Volume Boost", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    val boostPercent = (state.volumeBoostLevel * 100).toInt()
                    Text(
                        text = if (boostPercent == 0) "Off" else "+$boostPercent%",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Slider(
                        value = state.volumeBoostLevel,
                        onValueChange = viewModel::setVolumeBoost,
                        valueRange = 0f..1.0f
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}
