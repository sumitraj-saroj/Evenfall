package com.evenfall.app

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.evenfall.app.data.datastore.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FirstRunHintsTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testFirstRunHintsLifecycleAndReset() = runBlocking {
        val testFile = tempFolder.newFile("test_hints.preferences_pb")
        val job = Job()
        val scope = CoroutineScope(Dispatchers.IO + job)
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { testFile }
        )
        val prefs = PreferencesManager(dataStore)

        // 1. Initial State: all hints should default to false (not shown yet)
        assertFalse("Long press skip hint should not be shown initially", prefs.hintLongPressSkipShown.first())
        assertFalse("Bookmark hint should not be shown initially", prefs.hintBookmarkShown.first())
        assertFalse("Mini player swipe hint should not be shown initially", prefs.hintMiniPlayerSwipeShown.first())

        // 2. Mark hints as shown/dismissed
        prefs.setHintLongPressSkipShown(true)
        prefs.setHintBookmarkShown(true)
        prefs.setHintMiniPlayerSwipeShown(true)

        assertTrue("Long press skip hint should be marked shown", prefs.hintLongPressSkipShown.first())
        assertTrue("Bookmark hint should be marked shown", prefs.hintBookmarkShown.first())
        assertTrue("Mini player swipe hint should be marked shown", prefs.hintMiniPlayerSwipeShown.first())

        // 3. Reset tips in Settings
        prefs.resetFirstRunHints()

        assertFalse("Long press skip hint should be reset to false", prefs.hintLongPressSkipShown.first())
        assertFalse("Bookmark hint should be reset to false", prefs.hintBookmarkShown.first())
        assertFalse("Mini player swipe hint should be reset to false", prefs.hintMiniPlayerSwipeShown.first())

        job.cancelAndJoin()
    }
}
