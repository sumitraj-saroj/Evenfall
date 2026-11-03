package com.evenfall.app.data.scanner

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.evenfall.app.data.datastore.ScanMode
import com.evenfall.app.data.db.dao.BookDao
import com.evenfall.app.data.db.dao.ChapterDao
import com.evenfall.app.data.db.dao.ScanIssueDao
import com.evenfall.app.data.db.entity.BookEntity
import com.evenfall.app.data.db.entity.ChapterEntity
import com.evenfall.app.data.db.entity.ScanIssueEntity
import com.evenfall.app.data.db.entity.ScanIssueReason
import com.evenfall.app.data.parser.AudioMetadataParser
import com.evenfall.app.data.parser.CoverArtResolver
import com.evenfall.app.data.parser.FolderImageCandidate
import com.evenfall.app.data.parser.TitleSanitizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

data class ScannedBookPreview(
    val title: String,
    val author: String,
    val durationMs: Long,
    val fileCount: Int,
    val coverPath: String?,
    val folderUri: String,
    val identifier: String,
    val files: List<ScannedAudioFile>,
    val seriesName: String? = null,
    val seriesOrder: Float? = null
)

data class ScannedAudioFile(
    val name: String,
    val uri: Uri,
    val title: String,
    val author: String,
    val durationMs: Long,
    val trackNumber: Int?,
    val coverPath: String?,
    val album: String? = null,
    val seriesName: String? = null,
    val seriesOrder: Float? = null
)

sealed class ScanProgress {
    data class Scanning(val message: String, val current: Int = 0, val total: Int = 0) : ScanProgress()
    data class Found(val books: List<ScannedBookPreview>) : ScanProgress()
    data class Error(val message: String) : ScanProgress()
    data object Cancelled : ScanProgress()
}

