package com.evenfall.app

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.evenfall.app.data.datastore.LibrarySortBy
import com.evenfall.app.data.datastore.LibraryTab
import com.evenfall.app.data.datastore.LibraryViewMode
import com.evenfall.app.data.datastore.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SortFilterPersistenceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testSortFilterAndViewModeSurviveRestarts() = runBlocking {
        val testFile = tempFolder.newFile("test_prefs.preferences_pb")

        // Session 1: User changes tab, viewMode, sort, and filters
        val job1 = Job()
        val scope1 = CoroutineScope(Dispatchers.IO + job1)
        val dataStore1 = PreferenceDataStoreFactory.create(
            scope = scope1,
            produceFile = { testFile }
        )
        val prefsManager1 = PreferencesManager(dataStore1)

        // Set non-default values
        prefsManager1.setLibraryTab(LibraryTab.SERIES)
        prefsManager1.setLibraryViewMode(LibraryViewMode.GRID)
        prefsManager1.setLibrarySortBy(LibrarySortBy.AUTHOR)
        prefsManager1.setLibrarySortAscending(true)
        prefsManager1.setFilterStatus(setOf("IN_PROGRESS", "FINISHED"))
        prefsManager1.setFilterCollectionId(42L)
        prefsManager1.setFilterSeriesOnly(true)
        prefsManager1.setFilterUnavailableOnly(false)
        prefsManager1.setAutoPlayNextInSeries(true)

        // Verify Session 1 values
        assertEquals(LibraryTab.SERIES, prefsManager1.libraryTab.first())
        assertEquals(LibraryViewMode.GRID, prefsManager1.libraryViewMode.first())
        assertEquals(LibrarySortBy.AUTHOR, prefsManager1.librarySortBy.first())
        assertTrue(prefsManager1.librarySortAscending.first())
        assertEquals(setOf("IN_PROGRESS", "FINISHED"), prefsManager1.filterStatus.first())
        assertEquals(42L, prefsManager1.filterCollectionId.first())
        assertTrue(prefsManager1.filterSeriesOnly.first())
        assertFalse(prefsManager1.filterUnavailableOnly.first())
        assertTrue(prefsManager1.autoPlayNextInSeries.first())

        // Close Session 1 by cancelling and waiting for completion
        job1.cancelAndJoin()

        // Session 2: App restarts (new PreferencesManager reading from the same persisted file)
        val job2 = Job()
        val scope2 = CoroutineScope(Dispatchers.IO + job2)
        val dataStore2 = PreferenceDataStoreFactory.create(
            scope = scope2,
            produceFile = { testFile }
        )
        val prefsManager2 = PreferencesManager(dataStore2)

        // Acceptance criteria: Sort, filter, and view-mode state survives restarts.
        assertEquals(LibraryTab.SERIES, prefsManager2.libraryTab.first())
        assertEquals(LibraryViewMode.GRID, prefsManager2.libraryViewMode.first())
        assertEquals(LibrarySortBy.AUTHOR, prefsManager2.librarySortBy.first())
        assertTrue(prefsManager2.librarySortAscending.first())
        assertEquals(setOf("IN_PROGRESS", "FINISHED"), prefsManager2.filterStatus.first())
        assertEquals(42L, prefsManager2.filterCollectionId.first())
        assertTrue(prefsManager2.filterSeriesOnly.first())
        assertFalse(prefsManager2.filterUnavailableOnly.first())
        assertTrue(prefsManager2.autoPlayNextInSeries.first())

        job2.cancelAndJoin()
    }
}
