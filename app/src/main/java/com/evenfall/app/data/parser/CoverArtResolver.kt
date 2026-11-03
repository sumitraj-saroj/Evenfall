package com.evenfall.app.data.parser

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

enum class CoverSource {
    MANUAL_OVERRIDE,
    EMBEDDED,
    FOLDER_IMAGE,
    LETTER_AVATAR
}

data class FolderImageCandidate(
    val fileName: String,
    val uriString: String,
    val lastModified: Long = 0L,
    val sizeBytes: Long = 0L
) {
    val extension: String
        get() = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
}

data class CoverResolutionResult(
    val source: CoverSource,
    val path: String?,
    val candidate: FolderImageCandidate? = null
)

@Singleton
class CoverArtResolver(
    private val coversDir: File,
    private val contentResolver: ContentResolver? = null
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(
        File(context.cacheDir, "book_covers").apply { mkdirs() },
        context.contentResolver
    )

    companion object {
        val SUPPORTED_IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")

        /**
         * Returns priority rank (1 = cover.*, 2 = folder.*, 3 = front.*, 4 = albumart*) or null if not matching.
         */
        fun getFolderImagePriority(fileName: String): Int? {
            val dotIndex = fileName.lastIndexOf('.')
            if (dotIndex <= 0) return null
            val ext = fileName.substring(dotIndex + 1).lowercase(Locale.ROOT)
            if (ext !in SUPPORTED_IMAGE_EXTENSIONS) return null

            val baseName = fileName.substring(0, dotIndex).lowercase(Locale.ROOT)
            return when {
                baseName == "cover" -> 1
                baseName == "folder" -> 2
                baseName == "front" -> 3
                baseName.startsWith("albumart") -> 4
                else -> null
            }
        }

        fun isSupportedFolderImage(fileName: String): Boolean {
            return getFolderImagePriority(fileName) != null
        }

        /**
         * Extension priority for tie-breaking: jpg -> jpeg -> png -> webp
         */
        fun getExtensionPriority(ext: String): Int {
            return when (ext.lowercase(Locale.ROOT)) {
                "jpg" -> 1
                "jpeg" -> 2
                "png" -> 3
                "webp" -> 4
                else -> 5
            }
        }

        /**
         * Selects the best folder image candidate based on resolution rules:
         * 1. Priority rank (cover.* > folder.* > front.* > albumart*)
         * 2. Extension priority (jpg > jpeg > png > webp)
         * 3. Case-insensitive alphabetical filename
         */
        fun findBestFolderImage(candidates: List<FolderImageCandidate>): FolderImageCandidate? {
            return candidates
                .filter { isSupportedFolderImage(it.fileName) }
                .minWithOrNull(
                    compareBy<FolderImageCandidate> { getFolderImagePriority(it.fileName) ?: Int.MAX_VALUE }
                        .thenBy { getExtensionPriority(it.extension) }
                        .thenBy { it.fileName.lowercase(Locale.ROOT) }
                )
        }
    }

    /**
     * Resolves cover according to strict precedence order:
     * 1. Manual override
     * 2. Embedded art
     * 3. Folder image (cover.* > folder.* > front.* > albumart*)
     * 4. Letter avatar (null)
     */
    fun resolveCover(
        manualOverrideUri: String?,
        embeddedArtPath: String?,
        folderCandidates: List<FolderImageCandidate>,
        inputStreamProvider: ((FolderImageCandidate) -> InputStream?)? = null
    ): CoverResolutionResult {
        // 1. Manual override
        if (!manualOverrideUri.isNullOrBlank()) {
            return CoverResolutionResult(
                source = CoverSource.MANUAL_OVERRIDE,
                path = manualOverrideUri
            )
        }

        // 2. Embedded art
        if (!embeddedArtPath.isNullOrBlank()) {
            return CoverResolutionResult(
                source = CoverSource.EMBEDDED,
                path = embeddedArtPath
            )
        }

        // 3. Folder image
        val bestCandidate = findBestFolderImage(folderCandidates)
        if (bestCandidate != null) {
            val cachedPath = getOrCacheFolderImage(bestCandidate, inputStreamProvider)
            if (cachedPath != null) {
                return CoverResolutionResult(
                    source = CoverSource.FOLDER_IMAGE,
                    path = cachedPath,
                    candidate = bestCandidate
                )
            }
        }

        // 4. Letter avatar fallback
        return CoverResolutionResult(
            source = CoverSource.LETTER_AVATAR,
            path = null
        )
    }

    /**
     * Retrieves cached folder image or extracts and caches it to disk.
     * Invalidates obsolete cached files when timestamp or size changes.
     */
    fun getOrCacheFolderImage(
        candidate: FolderImageCandidate,
        customStreamProvider: ((FolderImageCandidate) -> InputStream?)? = null
    ): String? {
        if (!coversDir.exists()) coversDir.mkdirs()
        val safeKey = candidate.uriString.hashCode().toUInt().toString()
        val expectedFileName = "folder_${safeKey}_${candidate.lastModified}_${candidate.sizeBytes}.${candidate.extension}"
        val targetFile = File(coversDir, expectedFileName)

        // Cache hit: valid file exists and matches current timestamp and size
        if (targetFile.exists() && targetFile.length() > 0L) {
            return targetFile.absolutePath
        }

        // Cache miss or stale: Invalidate old cache files for this candidate
        invalidateCacheForCandidate(safeKey)

        // Read stream and write new cache
        return try {
            val stream = customStreamProvider?.invoke(candidate)
                ?: contentResolver?.openInputStream(Uri.parse(candidate.uriString))
                ?: return null

            stream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            targetFile.absolutePath
        } catch (_: Exception) {
            if (targetFile.exists()) targetFile.delete()
            null
        }
    }

    /**
     * Deletes any cached files corresponding to this key prefix.
     */
    fun invalidateCacheForCandidate(key: String) {
        val prefix = "folder_${key}_"
        coversDir.listFiles { _, name -> name.startsWith(prefix) }?.forEach { file ->
            try { file.delete() } catch (_: Exception) {}
        }
    }
}
