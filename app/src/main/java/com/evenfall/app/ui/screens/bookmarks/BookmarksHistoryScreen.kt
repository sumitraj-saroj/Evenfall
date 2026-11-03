package com.evenfall.app.ui.screens.bookmarks

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.evenfall.app.ui.components.EmptyStateView
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.evenfall.app.R
import com.evenfall.app.data.db.entity.BookmarkEntity
import com.evenfall.app.data.db.entity.BookmarkType
import com.evenfall.app.data.db.entity.HistoryEntity
import com.evenfall.app.data.db.entity.HistoryEventType
import com.evenfall.app.ui.components.SparkleDecorations
import com.evenfall.app.ui.theme.CookieShape
import com.evenfall.app.ui.theme.SerifFont
import com.evenfall.app.util.BookmarkExporter
import com.evenfall.app.util.ExportFormat
import com.evenfall.app.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksHistoryScreen(
    onBack: () -> Unit,
    viewModel: BookmarksHistoryViewModel = hiltViewModel(),
    hasMiniPlayer: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsState()
    val book = uiState.book
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    var showSortMenu by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var selectedExportFormat by remember { mutableStateOf(ExportFormat.MARKDOWN) }

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/*")
    ) { uri: Uri? ->
        if (uri != null) {
            val content = viewModel.exportBookmarks(selectedExportFormat)
            BookmarkExporter.writeToUri(context, uri, content)
        }
    }

    // Calm gradient background matching Images 4 & 5
    val backgroundBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F1E36),
                Color(0xFF091222),
                Color(0xFF060B14)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFC7DEFA),
                Color(0xFFE2E7FD),
                Color(0xFFEDE9F8)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        SparkleDecorations(tint = if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFF1E3A8A).copy(alpha = 0.15f))

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = book?.title ?: "Echoes of Tomorrow",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = SerifFont,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = book?.author ?: "Daniel Hartwell",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showExportDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(R.string.export_bookmarks)
                            )
                        }
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Sort,
                                    contentDescription = stringResource(R.string.cd_sort_bookmarks)
                                )
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.filter_position)) },
                                    onClick = {
                                        viewModel.setSortOrder(BookmarkSortOrder.POSITION)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.filter_newest_first)) },
                                    onClick = {
                                        viewModel.setSortOrder(BookmarkSortOrder.NEWEST)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.filter_oldest_first)) },
                                    onClick = {
                                        viewModel.setSortOrder(BookmarkSortOrder.OLDEST)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.title_label)) },
                                    onClick = {
                                        viewModel.setSortOrder(BookmarkSortOrder.TITLE)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    modifier = Modifier.statusBarsPadding()
                )
            },
            floatingActionButton = {
                if (uiState.currentTab == BookmarksTab.BOOKMARKS) {
                    ExtendedFloatingActionButton(
                        onClick = { showSaveDialog = true },
                        icon = { Icon(Icons.Default.BookmarkAdd, contentDescription = null) },
                        text = { Text(stringResource(R.string.save_this_moment), fontWeight = FontWeight.SemiBold) },
                        containerColor = Color(0xFF0284C7),
                        contentColor = Color.White,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.navigationBarsPadding()
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding()
            ) {
                // Segmented Tabs: "Bookmarks N" and "History" (matching Images 4 & 5)
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = if (isDark) Color(0xFF162544).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.65f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                    ) {
                        val isBookmarks = uiState.currentTab == BookmarksTab.BOOKMARKS
                        // Bookmarks Tab
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = if (isBookmarks) Color(0xFF0F4C81) else Color.Transparent,
                            contentColor = if (isBookmarks) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { viewModel.selectTab(BookmarksTab.BOOKMARKS) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Bookmarks ${uiState.bookmarkCount}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // History Tab
                        val isHistory = uiState.currentTab == BookmarksTab.HISTORY
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = if (isHistory) Color(0xFF0F4C81) else Color.Transparent,
                            contentColor = if (isHistory) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { viewModel.selectTab(BookmarksTab.HISTORY) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "History",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.currentTab == BookmarksTab.BOOKMARKS) {
                    // Mini Timeline with markers above segments (matching Image 4)
                    val chapters = uiState.chapters
                    val totalChapters = chapters.size.coerceAtLeast(1)
                    val curIdx = uiState.currentChapterIndex
                    val curCh = chapters.getOrNull(curIdx)
                    val chapterProg = if (curCh != null && curCh.durationMs > 0) {
                        uiState.currentPositionMs.toFloat() / curCh.durationMs
                    } else 0f

                    BookmarksTimeline(
                        totalSegments = totalChapters,
                        currentSegment = curIdx,
                        progressWithinCurrent = chapterProg,
                        bookmarks = uiState.bookmarks,
                        totalDurationMs = book?.totalDurationMs ?: 1L,
                        currentPositionMs = book?.currentPositionMs ?: 0L
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Filter chips: All, Notes, Favorites, Quotes (matching Image 4)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = uiState.selectedFilter == BookmarkType.ALL,
                                onClick = { viewModel.selectFilter(BookmarkType.ALL) },
                                label = { Text(stringResource(R.string.filter_all_count, uiState.bookmarkCount)) },
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                        item {
                            FilterChip(
                                selected = uiState.selectedFilter == BookmarkType.NOTE,
                                onClick = { viewModel.selectFilter(BookmarkType.NOTE) },
                                label = { Text(stringResource(R.string.filter_notes_count, uiState.notesCount)) },
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                        item {
                            FilterChip(
                                selected = uiState.selectedFilter == BookmarkType.FAVORITE,
                                onClick = { viewModel.selectFilter(BookmarkType.FAVORITE) },
                                label = { Text(stringResource(R.string.filter_favorites_count, uiState.favoritesCount)) },
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                        item {
                            FilterChip(
                                selected = uiState.selectedFilter == BookmarkType.QUOTE,
                                onClick = { viewModel.selectFilter(BookmarkType.QUOTE) },
                                label = { Text(stringResource(R.string.filter_quotes_count, uiState.quotesCount)) },
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (uiState.bookmarks.isEmpty()) {
                        EmptyStateView(
                            illustrationRes = R.drawable.ic_empty_bookmarks,
                            title = stringResource(R.string.empty_bookmarks_title),
                            message = stringResource(R.string.empty_bookmarks_desc),
                            actionLabel = stringResource(R.string.save_this_moment),
                            onAction = { showSaveDialog = true },
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    } else {
                        // Group bookmarks by chapter
                        val bookmarkChapters = uiState.bookmarks.groupBy { it.chapterIndex }
                        val allDisplayChapterIndices = (bookmarkChapters.keys + curIdx).sorted()

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = if (hasMiniPlayer) 100.dp else 90.dp)
                        ) {
                            allDisplayChapterIndices.forEach { chapterIdx ->
                                val bList = bookmarkChapters[chapterIdx] ?: emptyList()
                                val chapterName = bList.firstOrNull()?.chapterTitle
                                    ?: uiState.chapters.getOrNull(chapterIdx)?.title
                                    ?: "Chapter ${chapterIdx + 1}"

                                item {
                                    Text(
                                        text = "Chapter ${chapterIdx + 1} · $chapterName",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (isDark) Color(0xFF90CAF9) else Color(0xFF1565C0),
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                                    )
                                }

                                // Show "You are here" card if current position is in this chapter
                                if (chapterIdx == curIdx) {
                                    item {
                                        YouAreHereCard(
                                            positionMs = uiState.currentPositionMs,
                                            progressPercent = if (book != null && book.totalDurationMs > 0) {
                                                ((book.currentPositionMs.toFloat() / book.totalDurationMs) * 100).toInt()
                                            } else 0
                                        )
                                    }
                                }

                                items(bList) { bookmark ->
                                    BookmarkRowCard(
                                        bookmark = bookmark,
                                        isDark = isDark,
                                        onClick = { viewModel.jumpToBookmark(bookmark) },
                                        onDelete = { viewModel.deleteBookmark(bookmark) }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // History Tab (matching Image 5)
                    if (uiState.historyEvents.isEmpty()) {
                        EmptyStateView(
                            illustrationRes = R.drawable.ic_empty_history,
                            title = stringResource(R.string.empty_history_title),
                            message = stringResource(R.string.empty_history_desc),
                            modifier = Modifier.padding(top = 28.dp)
                        )
                    } else {
                        val groupedHistory = uiState.historyEvents.groupBy { TimeUtils.formatHistoryTimeGroup(it.timestamp) }
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            contentPadding = PaddingValues(bottom = if (hasMiniPlayer) 100.dp else 40.dp)
                        ) {
                            groupedHistory.forEach { (timeGroup, events) ->
                                item {
                                    Text(
                                        text = timeGroup,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isDark) Color(0xFF90CAF9) else Color(0xFF1565C0),
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                                    )
                                }

                                item {
                                    Surface(
                                        shape = RoundedCornerShape(24.dp),
                                        color = if (isDark) Color(0xFF162544).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.9f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            events.forEachIndexed { idx, event ->
                                                HistoryEventRow(
                                                    event = event,
                                                    isDark = isDark,
                                                    isLast = idx == events.size - 1,
                                                    onJumpToPosition = { viewModel.jumpToHistory(event) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Save This Moment Dialog
        if (showSaveDialog) {
            var momentTitle by remember { mutableStateOf("") }
            var momentNote by remember { mutableStateOf("") }
            var momentType by remember { mutableStateOf(BookmarkType.BOOKMARK) }

            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = { Text(stringResource(R.string.save_this_moment), fontFamily = SerifFont) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = momentTitle,
                            onValueChange = { momentTitle = it },
                            label = { Text(stringResource(R.string.title_label)) },
                            placeholder = { Text(stringResource(R.string.placeholder_bookmark_title)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = momentNote,
                            onValueChange = { momentNote = it },
                            label = { Text(stringResource(R.string.note_optional_label)) },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(stringResource(R.string.type_label), style = MaterialTheme.typography.titleSmall)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                BookmarkType.BOOKMARK to stringResource(R.string.bookmark_type_bookmark),
                                BookmarkType.FAVORITE to stringResource(R.string.bookmark_type_favorite),
                                BookmarkType.QUOTE to stringResource(R.string.bookmark_type_quote),
                                BookmarkType.NOTE to stringResource(R.string.bookmark_type_note)
                            ).forEach { (t, label) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { momentType = t }
                                ) {
                                    RadioButton(selected = momentType == t, onClick = { momentType = t })
                                    Text(label, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.saveCurrentMoment(momentTitle, momentNote.ifBlank { null }, momentType)
                            showSaveDialog = false
                        }
                    ) {
                        Text(stringResource(R.string.save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        // Export Bookmarks Dialog
        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = {
                    Text(
                        stringResource(R.string.export_bookmarks),
                        fontFamily = SerifFont,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Choose export format:",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedExportFormat = ExportFormat.MARKDOWN }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedExportFormat == ExportFormat.MARKDOWN,
                                onClick = { selectedExportFormat = ExportFormat.MARKDOWN }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.export_format_markdown), fontSize = 14.sp)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedExportFormat = ExportFormat.TEXT }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedExportFormat == ExportFormat.TEXT,
                                onClick = { selectedExportFormat = ExportFormat.TEXT }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.export_format_text), fontSize = 14.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val content = viewModel.exportBookmarks(selectedExportFormat)
                            val title = "${book?.displayTitle ?: "Bookmarks"} - Evenfall"
                            BookmarkExporter.shareExport(context, content, title)
                            showExportDialog = false
                        }
                    ) {
                        Text(stringResource(R.string.share_export))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            val ext = if (selectedExportFormat == ExportFormat.MARKDOWN) "md" else "txt"
                            val safeTitle = (book?.displayTitle ?: "bookmarks").replace(Regex("[^a-zA-Z0-9.-]"), "_")
                            saveFileLauncher.launch("$safeTitle-bookmarks.$ext")
                            showExportDialog = false
                        }
                    ) {
                        Text(stringResource(R.string.save_export))
                    }
                }
            )
        }
    }
}

@Composable
fun BookmarkRowCard(
    bookmark: BookmarkEntity,
    isDark: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isDark) Color(0xFF162544).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.95f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Badge matching screenshot
            when (bookmark.type) {
                BookmarkType.FAVORITE -> {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE1BEE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color(0xFF7B1FA2),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                BookmarkType.QUOTE -> {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE0F2FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "99",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7),
                            fontFamily = SerifFont
                        )
                    }
                }
                BookmarkType.NOTE -> {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CookieShape(lobes = 10, depth = 0.16f))
                            .background(Color(0xFF0D9488)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF60A5FA)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bookmark.title,
                    fontFamily = SerifFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val timeStr = TimeUtils.formatTimestamp(bookmark.positionMs)
                val percent = (bookmark.bookProgressFraction * 100).toInt()
                Text(
                    text = "$timeStr · $percent%",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = TimeUtils.formatRelativeDate(bookmark.timestamp),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun YouAreHereCard(
    positionMs: Long,
    progressPercent: Int
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF0284C7),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CookieShape(lobes = 10, depth = 0.16f))
                    .background(Color(0xFF0369A1)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = "You are here",
                    fontFamily = SerifFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${TimeUtils.formatTimestamp(positionMs)} · $progressPercent%",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
fun HistoryEventRow(
    event: HistoryEntity,
    isDark: Boolean,
    isLast: Boolean,
    onJumpToPosition: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // Timestamp text (e.g. 8:15 AM)
        Text(
            text = TimeUtils.formatTimeOfDay(event.timestamp),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(62.dp)
        )

        // Vertical timeline line + node icon
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(28.dp)
        ) {
            when (event.eventType) {
                HistoryEventType.PLAYED -> {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0284C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                HistoryEventType.PAUSED -> {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF94A3B8)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                HistoryEventType.INTERRUPTED -> {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF64748B))
                    )
                }
                HistoryEventType.SLEEP_TIMER_ON -> {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFCE93D8)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = null,
                            tint = Color(0xFF4A148C),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                HistoryEventType.SLEEP_TIMER_ENDED -> {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE1BEE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Nightlight,
                            contentDescription = null,
                            tint = Color(0xFF6A1B9A),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                HistoryEventType.SHOOK_TO_EXTEND -> {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFCE93D8)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ScreenRotation,
                            contentDescription = null,
                            tint = Color(0xFF4A148C),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(38.dp)
                        .background(if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1))
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Event description & Jump Position Chip
        Column(modifier = Modifier.weight(1f)) {
            val label = when (event.eventType) {
                HistoryEventType.PLAYED -> if (event.outputDevice != null) "Played · ${event.outputDevice}" else "Played"
                HistoryEventType.PAUSED -> if (event.outputDevice != null) "Paused · ${event.outputDevice}" else "Paused"
                HistoryEventType.INTERRUPTED -> "Interrupted for ${(event.durationInterruptedMs / 60000).coerceAtLeast(1)} min"
                HistoryEventType.SLEEP_TIMER_ON -> "Sleep timer on"
                HistoryEventType.SLEEP_TIMER_ENDED -> "Sleep timer ended"
                HistoryEventType.SHOOK_TO_EXTEND -> "Shook to keep listening"
            }
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Clickable Position Chip (e.g. "Ch. 12 · 34:18")
            if (event.positionMs > 0 || event.chapterIndex > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF1E3A5F) else Color(0xFFD0E1FD),
                    modifier = Modifier.clickable(onClick = onJumpToPosition)
                ) {
                    Text(
                        text = "Ch. ${event.chapterIndex + 1} · ${TimeUtils.formatTimestamp(event.positionMs)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFBFD8F8) else Color(0xFF0F3E76),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
