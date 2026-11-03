package com.evenfall.app.ui.screens.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.evenfall.app.R
import com.evenfall.app.data.datastore.LibrarySortBy
import com.evenfall.app.data.datastore.LibraryTab
import com.evenfall.app.data.datastore.LibraryViewMode
import com.evenfall.app.data.db.entity.BookEntity
import com.evenfall.app.ui.components.AddToCollectionDialog
import com.evenfall.app.ui.components.BookCoverArt
import com.evenfall.app.ui.components.EditBookDetailsDialog
import com.evenfall.app.ui.components.SortFilterBottomSheet
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
    onOpenFixLibrary: () -> Unit = {},
    onOpenManageCollections: () -> Unit = {},
    onOpenStats: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
    hasMiniPlayer: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSortFilterSheet by remember { mutableStateOf(false) }
    var bookToEdit by remember { mutableStateOf<BookEntity?>(null) }
    var bookForCollection by remember { mutableStateOf<BookEntity?>(null) }
    var bookCollectionIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var bookToResetPositionPrompt by remember { mutableStateOf<BookEntity?>(null) }
    var bookToExplainUnavailable by remember { mutableStateOf<BookEntity?>(null) }

    BackHandler(enabled = uiState.isSearchFocused || uiState.searchQuery.isNotBlank()) {
        viewModel.clearSearchQuery()
        viewModel.onSearchFocusChanged(false)
    }

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
                                contentDescription = stringResource(R.string.search_hint),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            TextField(
                                value = uiState.searchQuery,
                                onValueChange = viewModel::onSearchQueryChanged,
                                placeholder = {
                                    Text(
                                        stringResource(R.string.search_hint),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {
                                    viewModel.saveRecentSearch(uiState.searchQuery)
                                }),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .onFocusChanged { focusState ->
                                        viewModel.onSearchFocusChanged(focusState.isFocused)
                                    }
                            )
                            if (uiState.searchQuery.isNotBlank()) {
                                IconButton(
                                    onClick = viewModel::clearSearchQuery,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.clear_search),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onOpenStats,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueryStats,
                            contentDescription = stringResource(R.string.action_stats),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.cd_settings),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
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
                // Greeting Header (in Serif font matching Image 1)
                Column(modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)) {
                    Text(
                        text = uiState.greeting,
                        fontFamily = SerifFont,
                        fontSize = 30.sp,
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

                // Segmented Tabs: Books / Series / Authors
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val tabs = listOf(
                            LibraryTab.BOOKS to stringResource(R.string.tab_books),
                            LibraryTab.SERIES to stringResource(R.string.tab_series),
                            LibraryTab.AUTHORS to stringResource(R.string.tab_authors)
                        )
                        for ((tab, label) in tabs) {
                            val isSelected = uiState.selectedTab == tab
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clickable { viewModel.setLibraryTab(tab) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Active Filters and Toolbar row (only for Books tab)
                if (uiState.selectedTab == LibraryTab.BOOKS) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Scrollable active filter chips
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (uiState.filterState.hasActiveFilters) {
                                for (st in uiState.filterState.statuses) {
                                    val label = when (st) {
                                        "IN_PROGRESS" -> stringResource(R.string.status_in_progress)
                                        "NOT_STARTED" -> stringResource(R.string.status_not_started)
                                        else -> stringResource(R.string.status_finished)
                                    }
                                    AssistChip(
                                        onClick = { viewModel.setFilterStatus(st, false) },
                                        label = { Text(label, fontSize = 11.sp) },
                                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp)) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                        )
                                    )
                                }

                                uiState.filterState.collectionId?.let { colId ->
                                    val colName = uiState.allCollections.firstOrNull { it.id == colId }?.name ?: "Collection"
                                    AssistChip(
                                        onClick = { viewModel.setFilterCollection(null) },
                                        label = { Text(colName, fontSize = 11.sp) },
                                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp)) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                        )
                                    )
                                }

                                if (uiState.filterState.seriesOnly) {
                                    AssistChip(
                                        onClick = { viewModel.setFilterSeriesOnly(false) },
                                        label = { Text(stringResource(R.string.filter_series_only), fontSize = 11.sp) },
                                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp)) }
                                    )
                                }

                                if (uiState.filterState.unavailableOnly) {
                                    AssistChip(
                                        onClick = { viewModel.setFilterUnavailableOnly(false) },
                                        label = { Text(stringResource(R.string.filter_unavailable), fontSize = 11.sp) },
                                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp)) }
                                    )
                                }

                                TextButton(
                                    onClick = viewModel::clearFilters,
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(stringResource(R.string.reset_filters), fontSize = 11.sp)
                                }
                            }
                        }

                        // Grid/List toggle & Sort/Filter Sheet Button
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = viewModel::toggleViewMode,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = if (uiState.viewMode == LibraryViewMode.GRID) Icons.AutoMirrored.Outlined.List else Icons.Default.GridView,
                                    contentDescription = if (uiState.viewMode == LibraryViewMode.GRID) stringResource(R.string.view_mode_list) else stringResource(R.string.view_mode_grid),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = { showSortFilterSheet = true },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = stringResource(R.string.sort_and_filter),
                                        tint = if (uiState.filterState.hasActiveFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (uiState.filterState.hasActiveFilters) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .size(8.dp)
                                                .align(Alignment.TopEnd)
                                        ) {}
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Content Padding bottom for mini player
                val bottomPadding = if (hasMiniPlayer) 88.dp else 24.dp

                if (uiState.searchQuery.isNotBlank()) {
                    SearchResultsGroupedView(
                        searchQuery = uiState.searchQuery,
                        results = uiState.searchResults,
                        onOpenBook = { b ->
                            viewModel.openSearchResultBook(b)
                            if (b.isUnavailable) bookToExplainUnavailable = b
                            else onOpenBook(b.id)
                        },
                        onOpenBookmark = { bm ->
                            viewModel.openSearchResultBookmark(bm)
                            onOpenBook(bm.bookmark.bookId)
                        },
                        onOpenChapter = { ch ->
                            viewModel.openSearchResultChapter(ch)
                            onOpenBook(ch.chapter.bookId)
                        },
                        modifier = Modifier.padding(bottom = bottomPadding)
                    )
                } else if (uiState.isSearchFocused && uiState.recentSearches.isNotEmpty()) {
                    RecentSearchesView(
                        recentSearches = uiState.recentSearches,
                        onSelectQuery = viewModel::selectRecentSearch,
                        onDeleteQuery = viewModel::deleteRecentSearch,
                        onClearAll = viewModel::clearRecentSearches,
                        modifier = Modifier.padding(bottom = bottomPadding)
                    )
                } else {
                    // Tab Content Rendering
                    when (uiState.selectedTab) {
                    LibraryTab.SERIES -> {
                        SeriesViewContent(
                            seriesGroups = uiState.seriesGroups,
                            onOpenBook = { id -> onOpenBook(id) },
                            onEditDetails = { b -> bookToEdit = b },
                            onAddToCollection = { b ->
                                viewModel.getCollectionsForBook(b.id) { ids ->
                                    bookCollectionIds = ids
                                    bookForCollection = b
                                }
                            },
                            onMarkFinished = { b -> viewModel.markAsFinished(b.id) },
                            onMarkUnfinished = { b -> bookToResetPositionPrompt = b },
                            contentPadding = PaddingValues(bottom = bottomPadding)
                        )
                    }

                    LibraryTab.AUTHORS -> {
                        AuthorViewContent(
                            authorGroups = uiState.authorGroups,
                            onOpenBook = { id -> onOpenBook(id) },
                            onEditDetails = { b -> bookToEdit = b },
                            onAddToCollection = { b ->
                                viewModel.getCollectionsForBook(b.id) { ids ->
                                    bookCollectionIds = ids
                                    bookForCollection = b
                                }
                            },
                            onMarkFinished = { b -> viewModel.markAsFinished(b.id) },
                            onMarkUnfinished = { b -> bookToResetPositionPrompt = b },
                            contentPadding = PaddingValues(bottom = bottomPadding)
                        )
                    }

                    LibraryTab.BOOKS -> {
                        if (uiState.viewMode == LibraryViewMode.GRID) {
                            BookCoverGridContent(
                                books = uiState.filteredBooksList,
                                onOpenBook = { id -> onOpenBook(id) },
                                onEditDetails = { b -> bookToEdit = b },
                                onAddToCollection = { b ->
                                    viewModel.getCollectionsForBook(b.id) { ids ->
                                        bookCollectionIds = ids
                                        bookForCollection = b
                                    }
                                },
                                onMarkFinished = { b -> viewModel.markAsFinished(b.id) },
                                onMarkUnfinished = { b -> bookToResetPositionPrompt = b },
                                contentPadding = PaddingValues(bottom = bottomPadding)
                            )
                        } else {
                            // Standard List View with Hero Card
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = bottomPadding),
                                verticalArrangement = Arrangement.spacedBy(18.dp)
                            ) {
                                // Lost-Folder Warning Banner
                                if (uiState.hasUnavailableBooks) {
                                    item {
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Warning,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                                Spacer(modifier = Modifier.width(14.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = stringResource(R.string.lost_folder_banner_title),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = MaterialTheme.colorScheme.onErrorContainer
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = stringResource(R.string.lost_folder_banner_desc),
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Button(
                                                    onClick = onOpenFixLibrary,
                                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.error,
                                                        contentColor = MaterialTheme.colorScheme.onError
                                                    )
                                                ) {
                                                    Text(stringResource(R.string.fix_library))
                                                }
                                            }
                                        }
                                    }
                                }

                                // Hero Card: Continue Listening (when no search/filter active)
                                if (!uiState.filterState.hasActiveFilters && uiState.searchQuery.isBlank()) {
                                    uiState.heroBook?.let { hero ->
                                        item {
                                            ContinueListeningHeroCard(
                                                book = hero,
                                                onClick = {
                                                    if (hero.isUnavailable) bookToExplainUnavailable = hero
                                                    else onOpenBook(hero.id)
                                                },
                                                onEditDetails = { bookToEdit = hero },
                                                onAddToCollection = {
                                                    viewModel.getCollectionsForBook(hero.id) { ids ->
                                                        bookCollectionIds = ids
                                                        bookForCollection = hero
                                                    }
                                                },
                                                onMarkFinished = { viewModel.markAsFinished(hero.id) }
                                            )
                                        }
                                    }
                                }

                                // If filtering or searching, show full filtered list cleanly
                                if (uiState.filterState.hasActiveFilters || uiState.searchQuery.isNotBlank()) {
                                    items(uiState.filteredBooksList, key = { it.id }) { book ->
                                        CurrentBookRow(
                                            book = book,
                                            onClick = {
                                                if (book.isUnavailable) bookToExplainUnavailable = book
                                                else onOpenBook(book.id)
                                            },
                                            onEditDetails = { bookToEdit = book },
                                            onAddToCollection = {
                                                viewModel.getCollectionsForBook(book.id) { ids ->
                                                    bookCollectionIds = ids
                                                    bookForCollection = book
                                                }
                                            },
                                            onMarkFinished = { viewModel.markAsFinished(book.id) },
                                            onMarkUnfinished = { bookToResetPositionPrompt = book }
                                        )
                                    }
                                } else {
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
                                                            onClick = {
                                                                if (book.isUnavailable) bookToExplainUnavailable = book
                                                                else onOpenBook(book.id)
                                                            },
                                                            onEditDetails = { bookToEdit = book },
                                                            onAddToCollection = {
                                                                viewModel.getCollectionsForBook(book.id) { ids ->
                                                                    bookCollectionIds = ids
                                                                    bookForCollection = book
                                                                }
                                                            },
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
                                        items(uiState.notStartedBooks, key = { it.id }) { book ->
                                            CurrentBookRow(
                                                book = book,
                                                onClick = {
                                                    if (book.isUnavailable) bookToExplainUnavailable = book
                                                    else onOpenBook(book.id)
                                                },
                                                onEditDetails = { bookToEdit = book },
                                                onAddToCollection = {
                                                    viewModel.getCollectionsForBook(book.id) { ids ->
                                                        bookCollectionIds = ids
                                                        bookForCollection = book
                                                    }
                                                },
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
                                        items(uiState.finishedBooks, key = { it.id }) { book ->
                                            CurrentBookRow(
                                                book = book,
                                                onClick = {
                                                    if (book.isUnavailable) bookToExplainUnavailable = book
                                                    else onOpenBook(book.id)
                                                },
                                                onEditDetails = { bookToEdit = book },
                                                onAddToCollection = {
                                                    viewModel.getCollectionsForBook(book.id) { ids ->
                                                        bookCollectionIds = ids
                                                        bookForCollection = book
                                                    }
                                                },
                                                onMarkFinished = { viewModel.markAsFinished(book.id) },
                                                onMarkUnfinished = { bookToResetPositionPrompt = book }
                                            )
                                        }
                                    }
                                }

                                item {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
        }

        // Dialogs
        if (showSortFilterSheet) {
            SortFilterBottomSheet(
                currentSortBy = uiState.sortBy,
                currentSortAscending = uiState.sortAscending,
                currentFilterState = uiState.filterState,
                collections = uiState.allCollections,
                onApplySortAndFilter = { sort, asc, filters ->
                    viewModel.applySortAndFilters(sort, asc, filters)
                },
                onResetFilters = viewModel::clearFilters,
                onManageCollections = onOpenManageCollections,
                onDismiss = { showSortFilterSheet = false }
            )
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

        if (bookForCollection != null) {
            AddToCollectionDialog(
                book = bookForCollection!!,
                allCollections = uiState.allCollections,
                currentCollectionIds = bookCollectionIds,
                onSaveCollections = { bookId, selectedIds ->
                    viewModel.setCollectionsForBook(bookId, selectedIds)
                    bookForCollection = null
                },
                onCreateCollection = { name ->
                    viewModel.createCollection(name)
                },
                onDismiss = { bookForCollection = null }
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

        if (bookToExplainUnavailable != null) {
            AlertDialog(
                onDismissRequest = { bookToExplainUnavailable = null },
                title = { Text(stringResource(R.string.book_unavailable_title)) },
                text = { Text(stringResource(R.string.book_unavailable_desc)) },
                confirmButton = {
                    Button(
                        onClick = {
                            bookToExplainUnavailable = null
                            onOpenFixLibrary()
                        },
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(stringResource(R.string.relink_folder))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { bookToExplainUnavailable = null },
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            )
        }

        // Up next in series prompt dialog
        uiState.nextInSeriesPrompt?.let { nextBook ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissNextInSeriesPrompt() },
                title = {
                    Text(
                        text = stringResource(R.string.up_next_title),
                        fontFamily = SerifFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                text = {
                    Text(stringResource(R.string.up_next_prompt, nextBook.displayTitle))
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.playNextInSeries(nextBook)
                            onOpenBook(nextBook.id)
                        },
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(stringResource(R.string.play_now))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.dismissNextInSeriesPrompt() },
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(stringResource(R.string.not_now))
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
    onAddToCollection: () -> Unit = {},
    onMarkFinished: () -> Unit = {}
) {
    val progressFraction = if (book.totalDurationMs > 0) {
        (book.currentPositionMs.toFloat() / book.totalDurationMs).coerceIn(0f, 1f)
    } else 0f
    val progressPercent = (progressFraction * 100).toInt()
    val timeLeftMs = (book.totalDurationMs - book.currentPositionMs).coerceAtLeast(0L)
    var showHeroMenu by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFE8DCC4), // Warm parchment amber
                            Color(0xFFD4C4A8)
                        )
                    )
                )
                .padding(22.dp)
        ) {
            Column {
                // Top row: "CONTINUE LISTENING" label + Overflow menu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "CONTINUE LISTENING",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Color(0xFF6B5E4A)
                    )

                    Box {
                        IconButton(
                            onClick = { showHeroMenu = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Hero options",
                                tint = Color(0xFF6B5E4A)
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
                                text = { Text(stringResource(R.string.add_to_collection)) },
                                leadingIcon = { Icon(Icons.Default.BookmarkBorder, contentDescription = null) },
                                onClick = {
                                    showHeroMenu = false
                                    onAddToCollection()
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

                Spacer(modifier = Modifier.height(14.dp))

                // Middle: Large Cover Art (~140dp) + Title + Author + Chapter
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BookCoverArt(
                        title = book.displayTitle,
                        author = book.displayAuthor,
                        coverPath = book.displayCoverUri,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .size(110.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )

                    Spacer(modifier = Modifier.width(18.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = book.displayTitle,
                            fontFamily = SerifFont,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = book.displayAuthor,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF475569)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Chapter ${book.currentChapterIndex + 1}",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
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
                        text = "${TimeUtils.formatDuration(timeLeftMs)} left",
                        fontSize = 12.sp,
                        color = Color(0xFF475569)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Surface(
                        shape = CookieShape(12),
                        color = Color(0xFF0F4C81),
                        modifier = Modifier
                            .size(48.dp)
                            .clickable(onClick = onClick)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentBookRow(
    book: BookEntity,
    onClick: () -> Unit,
    onEditDetails: () -> Unit = {},
    onAddToCollection: () -> Unit = {},
    onMarkFinished: () -> Unit = {},
    onMarkUnfinished: () -> Unit = {},
    onReLink: (() -> Unit)? = null
) {
    val progressFraction = if (book.totalDurationMs > 0) {
        (book.currentPositionMs.toFloat() / book.totalDurationMs).coerceIn(0f, 1f)
    } else 0f
    val timeLeftMs = (book.totalDurationMs - book.currentPositionMs).coerceAtLeast(0L)
    var showMenu by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.StartToEnd || dismissValue == SwipeToDismissBoxValue.EndToStart) {
                if (!book.isFinished) {
                    onMarkFinished()
                } else {
                    onMarkUnfinished()
                }
            }
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val alignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            val icon = if (!book.isFinished) Icons.Default.Check else Icons.Default.Replay
            val iconDesc = if (!book.isFinished) {
                stringResource(R.string.mark_as_finished)
            } else {
                stringResource(R.string.mark_as_not_started)
            }
            val bgColor = if (!book.isFinished) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.tertiaryContainer
            }
            val tint = if (!book.isFinished) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onTertiaryContainer
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(bgColor)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = iconDesc,
                    tint = tint,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Book Cover
                Box {
                    BookCoverArt(
                        title = book.displayTitle,
                        author = book.displayAuthor,
                        coverPath = book.displayCoverUri,
                        shape = CookieShape(lobes = 12, depth = 0.16f),
                        modifier = Modifier.size(56.dp)
                    )
                    if (book.isUnavailable) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .size(18.dp)
                                .align(Alignment.BottomEnd)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Unavailable",
                                    tint = MaterialTheme.colorScheme.onError,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Title, Author, Progress
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = book.displayTitle,
                        fontFamily = SerifFont,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (book.isUnavailable) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = book.displayAuthor,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    if (book.isUnavailable) {
                        Text(
                            text = stringResource(R.string.book_unavailable_label),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    } else if (book.isFinished) {
                        val finishedDateStr = book.finishedAt?.let { TimeUtils.formatDate(it) } ?: ""
                        Text(
                            text = if (finishedDateStr.isNotEmpty()) "Finished on $finishedDateStr" else "Finished",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    } else if (book.currentPositionMs > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .width(80.dp)
                                    .height(4.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                strokeCap = StrokeCap.Round
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${TimeUtils.formatDuration(timeLeftMs)} left",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = TimeUtils.formatDuration(book.totalDurationMs),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.add_to_collection)) },
                            leadingIcon = { Icon(Icons.Default.BookmarkBorder, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onAddToCollection()
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
}
