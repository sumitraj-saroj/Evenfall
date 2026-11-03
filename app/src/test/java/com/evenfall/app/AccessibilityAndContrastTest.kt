package com.evenfall.app

import androidx.compose.ui.graphics.Color
import com.evenfall.app.ui.screens.player.PlayerBookColors
import com.evenfall.app.ui.screens.player.PlayerThemeExtractor
import com.evenfall.app.util.TimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AccessibilityAndContrastTest {

    @Test
    fun testAccessibleDurationFormatting() {
        assertEquals("1 second", TimeUtils.formatAccessibleDuration(1_000L))
        assertEquals("30 seconds", TimeUtils.formatAccessibleDuration(30_000L))
        assertEquals("1 minute 15 seconds", TimeUtils.formatAccessibleDuration(75_000L))
        assertEquals("14 minutes 20 seconds", TimeUtils.formatAccessibleDuration(860_000L))
        assertEquals("42 minutes", TimeUtils.formatAccessibleDuration(42 * 60 * 1000L))
        assertEquals("1 hour 15 minutes 30 seconds", TimeUtils.formatAccessibleDuration((75 * 60 + 30) * 1000L))
    }

    @Test
    fun testWcagContrastCalculations() {
        // Black and White is maximum 21:1
        val white = Color.White
        val black = Color.Black
        val ratioMax = PlayerThemeExtractor.calculateContrastRatio(white, black)
        assertTrue("White on black should exceed 20:1", ratioMax >= 20.0)

        // Equal colors have 1:1
        val ratioSame = PlayerThemeExtractor.calculateContrastRatio(Color.Gray, Color.Gray)
        assertEquals(1.0, ratioSame, 0.05)
    }

    @Test
    fun testDynamicPaletteContrastGuard() {
        val lightThemeColors = PlayerThemeExtractor.getColorsForBook("Echoes of Tomorrow", isDark = false)
        val darkThemeColors = PlayerThemeExtractor.getColorsForBook("Echoes of Tomorrow", isDark = true)

        // Play button content against container should be >= 4.5:1 (WCAG AA body/icon)
        val playRatioLight = PlayerThemeExtractor.calculateContrastRatio(
            lightThemeColors.playButtonContent,
            lightThemeColors.playButtonContainer
        )
        assertTrue(
            "Play button content must meet WCAG AA (>= 4.5:1), got $playRatioLight",
            playRatioLight >= 4.5
        )

        val playRatioDark = PlayerThemeExtractor.calculateContrastRatio(
            darkThemeColors.playButtonContent,
            darkThemeColors.playButtonContainer
        )
        assertTrue(
            "Play button content in dark mode must meet WCAG AA (>= 4.5:1), got $playRatioDark",
            playRatioDark >= 4.5
        )

        // Low contrast fallback test
        val lowContrastFg = Color(0xFF101828)
        val lowContrastBg = Color(0xFF111827) // nearly identical dark gray
        val guardedFg = PlayerThemeExtractor.ensureContrast(lowContrastFg, lowContrastBg, minRatio = 4.5)

        val adjustedRatio = PlayerThemeExtractor.calculateContrastRatio(guardedFg, lowContrastBg)
        assertTrue(
            "Contrast guard must adjust color to satisfy >= 4.5:1, got $adjustedRatio",
            adjustedRatio >= 4.5
        )
    }

    @Test
    fun testStringResourcesCoverage() {
        // Verify key accessibility and localization strings exist in strings.xml
        val stringsFile = File("src/main/res/values/strings.xml")
        val content = if (stringsFile.exists()) stringsFile.readText() else File("app/src/main/res/values/strings.xml").readText()

        val expectedKeys = listOf(
            "empty_library_title",
            "empty_permission_title",
            "empty_search_title",
            "empty_bookmarks_title",
            "empty_history_title",
            "error_playback_title",
            "hint_long_press_skip",
            "hint_bookmark",
            "hint_mini_player_swipe",
            "reset_tips_title",
            "reset_tips_action"
        )

        for (key in expectedKeys) {
            assertTrue("strings.xml must contain string resource '$key'", content.contains("name=\"$key\""))
        }
    }
}
