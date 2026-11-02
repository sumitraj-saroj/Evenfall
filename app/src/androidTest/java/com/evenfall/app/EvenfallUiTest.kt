package com.evenfall.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.evenfall.app.data.db.entity.BookEntity
import com.evenfall.app.ui.components.MorphingPlayPauseButton
import com.evenfall.app.ui.screens.home.ContinueListeningHeroCard
import com.evenfall.app.ui.theme.EvenfallTheme
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
}
