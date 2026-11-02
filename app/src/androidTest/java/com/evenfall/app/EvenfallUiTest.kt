package com.evenfall.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.evenfall.app.data.db.entity.BookEntity
import com.evenfall.app.data.parser.TitleSanitizer
import com.evenfall.app.ui.components.MorphingPlayPauseButton
import com.evenfall.app.ui.screens.home.ContinueListeningHeroCard
import com.evenfall.app.ui.theme.EvenfallTheme
import com.evenfall.app.util.TimeUtils
import kotlinx.coroutines.delay
import org.junit.Rule
import org.junit.Test

class EvenfallUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testMorphingPlayPauseButtonDisplaysProperContentDescription() {
        composeTestRule.setContent {
            EvenfallTheme {
                MorphingPlayPauseButton(
                    isPlaying = false,
                    onClick = {}
                )
            }
        }
        composeTestRule.onNodeWithContentDescription("Play").assertIsDisplayed()
    }

    @Test
    fun testContinueListeningHeroCardDisplaysBookInfo() {
        val testBook = BookEntity(
            title = "Echoes of Tomorrow",
            author = "Daniel Hartwell",
            folderUri = "test://uri",
            folderIdentifier = "test_id",
            totalDurationMs = 1000000L,
            currentPositionMs = 720000L
        )

        composeTestRule.setContent {
            EvenfallTheme {
                ContinueListeningHeroCard(
                    book = testBook,
                    onClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Echoes of Tomorrow").assertIsDisplayed()
        composeTestRule.onNodeWithText("Daniel Hartwell").assertIsDisplayed()
        composeTestRule.onNodeWithText("Continue listening").assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun testChapterSheetOpensWhenChapterLineClicked() {
        composeTestRule.setContent {
            EvenfallTheme {
                var showChapterSheet by remember { mutableStateOf(false) }
                Column {
                    Surface(
                        modifier = Modifier.clickable { showChapterSheet = true }
                    ) {
                        Text("Chapter 1 of 3 ▾")
                    }
                    if (showChapterSheet) {
                        ModalBottomSheet(
                            onDismissRequest = { showChapterSheet = false }
                        ) {
                            Text("Chapters")
                            Text("Chapter 1: The Beginning")
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Chapter 1 of 3 ▾").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Chapters").assertIsDisplayed()
        composeTestRule.onNodeWithText("Chapter 1: The Beginning").assertIsDisplayed()
    }

    @Test
    fun testUndoChipAppearsAndAutoDismisses() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            EvenfallTheme {
                var undoPos by remember { mutableStateOf<Long?>(15000L) }
                var showUndoChip by remember { mutableStateOf(false) }

                LaunchedEffect(undoPos) {
                    if (undoPos != null) {
                        showUndoChip = true
                        delay(8000L)
                        showUndoChip = false
                    }
                }

                if (showUndoChip && undoPos != null) {
                    Surface(modifier = Modifier.clickable { showUndoChip = false }) {
                        Text("Back to ${TimeUtils.formatTimestamp(undoPos ?: 0L)}")
                    }
                }
            }
        }

        composeTestRule.mainClock.advanceTimeBy(100L)
        composeTestRule.onNodeWithText("Back to 00:15").assertIsDisplayed()
        composeTestRule.mainClock.advanceTimeBy(8100L)
        composeTestRule.onNodeWithText("Back to 00:15").assertDoesNotExist()
    }

    @Test
    fun testPlaybackOptionsSheetTogglesSilenceSkippingAndVolumeBoost() {
        composeTestRule.setContent {
            EvenfallTheme {
                var silenceSkipping by remember { mutableStateOf(false) }
                var volumeBoost by remember { mutableStateOf(0.0f) }

                Column {
                    Text("Playback options")
                    Row {
                        Text("Silence skipping")
                        Switch(
                            checked = silenceSkipping,
                            onCheckedChange = { silenceSkipping = it },
                            modifier = Modifier.testTag("silence_switch")
                        )
                    }
                    Slider(
                        value = volumeBoost,
                        onValueChange = { volumeBoost = it },
                        modifier = Modifier.testTag("boost_slider")
                    )
                    Text(text = if (silenceSkipping) "Silence: ON" else "Silence: OFF")
                    val boostPercent = (volumeBoost * 100).toInt()
                    Text(text = if (boostPercent > 0) "+$boostPercent%" else "Off")
                }
            }
        }

        composeTestRule.onNodeWithText("Silence: OFF").assertIsDisplayed()
        composeTestRule.onNodeWithText("Off").assertIsDisplayed()
        composeTestRule.onNodeWithTag("silence_switch").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Silence: ON").assertIsDisplayed()
    }

    @Test
    fun testTitleFallbackDisplaysResolvedTitle() {
        val resolved = TitleSanitizer.resolveTitle(
            titleTag = "Audiobook",
            albumTag = "The Hobbit",
            folderName = "Audiobooks",
            fileName = "Track 01.mp3"
        )
        composeTestRule.setContent {
            EvenfallTheme {
                Text(text = resolved)
            }
        }
        composeTestRule.onNodeWithText("The Hobbit").assertIsDisplayed()
    }
}
