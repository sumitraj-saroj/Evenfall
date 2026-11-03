package com.evenfall.app.data.parser

import android.content.Context
import android.net.Uri
import java.io.InputStream

data class ParsedSeries(
    val name: String,
    val order: Float?
)

object SeriesTagParser {

    private val SERIES_PATTERNS = listOf(
        // "Series: Series Name, Book N"
        Regex("""^Series:\s*(.*?)\s*[,:\-]\s*Book\s*(\d+(?:\.\d+)?)$""", RegexOption.IGNORE_CASE),
        // "Series Name #N" or "Series Name # N"
        Regex("""^(.*?)\s*#\s*(\d+(?:\.\d+)?)$""", RegexOption.IGNORE_CASE),
        // "Series Name (Series Name #N)" or "Title (Series #N)"
        Regex("""^(.*?)\s*\((?:.+?|Series|Book|Vol|Volume)\s*#?\s*(\d+(?:\.\d+)?)\)$""", RegexOption.IGNORE_CASE),
        // "Series Name, Book N" or "Series Name - Book N" or "Series Name: Book N"
        Regex("""^(.*?)\s*[,:\-]\s*Book\s*(\d+(?:\.\d+)?)$""", RegexOption.IGNORE_CASE),
        // "Series Name: Vol. N" or "Series Name - Volume N"
        Regex("""^(.*?)\s*[,:\-]?\s*(?:Vol\.|Volume)\s*(\d+(?:\.\d+)?)$""", RegexOption.IGNORE_CASE),
        // "Series Name - N" (e.g. "Dresden Files - 04")
        Regex("""^(.*?)\s*[-]\s*(\d+(?:\.\d+)?)$""")
    )

    fun parseFromText(album: String?, title: String?): ParsedSeries? {
        // Try album first, as audiobooks most frequently store series in the album tag
        if (!album.isNullOrBlank()) {
            val fromAlbum = matchPatterns(album.trim())
            if (fromAlbum != null) return fromAlbum
        }

        // Try title
        if (!title.isNullOrBlank()) {
            val fromTitle = matchPatterns(title.trim())
            if (fromTitle != null) return fromTitle
        }

        return null
    }

    private fun matchPatterns(text: String): ParsedSeries? {
        for (pattern in SERIES_PATTERNS) {
            val match = pattern.find(text)
            if (match != null) {
                val rawName = match.groupValues[1].trim()
                val rawOrder = match.groupValues[2].toFloatOrNull()
                val cleanName = rawName
                    .replace(Regex("""^Series:\s*""", RegexOption.IGNORE_CASE), "")
                    .trimEnd(',', '-', ':', ' ')
                if (cleanName.isNotBlank() && cleanName.length >= 2) {
                    return ParsedSeries(name = cleanName, order = rawOrder)
                }
            }
        }
        return null
    }

