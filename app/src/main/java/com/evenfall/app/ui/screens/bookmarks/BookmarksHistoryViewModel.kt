package com.evenfall.app.ui.screens.bookmarks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evenfall.app.data.db.dao.BookmarkDao
import com.evenfall.app.data.db.dao.BookDao
import com.evenfall.app.data.db.dao.ChapterDao
import com.evenfall.app.data.db.dao.HistoryDao
import com.evenfall.app.data.db.entity.BookmarkEntity
import com.evenfall.app.data.db.entity.BookmarkType
import com.evenfall.app.data.db.entity.BookEntity
import com.evenfall.app.data.db.entity.ChapterEntity
import com.evenfall.app.data.db.entity.HistoryEntity
import com.evenfall.app.playback.PlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class BookmarksTab {
    BOOKMARKS,
    HISTORY
}

enum class BookmarkSortOrder {
    POSITION,
    NEWEST,
    OLDEST,
    TITLE
}

data class BookmarksHistoryUiState(
    val book: BookEntity? = null,
    val chapters: List<ChapterEntity> = emptyList(),
    val currentTab: BookmarksTab = BookmarksTab.BOOKMARKS,
    val selectedFilter: BookmarkType = BookmarkType.ALL,
    val bookmarks: List<BookmarkEntity> = emptyList(),
    val bookmarkCount: Int = 0,
    val notesCount: Int = 0,
    val favoritesCount: Int = 0,
    val quotesCount: Int = 0,
    val historyEvents: List<HistoryEntity> = emptyList(),
    val currentChapterIndex: Int = 0,
    val currentPositionMs: Long = 0L,
    val sortOrder: BookmarkSortOrder = BookmarkSortOrder.POSITION
)

@HiltViewModel
class BookmarksHistoryViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val bookDao: BookDao,
    private val chapterDao: ChapterDao,
    private val bookmarkDao: BookmarkDao,
    private val historyDao: HistoryDao,
    private val playbackManager: PlaybackManager
) : ViewModel() {

    private val bookId: Long = savedStateHandle["bookId"] ?: 1L

    private val _currentTab = MutableStateFlow(BookmarksTab.BOOKMARKS)
    val currentTab = _currentTab.asStateFlow()

    private val _selectedFilter = MutableStateFlow(BookmarkType.ALL)
    val selectedFilter = _selectedFilter.asStateFlow()

    private val _sortOrder = MutableStateFlow(BookmarkSortOrder.POSITION)
    val sortOrder = _sortOrder.asStateFlow()

    init {
        // Auto-prune history beyond 30 days
        viewModelScope.launch {
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 3600 * 1000)
            historyDao.pruneOldHistory(bookId, thirtyDaysAgo)
            historyDao.pruneOldEvents(bookId, maxEvents = 100)
        }
    }

    private val processedBookmarks = combine(
        bookmarkDao.getBookmarksForBook(bookId),
        _selectedFilter,
        _sortOrder
    ) { allBookmarks, filter, sort ->
        val filtered = when (filter) {
            BookmarkType.ALL -> allBookmarks
            else -> allBookmarks.filter { it.type == filter }
        }

        val sorted = when (sort) {
            BookmarkSortOrder.POSITION -> filtered.sortedWith(compareBy({ it.chapterIndex }, { it.positionMs }))
            BookmarkSortOrder.NEWEST -> filtered.sortedByDescending { it.timestamp }
            BookmarkSortOrder.OLDEST -> filtered.sortedBy { it.timestamp }
            BookmarkSortOrder.TITLE -> filtered.sortedBy { it.title.lowercase() }
        }

        Triple(allBookmarks, filter, sorted)
    }

    val uiState: StateFlow<BookmarksHistoryUiState> = combine(
        bookDao.getBookById(bookId),
        chapterDao.getChaptersForBook(bookId),
        processedBookmarks,
        historyDao.getHistoryForBook(bookId),
        _currentTab
    ) { book, chapters, bookmarkData, history, tab ->
        val (allBookmarks, filter, sortedBookmarks) = bookmarkData
        val notes = allBookmarks.count { it.type == BookmarkType.NOTE }
        val favorites = allBookmarks.count { it.type == BookmarkType.FAVORITE }
        val quotes = allBookmarks.count { it.type == BookmarkType.QUOTE }

        BookmarksHistoryUiState(
            book = book,
            chapters = chapters,
            currentTab = tab,
            selectedFilter = filter,
            bookmarks = sortedBookmarks,
            bookmarkCount = allBookmarks.size,
            notesCount = notes,
            favoritesCount = favorites,
            quotesCount = quotes,
            historyEvents = history,
            currentChapterIndex = book?.currentChapterIndex ?: 0,
            currentPositionMs = book?.chapterPositionMs ?: 0L,
            sortOrder = _sortOrder.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BookmarksHistoryUiState()
    )

    fun selectTab(tab: BookmarksTab) {
        _currentTab.value = tab
    }

    fun selectFilter(filter: BookmarkType) {
        _selectedFilter.value = filter
    }

    fun setSortOrder(order: BookmarkSortOrder) {
        _sortOrder.value = order
    }

    fun jumpToBookmark(bookmark: BookmarkEntity) {
        playbackManager.loadBook(bookmark.bookId, autoPlay = true)
        playbackManager.switchToChapter(bookmark.chapterIndex)
        playbackManager.seekTo(bookmark.positionMs, recordUndo = true)
    }

    fun jumpToHistory(history: HistoryEntity) {
        playbackManager.loadBook(history.bookId, autoPlay = true)
        playbackManager.switchToChapter(history.chapterIndex)
        playbackManager.seekTo(history.positionMs, recordUndo = true)
    }

    fun saveCurrentMoment(title: String, note: String?, type: BookmarkType) {
        val state = uiState.value
        val book = state.book ?: return
        val chapter = state.chapters.getOrNull(state.currentChapterIndex)
        val chapterTitle = chapter?.title ?: "Chapter ${state.currentChapterIndex + 1}"
        val fraction = if (book.totalDurationMs > 0) {
            book.currentPositionMs.toFloat() / book.totalDurationMs
        } else 0f

        viewModelScope.launch {
            bookmarkDao.insert(
                BookmarkEntity(
                    bookId = book.id,
                    chapterIndex = state.currentChapterIndex,
                    chapterTitle = chapterTitle,
                    positionMs = state.currentPositionMs,
                    bookProgressFraction = fraction,
                    title = title.ifBlank { "Moment at ${chapterTitle}" },
                    note = note,
                    type = type
                )
            )
        }
    }

    fun deleteBookmark(bookmark: BookmarkEntity) {
        viewModelScope.launch {
            bookmarkDao.delete(bookmark)
        }
    }
}
