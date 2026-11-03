package com.evenfall.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evenfall.app.data.DemoDataSeeder
import com.evenfall.app.data.datastore.LibrarySortBy
import com.evenfall.app.data.datastore.LibraryTab
import com.evenfall.app.data.datastore.LibraryViewMode
import com.evenfall.app.data.datastore.PreferencesManager
import com.evenfall.app.data.db.dao.BookDao
import com.evenfall.app.data.db.dao.ChapterDao
import com.evenfall.app.data.db.dao.CollectionDao
import com.evenfall.app.data.db.entity.BookEntity
import com.evenfall.app.data.db.entity.CollectionEntity
import com.evenfall.app.data.repository.LibraryRepository
import com.evenfall.app.playback.PlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class SortOption {
    RECENTLY_PLAYED,
    TITLE,
    AUTHOR,
    DURATION
}

data class SeriesGroup(
    val seriesName: String,
    val author: String,
    val books: List<BookEntity>,
    val totalBooks: Int,
    val finishedBooks: Int,
    val totalDurationMs: Long,
    val listenedDurationMs: Long
) {
    val progressFraction: Float
        get() = if (totalDurationMs > 0) (listenedDurationMs.toFloat() / totalDurationMs).coerceIn(0f, 1f) else 0f
}

data class AuthorGroup(
    val author: String,
    val books: List<BookEntity>,
    val totalBooks: Int,
    val totalDurationMs: Long,
    val listenedDurationMs: Long
)

data class FilterState(
    val statuses: Set<String> = emptySet(), // "NOT_STARTED", "IN_PROGRESS", "FINISHED"
    val collectionId: Long? = null,
    val seriesOnly: Boolean = false,
    val unavailableOnly: Boolean = false
) {
    val hasActiveFilters: Boolean
        get() = statuses.isNotEmpty() || collectionId != null || seriesOnly || unavailableOnly
}

