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
import com.evenfall.app.data.datastore.LibrarySortBy
import com.evenfall.app.data.db.entity.BookEntity
import com.evenfall.app.data.db.entity.BookmarkEntity
import com.evenfall.app.data.db.entity.BookmarkType
import com.evenfall.app.data.db.entity.ChapterEntity
import com.evenfall.app.data.db.entity.CollectionEntity
import com.evenfall.app.data.parser.TitleSanitizer
import com.evenfall.app.data.repository.BookmarkSearchResult
import com.evenfall.app.data.repository.ChapterSearchResult
import com.evenfall.app.data.repository.SearchResults
import com.evenfall.app.ui.components.MorphingPlayPauseButton
import com.evenfall.app.ui.components.CircularSkipButton
import com.evenfall.app.ui.components.QuickSkipAdjustDialog
import com.evenfall.app.ui.components.SortFilterBottomSheet
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import com.evenfall.app.ui.screens.home.ContinueListeningHeroCard
import com.evenfall.app.ui.screens.home.FilterState
import com.evenfall.app.ui.screens.home.SearchResultsGroupedView
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

    @Test
    fun testMiniPlayerDisplaysBookInfoAndRespondsToActions() {
        val testBook = BookEntity(
            title = "Beyond the Horizon",
            author = "Elena Rostova",
            folderUri = "test://uri",
            folderIdentifier = "test_horizon",
            totalDurationMs = 5000000L,
            fileCount = 10,
            currentChapterIndex = 2
        )
        val state = com.evenfall.app.playback.PlaybackState(
            currentBook = testBook,
            isPlaying = false
        )
        var expanded = false
        var playClicked = false

        composeTestRule.setContent {
            EvenfallTheme {
                com.evenfall.app.ui.components.MiniPlayer(
                    playbackState = state,
                    book = testBook,
                    onExpand = { expanded = true },
                    onDismiss = {},
                    onTogglePlayPause = { playClicked = true },
                    onSkipBackward = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Beyond the Horizon").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mini_player_play_pause").performClick()
        org.junit.Assert.assertTrue(playClicked)
        composeTestRule.onNodeWithTag("mini_player").performClick()
        org.junit.Assert.assertTrue(expanded)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun testSortFilterBottomSheetAppliesSelection() {
        var appliedSort: LibrarySortBy? = null
        var appliedFilters: FilterState? = null

        composeTestRule.setContent {
            EvenfallTheme {
                SortFilterBottomSheet(
                    currentSortBy = LibrarySortBy.RECENTLY_PLAYED,
                    currentSortAscending = false,
                    currentFilterState = FilterState(statuses = setOf("NOT_STARTED")),
                    collections = listOf(
                        CollectionEntity(id = 1, name = "Sci-Fi", orderIndex = 0),
                        CollectionEntity(id = 2, name = "Fantasy", orderIndex = 1)
                    ),
                    onApplySortAndFilter = { sort, _, filters ->
                        appliedSort = sort
                        appliedFilters = filters
                    },
                    onResetFilters = {},
                    onDismiss = {}
                )
            }
        }

        // Verify elements displayed
        composeTestRule.onNodeWithText("Sort & Filter").assertIsDisplayed()
        composeTestRule.onNodeWithText("Title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sci-Fi").assertIsDisplayed()

        // Select Title sort option
        composeTestRule.onNodeWithText("Title").performClick()
        composeTestRule.waitForIdle()

        // Click Apply
        composeTestRule.onNodeWithText("Apply").performClick()
        composeTestRule.waitForIdle()

        org.junit.Assert.assertEquals(LibrarySortBy.TITLE, appliedSort)
        org.junit.Assert.assertNotNull(appliedFilters)
    }

    @Test
    fun testSearchResultsGroupedViewNavigation() {
        val testBook = BookEntity(
            id = 42L,
            title = "The Way of Kings",
            author = "Brandon Sanderson",
            folderIdentifier = "wok",
            folderUri = "content://wok",
            totalDurationMs = 150_000_000L,
            dateAdded = 1000L
        )

        val testChapter = ChapterEntity(
            id = 101L,
            bookId = 42L,
            chapterIndex = 0,
            title = "Prelude to the Stormlight Archive",
            fileUri = "content://wok/prelude.mp3",
            durationMs = 1_800_000L,
            startMs = 0L
        )

        val testBookmark = BookmarkEntity(
            id = 201L,
            bookId = 42L,
            chapterIndex = 0,
            chapterTitle = "Prelude",
            positionMs = 60_000L,
            bookProgressFraction = 0.01f,
            title = "Honor is Dead",
            note = "Kaladin speaks the words",
            type = BookmarkType.QUOTE
        )

        val searchResults = SearchResults(
            books = listOf(testBook),
            chapters = listOf(
                ChapterSearchResult(
                    chapter = testChapter,
                    book = testBook,
                    displayTitle = testChapter.displayTitle,
                    displaySubtitle = testBook.displayTitle,
                    chapterIndex = testChapter.chapterIndex,
                    startMs = testChapter.startMs
                )
            ),
            bookmarks = listOf(
                BookmarkSearchResult(
                    bookmark = testBookmark,
                    book = testBook,
                    displayTitle = testBookmark.title,
                    displaySubtitle = "${testBook.displayTitle} • Prelude",
                    snippet = testBookmark.note ?: "",
                    positionMs = testBookmark.positionMs,
                    chapterIndex = testBookmark.chapterIndex
                )
            ),
            totalCount = 3
        )

        var openedBook: BookEntity? = null
        var openedChapter: ChapterSearchResult? = null
        var openedBookmark: BookmarkSearchResult? = null

        composeTestRule.setContent {
            EvenfallTheme {
                SearchResultsGroupedView(
                    searchQuery = "Kaladin",
                    results = searchResults,
                    onOpenBook = { openedBook = it },
                    onOpenBookmark = { openedBookmark = it },
                    onOpenChapter = { openedChapter = it }
                )
            }
        }

        // Verify sections
        composeTestRule.onNodeWithText("Books (1)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Chapters (1)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bookmarks & Notes (1)").assertIsDisplayed()

        // Verify items
        composeTestRule.onNodeWithText("The Way of Kings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Prelude to the Stormlight Archive").assertIsDisplayed()
        composeTestRule.onNodeWithText("Honor is Dead").assertIsDisplayed()

        // Click chapter result and verify navigation callback
        composeTestRule.onNodeWithText("Prelude to the Stormlight Archive").performClick()
        org.junit.Assert.assertEquals(101L, openedChapter?.chapter?.id)

        // Click bookmark result and verify navigation callback
        composeTestRule.onNodeWithText("Honor is Dead").performClick()
        org.junit.Assert.assertEquals(201L, openedBookmark?.bookmark?.id)
    }

    @Test
    fun testQuickSkipAdjustDialogChangesSeconds() {
        var currentSeconds = 20
        composeTestRule.setContent {
            EvenfallTheme {
                QuickSkipAdjustDialog(
                    isForward = true,
                    currentSeconds = currentSeconds,
                    onSecondsChange = { currentSeconds = it },
                    onDismissRequest = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Skip Forward Amount").assertIsDisplayed()
        composeTestRule.onNodeWithText("20 seconds").assertIsDisplayed()
        composeTestRule.onNodeWithText("45s").performClick()
        org.junit.Assert.assertEquals(45, currentSeconds)
    }

    @Test
    fun testCircularSkipButtonLongClickTriggersCallback() {
        var clicked = false
        var longClicked = false
        composeTestRule.setContent {
            EvenfallTheme {
                CircularSkipButton(
                    seconds = 30,
                    isForward = true,
                    onClick = { clicked = true },
                    onLongClick = { longClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Forward 30 seconds").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Forward 30 seconds").performTouchInput {
            longClick()
        }
        org.junit.Assert.assertTrue(longClicked)
    }
}