@Singleton
class LibraryScanner @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val metadataParser: AudioMetadataParser,
    private val coverArtResolver: CoverArtResolver,
    private val bookDao: BookDao,
    private val chapterDao: ChapterDao,
    private val scanIssueDao: ScanIssueDao
) {
    private val supportedExtensions = setOf("m4b", "mp3", "m4a", "ogg", "oga", "opus")
    private val isCancelled = AtomicBoolean(false)

    fun cancelScan() {
        isCancelled.set(true)
    }

    fun resetCancellation() {
        isCancelled.set(false)
    }

    suspend fun parseAudioFileSafely(
        doc: DocumentFile,
        parentFolderName: String
    ): ScannedAudioFile? {
        val name = doc.name ?: "Unknown Audio"
        val uri = doc.uri
        val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)

        if (ext.isNotEmpty() && !supportedExtensions.contains(ext)) {
            scanIssueDao.insert(
                ScanIssueEntity(
                    filePath = uri.toString(),
                    fileName = name,
                    reason = ScanIssueReason.UNSUPPORTED_CODEC,
                    details = "Unsupported file extension '.$ext'"
                )
            )
            return null
        }

        return try {
            val meta = withTimeoutOrNull(5000L) {
                val fallbackTitle = name.substringBeforeLast(".")
                metadataParser.parseFileMetadata(uri, fallbackTitle, parentFolderName)
            }

            if (meta == null) {
                scanIssueDao.insert(
                    ScanIssueEntity(
                        filePath = uri.toString(),
                        fileName = name,
                        reason = ScanIssueReason.TIMEOUT,
                        details = "Metadata extraction timed out after 5000ms"
                    )
                )
                return null
            }

            if (meta.durationMs <= 0L) {
                scanIssueDao.insert(
                    ScanIssueEntity(
                        filePath = uri.toString(),
                        fileName = name,
                        reason = ScanIssueReason.NO_AUDIO_TRACK,
                        details = "No audio track detected or 0ms duration"
                    )
                )
                return null
            }

            val resolvedTitle = TitleSanitizer.resolveTitle(
                titleTag = meta.title,
                albumTag = meta.album,
                folderName = parentFolderName,
                fileName = name.substringBeforeLast(".")
            )

            ScannedAudioFile(
                name = name,
                uri = uri,
                title = resolvedTitle,
                author = meta.author,
                durationMs = meta.durationMs,
                trackNumber = meta.trackNumber,
                coverPath = meta.coverPath,
                album = meta.album,
                seriesName = meta.seriesName,
                seriesOrder = meta.seriesOrder
            )
        } catch (e: SecurityException) {
            scanIssueDao.insert(
                ScanIssueEntity(
                    filePath = uri.toString(),
                    fileName = name,
                    reason = ScanIssueReason.PERMISSION_DENIED,
                    details = e.localizedMessage ?: "Permission denied by system storage"
                )
            )
            null
        } catch (e: Exception) {
            scanIssueDao.insert(
                ScanIssueEntity(
                    filePath = uri.toString(),
                    fileName = name,
                    reason = ScanIssueReason.UNREADABLE,
                    details = e.localizedMessage ?: "Failed to read audio file"
                )
            )
            null
        }
    }

    fun scanFolderPreview(treeUri: Uri, scanMode: ScanMode): Flow<ScanProgress> = flow {
        isCancelled.set(false)
        emit(ScanProgress.Scanning("Scanning folder...", 0, 0))

        try {
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
            if (rootDoc == null || !rootDoc.isDirectory) {
                emit(ScanProgress.Error("Unable to open folder"))
                return@flow
            }

            val books = mutableListOf<ScannedBookPreview>()
            val naturalComparator = AudioMetadataParser.naturalOrderComparator()

            if (scanMode == ScanMode.FILE_IS_BOOK) {
                val files = findAudioFilesRecursive(rootDoc)
                val total = files.size
                var current = 0

                for (doc in files) {
                    if (isCancelled.get() || !currentCoroutineContext().isActive) {
                        emit(ScanProgress.Cancelled)
                        return@flow
                    }
                    current++
                    emit(ScanProgress.Scanning("Scanning $current of $total", current, total))

                    val scannedFile = parseAudioFileSafely(doc, rootDoc.name ?: "Audiobook") ?: continue
                    val resolvedCover = if (scannedFile.coverPath != null) {
                        scannedFile.coverPath
                    } else {
                        val parent = doc.parentFile ?: rootDoc
                        val folderCandidates = findFolderImageCandidates(parent)
                        coverArtResolver.resolveCover(
                            manualOverrideUri = null,
                            embeddedArtPath = null,
                            folderCandidates = folderCandidates
                        ).path
                    }

                    books.add(
                        ScannedBookPreview(
                            title = scannedFile.title,
                            author = scannedFile.author,
                            durationMs = scannedFile.durationMs,
                            fileCount = 1,
                            coverPath = resolvedCover,
                            folderUri = doc.uri.toString(),
                            identifier = doc.uri.toString(),
                            files = listOf(scannedFile),
                            seriesName = scannedFile.seriesName,
                            seriesOrder = scannedFile.seriesOrder
                        )
                    )
                }
            } else {
                val children = rootDoc.listFiles()
                val subdirectories = children.filter { it.isDirectory }
                val looseAudioFiles = children.filter { isAudioFile(it) }

                // Count total files across all folders
                val dirAudioFiles = mutableListOf<Pair<DocumentFile, List<DocumentFile>>>()
                var total = looseAudioFiles.size

                for (dir in subdirectories) {
                    val allDirChildren = dir.listFiles()
                    val audioFiles = allDirChildren.filter { isAudioFile(it) }
                    if (audioFiles.isNotEmpty()) {
                        dirAudioFiles.add(dir to audioFiles)
                        total += audioFiles.size
                    }
                }

                var current = 0

                for ((dir, audioFiles) in dirAudioFiles) {
                    if (isCancelled.get() || !currentCoroutineContext().isActive) {
                        emit(ScanProgress.Cancelled)
                        return@flow
                    }

                    val folderName = dir.name ?: "Unknown Book"
                    val parsedFiles = mutableListOf<ScannedAudioFile>()

                    for (fileDoc in audioFiles) {
                        if (isCancelled.get() || !currentCoroutineContext().isActive) {
                            emit(ScanProgress.Cancelled)
                            return@flow
                        }
                        current++
                        emit(ScanProgress.Scanning("Scanning $current of $total", current, total))

                        val parsed = parseAudioFileSafely(fileDoc, folderName)
                        if (parsed != null) {
                            parsedFiles.add(parsed)
                        }
                    }

                    if (parsedFiles.isNotEmpty()) {
                        val folderCandidates = findFolderImageCandidates(dir.listFiles())
                        val book = assembleBookFromFiles(
                            folderName = folderName,
                            folderUri = dir.uri.toString(),
                            parsedFiles = parsedFiles,
                            folderCandidates = folderCandidates,
                            naturalComparator = naturalComparator
                        )
                        books.add(book)
                    }
                }

                // Loose files in root directory
                if (looseAudioFiles.isNotEmpty()) {
                    val rootName = rootDoc.name ?: "Audiobooks"
                    val parsedFiles = mutableListOf<ScannedAudioFile>()

                    for (fileDoc in looseAudioFiles) {
                        if (isCancelled.get() || !currentCoroutineContext().isActive) {
                            emit(ScanProgress.Cancelled)
                            return@flow
                        }
                        current++
                        emit(ScanProgress.Scanning("Scanning $current of $total", current, total))

                        val parsed = parseAudioFileSafely(fileDoc, rootName)
                        if (parsed != null) {
                            parsedFiles.add(parsed)
                        }
                    }

                    if (parsedFiles.isNotEmpty()) {
                        val folderCandidates = findFolderImageCandidates(children)
                        val book = assembleBookFromFiles(
                            folderName = rootName,
                            folderUri = rootDoc.uri.toString(),
                            parsedFiles = parsedFiles,
                            folderCandidates = folderCandidates,
                            naturalComparator = naturalComparator
                        )
                        books.add(book)
                    }
                }
            }

            emit(ScanProgress.Found(books))
        } catch (e: Exception) {
            emit(ScanProgress.Error(e.localizedMessage ?: "Scan failed"))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun saveScannedBooks(books: List<ScannedBookPreview>) = withContext(Dispatchers.IO) {
        for (scanned in books) {
            val existingBook = bookDao.getBookByFolderIdentifier(scanned.identifier)
            val bookId: Long = if (existingBook != null) {
                // Update book metadata while PRESERVING current playback progress!
                val updated = existingBook.copy(
                    title = scanned.title,
                    author = scanned.author,
                    coverUri = scanned.coverPath ?: existingBook.coverUri,
                    totalDurationMs = scanned.durationMs,
                    fileCount = scanned.fileCount,
                    seriesName = existingBook.seriesName ?: scanned.seriesName,
                    seriesOrder = existingBook.seriesOrder ?: scanned.seriesOrder
                )
                bookDao.update(updated)
                existingBook.id
            } else {
                val newBook = BookEntity(
                    title = scanned.title,
                    author = scanned.author,
                    coverUri = scanned.coverPath,
                    folderUri = scanned.folderUri,
                    folderIdentifier = scanned.identifier,
                    totalDurationMs = scanned.durationMs,
                    currentPositionMs = 0L,
                    currentChapterIndex = 0,
                    chapterPositionMs = 0L,
                    playbackSpeed = 1.0f,
                    isFinished = false,
                    fileCount = scanned.fileCount,
                    seriesName = scanned.seriesName,
                    seriesOrder = scanned.seriesOrder
                )
                bookDao.insert(newBook)
            }

            // Save chapters while PRESERVING existing chapter title overrides!
            val existingChapters = chapterDao.getChaptersForBookSync(bookId).associateBy { it.chapterIndex }
            chapterDao.deleteChaptersForBook(bookId)
            val chapters = mutableListOf<ChapterEntity>()
            var runningOffset = 0L

            scanned.files.forEachIndexed { index, file ->
                val prevOverride = existingChapters[index]?.titleOverride
                chapters.add(
                    ChapterEntity(
                        bookId = bookId,
                        chapterIndex = index,
                        title = file.title.ifEmpty { "Chapter ${index + 1}" },
                        titleOverride = prevOverride,
                        startMs = runningOffset,
                        durationMs = file.durationMs,
                        fileUri = file.uri.toString()
                    )
                )
                runningOffset += file.durationMs
            }
            chapterDao.insertAll(chapters)
        }
    }

    private fun assembleBookFromFiles(
        folderName: String,
        folderUri: String,
        parsedFiles: List<ScannedAudioFile>,
        folderCandidates: List<FolderImageCandidate> = emptyList(),
        naturalComparator: Comparator<String>
    ): ScannedBookPreview {
        val sortedFiles = if (parsedFiles.all { it.trackNumber != null }) {
            parsedFiles.sortedBy { it.trackNumber }
        } else {
            parsedFiles.sortedWith { f1, f2 -> naturalComparator.compare(f1.name, f2.name) }
        }

        val firstWithAuthor = sortedFiles.firstOrNull { it.author != "Unknown Author" }
        val author = firstWithAuthor?.author ?: "Unknown Author"

        val firstValidTitle = sortedFiles.map { it.title }.firstOrNull { !TitleSanitizer.isGenericTitle(it) }
        val firstValidAlbum = sortedFiles.mapNotNull { it.album }.firstOrNull { !TitleSanitizer.isGenericTitle(it) }
        val cleanFolderName = folderName.replace("_", " ").trim()
        val firstFileName = sortedFiles.firstOrNull()?.name?.substringBeforeLast(".")

        val title = TitleSanitizer.resolveTitle(
            titleTag = firstValidTitle,
            albumTag = firstValidAlbum,
            folderName = cleanFolderName,
            fileName = firstFileName
        )
        val totalDuration = sortedFiles.sumOf { it.durationMs }
        val embeddedCover = sortedFiles.firstOrNull { it.coverPath != null }?.coverPath
        val cover = coverArtResolver.resolveCover(
            manualOverrideUri = null,
            embeddedArtPath = embeddedCover,
            folderCandidates = folderCandidates
        ).path

        val firstWithSeries = sortedFiles.firstOrNull { it.seriesName != null }
        val seriesName = firstWithSeries?.seriesName
        val seriesOrder = firstWithSeries?.seriesOrder

        return ScannedBookPreview(
            title = title,
            author = author,
            durationMs = totalDuration,
            fileCount = sortedFiles.size,
            coverPath = cover,
            folderUri = folderUri,
            identifier = folderUri,
            files = sortedFiles,
            seriesName = seriesName,
            seriesOrder = seriesOrder
        )
    }

    private fun findFolderImageCandidates(dir: DocumentFile): List<FolderImageCandidate> {
        return findFolderImageCandidates(dir.listFiles())
    }

    private fun findFolderImageCandidates(files: Array<DocumentFile>): List<FolderImageCandidate> {
        return files
            .filter { !it.isDirectory && CoverArtResolver.isSupportedFolderImage(it.name ?: "") }
            .map { doc ->
                FolderImageCandidate(
                    fileName = doc.name ?: "",
                    uriString = doc.uri.toString(),
                    lastModified = doc.lastModified(),
                    sizeBytes = doc.length()
                )
            }
    }

    private fun findAudioFilesRecursive(dir: DocumentFile): List<DocumentFile> {
        val result = mutableListOf<DocumentFile>()
        for (item in dir.listFiles()) {
            if (item.isDirectory) {
                result.addAll(findAudioFilesRecursive(item))
            } else if (isAudioFile(item)) {
                result.add(item)
            }
        }
        return result
    }

    private fun isAudioFile(doc: DocumentFile): Boolean {
        if (doc.isDirectory) return false
        val name = doc.name?.lowercase(Locale.ROOT) ?: return false
        val ext = name.substringAfterLast(".", "")
        return supportedExtensions.contains(ext)
    }
}

