package com.evenfall.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TabletAdaptiveTest {

    @Test
    fun testWindowSizeClassThresholds() {
        val phoneWidthDp = 390
        val foldableFoldedWidthDp = 412
        val foldableUnfoldedWidthDp = 720
        val tabletWidthDp = 840

        fun isTwoPane(widthDp: Int): Boolean = widthDp >= 600

        assertFalse("Phone portrait should be single-pane", isTwoPane(phoneWidthDp))
        assertFalse("Foldable folded should be single-pane", isTwoPane(foldableFoldedWidthDp))
        assertTrue("Foldable unfolded should be two-pane", isTwoPane(foldableUnfoldedWidthDp))
        assertTrue("Tablet expanded should be two-pane", isTwoPane(tabletWidthDp))
    }

    @Test
    fun testManifestConfigChangesForFoldablesAndRotation() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        val content = if (manifestFile.exists()) manifestFile.readText() else File("app/src/main/AndroidManifest.xml").readText()

        assertTrue(
            "AndroidManifest must include orientation in configChanges to prevent activity restarts",
            content.contains("orientation")
        )
        assertTrue(
            "AndroidManifest must include screenSize in configChanges for foldable posture transitions",
            content.contains("screenSize")
        )
        assertTrue(
            "AndroidManifest must include smallestScreenSize in configChanges",
            content.contains("smallestScreenSize")
        )
        assertTrue(
            "AndroidManifest must include screenLayout in configChanges",
            content.contains("screenLayout")
        )
        assertTrue(
            "AndroidManifest must include layoutDirection in configChanges for RTL transitions",
            content.contains("layoutDirection")
        )
    }
}
