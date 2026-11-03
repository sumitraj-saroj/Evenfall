package com.evenfall.app.data.scanner

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.evenfall.app.data.datastore.ScanMode
import com.evenfall.app.data.db.dao.BookDao
import com.evenfall.app.data.db.dao.ChapterDao
import com.evenfall.app.data.db.entity.BookEntity
import com.evenfall.app.data.db.entity.ChapterEntity
import com.evenfall.app.data.parser.AudioMetadataParser
import com.evenfall.app.data.parser.CoverArtResolver
import com.evenfall.app.data.parser.FolderImageCandidate
import com.evenfall.app.data.parser.TitleSanitizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.Locale
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
    val files: List<ScannedAudioFile>
)

data class ScannedAudioFile(
    val name: String,
    val uri: Uri,
    val title: String,
    val author: String,
    val durationMs: Long,
    val trackNumber: Int?,
    val coverPath: String?,
    val album: String? = null
)

sealed class ScanProgress {
    data class Scanning(val message: String) : ScanProgress()
    data class Found(val books: List<ScannedBookPreview>) : ScanProgress()
    data class Error(val message: String) : ScanProgress()
}

@Singleton
class LibraryScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val metadataParser: AudioMetadataParser,
    private val coverArtResolver: CoverArtResolver,
    private val bookDao: BookDao,
    private val chapterDao: ChapterDao
) {
    private val supportedExtensions = setOf("m4b", "mp3", "m4a", "ogg", "oga", "opus")

    fun scanFolderPreview(treeUri: Uri, scanMode: ScanMode): Flow<ScanProgress> = flow {
        emit(ScanProgress.Scanning("Scanning folder..."))
        try {
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
            if (rootDoc == null || !rootDoc.isDirectory) {
                emit(ScanProgress.Error("Unable to open folder"))
                return@flow
            }

            val books = mutableListOf<ScannedBookPreview>()
            val naturalComparator = AudioMetadataParser.naturalOrderComparator()

            if (scanMode == ScanMode.FILE_IS_BOOK) {
                // Every single audio file is its own book
                val files = findAudioFilesRecursive(rootDoc)
                for (doc in files) {
                    val rawName = doc.name ?: "Unknown Audio"
                    val fallbackTitle = rawName.substringBeforeLast(".")
                    val meta = metadataParser.parseFileMetadata(doc.uri, fallbackTitle, rootDoc.name)
                    val resolvedTitle = TitleSanitizer.resolveTitle(
                        titleTag = meta.title,
                        albumTag = meta.album,
                        folderName = rootDoc.name,
                        fileName = fallbackTitle
                    )
                    val resolvedCover = if (meta.coverPath != null) {
                        meta.coverPath
                    } else {
                        val parent = doc.parentFile ?: rootDoc
                        val folderCandidates = findFolderImageCandidates(parent)
                        coverArtResolver.resolveCover(
                            manualOverrideUri = null,
                            embeddedArtPath = null,
                            folderCandidates = folderCandidates
                        ).path
                    }
                    val scannedFile = ScannedAudioFile(
                        name = rawName,
                        uri = doc.uri,
                        title = resolvedTitle,
                        author = meta.author,
                        durationMs = meta.durationMs,
                        trackNumber = meta.trackNumber,
                        coverPath = resolvedCover,
                        album = meta.album
                    )
                    books.add(
                        ScannedBookPreview(
                            title = resolvedTitle,
                            author = meta.author,
                            durationMs = meta.durationMs,
                            fileCount = 1,
                            coverPath = resolvedCover,
                            folderUri = doc.uri.toString(),
                            identifier = doc.uri.toString(),
                            files = listOf(scannedFile)
                        )
                    )
                }
            } else {
                // Each subfolder is a book
                val children = rootDoc.listFiles()
                val subdirectories = children.filter { it.isDirectory }
                val looseAudioFiles = children.filter { isAudioFile(it) }

                for (dir in subdirectories) {
                    val allDirChildren = dir.listFiles()
                    val audioFiles = allDirChildren.filter { isAudioFile(it) }
                    if (audioFiles.isNotEmpty()) {
                        val folderCandidates = findFolderImageCandidates(allDirChildren)
                        val book = processAudiobookFiles(
                            folderName = dir.name ?: "Unknown Book",
                            folderUri = dir.uri.toString(),
                            files = audioFiles,
                            folderCandidates = folderCandidates,
                            naturalComparator = naturalComparator
                        )
                        books.add(book)
                    }
                }

                // If root folder also has loose audio files, check if they make up a book
                if (looseAudioFiles.isNotEmpty()) {
                    val rootName = rootDoc.name ?: "Audiobooks"
                    val folderCandidates = findFolderImageCandidates(children)
                    val book = processAudiobookFiles(
                        folderName = rootName,
                        folderUri = rootDoc.uri.toString(),
                        files = looseAudioFiles,
                        folderCandidates = folderCandidates,
                        naturalComparator = naturalComparator
                    )
                    books.add(book)
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
                    fileCount = scanned.fileCount
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
                    fileCount = scanned.fileCount
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

    private fun processAudiobookFiles(
        folderName: String,
        folderUri: String,
        files: List<DocumentFile>,
        folderCandidates: List<FolderImageCandidate> = emptyList(),
        naturalComparator: Comparator<String>
    ): ScannedBookPreview {
        val parsedFiles = files.map { doc ->
            val name = doc.name ?: "Audio"
            val fallbackTitle = name.substringBeforeLast(".")
            val meta = metadataParser.parseFileMetadata(doc.uri, fallbackTitle, folderName)
            ScannedAudioFile(
                name = name,
                uri = doc.uri,
                title = meta.title,
                author = meta.author,
                durationMs = meta.durationMs,
                trackNumber = meta.trackNumber,
                coverPath = meta.coverPath,
                album = meta.album
            )
        }

        // Sort files: by track number if all have tracks, else natural filename order
        val sortedFiles = if (parsedFiles.all { it.trackNumber != null }) {
            parsedFiles.sortedBy { it.trackNumber }
        } else {
            parsedFiles.sortedWith { f1, f2 -> naturalComparator.compare(f1.name, f2.name) }
        }

        // Book title: resolve following fallback order: title tag -> album tag -> folder name -> file name
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

        return ScannedBookPreview(
            title = title,
            author = author,
            durationMs = totalDuration,
            fileCount = sortedFiles.size,
            coverPath = cover,
            folderUri = folderUri,
            identifier = folderUri,
            files = sortedFiles
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

    private fun findAudioFilesDirect(dir: DocumentFile): List<DocumentFile> {
        return dir.listFiles().filter { isAudioFile(it) }
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
