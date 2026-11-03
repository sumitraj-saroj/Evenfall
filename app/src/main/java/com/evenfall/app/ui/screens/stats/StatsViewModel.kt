package com.evenfall.app.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evenfall.app.data.datastore.PreferencesManager
import com.evenfall.app.data.db.dao.BookDao
import com.evenfall.app.data.db.dao.ListeningSessionDao
import com.evenfall.app.data.db.entity.BookEntity
import com.evenfall.app.data.db.entity.ListeningSessionEntity
import com.evenfall.app.playback.ListeningStatsCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import javax.inject.Inject

data class DayListeningStat(
    val dayLabel: String,
    val dateLabel: String,
    val listenedDurationMs: Long,
    val percentageOfMax: Float,
    val isToday: Boolean
)

data class TopBookItem(
    val book: BookEntity,
    val listenedMs: Long,
    val contentMs: Long
)

data class StatsUiState(
    val isStatsEnabled: Boolean = true,
    val todayListenedMs: Long = 0L,
    val weekListenedMs: Long = 0L,
    val monthListenedMs: Long = 0L,
    val allTimeListenedMs: Long = 0L,
    val allTimeContentMs: Long = 0L,
    val timeSavedMs: Long = 0L,
    val finishedBooksCount: Int = 0,
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val weeklyChart: List<DayListeningStat> = emptyList(),
    val topBooks: List<TopBookItem> = emptyList(),
    val hasAnyStats: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val listeningSessionDao: ListeningSessionDao,
    private val bookDao: BookDao,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val zoneId = ZoneId.systemDefault()

    val uiState: StateFlow<StatsUiState> = combine(
        preferencesManager.statsEnabled,
        listeningSessionDao.getAllSessions(),
        bookDao.getAllBooks()
    ) { statsEnabled, sessions, allBooks ->
        buildUiState(statsEnabled, sessions, allBooks)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatsUiState(isLoading = true)
    )

    private fun buildUiState(
        statsEnabled: Boolean,
        sessions: List<ListeningSessionEntity>,
        allBooks: List<BookEntity>
    ): StatsUiState {
        val today = LocalDate.now(zoneId)
        val startOfTodayMs = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val startOfWeekMs = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .atStartOfDay(zoneId).toInstant().toEpochMilli()
        val startOfMonthMs = today.with(TemporalAdjusters.firstDayOfMonth())
            .atStartOfDay(zoneId).toInstant().toEpochMilli()

        var todayListenedMs = 0L
        var weekListenedMs = 0L
        var monthListenedMs = 0L
        var allTimeListenedMs = 0L
        var allTimeContentMs = 0L

        val bookDurations = mutableMapOf<Long, Pair<Long, Long>>() // bookId -> (listenedMs, contentMs)

        for (session in sessions) {
            val listened = session.listenedDurationMs
            val content = session.contentDurationMs
            val ts = session.timestamp

            allTimeListenedMs += listened
            allTimeContentMs += content

            if (ts >= startOfTodayMs) {
                todayListenedMs += listened
            }
            if (ts >= startOfWeekMs) {
                weekListenedMs += listened
            }
            if (ts >= startOfMonthMs) {
                monthListenedMs += listened
            }

            val currentBookStats = bookDurations.getOrDefault(session.bookId, 0L to 0L)
            bookDurations[session.bookId] = (currentBookStats.first + listened) to (currentBookStats.second + content)
        }

        val timeSavedMs = ListeningStatsCalculator.calculateTimeSavedMs(allTimeListenedMs, allTimeContentMs)
        val streaks = ListeningStatsCalculator.calculateStreaks(sessions, today, zoneId)
        val finishedCount = allBooks.count { it.isFinished }

        // Weekly chart for the last 7 days
        val past7Days = (6 downTo 0).map { today.minusDays(it.toLong()) }
        val dailyDurations = ListeningStatsCalculator.aggregateDurationsForDates(sessions, past7Days, zoneId)
        val maxDayDuration = dailyDurations.values.maxOrNull()?.coerceAtLeast(60_000L) ?: 60_000L

        val dayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
        val dateFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

        val weeklyChart = past7Days.map { date ->
            val duration = dailyDurations[date] ?: 0L
            DayListeningStat(
                dayLabel = date.format(dayFormatter),
                dateLabel = date.format(dateFormatter),
                listenedDurationMs = duration,
                percentageOfMax = (duration.toFloat() / maxDayDuration.toFloat()).coerceIn(0f, 1f),
                isToday = (date == today)
            )
        }

        // Top books
        val booksMap = allBooks.associateBy { it.id }
        val topBooks = bookDurations.entries
            .sortedByDescending { it.value.first }
            .take(5)
            .mapNotNull { entry ->
                val book = booksMap[entry.key]
                if (book != null) {
                    TopBookItem(
                        book = book,
                        listenedMs = entry.value.first,
                        contentMs = entry.value.second
                    )
                } else null
            }

        return StatsUiState(
            isStatsEnabled = statsEnabled,
            todayListenedMs = todayListenedMs,
            weekListenedMs = weekListenedMs,
            monthListenedMs = monthListenedMs,
            allTimeListenedMs = allTimeListenedMs,
            allTimeContentMs = allTimeContentMs,
            timeSavedMs = timeSavedMs,
            finishedBooksCount = finishedCount,
            currentStreakDays = streaks.currentStreak,
            longestStreakDays = streaks.longestStreak,
            weeklyChart = weeklyChart,
            topBooks = topBooks,
            hasAnyStats = sessions.isNotEmpty(),
            isLoading = false
        )
    }

    fun setStatsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setStatsEnabled(enabled)
        }
    }

    fun deleteAllStats() {
        viewModelScope.launch(Dispatchers.IO) {
            listeningSessionDao.deleteAll()
        }
    }
}
