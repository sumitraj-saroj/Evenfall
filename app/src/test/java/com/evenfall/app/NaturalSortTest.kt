package com.evenfall.app

import com.evenfall.app.data.parser.AudioMetadataParser
import org.junit.Assert.assertEquals
import org.junit.Test

class NaturalSortTest {

    @Test
    fun testNaturalOrderComparator() {
        val filenames = listOf(
            "Chapter 10.mp3",
            "Chapter 1.mp3",
            "Chapter 2.mp3",
            "Chapter 20.mp3",
            "Chapter 3.mp3"
        )
        val sorted = filenames.sortedWith(AudioMetadataParser.naturalOrderComparator())
        assertEquals(
            listOf(
                "Chapter 1.mp3",
                "Chapter 2.mp3",
                "Chapter 3.mp3",
                "Chapter 10.mp3",
                "Chapter 20.mp3"
            ),
            sorted
        )
    }
}
