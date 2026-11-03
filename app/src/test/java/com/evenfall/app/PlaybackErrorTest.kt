package com.evenfall.app

import com.evenfall.app.data.db.entity.BookEntity
import com.evenfall.app.playback.PlaybackState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackErrorTest {

    @Test
    fun testPlaybackStateDefaultErrorState() {
        val state = PlaybackState()
        assertFalse(state.isPlaybackError)
        assertNull(state.playbackErrorMessage)
    }

    @Test
    fun testPlaybackStateWithError() {
        val book = BookEntity(
            id = 1L,
            title = "Test Book",
            author = "Test Author",
            folderUri = "content://books",
            folderIdentifier = "test_folder",
            currentPositionMs = 120_000L,
            chapterPositionMs = 30_000L,
            totalDurationMs = 500_000L
        )

        val stateWithError = PlaybackState(
            currentBook = book,
            isPlaying = false,
            totalPositionMs = 120_000L,
            chapterPositionMs = 30_000L,
            isPlaybackError = true,
            playbackErrorMessage = "The audio file for this chapter could not be accessed."
        )

        assertTrue(stateWithError.isPlaybackError)
        assertNotNull(stateWithError.playbackErrorMessage)
        assertFalse(stateWithError.isPlaying)
        // Position must never be lost
        assertEquals(120_000L, stateWithError.totalPositionMs)
        assertEquals(30_000L, stateWithError.chapterPositionMs)
        assertEquals(book, stateWithError.currentBook)
    }

    @Test
    fun testDismissPlaybackError() {
        val stateWithError = PlaybackState(
            isPlaybackError = true,
            playbackErrorMessage = "Error reading audio file"
        )

        val dismissedState = stateWithError.copy(
            isPlaybackError = false,
            playbackErrorMessage = null
        )

        assertFalse(dismissedState.isPlaybackError)
        assertNull(dismissedState.playbackErrorMessage)
    }
}
