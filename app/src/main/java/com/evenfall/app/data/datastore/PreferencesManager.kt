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

        val KEY_HEADSET_DOUBLE_PRESS = stringPreferencesKey("headset_double_press_action")
        val KEY_HEADSET_TRIPLE_PRESS = stringPreferencesKey("headset_triple_press_action")
        val KEY_HEADSET_LONG_PRESS = stringPreferencesKey("headset_long_press_action")

        val KEY_AUTO_RESUME_BT_ENABLED = booleanPreferencesKey("auto_resume_bt_enabled")
        val KEY_AUTO_RESUME_BT_DEVICES = stringSetPreferencesKey("auto_resume_bt_devices")

        val KEY_BEDTIME_ENABLED = booleanPreferencesKey("bedtime_timer_enabled")
        val KEY_BEDTIME_START_HOUR = intPreferencesKey("bedtime_start_hour")
        val KEY_BEDTIME_START_MINUTE = intPreferencesKey("bedtime_start_minute")
        val KEY_BEDTIME_DURATION_MINUTES = intPreferencesKey("bedtime_duration_minutes")
        val KEY_BEDTIME_DAYS = stringSetPreferencesKey("bedtime_days")
        val KEY_BEDTIME_AUTO_START = booleanPreferencesKey("bedtime_auto_start")

        val KEY_SMART_RESUME_TIER1 = intPreferencesKey("smart_resume_tier1")
        val KEY_SMART_RESUME_TIER2 = intPreferencesKey("smart_resume_tier2")
        val KEY_SMART_RESUME_TIER3 = intPreferencesKey("smart_resume_tier3")
        val KEY_SMART_RESUME_TIER4 = intPreferencesKey("smart_resume_tier4")
        val KEY_SMART_RESUME_TIER5 = intPreferencesKey("smart_resume_tier5")

        val KEY_PLAYER_GESTURES_ENABLED = booleanPreferencesKey("player_gestures_enabled")
        val KEY_GLOBAL_MONO_DOWNMIX = booleanPreferencesKey("global_mono_downmix")
        val KEY_GLOBAL_LOUDNESS_NORMALIZATION = booleanPreferencesKey("global_loudness_normalization")
        val KEY_GLOBAL_VOICE_BOOST_PRESET = stringPreferencesKey("global_voice_boost_preset")

        val KEY_STATS_ENABLED = booleanPreferencesKey("listening_stats_enabled")
        val KEY_HAPTIC_FEEDBACK_ENABLED = booleanPreferencesKey("haptic_feedback_enabled")

        val KEY_HINT_LONG_PRESS_SKIP_SHOWN = booleanPreferencesKey("hint_long_press_skip_shown")
        val KEY_HINT_BOOKMARK_SHOWN = booleanPreferencesKey("hint_bookmark_shown")
        val KEY_HINT_MINI_PLAYER_SWIPE_SHOWN = booleanPreferencesKey("hint_mini_player_swipe_shown")
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

    val headsetDoublePressAction: Flow<String> = dataStore.data.map { it[KEY_HEADSET_DOUBLE_PRESS] ?: "SKIP_FORWARD" }
    val headsetTriplePressAction: Flow<String> = dataStore.data.map { it[KEY_HEADSET_TRIPLE_PRESS] ?: "SKIP_BACK" }
    val headsetLongPressAction: Flow<String> = dataStore.data.map { it[KEY_HEADSET_LONG_PRESS] ?: "TOGGLE_SLEEP_TIMER" }

    val autoResumeBluetoothEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_AUTO_RESUME_BT_ENABLED] ?: false }
    val autoResumeBluetoothDevices: Flow<Set<String>> = dataStore.data.map { it[KEY_AUTO_RESUME_BT_DEVICES] ?: emptySet() }

    val bedtimeEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_BEDTIME_ENABLED] ?: false }
    val bedtimeStartHour: Flow<Int> = dataStore.data.map { it[KEY_BEDTIME_START_HOUR] ?: 22 }
    val bedtimeStartMinute: Flow<Int> = dataStore.data.map { it[KEY_BEDTIME_START_MINUTE] ?: 0 }
    val bedtimeDurationMinutes: Flow<Int> = dataStore.data.map { it[KEY_BEDTIME_DURATION_MINUTES] ?: 30 }
    val bedtimeDays: Flow<Set<String>> = dataStore.data.map { it[KEY_BEDTIME_DAYS] ?: setOf("1", "2", "3", "4", "5", "6", "7") }
    val bedtimeAutoStart: Flow<Boolean> = dataStore.data.map { it[KEY_BEDTIME_AUTO_START] ?: true }

    val smartResumeTier1Sec: Flow<Int> = dataStore.data.map { it[KEY_SMART_RESUME_TIER1] ?: 0 }
    val smartResumeTier2Sec: Flow<Int> = dataStore.data.map { it[KEY_SMART_RESUME_TIER2] ?: 5 }
    val smartResumeTier3Sec: Flow<Int> = dataStore.data.map { it[KEY_SMART_RESUME_TIER3] ?: 15 }
    val smartResumeTier4Sec: Flow<Int> = dataStore.data.map { it[KEY_SMART_RESUME_TIER4] ?: 30 }
    val smartResumeTier5Sec: Flow<Int> = dataStore.data.map { it[KEY_SMART_RESUME_TIER5] ?: 60 }

    val playerGesturesEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_PLAYER_GESTURES_ENABLED] ?: true }
    val globalMonoDownmix: Flow<Boolean> = dataStore.data.map { it[KEY_GLOBAL_MONO_DOWNMIX] ?: false }
    val globalLoudnessNormalization: Flow<Boolean> = dataStore.data.map { it[KEY_GLOBAL_LOUDNESS_NORMALIZATION] ?: false }
    val globalVoiceBoostPreset: Flow<String> = dataStore.data.map { it[KEY_GLOBAL_VOICE_BOOST_PRESET] ?: "Off" }
    val statsEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_STATS_ENABLED] ?: true }
    val hapticFeedbackEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_HAPTIC_FEEDBACK_ENABLED] ?: true }

    val hintLongPressSkipShown: Flow<Boolean> = dataStore.data.map { it[KEY_HINT_LONG_PRESS_SKIP_SHOWN] ?: false }
    val hintBookmarkShown: Flow<Boolean> = dataStore.data.map { it[KEY_HINT_BOOKMARK_SHOWN] ?: false }
    val hintMiniPlayerSwipeShown: Flow<Boolean> = dataStore.data.map { it[KEY_HINT_MINI_PLAYER_SWIPE_SHOWN] ?: false }

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

    suspend fun setHeadsetDoublePressAction(action: String) = dataStore.edit { it[KEY_HEADSET_DOUBLE_PRESS] = action }
    suspend fun setHeadsetTriplePressAction(action: String) = dataStore.edit { it[KEY_HEADSET_TRIPLE_PRESS] = action }
    suspend fun setHeadsetLongPressAction(action: String) = dataStore.edit { it[KEY_HEADSET_LONG_PRESS] = action }

    suspend fun setAutoResumeBluetoothEnabled(enabled: Boolean) = dataStore.edit { it[KEY_AUTO_RESUME_BT_ENABLED] = enabled }
    suspend fun setAutoResumeBluetoothDevices(devices: Set<String>) = dataStore.edit { it[KEY_AUTO_RESUME_BT_DEVICES] = devices }

    suspend fun setBedtimeEnabled(enabled: Boolean) = dataStore.edit { it[KEY_BEDTIME_ENABLED] = enabled }
    suspend fun setBedtimeStartHour(hour: Int) = dataStore.edit { it[KEY_BEDTIME_START_HOUR] = hour }
    suspend fun setBedtimeStartMinute(minute: Int) = dataStore.edit { it[KEY_BEDTIME_START_MINUTE] = minute }
    suspend fun setBedtimeDurationMinutes(minutes: Int) = dataStore.edit { it[KEY_BEDTIME_DURATION_MINUTES] = minutes }
    suspend fun setBedtimeDays(days: Set<String>) = dataStore.edit { it[KEY_BEDTIME_DAYS] = days }
    suspend fun setBedtimeAutoStart(autoStart: Boolean) = dataStore.edit { it[KEY_BEDTIME_AUTO_START] = autoStart }

    suspend fun setSmartResumeTiers(tier1: Int, tier2: Int, tier3: Int, tier4: Int, tier5: Int) = dataStore.edit {
        it[KEY_SMART_RESUME_TIER1] = tier1
        it[KEY_SMART_RESUME_TIER2] = tier2
        it[KEY_SMART_RESUME_TIER3] = tier3
        it[KEY_SMART_RESUME_TIER4] = tier4
        it[KEY_SMART_RESUME_TIER5] = tier5
    }

    suspend fun setPlayerGesturesEnabled(enabled: Boolean) = dataStore.edit { it[KEY_PLAYER_GESTURES_ENABLED] = enabled }
    suspend fun setGlobalMonoDownmix(enabled: Boolean) = dataStore.edit { it[KEY_GLOBAL_MONO_DOWNMIX] = enabled }
    suspend fun setGlobalLoudnessNormalization(enabled: Boolean) = dataStore.edit { it[KEY_GLOBAL_LOUDNESS_NORMALIZATION] = enabled }
    suspend fun setGlobalVoiceBoostPreset(preset: String) = dataStore.edit { it[KEY_GLOBAL_VOICE_BOOST_PRESET] = preset }
    suspend fun setStatsEnabled(enabled: Boolean) = dataStore.edit { it[KEY_STATS_ENABLED] = enabled }
    suspend fun setHapticFeedbackEnabled(enabled: Boolean) = dataStore.edit { it[KEY_HAPTIC_FEEDBACK_ENABLED] = enabled }

    suspend fun setHintLongPressSkipShown(shown: Boolean = true) = dataStore.edit { it[KEY_HINT_LONG_PRESS_SKIP_SHOWN] = shown }
    suspend fun setHintBookmarkShown(shown: Boolean = true) = dataStore.edit { it[KEY_HINT_BOOKMARK_SHOWN] = shown }
    suspend fun setHintMiniPlayerSwipeShown(shown: Boolean = true) = dataStore.edit { it[KEY_HINT_MINI_PLAYER_SWIPE_SHOWN] = shown }
    suspend fun resetFirstRunHints() = dataStore.edit {
        it[KEY_HINT_LONG_PRESS_SKIP_SHOWN] = false
        it[KEY_HINT_BOOKMARK_SHOWN] = false
        it[KEY_HINT_MINI_PLAYER_SWIPE_SHOWN] = false
    }
}
