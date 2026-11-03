package com.evenfall.app.data.parser

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

data class ParsedMetadata(
    val title: String,
    val author: String,
    val durationMs: Long,
    val coverPath: String?,
    val trackNumber: Int? = null,
    val album: String? = null,
    val seriesName: String? = null,
    val seriesOrder: Float? = null
)

data class ParsedChapter(
    val title: String,
    val startMs: Long,
    val durationMs: Long,
    val fileUri: String
)

@Singleton
class AudioMetadataParser @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val coversDir = File(context.cacheDir, "book_covers").apply { mkdirs() }

    fun parseFileMetadata(uri: Uri, fallbackTitle: String, folderName: String? = null): ParsedMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val rawTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.trim()?.ifEmpty { null }
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.trim()?.ifEmpty { null }
            val title = TitleSanitizer.resolveTitle(
                titleTag = rawTitle,
                albumTag = album,
                folderName = folderName,
                fileName = fallbackTitle
            )
            val author = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)
                ?: "Unknown Author"
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L
            val trackStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
            val trackNumber = trackStr?.split("/")?.firstOrNull()?.trim()?.toIntOrNull()

            val coverBytes = retriever.embeddedPicture
            var coverPath: String? = null
            if (coverBytes != null && coverBytes.isNotEmpty()) {
                val coverFile = File(coversDir, "cover_${uri.hashCode().toUInt()}.jpg")
                if (!coverFile.exists() || coverFile.length() == 0L) {
                    FileOutputStream(coverFile).use { it.write(coverBytes) }
                }
                coverPath = coverFile.absolutePath
            }

            val parsedSeries = SeriesTagParser.parse(context, uri, album, rawTitle)

            ParsedMetadata(
                title = title,
                author = author.trim(),
                durationMs = durationMs,
                coverPath = coverPath,
                trackNumber = trackNumber,
                album = album,
                seriesName = parsedSeries?.name,
                seriesOrder = parsedSeries?.order
            )
        } catch (_: Exception) {
            val resolvedFallback = TitleSanitizer.resolveTitle(null, null, folderName, fallbackTitle)
            val parsedSeries = SeriesTagParser.parse(null, null, null, fallbackTitle)
            ParsedMetadata(
                title = resolvedFallback,
                author = "Unknown Author",
                durationMs = 0L,
                coverPath = null,
                album = null,
                seriesName = parsedSeries?.name,
                seriesOrder = parsedSeries?.order
            )
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    /**
     * Natural sort comparator for filenames (e.g. "Chapter 1", "Chapter 2", "Chapter 10").
     */
    companion object {
        fun naturalOrderComparator(): Comparator<String> {
            val pattern = Pattern.compile("(\\d+)|(\\D+)")
            return Comparator { s1, s2 ->
                val m1 = pattern.matcher(s1)
                val m2 = pattern.matcher(s2)
                while (m1.find() && m2.find()) {
                    val part1 = m1.group()
                    val part2 = m2.group()
                    val cmp = if (Character.isDigit(part1[0]) && Character.isDigit(part2[0])) {
                        try {
                            part1.toBigInteger().compareTo(part2.toBigInteger())
                        } catch (_: Exception) {
                            part1.compareTo(part2)
                        }
                    } else {
                        part1.compareTo(part2, ignoreCase = true)
                    }
                    if (cmp != 0) return@Comparator cmp
                }
                s1.length.compareTo(s2.length)
            }
        }
    }
}
