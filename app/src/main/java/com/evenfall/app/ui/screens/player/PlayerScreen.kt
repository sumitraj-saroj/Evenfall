package com.evenfall.app.ui.screens.player

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.ui.input.pointer.pointerInput
import com.evenfall.app.ui.components.FirstRunHintCard
import com.evenfall.app.ui.components.QuickSkipAdjustDialog
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.evenfall.app.R
import com.evenfall.app.data.db.entity.ChapterEntity
import com.evenfall.app.playback.PlaybackState
import com.evenfall.app.ui.components.AddToCollectionDialog
import com.evenfall.app.ui.components.BookCoverArt
import com.evenfall.app.ui.components.CircularSkipButton
import com.evenfall.app.ui.components.DashedSegmentedBar
import com.evenfall.app.ui.components.EditBookDetailsDialog
import com.evenfall.app.ui.components.EditChapterTitleDialog
import com.evenfall.app.ui.components.MorphingPlayPauseButton
import com.evenfall.app.ui.components.WavyProgressLine
import com.evenfall.app.ui.theme.SerifFont
import com.evenfall.app.util.TimeUtils
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onCollapse: () -> Unit,
    onOpenBookmarks: (Long) -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val state by viewModel.playbackState.collectAsState()
    val skipSeconds by viewModel.skipSeconds.collectAsState()
    val compactPlayer by viewModel.compactPlayer.collectAsState()
    val playerGesturesEnabled by viewModel.playerGesturesEnabled.collectAsState()
    val book = state.currentBook
    val chapter = state.currentChapter

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val bookColors = PlayerThemeExtractor.getColorsForBook(book?.displayTitle ?: "Evenfall", isDark)

    var showChapterSheet by remember { mutableStateOf(false) }
    var showSleepSheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showPlaybackOptionsSheet by remember { mutableStateOf(false) }
    var showVolumeBoostSheet by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showEditDetailsDialog by remember { mutableStateOf(false) }
    var showAddToCollectionDialog by remember { mutableStateOf(false) }
    var quickSkipDialogData by remember { mutableStateOf<Pair<Boolean, Int>?>(null) }
    val allCollections by viewModel.allCollections.collectAsState(initial = emptyList())
    var currentBookCollectionIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var editingChapter by remember { mutableStateOf<ChapterEntity?>(null) }
    val haptics = com.evenfall.app.ui.util.rememberEvenfallHaptics()
    val hintLongPressSkipShown by viewModel.hintLongPressSkipShown.collectAsState()
    val hintBookmarkShown by viewModel.hintBookmarkShown.collectAsState()

    // Auto-dismiss undo seek chip after 8 seconds or when tapped
    var showUndoChip by remember { mutableStateOf(false) }
    LaunchedEffect(state.undoSeekPositionMs) {
        if (state.undoSeekPositionMs != null) {
            showUndoChip = true
            delay(8000L)
            showUndoChip = false
        } else {
            showUndoChip = false
        }
    }

    val totalChapters = state.chapters.size.coerceAtLeast(1)
    val hasMultipleChapters = totalChapters > 1
    val hasManyChapters = totalChapters >= 5
    val currentChapterIdx = state.currentChapter?.chapterIndex ?: 0

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bookColors.backgroundBrush)
    ) {
        // Decorative sparkles are removed from Now Playing screen per requirements

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                // Top bar with status bar padding: collapse chevron on left, overflow on right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = if (isLandscape) 2.dp else 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Collapse chevron
                    Surface(
                        shape = CircleShape,
                        color = bookColors.cardBackground,
                        modifier = Modifier.size(if (isLandscape) 36.dp else 44.dp)
                    ) {
                        IconButton(onClick = onCollapse) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = stringResource(R.string.cd_collapse_player),
                                tint = bookColors.onBackground
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // In dense mode only: show separate chapter list icon
                        if (!compactPlayer) {
                            Surface(
                                shape = CircleShape,
                                color = bookColors.cardBackground,
                                modifier = Modifier.size(if (isLandscape) 36.dp else 44.dp)
                            ) {
                                IconButton(onClick = { showChapterSheet = true }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                                        contentDescription = stringResource(R.string.cd_chapter_list),
                                        tint = bookColors.onBackground
                                    )
                                }
                            }
                        }

                        // Overflow menu
                        Box {
                            Surface(
                                shape = CircleShape,
                                color = bookColors.cardBackground,
                                modifier = Modifier.size(if (isLandscape) 36.dp else 44.dp)
                            ) {
                                IconButton(onClick = { showOverflowMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = stringResource(R.string.cd_more_options),
                                        tint = bookColors.onBackground
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.save_bookmark)) },
                                    leadingIcon = { Icon(Icons.Default.BookmarkBorder, contentDescription = null) },
                                    onClick = {
                                        viewModel.quickSaveBookmark()
                                        haptics.bookmarkSaved()
                                        showOverflowMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.bookmarks_and_history)) },
                                    leadingIcon = { Icon(Icons.Default.Bookmark, contentDescription = null) },
                                    onClick = {
                                        book?.let { onOpenBookmarks(it.id) }
                                        showOverflowMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.playback_speed_title)) },
                                    leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null) },
                                    onClick = {
                                        showSpeedSheet = true
                                        showOverflowMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.edit_details)) },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        showEditDetailsDialog = true
                                        showOverflowMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.add_to_collection)) },
                                    leadingIcon = { Icon(Icons.Default.BookmarkBorder, contentDescription = null) },
                                    onClick = {
                                        book?.let { b ->
                                            viewModel.getCollectionsForBook(b.id) { ids ->
                                                currentBookCollectionIds = ids
                                                showAddToCollectionDialog = true
                                            }
                                        }
                                        showOverflowMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.playback_options)) },
                                    leadingIcon = { Icon(Icons.Default.MoreHoriz, contentDescription = null) },
                                    onClick = {
                                        showPlaybackOptionsSheet = true
                                        showOverflowMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            if (isLandscape) {
                // LANDSCAPE LAYOUT: Cover on left, controls on right
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 24.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(0.38f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .pointerInput(playerGesturesEnabled) {
                                    if (playerGesturesEnabled) {
                                        detectTapGestures(
                                            onDoubleTap = { viewModel.togglePlayPause() }
                                        )
                                    }
                                }
                        ) {
                            BookCoverArt(
                                title = book?.displayTitle ?: "Evenfall",
                                author = book?.displayAuthor ?: "",
                                coverPath = book?.displayCoverUri,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier
                            .weight(0.62f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        PlayerContent(
                            book = book,
                            chapter = chapter,
                            state = state,
                            skipSeconds = skipSeconds,
                            compactPlayer = compactPlayer,
                            playerGesturesEnabled = playerGesturesEnabled,
                            bookColors = bookColors,
                            isDark = isDark,
                            hasMultipleChapters = hasMultipleChapters,
                            hasManyChapters = hasManyChapters,
                            totalChapters = totalChapters,
                            currentChapterIdx = currentChapterIdx,
                            showUndoChip = showUndoChip,
                            showCover = false,
                            isLandscape = true,
                            onOpenChapterSheet = { showChapterSheet = true },
                            onSeekToFraction = { fraction ->
                                haptics.chapterSnapped()
                                viewModel.seekToFraction(fraction)
                            },
                            onUndoSeek = {
                                viewModel.undoSeek()
                                showUndoChip = false
                            },
                            onPreviousChapter = viewModel::previousChapter,
                            onSkipBackward = viewModel::skipBackward,
                            onLongSkipBackward = {
                                viewModel.dismissHintLongPressSkip()
                                quickSkipDialogData = false to state.skipBackSeconds
                            },
                            onTogglePlayPause = viewModel::togglePlayPause,
                            onSkipForward = viewModel::skipForward,
                            onLongSkipForward = {
                                viewModel.dismissHintLongPressSkip()
                                quickSkipDialogData = true to state.skipForwardSeconds
                            },
                            onNextChapter = viewModel::nextChapter,
                            onSleepClick = { showSleepSheet = true },
                            onSpeedClick = { showSpeedSheet = true },
                            onBookmarksClick = {
                                viewModel.dismissHintBookmark()
                                book?.let { onOpenBookmarks(it.id) }
                            },
                            onMoreClick = { showPlaybackOptionsSheet = true },
                            onSilenceToggle = viewModel::toggleSilenceSkipping,
                            onVolumeBoostClick = { showVolumeBoostSheet = true },
                            onDismissWelcomeBack = viewModel::dismissWelcomeBack,
                            onRewindToChapterStart = viewModel::rewindToChapterStart,
                            onDismissBedtimePrompt = viewModel::dismissBedtimePrompt,
                            onAcceptBedtimePrompt = viewModel::acceptBedtimePrompt,
                            hintLongPressSkipVisible = !hintLongPressSkipShown,
                            onDismissHintLongPressSkip = viewModel::dismissHintLongPressSkip,
                            hintBookmarkVisible = !hintBookmarkShown && state.chapterPositionMs > 5_000L,
                            onDismissHintBookmark = viewModel::dismissHintBookmark
                        )
                    }
                }
            } else {
                // PORTRAIT LAYOUT
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 24.dp)
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    PlayerContent(
                        book = book,
                        chapter = chapter,
                        state = state,
                        skipSeconds = skipSeconds,
                        compactPlayer = compactPlayer,
                        playerGesturesEnabled = playerGesturesEnabled,
                        bookColors = bookColors,
                        isDark = isDark,
                        hasMultipleChapters = hasMultipleChapters,
                        hasManyChapters = hasManyChapters,
                        totalChapters = totalChapters,
                        currentChapterIdx = currentChapterIdx,
                        showUndoChip = showUndoChip,
                        onOpenChapterSheet = { showChapterSheet = true },
                        onSeekToFraction = { fraction ->
                            haptics.chapterSnapped()
                            viewModel.seekToFraction(fraction)
                        },
                        onUndoSeek = {
                            viewModel.undoSeek()
                            showUndoChip = false
                        },
                        onPreviousChapter = viewModel::previousChapter,
                        onSkipBackward = viewModel::skipBackward,
                        onLongSkipBackward = {
                            viewModel.dismissHintLongPressSkip()
                            quickSkipDialogData = false to state.skipBackSeconds
                        },
                        onTogglePlayPause = viewModel::togglePlayPause,
                        onSkipForward = viewModel::skipForward,
                        onLongSkipForward = {
                            viewModel.dismissHintLongPressSkip()
                            quickSkipDialogData = true to state.skipForwardSeconds
                        },
                        onNextChapter = viewModel::nextChapter,
                        onSleepClick = { showSleepSheet = true },
                        onSpeedClick = { showSpeedSheet = true },
                        onBookmarksClick = {
                            viewModel.dismissHintBookmark()
                            book?.let { onOpenBookmarks(it.id) }
                        },
                        onMoreClick = { showPlaybackOptionsSheet = true },
                        onSilenceToggle = viewModel::toggleSilenceSkipping,
                        onVolumeBoostClick = { showVolumeBoostSheet = true },
                        onDismissWelcomeBack = viewModel::dismissWelcomeBack,
                        onRewindToChapterStart = viewModel::rewindToChapterStart,
                        onDismissBedtimePrompt = viewModel::dismissBedtimePrompt,
                        onAcceptBedtimePrompt = viewModel::acceptBedtimePrompt,
                        hintLongPressSkipVisible = !hintLongPressSkipShown,
                        onDismissHintLongPressSkip = viewModel::dismissHintLongPressSkip,
                        hintBookmarkVisible = !hintBookmarkShown && state.chapterPositionMs > 5_000L,
                        onDismissHintBookmark = viewModel::dismissHintBookmark
                    )
                }
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
                        .navigationBarsPadding()
                ) {
                    Text(
                        text = "Chapters",
                        fontFamily = SerifFont,
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
                                        text = ch.displayTitle,
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

                                IconButton(
                                    onClick = { editingChapter = ch },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = stringResource(R.string.edit_chapter_title),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Skip Amount Adjustment Dialog
        quickSkipDialogData?.let { (isForward, sec) ->
            QuickSkipAdjustDialog(
                isForward = isForward,
                currentSeconds = sec,
                onSecondsChange = { newSec ->
                    quickSkipDialogData = isForward to newSec
                    if (isForward) {
                        viewModel.setBookSkipForwardOverride(newSec)
                    } else {
                        viewModel.setBookSkipBackOverride(newSec)
                    }
                },
                onDismissRequest = { quickSkipDialogData = null }
            )
        }

        // Playback Options Bottom Sheet (Grouped: This book, Voice, Advanced)
        if (showPlaybackOptionsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPlaybackOptionsSheet = false },
                sheetState = rememberModalBottomSheetState()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 24.dp)
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                ) {
                    Text(
                        text = "Playback options",
                        fontFamily = SerifFont,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    // SECTION 1: THIS BOOK
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.this_book_section),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (state.hasCustomPlaybackOverrides) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = if (state.hasCustomPlaybackOverrides) stringResource(R.string.custom_overrides_active) else stringResource(R.string.using_defaults),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (state.hasCustomPlaybackOverrides) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (state.hasCustomPlaybackOverrides) {
                            TextButton(
                                onClick = { viewModel.resetBookPlaybackOverrides() },
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.reset_to_defaults),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Book Speed override
                    val currentSpeedOverride = state.currentBook?.speedOverride
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.book_speed),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (currentSpeedOverride != null) "${String.format(Locale.US, "%.2f", currentSpeedOverride)}x (Custom)" else "${String.format(Locale.US, "%.2f", state.playbackSpeed)}x (Default)",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (currentSpeedOverride != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Slider(
                            value = state.playbackSpeed,
                            onValueChange = { viewModel.setBookSpeedOverride(it) },
                            valueRange = 0.5f..3.0f,
                            steps = 49
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Skip backward & forward overrides
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.book_skip_backward),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(10, 15, 20, 30).forEach { sec ->
                                    val isSelected = state.skipBackSeconds == sec
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.clickable { viewModel.setBookSkipBackOverride(sec) }
                                    ) {
                                        Text(
                                            text = "${sec}s",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.book_skip_forward),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(15, 20, 30, 45).forEach { sec ->
                                    val isSelected = state.skipForwardSeconds == sec
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.clickable { viewModel.setBookSkipForwardOverride(sec) }
                                    ) {
                                        Text(
                                            text = "${sec}s",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Silence skipping override
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.book_silence_skipping),
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Skip silence for this book",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.silenceSkippingEnabled,
                            onCheckedChange = { viewModel.setBookSilenceSkippingOverride(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Volume boost override
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.book_volume_boost),
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Boost level for quiet narration",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val boostPercent = (state.volumeBoostLevel * 100).toInt()
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (boostPercent > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(start = 12.dp)
                            ) {
                                Text(
                                    text = if (boostPercent == 0) "Off" else "+$boostPercent%",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (boostPercent > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Slider(
                            value = state.volumeBoostLevel,
                            onValueChange = { viewModel.setBookVolumeBoostOverride(it) },
                            valueRange = 0f..1.0f
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // SECTION 2: VOICE & CLARITY
                    Text(
                        text = stringResource(R.string.voice_section),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Mono downmix
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.mono_downmix),
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = stringResource(R.string.mono_downmix_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.monoDownmixEnabled,
                            onCheckedChange = { viewModel.setMonoDownmix(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Loudness Normalization
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.loudness_normalization),
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = stringResource(R.string.loudness_normalization_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.loudnessNormalizationEnabled,
                            onCheckedChange = { viewModel.setLoudnessNormalization(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Voice Boost preset (Off / Light / Strong)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.voice_boost),
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stringResource(R.string.voice_boost_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf("Off", "Light", "Strong").forEach { preset ->
                                val isSelected = state.voiceBoostPreset.equals(preset, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.clickable { viewModel.setVoiceBoostPreset(preset) }
                                ) {
                                    Text(
                                        text = when(preset) {
                                            "Light" -> stringResource(R.string.voice_boost_light)
                                            "Strong" -> stringResource(R.string.voice_boost_strong)
                                            else -> stringResource(R.string.voice_boost_off)
                                        },
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // SECTION 3: ADVANCED
                    Text(
                        text = stringResource(R.string.advanced_section),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Sleep timer fade-out and shake-to-extend are active. Double-tap cover art to play/pause.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
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
                        .navigationBarsPadding()
                ) {
                    Text(stringResource(R.string.sleep_timer_title), fontFamily = SerifFont, style = MaterialTheme.typography.titleLarge)
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
                                    haptics.sleepTimerSet()
                                    showSleepSheet = false
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = pluralStringResource(R.plurals.minutes_count, min, min),
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
                                haptics.sleepTimerSet()
                                showSleepSheet = false
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.end_of_chapter),
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
                            Text(stringResource(R.string.turn_off_sleep_timer), color = MaterialTheme.colorScheme.error)
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
                        .padding(24.dp)
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.playback_speed_title), fontFamily = SerifFont, style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${String.format(Locale.getDefault(), "%.2f", state.playbackSpeed)}x",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Slider(
                        value = state.playbackSpeed,
                        onValueChange = viewModel::setPlaybackSpeed,
                        valueRange = 0.5f..3.0f,
                        steps = 49
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

        // Volume Boost Bottom Sheet (for dense mode direct tap)
        if (showVolumeBoostSheet) {
            ModalBottomSheet(
                onDismissRequest = { showVolumeBoostSheet = false },
                sheetState = rememberModalBottomSheetState()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.volume_boost_title), fontFamily = SerifFont, style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    val boostPercent = (state.volumeBoostLevel * 100).toInt()
                    Text(
                        text = if (boostPercent == 0) stringResource(R.string.off) else "+$boostPercent%",
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

        if (showEditDetailsDialog && book != null) {
            EditBookDetailsDialog(
                book = book,
                onDismiss = { showEditDetailsDialog = false },
                onSave = { title, author, narrator, seriesName, seriesOrder, coverUri ->
                    viewModel.updateMetadataOverrides(title, author, narrator, seriesName, seriesOrder, coverUri)
                },
                onResetToOriginal = {
                    viewModel.resetMetadataOverrides()
                }
            )
        }

        if (editingChapter != null) {
            EditChapterTitleDialog(
                currentTitle = editingChapter!!.displayTitle,
                onDismiss = { editingChapter = null },
                onConfirm = { newTitle ->
                    viewModel.updateChapterTitle(editingChapter!!.id, newTitle)
                    editingChapter = null
                }
            )
        }

        if (showAddToCollectionDialog && book != null) {
            AddToCollectionDialog(
                book = book,
                allCollections = allCollections,
                currentCollectionIds = currentBookCollectionIds,
                onSaveCollections = { bookId, selectedIds ->
                    viewModel.setCollectionsForBook(bookId, selectedIds)
                    showAddToCollectionDialog = false
                },
                onCreateCollection = { name ->
                    viewModel.createCollection(name)
                },
                onDismiss = { showAddToCollectionDialog = false }
            )
        }

        if (state.isPlaybackError) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissPlaybackError() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = {
                    Text(
                        text = stringResource(R.string.error_playback_title),
                        fontFamily = SerifFont,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = state.playbackErrorMessage ?: stringResource(R.string.error_playback_file_missing_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.retryPlayback() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(stringResource(R.string.error_playback_retry))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissPlaybackError() }) {
                        Text(stringResource(R.string.error_playback_dismiss))
                    }
                }
            )
        }
    }
}

@Composable
private fun PlayerContent(
    book: com.evenfall.app.data.db.entity.BookEntity?,
    chapter: com.evenfall.app.data.db.entity.ChapterEntity?,
    state: PlaybackState,
    skipSeconds: Int,
    compactPlayer: Boolean,
    playerGesturesEnabled: Boolean = true,
    bookColors: PlayerBookColors,
    isDark: Boolean,
    hasMultipleChapters: Boolean,
    hasManyChapters: Boolean,
    totalChapters: Int,
    currentChapterIdx: Int,
    showUndoChip: Boolean,
    showCover: Boolean = true,
    isLandscape: Boolean = false,
    onOpenChapterSheet: () -> Unit,
    onSeekToFraction: (Float) -> Unit,
    onUndoSeek: () -> Unit,
    onPreviousChapter: () -> Unit,
    onSkipBackward: () -> Unit,
    onLongSkipBackward: () -> Unit = {},
    onTogglePlayPause: () -> Unit,
    onSkipForward: () -> Unit,
    onLongSkipForward: () -> Unit = {},
    onNextChapter: () -> Unit,
    onSleepClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onBookmarksClick: () -> Unit,
    onMoreClick: () -> Unit,
    onSilenceToggle: () -> Unit,
    onVolumeBoostClick: () -> Unit,
    onDismissWelcomeBack: () -> Unit = {},
    onRewindToChapterStart: () -> Unit = {},
    onDismissBedtimePrompt: () -> Unit = {},
    onAcceptBedtimePrompt: () -> Unit = {},
    hintLongPressSkipVisible: Boolean = false,
    onDismissHintLongPressSkip: () -> Unit = {},
    hintBookmarkVisible: Boolean = false,
    onDismissHintBookmark: () -> Unit = {}
) {
    if (showCover) {
        // 1. Cover Art with double-tap gesture to play/pause
        Box(
            modifier = Modifier
                .size(if (compactPlayer) 205.dp else 240.dp)
                .pointerInput(playerGesturesEnabled) {
                    if (playerGesturesEnabled) {
                        detectTapGestures(
                            onDoubleTap = { onTogglePlayPause() }
                        )
                    }
                }
        ) {
            BookCoverArt(
                title = book?.displayTitle ?: "Evenfall",
                author = book?.displayAuthor ?: "",
                coverPath = book?.displayCoverUri,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(if (compactPlayer) 16.dp else 12.dp))
    }

    if (state.showWelcomeBackCard) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = bookColors.cardBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.welcome_back_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = bookColors.onBackground
                    )
                    state.welcomeBackChapterTitle?.let { chapterTitle ->
                        Text(
                            text = stringResource(R.string.welcome_back_subtitle, chapterTitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = bookColors.onBackgroundMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = onRewindToChapterStart,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.rewind_to_chapter_start),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = onDismissWelcomeBack) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.dismiss),
                        tint = bookColors.onBackgroundMuted
                    )
                }
            }
        }
    }

    if (state.showBedtimePrompt) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.bedtime_prompt_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    val duration = state.bedtimePromptDurationMinutes
                    val durationLabel = if (duration == -1) "end of chapter" else "$duration min"
                    Text(
                        text = "It's bedtime. Would you like to start a $durationLabel sleep timer?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row {
                        Button(
                            onClick = onAcceptBedtimePrompt,
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(stringResource(R.string.start_timer), style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = onDismissBedtimePrompt,
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(stringResource(R.string.dismiss), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }

    // 2. Title and Author: serif font, one line ellipsized, smaller size
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = book?.displayTitle ?: "Untitled",
            fontFamily = SerifFont,
            fontSize = if (isLandscape) 17.sp else (if (compactPlayer) 20.sp else 23.sp),
            fontWeight = FontWeight.Bold,
            color = bookColors.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = book?.displayAuthor ?: "Unknown Author",
            fontFamily = SerifFont,
            fontSize = if (isLandscape) 12.sp else 14.sp,
            color = bookColors.onBackgroundMuted,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        )
    }

    Spacer(modifier = Modifier.height(if (isLandscape) 2.dp else 8.dp))

    // 3. Chapter Line / Pill
    if (compactPlayer) {
        // Compact: single tappable text button "Chapter X of N ▾", hidden when N <= 1
        if (hasMultipleChapters) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .defaultMinSize(minHeight = if (isLandscape) 36.dp else 48.dp)
                    .clickable(
                        onClickLabel = "Select chapter",
                        onClick = onOpenChapterSheet
                    )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = if (isLandscape) 2.dp else 6.dp)
                ) {
                    Text(
                        text = "Chapter ${currentChapterIdx + 1} of $totalChapters ▾",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = bookColors.onBackground
                    )
                    val chTitle = chapter?.displayTitle
                    if (!chTitle.isNullOrBlank() && !chTitle.equals("Chapter ${currentChapterIdx + 1}", ignoreCase = true)) {
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = chTitle,
                            fontSize = 11.sp,
                            color = bookColors.onBackgroundMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    } else {
        // Denser original layout: Big chapter pill with prev/next side buttons
        if (hasManyChapters) {
            DashedSegmentedBar(
                totalSegments = totalChapters,
                currentSegment = currentChapterIdx,
                progressWithinCurrent = state.chapterProgressFraction,
                activeColor = bookColors.activeSliderColor,
                inactiveColor = bookColors.inactiveSliderColor,
                barHeight = 5.dp,
                gapWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${state.totalProgressPercent}% of book",
                    fontSize = 12.sp,
                    color = bookColors.onBackgroundMuted
                )
                Text(
                    text = TimeUtils.formatTimeLeft(state.totalTimeRemainingMs),
                    fontSize = 12.sp,
                    color = bookColors.onBackgroundMuted
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

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
                    onClick = onPreviousChapter,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = stringResource(R.string.cd_previous_chapter),
                        tint = bookColors.onBackground
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenChapterSheet),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Chapter ${currentChapterIdx + 1} of $totalChapters",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = bookColors.onBackgroundMuted
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                    onClick = onNextChapter,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = stringResource(R.string.cd_next_chapter),
                        tint = bookColors.onBackground
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(if (isLandscape) 4.dp else 10.dp))

    // 4. Temporary "Back to mm:ss" undo chip positioned ABOVE seek slider (centered)
    AnimatedVisibility(
        visible = showUndoChip && state.undoSeekPositionMs != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = bookColors.undoChipBackground,
            modifier = Modifier
                .defaultMinSize(minHeight = 44.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(
                    onClickLabel = stringResource(R.string.cd_undo_seek),
                    onClick = onUndoSeek
                )
                .padding(bottom = 6.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = stringResource(R.string.cd_undo_seek),
                    tint = bookColors.undoChipContent,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Back to ${TimeUtils.formatTimestamp(state.undoSeekPositionMs ?: 0L)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = bookColors.undoChipContent
                )
            }
        }
    }

    // 5. Seek Slider (wavy while playing, flat when paused)
    Column(modifier = Modifier.fillMaxWidth()) {
        WavyProgressLine(
            progress = state.chapterProgressFraction,
            onSeek = onSeekToFraction,
            isPlaying = state.isPlaying,
            activeColor = bookColors.activeSliderColor,
            inactiveColor = bookColors.inactiveSliderColor,
            thumbColor = bookColors.activeSliderColor
        )

        // Elapsed time and remaining time on sides
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = TimeUtils.formatTimestamp(state.chapterPositionMs),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = bookColors.onBackgroundMuted
            )
            Text(
                text = "-${TimeUtils.formatTimestamp(state.chapterTimeRemainingMs)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = bookColors.onBackgroundMuted
            )
        }

        // Book progress: one small muted line centered under the slider (Compact mode)
        if (compactPlayer) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${state.totalProgressPercent}% · ${TimeUtils.formatTimeLeft(state.totalTimeRemainingMs)}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = bookColors.onBackgroundMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    Spacer(modifier = Modifier.height(if (isLandscape) 4.dp else 18.dp))

    FirstRunHintCard(
        text = stringResource(R.string.hint_long_press_skip),
        visible = hintLongPressSkipVisible,
        onDismiss = onDismissHintLongPressSkip,
        containerColor = bookColors.cardBackground,
        contentColor = bookColors.onBackground,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
    )

    // 6. Transport Controls: Prev Chapter, Skip Back, Play/Pause, Skip Forward, Next Chapter
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous Chapter (hidden if single chapter)
        if (hasMultipleChapters) {
            IconButton(
                onClick = onPreviousChapter,
                modifier = Modifier.size(if (isLandscape) 36.dp else 48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = stringResource(R.string.cd_previous_chapter),
                    tint = bookColors.onBackgroundMuted,
                    modifier = Modifier.size(if (isLandscape) 20.dp else 26.dp)
                )
            }
            Spacer(modifier = Modifier.width(if (isLandscape) 6.dp else 10.dp))
        }

        // Skip Backward (tonal / muted style)
        CircularSkipButton(
            seconds = state.skipBackSeconds,
            isForward = false,
            onClick = onSkipBackward,
            onLongClick = onLongSkipBackward,
            size = if (isLandscape) 42.dp else 52.dp,
            containerColor = bookColors.cardBackground,
            contentColor = bookColors.onBackground
        )

        Spacer(modifier = Modifier.width(if (isLandscape) 10.dp else 20.dp))

        // Play/Pause (only strong accent color)
        MorphingPlayPauseButton(
            isPlaying = state.isPlaying,
            onClick = onTogglePlayPause,
            size = if (isLandscape) 56.dp else 80.dp,
            iconSize = if (isLandscape) 26.dp else 38.dp,
            containerColor = bookColors.playButtonContainer,
            contentColor = bookColors.playButtonContent
        )

        Spacer(modifier = Modifier.width(if (isLandscape) 10.dp else 20.dp))

        // Skip Forward (tonal / muted style)
        CircularSkipButton(
            seconds = state.skipForwardSeconds,
            isForward = true,
            onClick = onSkipForward,
            onLongClick = onLongSkipForward,
            size = if (isLandscape) 42.dp else 52.dp,
            containerColor = bookColors.cardBackground,
            contentColor = bookColors.onBackground
        )

        // Next Chapter (hidden if single chapter)
        if (hasMultipleChapters) {
            Spacer(modifier = Modifier.width(if (isLandscape) 6.dp else 10.dp))
            IconButton(
                onClick = onNextChapter,
                modifier = Modifier.size(if (isLandscape) 36.dp else 48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = stringResource(R.string.cd_next_chapter),
                    tint = bookColors.onBackgroundMuted,
                    modifier = Modifier.size(if (isLandscape) 20.dp else 26.dp)
                )
            }
        }
    }

    // Spacing between controls and toolbar (at least 24dp in portrait, compact in landscape)
    Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 28.dp))

    FirstRunHintCard(
        text = stringResource(R.string.hint_bookmark),
        visible = hintBookmarkVisible,
        onDismiss = onDismissHintBookmark,
        containerColor = bookColors.cardBackground,
        contentColor = bookColors.onBackground,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
    )

    // 7. Bottom Toolbar
    if (compactPlayer) {
        // Compact: Low-contrast tonal container with 4 actions (Sleep, Speed, Bookmarks, More)
        CompactBottomToolbar(
            state = state,
            bookColors = bookColors,
            isDark = isDark,
            isLandscape = isLandscape,
            onSleepClick = onSleepClick,
            onSpeedClick = onSpeedClick,
            onBookmarksClick = onBookmarksClick,
            onMoreClick = onMoreClick
        )
    } else {
        // Dense original: 5-action toolbar
        DenseBottomToolbar(
            state = state,
            bookColors = bookColors,
            onSleepClick = onSleepClick,
            onSpeedClick = onSpeedClick,
            onBookmarksClick = onBookmarksClick,
            onSilenceToggle = onSilenceToggle,
            onVolumeBoostClick = onVolumeBoostClick
        )
    }

    Spacer(modifier = Modifier.height(if (isLandscape) 4.dp else 10.dp))
}

@Composable
private fun CompactBottomToolbar(
    state: PlaybackState,
    bookColors: PlayerBookColors,
    isDark: Boolean,
    isLandscape: Boolean = false,
    onSleepClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onBookmarksClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val toolbarBg = bookColors.cardBackground
    val hasActiveOptions = state.silenceSkippingEnabled || state.volumeBoostLevel > 0f

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = toolbarBg,
        modifier = modifier
            .fillMaxWidth()
            .height(if (isLandscape) 48.dp else 56.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Sleep Timer (live countdown chip e.g. "29:57" when active)
            if (state.sleepTimerIsActive && state.sleepTimerRemainingMs != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = bookColors.playButtonContainer.copy(alpha = 0.2f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(
                            onClickLabel = "Sleep timer active",
                            onClick = onSleepClick
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = stringResource(R.string.cd_sleep_timer_active),
                            tint = bookColors.onBackground,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = TimeUtils.formatTimestamp(state.sleepTimerRemainingMs),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = bookColors.onBackground
                        )
                    }
                }
            } else {
                IconButton(onClick = onSleepClick) {
                    Icon(
                        imageVector = Icons.Default.Bedtime,
                        contentDescription = stringResource(R.string.cd_sleep_timer),
                        tint = bookColors.onBackground
                    )
                }
            }

            // 2. Playback speed (shows current value e.g. "1.0x")
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(
                        onClickLabel = stringResource(R.string.playback_speed_title),
                        onClick = onSpeedClick
                    )
            ) {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${String.format(Locale.getDefault(), "%.1f", state.playbackSpeed)}x",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = bookColors.onBackground
                    )
                }
            }

            // 3. Bookmarks
            IconButton(onClick = onBookmarksClick) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = stringResource(R.string.bookmarks_and_history),
                    tint = bookColors.onBackground
                )
            }

            // 4. More (opens Playback options sheet, shows dot indicator if active)
            Box(contentAlignment = Alignment.Center) {
                IconButton(onClick = onMoreClick) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = stringResource(R.string.playback_options),
                        tint = bookColors.onBackground
                    )
                }
                if (hasActiveOptions) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 10.dp, end = 10.dp)
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(bookColors.playButtonContainer)
                    )
                }
            }
        }
    }
}