    /**
     * Inspects M4B / MP4 atom stream for mvnm (movement name) and mvin (movement index).
     */
    fun parseFromMp4Stream(inputStream: InputStream): ParsedSeries? {
        return try {
            val maxBytes = 512 * 1024 // Read up to first 512KB
            val buffer = ByteArray(maxBytes)
            var bytesRead = 0
            while (bytesRead < maxBytes) {
                val r = inputStream.read(buffer, bytesRead, maxBytes - bytesRead)
                if (r <= 0) break
                bytesRead += r
            }

            var seriesName: String? = null
            var seriesOrder: Float? = null

            var offset = 0
            while (offset + 8 <= bytesRead) {
                // Check for mvnm (series/movement name)
                if (buffer[offset] == 'm'.code.toByte() &&
                    buffer[offset + 1] == 'v'.code.toByte() &&
                    buffer[offset + 2] == 'n'.code.toByte() &&
                    buffer[offset + 3] == 'm'.code.toByte()
                ) {
                    val nameStr = extractStringFromDataAtom(buffer, offset + 4, bytesRead)
                    if (!nameStr.isNullOrBlank()) {
                        seriesName = nameStr.trim()
                    }
                }
                // Check for mvin (series/movement index)
                if (buffer[offset] == 'm'.code.toByte() &&
                    buffer[offset + 1] == 'v'.code.toByte() &&
                    buffer[offset + 2] == 'i'.code.toByte() &&
                    buffer[offset + 3] == 'n'.code.toByte()
                ) {
                    val orderNum = extractNumberFromDataAtom(buffer, offset + 4, bytesRead)
                    if (orderNum != null) {
                        seriesOrder = orderNum
                    }
                }
                offset++
            }

            if (seriesName != null) {
                ParsedSeries(seriesName, seriesOrder)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractStringFromDataAtom(buffer: ByteArray, startOffset: Int, maxBytes: Int): String? {
        var pos = startOffset
        while (pos + 16 <= maxBytes && pos < startOffset + 32) {
            if (buffer[pos + 4] == 'd'.code.toByte() &&
                buffer[pos + 5] == 'a'.code.toByte() &&
                buffer[pos + 6] == 't'.code.toByte() &&
                buffer[pos + 7] == 'a'.code.toByte()
            ) {
                val dataLen = ((buffer[pos].toInt() and 0xFF) shl 24) or
                        ((buffer[pos + 1].toInt() and 0xFF) shl 16) or
                        ((buffer[pos + 2].toInt() and 0xFF) shl 8) or
                        (buffer[pos + 3].toInt() and 0xFF)
                val strLen = dataLen - 16
                if (strLen > 0 && pos + 16 + strLen <= maxBytes) {
                    return String(buffer, pos + 16, strLen, Charsets.UTF_8)
                }
            }
            pos++
        }
        return null
    }

    private fun extractNumberFromDataAtom(buffer: ByteArray, startOffset: Int, maxBytes: Int): Float? {
        var pos = startOffset
        while (pos + 16 <= maxBytes && pos < startOffset + 32) {
            if (buffer[pos + 4] == 'd'.code.toByte() &&
                buffer[pos + 5] == 'a'.code.toByte() &&
                buffer[pos + 6] == 't'.code.toByte() &&
                buffer[pos + 7] == 'a'.code.toByte()
            ) {
                val dataLen = ((buffer[pos].toInt() and 0xFF) shl 24) or
                        ((buffer[pos + 1].toInt() and 0xFF) shl 16) or
                        ((buffer[pos + 2].toInt() and 0xFF) shl 8) or
                        (buffer[pos + 3].toInt() and 0xFF)
                val payloadLen = dataLen - 16
                if (payloadLen == 1) {
                    return (buffer[pos + 16].toInt() and 0xFF).toFloat()
                } else if (payloadLen == 2) {
                    val num = ((buffer[pos + 16].toInt() and 0xFF) shl 8) or
                            (buffer[pos + 17].toInt() and 0xFF)
                    return num.toFloat()
                } else if (payloadLen == 4) {
                    val num = ((buffer[pos + 16].toInt() and 0xFF) shl 24) or
                            ((buffer[pos + 17].toInt() and 0xFF) shl 16) or
                            ((buffer[pos + 18].toInt() and 0xFF) shl 8) or
                            (buffer[pos + 19].toInt() and 0xFF)
                    return num.toFloat()
                } else if (payloadLen in 1..8) {
                    val str = String(buffer, pos + 16, payloadLen, Charsets.UTF_8).trim()
                    return str.toFloatOrNull()
                }
            }
            pos++
        }
        return null
    }

    fun parse(context: Context?, uri: Uri?, album: String?, title: String?): ParsedSeries? {
        if (context != null && uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val mp4Result = parseFromMp4Stream(stream)
                    if (mp4Result != null) return mp4Result
                }
            } catch (_: Exception) {}
        }
        return parseFromText(album, title)
    }
}