data class HomeUiState(
    val greeting: String = "Good day",
    val searchQuery: String = "",
    val selectedTab: LibraryTab = LibraryTab.BOOKS,
    val viewMode: LibraryViewMode = LibraryViewMode.LIST,
    val sortBy: LibrarySortBy = LibrarySortBy.RECENTLY_PLAYED,
    val sortAscending: Boolean = false,
    val filterState: FilterState = FilterState(),
    val allCollections: List<CollectionEntity> = emptyList(),
    val heroBook: BookEntity? = null,
    val currentBooks: List<BookEntity> = emptyList(),
    val notStartedBooks: List<BookEntity> = emptyList(),
    val finishedBooks: List<BookEntity> = emptyList(),
    val filteredBooksList: List<BookEntity> = emptyList(),
    val seriesGroups: List<SeriesGroup> = emptyList(),
    val authorGroups: List<AuthorGroup> = emptyList(),
    val isRefreshing: Boolean = false,
    val sortOption: SortOption = SortOption.RECENTLY_PLAYED,
    val hasUnavailableBooks: Boolean = false,
    val nextInSeriesPrompt: BookEntity? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val bookDao: BookDao,
    private val chapterDao: ChapterDao,
    private val collectionDao: CollectionDao,
    private val preferencesManager: PreferencesManager,
    private val playbackManager: PlaybackManager,
    private val demoDataSeeder: DemoDataSeeder
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    init {
        viewModelScope.launch {
            demoDataSeeder.seedDemoDataIfEmpty()
        }
    }

    private data class LibraryPrefs(
        val tab: LibraryTab,
        val viewMode: LibraryViewMode,
        val sortBy: LibrarySortBy,
        val sortAscending: Boolean,
        val filterStatus: Set<String>,
        val filterCollectionId: Long?,
        val filterSeriesOnly: Boolean,
        val filterUnavailableOnly: Boolean
    )

    private val viewAndSortFlow = combine(
        preferencesManager.libraryTab,
        preferencesManager.libraryViewMode,
        preferencesManager.librarySortBy,
        preferencesManager.librarySortAscending
    ) { tab, viewMode, sortBy, sortAsc ->
        listOf(tab, viewMode, sortBy, sortAsc)
    }

    private val filterPreferencesFlow = combine(
        preferencesManager.filterStatus,
        preferencesManager.filterCollectionId,
        preferencesManager.filterSeriesOnly,
        preferencesManager.filterUnavailableOnly
    ) { status, colId, seriesOnly, unavailOnly ->
        FilterState(status, colId, seriesOnly, unavailOnly)
    }

    private val libraryPrefsFlow = combine(viewAndSortFlow, filterPreferencesFlow) { vs, filters ->
        LibraryPrefs(
            tab = vs[0] as LibraryTab,
            viewMode = vs[1] as LibraryViewMode,
            sortBy = vs[2] as LibrarySortBy,
            sortAscending = vs[3] as Boolean,
            filterStatus = filters.statuses,
            filterCollectionId = filters.collectionId,
            filterSeriesOnly = filters.seriesOnly,
            filterUnavailableOnly = filters.unavailableOnly
        )
    }

    private data class DynamicState(
        val query: String,
        val collections: List<CollectionEntity>,
        val nextInSeries: BookEntity?,
        val refreshing: Boolean
    )

    private val dynamicStateFlow = combine(
        _searchQuery,
        collectionDao.getAllCollections(),
        playbackManager.nextInSeriesPrompt,
        _isRefreshing
    ) { query, collections, nextInSeries, refreshing ->
        DynamicState(query, collections, nextInSeries, refreshing)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        libraryRepository.getAllBooks(),
        libraryPrefsFlow,
        dynamicStateFlow
    ) { allBooks, prefs, dynamicState ->
        val query = dynamicState.query
        val collections = dynamicState.collections
        val nextInSeries = dynamicState.nextInSeries
        val refreshing = dynamicState.refreshing
        val filterState = FilterState(
            statuses = prefs.filterStatus,
            collectionId = prefs.filterCollectionId,
            seriesOnly = prefs.filterSeriesOnly,
            unavailableOnly = prefs.filterUnavailableOnly
        )

        val collectionBookIds = if (prefs.filterCollectionId != null) {
            collectionDao.getBooksForCollectionSync(prefs.filterCollectionId).map { it.id }.toSet()
        } else {
            null
        }

        val filteredBooks = allBooks.filter { book ->
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                val q = query.trim().lowercase()
                book.displayTitle.lowercase().contains(q) ||
                        book.displayAuthor.lowercase().contains(q) ||
                        (book.displaySeriesName?.lowercase()?.contains(q) == true)
            }

            val matchesStatus = if (prefs.filterStatus.isEmpty()) {
                true
            } else {
                (prefs.filterStatus.contains("NOT_STARTED") && book.currentPositionMs == 0L && !book.isFinished) ||
                        (prefs.filterStatus.contains("IN_PROGRESS") && book.currentPositionMs > 0L && !book.isFinished) ||
                        (prefs.filterStatus.contains("FINISHED") && book.isFinished)
            }

            val matchesCollection = collectionBookIds == null || collectionBookIds.contains(book.id)
            val matchesSeries = !prefs.filterSeriesOnly || !book.displaySeriesName.isNullOrBlank()
            val matchesUnavailable = !prefs.filterUnavailableOnly || book.isUnavailable

            matchesQuery && matchesStatus && matchesCollection && matchesSeries && matchesUnavailable
        }

        val sortedBooks = when (prefs.sortBy) {
            LibrarySortBy.RECENTLY_PLAYED -> filteredBooks.sortedByDescending { it.lastPlayedTimestamp }
            LibrarySortBy.TITLE -> filteredBooks.sortedBy { it.displayTitle.lowercase() }
            LibrarySortBy.AUTHOR -> filteredBooks.sortedBy { it.displayAuthor.lowercase() }
            LibrarySortBy.DURATION -> filteredBooks.sortedByDescending { it.totalDurationMs }
            LibrarySortBy.PROGRESS -> filteredBooks.sortedByDescending {
                if (it.totalDurationMs > 0) it.currentPositionMs.toFloat() / it.totalDurationMs else 0f
            }
            LibrarySortBy.DATE_ADDED -> filteredBooks.sortedByDescending { it.dateAdded }
        }.let { list ->
            if (prefs.sortAscending) list.reversed() else list
        }

        val hero = sortedBooks.firstOrNull { it.currentPositionMs > 0 && !it.isFinished }
            ?: sortedBooks.firstOrNull()

        val current = sortedBooks.filter { it.currentPositionMs > 0 && !it.isFinished && it.id != hero?.id }
        val notStarted = sortedBooks.filter { it.currentPositionMs == 0L && !it.isFinished && it.id != hero?.id }
        val finished = sortedBooks.filter { it.isFinished && it.id != hero?.id }
        val hasUnavailable = allBooks.any { it.isUnavailable }

        // Build Series Groups
        val seriesMap = allBooks
            .filter { !it.displaySeriesName.isNullOrBlank() }
            .groupBy { it.displaySeriesName!!.trim() }

        val seriesGroups = seriesMap.map { (seriesName, books) ->
            val sortedSeriesBooks = books.sortedWith(
                compareBy({ it.displaySeriesOrder ?: Float.MAX_VALUE }, { it.title })
            )
            val totalDuration = sortedSeriesBooks.sumOf { it.totalDurationMs }
            val listenedDuration = sortedSeriesBooks.sumOf { it.currentPositionMs }
            val finishedCount = sortedSeriesBooks.count { it.isFinished }
            val author = sortedSeriesBooks.firstOrNull()?.displayAuthor ?: "Unknown Author"

            SeriesGroup(
                seriesName = seriesName,
                author = author,
                books = sortedSeriesBooks,
                totalBooks = sortedSeriesBooks.size,
                finishedBooks = finishedCount,
                totalDurationMs = totalDuration,
                listenedDurationMs = listenedDuration
            )
        }.sortedBy { it.seriesName.lowercase() }

        // Build Author Groups
        val authorMap = allBooks.groupBy { it.displayAuthor.trim() }
        val authorGroups = authorMap.map { (author, books) ->
            val sortedAuthorBooks = books.sortedByDescending { it.lastPlayedTimestamp }
            val totalDuration = sortedAuthorBooks.sumOf { it.totalDurationMs }
            val listenedDuration = sortedAuthorBooks.sumOf { it.currentPositionMs }

            AuthorGroup(
                author = author,
                books = sortedAuthorBooks,
                totalBooks = sortedAuthorBooks.size,
                totalDurationMs = totalDuration,
                listenedDurationMs = listenedDuration
            )
        }.sortedBy { it.author.lowercase() }

        val legacySortOption = when (prefs.sortBy) {
            LibrarySortBy.TITLE -> SortOption.TITLE
            LibrarySortBy.AUTHOR -> SortOption.AUTHOR
            LibrarySortBy.DURATION -> SortOption.DURATION
            else -> SortOption.RECENTLY_PLAYED
        }

        HomeUiState(
            greeting = getTimeBasedGreeting(),
            searchQuery = query,
            selectedTab = prefs.tab,
            viewMode = prefs.viewMode,
            sortBy = prefs.sortBy,
            sortAscending = prefs.sortAscending,
            filterState = filterState,
            allCollections = collections,
            heroBook = hero,
            currentBooks = current,
            notStartedBooks = notStarted,
            finishedBooks = finished,
            filteredBooksList = sortedBooks,
            seriesGroups = seriesGroups,
            authorGroups = authorGroups,
            isRefreshing = refreshing,
            sortOption = legacySortOption,
            hasUnavailableBooks = hasUnavailable,
            nextInSeriesPrompt = nextInSeries
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun setLibraryTab(tab: LibraryTab) {
        viewModelScope.launch {
            preferencesManager.setLibraryTab(tab)
        }
    }

    fun toggleViewMode() {
        viewModelScope.launch {
            val current = uiState.value.viewMode
            val next = if (current == LibraryViewMode.LIST) LibraryViewMode.GRID else LibraryViewMode.LIST
            preferencesManager.setLibraryViewMode(next)
        }
    }

    fun setSortBy(sortBy: LibrarySortBy, ascending: Boolean = false) {
        viewModelScope.launch {
            preferencesManager.setLibrarySortBy(sortBy)
            preferencesManager.setLibrarySortAscending(ascending)
        }
    }

    fun toggleSortOrder() {
        viewModelScope.launch {
            preferencesManager.setLibrarySortAscending(!uiState.value.sortAscending)
        }
    }

    fun setFilterStatus(status: String, selected: Boolean) {
        viewModelScope.launch {
            val current = uiState.value.filterState.statuses.toMutableSet()
            if (selected) current.add(status) else current.remove(status)
            preferencesManager.setFilterStatus(current)
        }
    }

    fun setFilterCollection(collectionId: Long?) {
        viewModelScope.launch {
            preferencesManager.setFilterCollectionId(collectionId)
        }
    }

    fun setFilterSeriesOnly(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setFilterSeriesOnly(enabled)
        }
    }

    fun setFilterUnavailableOnly(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setFilterUnavailableOnly(enabled)
        }
    }

    fun getCollectionsForBook(bookId: Long, onResult: (Set<Long>) -> Unit) {
        viewModelScope.launch {
            val cols = collectionDao.getCollectionsForBookSync(bookId).map { it.id }.toSet()
            onResult(cols)
        }
    }

    fun setCollectionsForBook(bookId: Long, collectionIds: List<Long>) {
        viewModelScope.launch {
            collectionDao.setCollectionsForBook(bookId, collectionIds)
        }
    }

    fun createCollection(name: String) {
        viewModelScope.launch {
            val existing = collectionDao.getCollectionByName(name)
            if (existing == null) {
                val count = collectionDao.getAllCollectionsSync().size
                collectionDao.insertCollection(
                    CollectionEntity(name = name, orderIndex = count)
                )
            }
        }
    }

    fun clearFilters() {
        viewModelScope.launch {
            preferencesManager.clearFilters()
        }
    }

    fun applySortAndFilters(sortBy: LibrarySortBy, ascending: Boolean, filterState: FilterState) {
        viewModelScope.launch {
            preferencesManager.setLibrarySortBy(sortBy)
            preferencesManager.setLibrarySortAscending(ascending)
            preferencesManager.setFilterStatus(filterState.statuses)
            preferencesManager.setFilterCollectionId(filterState.collectionId)
            preferencesManager.setFilterSeriesOnly(filterState.seriesOnly)
            preferencesManager.setFilterUnavailableOnly(filterState.unavailableOnly)
        }
    }

    fun onSortOptionChanged(option: SortOption) {
        val mapped = when (option) {
            SortOption.RECENTLY_PLAYED -> LibrarySortBy.RECENTLY_PLAYED
            SortOption.TITLE -> LibrarySortBy.TITLE
            SortOption.AUTHOR -> LibrarySortBy.AUTHOR
            SortOption.DURATION -> LibrarySortBy.DURATION
        }
        setSortBy(mapped)
    }

    fun playNextInSeries(book: BookEntity) {
        playbackManager.playNextInSeries(book)
    }

    fun dismissNextInSeriesPrompt() {
        playbackManager.dismissNextInSeriesPrompt()
    }

    fun rescanLibrary() {
        viewModelScope.launch {
            _isRefreshing.value = true
            libraryRepository.rescanLibrary()
            _isRefreshing.value = false
        }
    }

    fun updateMetadataOverrides(
        bookId: Long,
        title: String,
        author: String,
        narrator: String?,
        seriesName: String?,
        seriesOrder: Float?,
        coverUri: String?
    ) {
        viewModelScope.launch {
            val book = bookDao.getBookByIdSync(bookId) ?: return@launch
            val titleOv = if (title != book.title) title else null
            val authorOv = if (author != book.author) author else null
            val coverOv = if (coverUri != book.coverUri) coverUri else null

            bookDao.updateMetadataOverrides(
                id = bookId,
                titleOverride = titleOv,
                authorOverride = authorOv,
                narratorOverride = narrator,
                seriesName = seriesName,
                seriesOrder = seriesOrder,
                coverOverrideUri = coverOv
            )
        }
    }

    fun resetMetadataOverrides(bookId: Long) {
        viewModelScope.launch {
            bookDao.resetMetadataOverrides(bookId)
            chapterDao.resetChapterOverridesForBook(bookId)
        }
    }

    fun markAsFinished(bookId: Long) {
        viewModelScope.launch {
            bookDao.updateFinishedStatus(bookId, true, System.currentTimeMillis())
        }
    }

    fun markAsUnfinished(bookId: Long, resetPosition: Boolean) {
        viewModelScope.launch {
            if (resetPosition) {
                bookDao.updatePlaybackPosition(
                    id = bookId,
                    totalPosMs = 0L,
                    chapterIdx = 0,
                    chapterPosMs = 0L,
                    lastPlayed = System.currentTimeMillis()
                )
                bookDao.updateFinishedStatus(bookId, false, null)
            } else {
                bookDao.updateFinishedStatus(bookId, false, null)
            }
        }
    }

    private fun getTimeBasedGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 4..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Good night"
        }
    }
}
