package com.evenfall.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.evenfall.app.ui.theme.ColorSchemeOption
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "evenfall_settings")

enum class ThemeMode {
    FOLLOW_SYSTEM,
    LIGHT,
    DARK
}

enum class ScanMode {
    SUBFOLDER_IS_BOOK,
    FILE_IS_BOOK
}

enum class LibraryTab {
    BOOKS,
    SERIES,
    AUTHORS
}

enum class LibraryViewMode {
    LIST,
    GRID
}

enum class LibrarySortBy {
    RECENTLY_PLAYED,
    TITLE,
    AUTHOR,
    DURATION,
    PROGRESS,
    DATE_ADDED
}

@Singleton
class PreferencesManager(
    private val dataStore: DataStore<Preferences>
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context.dataStore)

    companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_PURE_BLACK = booleanPreferencesKey("pure_black")
        val KEY_COLOR_SCHEME = stringPreferencesKey("color_scheme")
        val KEY_SKIP_AMOUNT = intPreferencesKey("skip_amount_seconds")
        val KEY_AUTO_REWIND = booleanPreferencesKey("auto_rewind_enabled")
        val KEY_SHAKE_TO_EXTEND = booleanPreferencesKey("shake_to_extend_enabled")
        val KEY_FADE_OUT_DURATION = intPreferencesKey("fade_out_duration_seconds")
        val KEY_SILENCE_SKIPPING = booleanPreferencesKey("silence_skipping_enabled")
        val KEY_VOLUME_BOOST = floatPreferencesKey("volume_boost_level")
        val KEY_DEFAULT_SPEED = floatPreferencesKey("default_speed")
        val KEY_SCAN_MODE = stringPreferencesKey("scan_mode")
        val KEY_LIBRARY_FOLDERS = stringSetPreferencesKey("library_folders")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_COMPACT_PLAYER = booleanPreferencesKey("compact_player")
        val KEY_AUTO_MARK_FINISHED = booleanPreferencesKey("auto_mark_finished")
        val KEY_BACKUP_FOLDER_URI = stringPreferencesKey("backup_folder_uri")
        val KEY_LAST_BACKUP_TIMESTAMP = longPreferencesKey("last_backup_timestamp")
        val KEY_AUTO_PLAY_NEXT_IN_SERIES = booleanPreferencesKey("auto_play_next_in_series")
        val KEY_LIBRARY_TAB = stringPreferencesKey("library_tab")
        val KEY_LIBRARY_VIEW_MODE = stringPreferencesKey("library_view_mode")
        val KEY_LIBRARY_SORT_BY = stringPreferencesKey("library_sort_by")
        val KEY_LIBRARY_SORT_ASCENDING = booleanPreferencesKey("library_sort_ascending")
        val KEY_FILTER_STATUS = stringSetPreferencesKey("filter_status")
        val KEY_FILTER_COLLECTION_ID = longPreferencesKey("filter_collection_id")
        val KEY_FILTER_SERIES_ONLY = booleanPreferencesKey("filter_series_only")
        val KEY_FILTER_UNAVAILABLE_ONLY = booleanPreferencesKey("filter_unavailable_only")
    }

    val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        when (prefs[KEY_THEME_MODE]) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.DARK.name -> ThemeMode.DARK
            else -> ThemeMode.FOLLOW_SYSTEM
        }
    }

    val pureBlack: Flow<Boolean> = dataStore.data.map { it[KEY_PURE_BLACK] ?: false }

    val colorSchemeOption: Flow<ColorSchemeOption> = dataStore.data.map { prefs ->
        val name = prefs[KEY_COLOR_SCHEME] ?: ColorSchemeOption.EVENFALL_BLUE.name
        try {
            ColorSchemeOption.valueOf(name)
        } catch (_: Exception) {
            ColorSchemeOption.EVENFALL_BLUE
        }
    }

    val skipAmountSeconds: Flow<Int> = dataStore.data.map { it[KEY_SKIP_AMOUNT] ?: 20 }

    val autoRewindEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_AUTO_REWIND] ?: true }

    val shakeToExtendEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_SHAKE_TO_EXTEND] ?: true }

    val fadeOutDurationSeconds: Flow<Int> = dataStore.data.map { it[KEY_FADE_OUT_DURATION] ?: 30 }

    val silenceSkippingEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_SILENCE_SKIPPING] ?: false }

    val volumeBoostLevel: Flow<Float> = dataStore.data.map { it[KEY_VOLUME_BOOST] ?: 0f }

    val defaultPlaybackSpeed: Flow<Float> = dataStore.data.map { it[KEY_DEFAULT_SPEED] ?: 1.0f }

    val scanMode: Flow<ScanMode> = dataStore.data.map { prefs ->
        when (prefs[KEY_SCAN_MODE]) {
            ScanMode.FILE_IS_BOOK.name -> ScanMode.FILE_IS_BOOK
            else -> ScanMode.SUBFOLDER_IS_BOOK
        }
    }

    val libraryFolders: Flow<Set<String>> = dataStore.data.map { it[KEY_LIBRARY_FOLDERS] ?: emptySet() }

    val isOnboardingCompleted: Flow<Boolean> = dataStore.data.map { it[KEY_ONBOARDING_COMPLETED] ?: false }

    val compactPlayer: Flow<Boolean> = dataStore.data.map { it[KEY_COMPACT_PLAYER] ?: true }

    val autoMarkFinished: Flow<Boolean> = dataStore.data.map { it[KEY_AUTO_MARK_FINISHED] ?: true }

    val backupFolderUri: Flow<String?> = dataStore.data.map { it[KEY_BACKUP_FOLDER_URI] }

    val lastBackupTimestamp: Flow<Long?> = dataStore.data.map { it[KEY_LAST_BACKUP_TIMESTAMP] }

    val autoPlayNextInSeries: Flow<Boolean> = dataStore.data.map { it[KEY_AUTO_PLAY_NEXT_IN_SERIES] ?: false }

    val libraryTab: Flow<LibraryTab> = dataStore.data.map { prefs ->
        when (prefs[KEY_LIBRARY_TAB]) {
            LibraryTab.SERIES.name -> LibraryTab.SERIES
            LibraryTab.AUTHORS.name -> LibraryTab.AUTHORS
            else -> LibraryTab.BOOKS
        }
    }

    val libraryViewMode: Flow<LibraryViewMode> = dataStore.data.map { prefs ->
        when (prefs[KEY_LIBRARY_VIEW_MODE]) {
            LibraryViewMode.GRID.name -> LibraryViewMode.GRID
            else -> LibraryViewMode.LIST
        }
    }

    val librarySortBy: Flow<LibrarySortBy> = dataStore.data.map { prefs ->
        try {
            prefs[KEY_LIBRARY_SORT_BY]?.let { LibrarySortBy.valueOf(it) } ?: LibrarySortBy.RECENTLY_PLAYED
        } catch (_: Exception) {
            LibrarySortBy.RECENTLY_PLAYED
        }
    }

    val librarySortAscending: Flow<Boolean> = dataStore.data.map { it[KEY_LIBRARY_SORT_ASCENDING] ?: false }

    val filterStatus: Flow<Set<String>> = dataStore.data.map { it[KEY_FILTER_STATUS] ?: emptySet() }

    val filterCollectionId: Flow<Long?> = dataStore.data.map { prefs ->
        prefs[KEY_FILTER_COLLECTION_ID]?.takeIf { it != -1L }
    }

    val filterSeriesOnly: Flow<Boolean> = dataStore.data.map { it[KEY_FILTER_SERIES_ONLY] ?: false }

    val filterUnavailableOnly: Flow<Boolean> = dataStore.data.map { it[KEY_FILTER_UNAVAILABLE_ONLY] ?: false }

    suspend fun setThemeMode(mode: ThemeMode) = dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    suspend fun setPureBlack(enabled: Boolean) = dataStore.edit { it[KEY_PURE_BLACK] = enabled }
    suspend fun setColorScheme(scheme: ColorSchemeOption) = dataStore.edit { it[KEY_COLOR_SCHEME] = scheme.name }
    suspend fun setSkipAmountSeconds(seconds: Int) = dataStore.edit { it[KEY_SKIP_AMOUNT] = seconds.coerceIn(5, 60) }
    suspend fun setAutoRewindEnabled(enabled: Boolean) = dataStore.edit { it[KEY_AUTO_REWIND] = enabled }
    suspend fun setShakeToExtendEnabled(enabled: Boolean) = dataStore.edit { it[KEY_SHAKE_TO_EXTEND] = enabled }
    suspend fun setFadeOutDurationSeconds(seconds: Int) = dataStore.edit { it[KEY_FADE_OUT_DURATION] = seconds }
    suspend fun setSilenceSkippingEnabled(enabled: Boolean) = dataStore.edit { it[KEY_SILENCE_SKIPPING] = enabled }
    suspend fun setVolumeBoostLevel(level: Float) = dataStore.edit { it[KEY_VOLUME_BOOST] = level }
    suspend fun setDefaultPlaybackSpeed(speed: Float) = dataStore.edit { it[KEY_DEFAULT_SPEED] = speed }
    suspend fun setScanMode(mode: ScanMode) = dataStore.edit { it[KEY_SCAN_MODE] = mode.name }
    suspend fun addLibraryFolder(uri: String) = dataStore.edit {
        val current = it[KEY_LIBRARY_FOLDERS]?.toMutableSet() ?: mutableSetOf()
        current.add(uri)
        it[KEY_LIBRARY_FOLDERS] = current
    }
    suspend fun removeLibraryFolder(uri: String) = dataStore.edit {
        val current = it[KEY_LIBRARY_FOLDERS]?.toMutableSet() ?: mutableSetOf()
        current.remove(uri)
        it[KEY_LIBRARY_FOLDERS] = current
    }
    suspend fun setOnboardingCompleted(completed: Boolean) = dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = completed }
    suspend fun setCompactPlayer(enabled: Boolean) = dataStore.edit { it[KEY_COMPACT_PLAYER] = enabled }
    suspend fun setAutoMarkFinished(enabled: Boolean) = dataStore.edit { it[KEY_AUTO_MARK_FINISHED] = enabled }
    suspend fun setBackupFolderUri(uri: String?) = dataStore.edit {
        if (uri != null) it[KEY_BACKUP_FOLDER_URI] = uri else it.remove(KEY_BACKUP_FOLDER_URI)
    }
    suspend fun setLastBackupTimestamp(timestamp: Long) = dataStore.edit { it[KEY_LAST_BACKUP_TIMESTAMP] = timestamp }
    suspend fun setAutoPlayNextInSeries(enabled: Boolean) = dataStore.edit { it[KEY_AUTO_PLAY_NEXT_IN_SERIES] = enabled }
    suspend fun setLibraryTab(tab: LibraryTab) = dataStore.edit { it[KEY_LIBRARY_TAB] = tab.name }
    suspend fun setLibraryViewMode(mode: LibraryViewMode) = dataStore.edit { it[KEY_LIBRARY_VIEW_MODE] = mode.name }
    suspend fun setLibrarySortBy(sort: LibrarySortBy) = dataStore.edit { it[KEY_LIBRARY_SORT_BY] = sort.name }
    suspend fun setLibrarySortAscending(ascending: Boolean) = dataStore.edit { it[KEY_LIBRARY_SORT_ASCENDING] = ascending }
    suspend fun setFilterStatus(statuses: Set<String>) = dataStore.edit { it[KEY_FILTER_STATUS] = statuses }
    suspend fun setFilterCollectionId(collectionId: Long?) = dataStore.edit {
        if (collectionId != null) it[KEY_FILTER_COLLECTION_ID] = collectionId else it.remove(KEY_FILTER_COLLECTION_ID)
    }
    suspend fun setFilterSeriesOnly(enabled: Boolean) = dataStore.edit { it[KEY_FILTER_SERIES_ONLY] = enabled }
    suspend fun setFilterUnavailableOnly(enabled: Boolean) = dataStore.edit { it[KEY_FILTER_UNAVAILABLE_ONLY] = enabled }
    suspend fun clearFilters() = dataStore.edit {
        it.remove(KEY_FILTER_STATUS)
        it.remove(KEY_FILTER_COLLECTION_ID)
        it.remove(KEY_FILTER_SERIES_ONLY)
        it.remove(KEY_FILTER_UNAVAILABLE_ONLY)
    }
}