@Composable
private fun DenseBottomToolbar(
    state: PlaybackState,
    bookColors: PlayerBookColors,
    onSleepClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onBookmarksClick: () -> Unit,
    onSilenceToggle: () -> Unit,
    onVolumeBoostClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = bookColors.bottomToolbarContainer,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onSleepClick) {
                Icon(
                    imageVector = Icons.Default.Bedtime,
                    contentDescription = stringResource(R.string.cd_sleep_timer),
                    tint = bookColors.bottomToolbarContent
                )
            }
            IconButton(onClick = onSpeedClick) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = stringResource(R.string.cd_playback_speed_format, state.playbackSpeed),
                    tint = bookColors.bottomToolbarContent
                )
            }
            IconButton(onClick = onBookmarksClick) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = stringResource(R.string.bookmarks_and_history),
                    tint = bookColors.bottomToolbarContent
                )
            }
            IconButton(onClick = onSilenceToggle) {
                Icon(
                    imageVector = Icons.Default.ContentCut,
                    contentDescription = stringResource(R.string.cd_silence_skipping),
                    tint = if (state.silenceSkippingEnabled) bookColors.bottomToolbarContent else bookColors.bottomToolbarContent.copy(alpha = 0.5f)
                )
            }
            IconButton(onClick = onVolumeBoostClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = stringResource(R.string.cd_volume_boost),
                    tint = if (state.volumeBoostLevel > 0f) bookColors.bottomToolbarContent else bookColors.bottomToolbarContent.copy(alpha = 0.7f)
                )
            }
        }
    }
}
