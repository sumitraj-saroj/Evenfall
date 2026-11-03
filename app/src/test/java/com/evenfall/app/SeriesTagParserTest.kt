package com.evenfall.app

import com.evenfall.app.data.parser.SeriesTagParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class SeriesTagParserTest {

    @Test
    fun testHashPattern() {
        val result = SeriesTagParser.parseFromText("The Stormlight Archive #1", null)
        assertNotNull(result)
        assertEquals("The Stormlight Archive", result!!.name)
        assertEquals(1.0f, result.order)
    }

    @Test
    fun testCommaBookPattern() {
        val result = SeriesTagParser.parseFromText("Mistborn, Book 2", null)
        assertNotNull(result)
        assertEquals("Mistborn", result!!.name)
        assertEquals(2.0f, result.order)
    }

    @Test
    fun testDashBookPattern() {
        val result = SeriesTagParser.parseFromText("Wheel of Time - Book 3", null)
        assertNotNull(result)
        assertEquals("Wheel of Time", result!!.name)
        assertEquals(3.0f, result.order)
    }

    @Test
    fun testColonBookPattern() {
        val result = SeriesTagParser.parseFromText("The Expanse: Book 4", null)
        assertNotNull(result)
        assertEquals("The Expanse", result!!.name)
        assertEquals(4.0f, result.order)
    }

    @Test
    fun testParenthesisSeriesPattern() {
        val result = SeriesTagParser.parseFromText("Dune (Dune Chronicles #1)", null)
        assertNotNull(result)
        assertEquals("Dune", result!!.name)
        assertEquals(1.0f, result.order)
    }

    @Test
    fun testVolumePattern() {
        val result = SeriesTagParser.parseFromText("Cradle Vol. 8", null)
        assertNotNull(result)
        assertEquals("Cradle", result!!.name)
        assertEquals(8.0f, result.order)
    }

    @Test
    fun testDecimalNovellaOrder() {
        val result = SeriesTagParser.parseFromText("Edgedancer, Book 2.5", null)
        assertNotNull(result)
        assertEquals("Edgedancer", result!!.name)
        assertEquals(2.5f, result.order)
    }

    @Test
    fun testPrefixSeriesPattern() {
        val result = SeriesTagParser.parseFromText("Series: Harry Potter, Book 5", null)
        assertNotNull(result)
        assertEquals("Harry Potter", result!!.name)
        assertEquals(5.0f, result.order)
    }

    @Test
    fun testAlbumPrecedenceOverTitle() {
        val result = SeriesTagParser.parseFromText(
            album = "The Dark Tower #3",
            title = "The Waste Lands, Book 99"
        )
        assertNotNull(result)
        assertEquals("The Dark Tower", result!!.name)
        assertEquals(3.0f, result.order)
    }

    @Test
    fun testFallbackToTitleWhenAlbumNullOrUnrelated() {
        val result = SeriesTagParser.parseFromText(
            album = "Audiobook",
            title = "Foundation, Book 1"
        )
        assertNotNull(result)
        assertEquals("Foundation", result!!.name)
        assertEquals(1.0f, result.order)
    }

    @Test
    fun testNonSeriesReturnsNull() {
        assertNull(SeriesTagParser.parseFromText(null, null))
        assertNull(SeriesTagParser.parseFromText("Random Non Series Album", "Regular Title"))
        assertNull(SeriesTagParser.parseFromText("", ""))
    }

    @Test
    fun testMp4StreamParsingWithMvnmAndMvinAtoms() {
        val out = ByteArrayOutputStream()
        // Dummy prefix
        out.write(ByteArray(32))

        // Write mvnm atom with child data atom
        val seriesName = "The Lord of the Rings"
        val nameBytes = seriesName.toByteArray(Charsets.UTF_8)
        val dataAtomLen = 16 + nameBytes.size
        val mvnmAtomLen = 8 + dataAtomLen

        // mvnm header
        out.write(byteArrayOf((mvnmAtomLen shr 24).toByte(), (mvnmAtomLen shr 16).toByte(), (mvnmAtomLen shr 8).toByte(), mvnmAtomLen.toByte()))
        out.write("mvnm".toByteArray(Charsets.US_ASCII))
        // data atom header
        out.write(byteArrayOf((dataAtomLen shr 24).toByte(), (dataAtomLen shr 16).toByte(), (dataAtomLen shr 8).toByte(), dataAtomLen.toByte()))
        out.write("data".toByteArray(Charsets.US_ASCII))
        out.write(ByteArray(8)) // flags + locale
        out.write(nameBytes)

        // Write mvin atom (part 2)
        val mvinDataLen = 16 + 2 // 2-byte short
        val mvinAtomLen = 8 + mvinDataLen
        out.write(byteArrayOf((mvinAtomLen shr 24).toByte(), (mvinAtomLen shr 16).toByte(), (mvinAtomLen shr 8).toByte(), mvinAtomLen.toByte()))
        out.write("mvin".toByteArray(Charsets.US_ASCII))
        out.write(byteArrayOf((mvinDataLen shr 24).toByte(), (mvinDataLen shr 16).toByte(), (mvinDataLen shr 8).toByte(), mvinDataLen.toByte()))
        out.write("data".toByteArray(Charsets.US_ASCII))
        out.write(ByteArray(8)) // flags + locale
        out.write(byteArrayOf(0x00, 0x02)) // Part 2

        val parsed = SeriesTagParser.parseFromMp4Stream(ByteArrayInputStream(out.toByteArray()))
        assertNotNull(parsed)
        assertEquals("The Lord of the Rings", parsed!!.name)
        assertEquals(2.0f, parsed.order)
    }
}
