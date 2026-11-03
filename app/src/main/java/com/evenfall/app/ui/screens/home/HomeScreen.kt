package com.evenfall.app.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.evenfall.app.R
import com.evenfall.app.data.db.entity.BookEntity
import com.evenfall.app.ui.components.BookCoverArt
import com.evenfall.app.ui.components.EditBookDetailsDialog
import com.evenfall.app.ui.components.SparkleDecorations
import com.evenfall.app.ui.theme.CookieShape
import com.evenfall.app.ui.theme.SerifFont
import com.evenfall.app.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenBook: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAddFolder: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    hasMiniPlayer: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }
    var bookToEdit by remember { mutableStateOf<BookEntity?>(null) }
    var bookToResetPositionPrompt by remember { mutableStateOf<BookEntity?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        SparkleDecorations()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                // Top Search Bar with status bar padding
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            TextField(
                                value = uiState.searchQuery,
                                onValueChange = viewModel::onSearchQueryChanged,
                                placeholder = {
                                    Text(
                                        "Search your library",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        fontSize = 15.sp
                                    )
                                },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    bottom = if (hasMiniPlayer) 88.dp else 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Greeting Header (in Serif font matching Image 1)
                item {
                    Column(modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)) {
                        Text(
                            text = uiState.greeting,
                            fontFamily = SerifFont,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val inProgressCount = (if (uiState.heroBook != null) 1 else 0) + uiState.currentBooks.size
                        Text(
                            text = "$inProgressCount books in progress",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Hero Card: Continue Listening (matching Image 1)
                uiState.heroBook?.let { hero ->
                    item {
                        ContinueListeningHeroCard(
                            book = hero,
                            onClick = { onOpenBook(hero.id) },
                            onEditDetails = { bookToEdit = hero },
                            onMarkFinished = { viewModel.markAsFinished(hero.id) }
                        )
                    }
                }

                // "Current" Section with Badge Count (matching Image 1)
                if (uiState.currentBooks.isNotEmpty()) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                        ) {
                            Text(
                                text = "Current",
                                fontFamily = SerifFont,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${uiState.currentBooks.size}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // Sort dropdown
                            Box {
                                IconButton(onClick = { showSortMenu = true }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.Sort,
                                        contentDescription = "Sort library",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Recently played") },
                                        onClick = {
                                            viewModel.onSortOptionChanged(SortOption.RECENTLY_PLAYED)
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Title") },
                                        onClick = {
                                            viewModel.onSortOptionChanged(SortOption.TITLE)
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Author") },
                                        onClick = {
                                            viewModel.onSortOptionChanged(SortOption.AUTHOR)
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Duration") },
                                        onClick = {
                                            viewModel.onSortOptionChanged(SortOption.DURATION)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                uiState.currentBooks.forEachIndexed { index, book ->
                                    CurrentBookRow(
                                        book = book,
                                        onClick = { onOpenBook(book.id) },
                                        onEditDetails = { bookToEdit = book },
                                        onMarkFinished = { viewModel.markAsFinished(book.id) },
                                        onMarkUnfinished = { bookToResetPositionPrompt = book }
                                    )
                                    if (index < uiState.currentBooks.size - 1) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // "Not started" Section
                if (uiState.notStartedBooks.isNotEmpty()) {
                    item {
                        Text(
                            text = "Not started",
                            fontFamily = SerifFont,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                    items(uiState.notStartedBooks) { book ->
                        CurrentBookRow(
                            book = book,
                            onClick = { onOpenBook(book.id) },
                            onEditDetails = { bookToEdit = book },
                            onMarkFinished = { viewModel.markAsFinished(book.id) },
                            onMarkUnfinished = { bookToResetPositionPrompt = book }
                        )
                    }
                }

                // "Finished" Section
                if (uiState.finishedBooks.isNotEmpty()) {
                    item {
                        Text(
                            text = "Finished",
                            fontFamily = SerifFont,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                    items(uiState.finishedBooks) { book ->
                        CurrentBookRow(
                            book = book,
                            onClick = { onOpenBook(book.id) },
                            onEditDetails = { bookToEdit = book },
                            onMarkFinished = { viewModel.markAsFinished(book.id) },
                            onMarkUnfinished = { bookToResetPositionPrompt = book }
                        )
                    }
                }

                // Add Folder Banner
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenAddFolder)
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Add audiobooks folder",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Evenfall will scan and organize books automatically",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        if (bookToEdit != null) {
            EditBookDetailsDialog(
                book = bookToEdit!!,
                onDismiss = { bookToEdit = null },
                onSave = { title, author, narrator, seriesName, seriesOrder, coverUri ->
                    viewModel.updateMetadataOverrides(
                        bookToEdit!!.id,
                        title,
                        author,
                        narrator,
                        seriesName,
                        seriesOrder,
                        coverUri
                    )
                    bookToEdit = null
                },
                onResetToOriginal = {
                    viewModel.resetMetadataOverrides(bookToEdit!!.id)
                    bookToEdit = null
                }
            )
        }

        if (bookToResetPositionPrompt != null) {
            AlertDialog(
                onDismissRequest = { bookToResetPositionPrompt = null },
                title = { Text(stringResource(R.string.mark_as_not_started)) },
                text = { Text(stringResource(R.string.reset_playback_position_prompt)) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.markAsUnfinished(bookToResetPositionPrompt!!.id, resetPosition = true)
                            bookToResetPositionPrompt = null
                        },
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(stringResource(R.string.start_over))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            viewModel.markAsUnfinished(bookToResetPositionPrompt!!.id, resetPosition = false)
                            bookToResetPositionPrompt = null
                        },
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(stringResource(R.string.keep_position))
                    }
                }
            )
        }
    }
}

@Composable
fun ContinueListeningHeroCard(
    book: BookEntity,
    onClick: () -> Unit,
    onEditDetails: () -> Unit = {},
    onMarkFinished: () -> Unit = {}
) {
    val progressFraction = if (book.totalDurationMs > 0) {
        (book.currentPositionMs.toFloat() / book.totalDurationMs).coerceIn(0f, 1f)
    } else 0f
    val progressPercent = (progressFraction * 100).toInt()
    val timeLeftMs = (book.totalDurationMs - book.currentPositionMs).coerceAtLeast(0L)
    var showHeroMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF6B9CE8),
                            Color(0xFFAAB8FC)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BookCoverArt(
                        title = book.displayTitle,
                        author = book.displayAuthor,
                        coverPath = book.displayCoverUri,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(84.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Continue listening",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1E3A8A).copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = book.displayTitle,
                            fontFamily = SerifFont,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = book.displayAuthor,
                            fontFamily = SerifFont,
                            fontSize = 13.sp,
                            color = Color(0xFF1E293B).copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showHeroMenu = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.cd_book_options, book.displayTitle),
                                tint = Color(0xFF1E3A8A)
                            )
                        }
                        DropdownMenu(
                            expanded = showHeroMenu,
                            onDismissRequest = { showHeroMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.edit_details)) },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    showHeroMenu = false
                                    onEditDetails()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.mark_as_finished)) },
                                leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) },
                                onClick = {
                                    showHeroMenu = false
                                    onMarkFinished()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = Color(0xFF0F4C81),
                    trackColor = Color.White.copy(alpha = 0.35f),
                    strokeCap = StrokeCap.Round
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom row: "% of book" on left, "time left" and cookie play button on right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$progressPercent% of book",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E3A8A)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = TimeUtils.formatTimeLeft(timeLeftMs),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E3A8A)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Scalloped Cookie Play Button
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CookieShape(lobes = 12, depth = 0.16f))
                            .background(Color(0xFF034E7B))
                            .clickable(onClick = onClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play ${book.displayTitle}",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CurrentBookRow(
    book: BookEntity,
    onClick: () -> Unit,
    onEditDetails: () -> Unit = {},
    onMarkFinished: () -> Unit = {},
    onMarkUnfinished: () -> Unit = {},
    onReLink: (() -> Unit)? = null
) {
    val progressFraction = if (book.totalDurationMs > 0) {
        (book.currentPositionMs.toFloat() / book.totalDurationMs).coerceIn(0f, 1f)
    } else 0f
    val timeLeftMs = (book.totalDurationMs - book.currentPositionMs).coerceAtLeast(0L)
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BookCoverArt(
                title = book.displayTitle,
                author = book.displayAuthor,
                coverPath = book.displayCoverUri,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(52.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.displayTitle,
                    fontFamily = SerifFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = book.displayAuthor,
                    fontFamily = SerifFont,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))

                if (book.isUnavailable) {
                    Text(
                        text = stringResource(R.string.folder_unavailable),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (book.isFinished && book.finishedAt != null) {
                    Text(
                        text = stringResource(R.string.finished_on, TimeUtils.formatDate(book.finishedAt)),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            strokeCap = StrokeCap.Round
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = TimeUtils.formatTimeLeft(timeLeftMs),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.cd_book_options, book.displayTitle),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit_details)) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onEditDetails()
                        }
                    )
                    if (!book.isFinished) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.mark_as_finished)) },
                            leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onMarkFinished()
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.mark_as_not_started)) },
                            leadingIcon = { Icon(Icons.Default.Replay, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onMarkUnfinished()
                            }
                        )
                    }
                }
            }
        }
    }
}
