package com.evenfall.app

import android.service.quicksettings.Tile
import com.evenfall.app.data.db.entity.BookEntity
import com.evenfall.app.shortcuts.EvenfallShortcutManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortcutsAndTileTest {

    @Test
    fun testShortcutConstants() {
        assertEquals("com.evenfall.app.action.CONTINUE_BOOK", EvenfallShortcutManager.ACTION_CONTINUE_BOOK)
        assertEquals("com.evenfall.app.action.OPEN_LIBRARY", EvenfallShortcutManager.ACTION_OPEN_LIBRARY)
        assertEquals("com.evenfall.app.extra.BOOK_ID", EvenfallShortcutManager.EXTRA_BOOK_ID)
        assertEquals("shortcut_continue_book", EvenfallShortcutManager.ID_CONTINUE_BOOK)
        assertEquals("shortcut_library", EvenfallShortcutManager.ID_LIBRARY)
    }

    @Test
    fun testTitleTruncationForShortLabel() {
        val longTitle = "The Count of Monte Cristo: Unabridged Edition"
        val shortTitle = if (longTitle.length > 18) longTitle.take(15) + "…" else longTitle
        assertEquals("The Count of Mo…", shortTitle)
        assertTrue(shortTitle.length <= 18)

        val regularTitle = "Dune"
        val shortRegular = if (regularTitle.length > 18) regularTitle.take(15) + "…" else regularTitle
        assertEquals("Dune", shortRegular)
    }

    @Test
    fun testTileStateComputation() {
        data class TileState(val state: Int, val label: String, val subtitle: String)

        fun computeTile(isPlaying: Boolean, currentBookTitle: String?): TileState {
            return if (isPlaying) {
                TileState(
                    state = Tile.STATE_ACTIVE,
                    label = "Pause Evenfall",
                    subtitle = currentBookTitle ?: "Evenfall"
                )
            } else {
                TileState(
                    state = Tile.STATE_INACTIVE,
                    label = "Resume Evenfall",
                    subtitle = currentBookTitle ?: "No audiobook loaded"
                )
            }
        }

        val active = computeTile(isPlaying = true, currentBookTitle = "Project Hail Mary")
        assertEquals(Tile.STATE_ACTIVE, active.state)
        assertEquals("Pause Evenfall", active.label)
        assertEquals("Project Hail Mary", active.subtitle)

        val paused = computeTile(isPlaying = false, currentBookTitle = "Project Hail Mary")
        assertEquals(Tile.STATE_INACTIVE, paused.state)
        assertEquals("Resume Evenfall", paused.label)
        assertEquals("Project Hail Mary", paused.subtitle)

        val empty = computeTile(isPlaying = false, currentBookTitle = null)
        assertEquals(Tile.STATE_INACTIVE, empty.state)
        assertEquals("Resume Evenfall", empty.label)
        assertEquals("No audiobook loaded", empty.subtitle)
    }
}
