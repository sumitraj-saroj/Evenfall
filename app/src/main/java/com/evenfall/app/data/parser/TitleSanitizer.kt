package com.evenfall.app.data.parser

import java.util.Locale

object TitleSanitizer {

    private val GENERIC_EXACT = setOf(
        "audiobook",
        "audio book",
        "audiobooks",
        "audio books",
        "unknown",
        "unknown title",
        "unknown audio",
        "unknown track",
        "unknown album",
        "unknown artist",
        "unknown book",
        "untitled",
        "audio",
        "track",
        "disc",
        "cd",
        "my audiobook",
        "new audiobook"
    )

    private val ROOT_FOLDER_NAMES = setOf(
        "audiobooks",
        "audiobook",
        "audio",
        "books",
        "downloads",
        "download",
        "music",
        "internal storage",
        "storage",
        "sdcard",
        "0",
        "emulated"
    )

    /**
     * Checks if a title candidate is empty or generic (e.g. "Audiobook", "Unknown", "Track 1", "01").
     */
    fun isGenericTitle(candidate: String?): Boolean {
        if (candidate.isNullOrBlank()) return true
        val trimmed = candidate.trim()
        val lower = trimmed.lowercase(Locale.ROOT)

        if (lower in GENERIC_EXACT) return true

        // Bare number: e.g. "1", "01", "123"
        if (trimmed.all { it.isDigit() }) return true

        // Track patterns: e.g. "Track 1", "Track 01", "track-2", "track_03", "track#4"
        if (lower.matches(Regex("""^track[\s_#.-]*\d+$"""))) return true

        // Audiobook patterns: e.g. "Audiobook 1", "Audiobook_02"
        if (lower.matches(Regex("""^audio[\s_-]*books?[\s_#.-]*\d*$"""))) return true

        // Chapter pattern when used as book title: e.g. "Chapter 1", "Chapter 01"
        if (lower.matches(Regex("""^chapter[\s_#.-]*\d+$"""))) return true

        // Disc/CD patterns: e.g. "Disc 1", "CD 1", "CD 02"
        if (lower.matches(Regex("""^(disc|cd)[\s_#.-]*\d+.*$"""))) return true

        return false
    }

    /**
     * Checks if a folder name is a generic storage/root container rather than a specific book title.
     */
    fun isGenericFolder(folderName: String?): Boolean {
        if (folderName.isNullOrBlank()) return true
        val lower = folderName.trim().lowercase(Locale.ROOT)
        return lower in ROOT_FOLDER_NAMES || isGenericTitle(folderName)
    }

    /**
     * Cleans a file name by stripping extension and normalizing separators.
     */
    fun cleanFileName(fileName: String?): String? {
        if (fileName.isNullOrBlank()) return null
        val baseName = fileName.substringBeforeLast(".").trim()
        val cleaned = baseName.replace("_", " ").replace("-", " ").trim()
        return cleaned.ifBlank { null }
    }

    /**
     * Resolves the book title using the fallback order:
     * 1. title tag (if not empty and not generic)
     * 2. album tag (if not empty and not generic)
     * 3. folder name (if not empty and not generic root folder)
     * 4. file name without extension
     */
    fun resolveTitle(
        titleTag: String?,
        albumTag: String?,
        folderName: String?,
        fileName: String?
    ): String {
        // 1. Title tag
        val cleanTitle = titleTag?.trim()?.ifEmpty { null }
        if (!isGenericTitle(cleanTitle)) {
            return cleanTitle!!
        }

        // 2. Album tag
        val cleanAlbum = albumTag?.trim()?.ifEmpty { null }
        if (!isGenericTitle(cleanAlbum)) {
            return cleanAlbum!!
        }

        // 3. Folder name
        val cleanFolder = folderName?.replace("_", " ")?.trim()?.ifEmpty { null }
        if (!isGenericFolder(cleanFolder)) {
            return cleanFolder!!
        }

        // 4. File name without extension
        val cleanFile = cleanFileName(fileName)
        if (!isGenericTitle(cleanFile)) {
            return cleanFile!!
        }

        // 5. Ultimate fallback if all were generic
        return cleanFolder
            ?: cleanFile
            ?: cleanAlbum
            ?: cleanTitle
            ?: "Untitled Audiobook"
    }
}
