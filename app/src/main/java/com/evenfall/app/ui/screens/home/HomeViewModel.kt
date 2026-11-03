package com.evenfall.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evenfall.app.data.DemoDataSeeder
import com.evenfall.app.data.db.dao.BookDao
import com.evenfall.app.data.db.dao.ChapterDao
import com.evenfall.app.data.db.entity.BookEntity
import com.evenfall.app.data.repository.LibraryRepository
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

data class HomeUiState(
    val greeting: String = "Good day",
    val searchQuery: String = "",
    val heroBook: BookEntity? = null,
    val currentBooks: List<BookEntity> = emptyList(),
    val notStartedBooks: List<BookEntity> = emptyList(),
    val finishedBooks: List<BookEntity> = emptyList(),
    val isRefreshing: Boolean = false,
    val sortOption: SortOption = SortOption.RECENTLY_PLAYED,
    val hasUnavailableBooks: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val bookDao: BookDao,
    private val chapterDao: ChapterDao,
    private val demoDataSeeder: DemoDataSeeder
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.RECENTLY_PLAYED)
    val sortOption = _sortOption.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    init {
        viewModelScope.launch {
            demoDataSeeder.seedDemoDataIfEmpty()
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        libraryRepository.getAllBooks(),
        _searchQuery,
        _sortOption,
        _isRefreshing
    ) { allBooks, query, sort, refreshing ->
        val filteredBooks = if (query.isBlank()) {
            allBooks
        } else {
            val q = query.trim().lowercase()
            allBooks.filter {
                it.displayTitle.lowercase().contains(q) || it.displayAuthor.lowercase().contains(q)
            }
        }

        val sortedBooks = when (sort) {
            SortOption.RECENTLY_PLAYED -> filteredBooks.sortedByDescending { it.lastPlayedTimestamp }
            SortOption.TITLE -> filteredBooks.sortedBy { it.displayTitle.lowercase() }
            SortOption.AUTHOR -> filteredBooks.sortedBy { it.displayAuthor.lowercase() }
            SortOption.DURATION -> filteredBooks.sortedByDescending { it.totalDurationMs }
        }

        val hero = sortedBooks.firstOrNull { it.currentPositionMs > 0 && !it.isFinished }
            ?: sortedBooks.firstOrNull()

        val current = sortedBooks.filter { it.currentPositionMs > 0 && !it.isFinished && it.id != hero?.id }
        val notStarted = sortedBooks.filter { it.currentPositionMs == 0L && !it.isFinished && it.id != hero?.id }
        val finished = sortedBooks.filter { it.isFinished && it.id != hero?.id }
        val hasUnavailable = allBooks.any { it.isUnavailable }

        HomeUiState(
            greeting = getTimeBasedGreeting(),
            searchQuery = query,
            heroBook = hero,
            currentBooks = current,
            notStartedBooks = notStarted,
            finishedBooks = finished,
            isRefreshing = refreshing,
            sortOption = sort,
            hasUnavailableBooks = hasUnavailable
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onSortOptionChanged(option: SortOption) {
        _sortOption.value = option
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
                val book = bookDao.getBookByIdSync(bookId) ?: return@launch
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
