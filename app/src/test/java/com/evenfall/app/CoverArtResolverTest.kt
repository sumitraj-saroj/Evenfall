package com.evenfall.app

import com.evenfall.app.data.parser.CoverArtResolver
import com.evenfall.app.data.parser.CoverSource
import com.evenfall.app.data.parser.FolderImageCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File

class CoverArtResolverTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var coversDir: File
    private lateinit var resolver: CoverArtResolver

    @Before
    fun setUp() {
        coversDir = tempFolder.newFolder("book_covers")
        resolver = CoverArtResolver(coversDir = coversDir, contentResolver = null)
    }

    @Test
    fun testManualOverrideWinsOverEmbeddedAndFolderImages() {
        val candidates = listOf(
            FolderImageCandidate(fileName = "cover.jpg", uriString = "file:///sdcard/Books/cover.jpg")
        )
        val result = resolver.resolveCover(
            manualOverrideUri = "file:///data/user/0/com.evenfall.app/files/custom_covers/cover_1.jpg",
            embeddedArtPath = "/data/user/0/com.evenfall.app/cache/book_covers/cover_embedded.jpg",
            folderCandidates = candidates
        )
        assertEquals(CoverSource.MANUAL_OVERRIDE, result.source)
        assertEquals("file:///data/user/0/com.evenfall.app/files/custom_covers/cover_1.jpg", result.path)
    }

    @Test
    fun testEmbeddedArtWinsOverFolderImages() {
        val candidates = listOf(
            FolderImageCandidate(fileName = "cover.jpg", uriString = "file:///sdcard/Books/cover.jpg"),
            FolderImageCandidate(fileName = "folder.png", uriString = "file:///sdcard/Books/folder.png")
        )
        val result = resolver.resolveCover(
            manualOverrideUri = null,
            embeddedArtPath = "/data/user/0/com.evenfall.app/cache/book_covers/cover_embedded.jpg",
            folderCandidates = candidates
        )
        assertEquals(CoverSource.EMBEDDED, result.source)
        assertEquals("/data/user/0/com.evenfall.app/cache/book_covers/cover_embedded.jpg", result.path)
    }

    @Test
    fun testFolderImagePrecedenceOrder() {
        // cover.* > folder.* > front.* > albumart*
        val candidateAlbumArt = FolderImageCandidate("albumart.jpg", "uri://albumart.jpg")
        val candidateFront = FolderImageCandidate("front.jpg", "uri://front.jpg")
        val candidateFolder = FolderImageCandidate("folder.jpg", "uri://folder.jpg")
        val candidateCover = FolderImageCandidate("cover.jpg", "uri://cover.jpg")

        // 1. All present: cover wins
        val best1 = CoverArtResolver.findBestFolderImage(
            listOf(candidateAlbumArt, candidateFront, candidateFolder, candidateCover)
        )
        assertEquals("cover.jpg", best1?.fileName)

        // 2. Cover missing: folder wins
        val best2 = CoverArtResolver.findBestFolderImage(
            listOf(candidateAlbumArt, candidateFront, candidateFolder)
        )
        assertEquals("folder.jpg", best2?.fileName)

        // 3. Cover & folder missing: front wins
        val best3 = CoverArtResolver.findBestFolderImage(
            listOf(candidateAlbumArt, candidateFront)
        )
        assertEquals("front.jpg", best3?.fileName)

        // 4. Only albumart present: albumart wins
        val best4 = CoverArtResolver.findBestFolderImage(
            listOf(candidateAlbumArt)
        )
        assertEquals("albumart.jpg", best4?.fileName)
    }

    @Test
    fun testCaseInsensitiveMatching() {
        val candidates = listOf(
            FolderImageCandidate("ALBUMART_SMALL.PNG", "uri://1"),
            FolderImageCandidate("Front.WEBP", "uri://2"),
            FolderImageCandidate("Folder.JPEG", "uri://3"),
            FolderImageCandidate("COVER.PNG", "uri://4")
        )
        val best = CoverArtResolver.findBestFolderImage(candidates)
        assertNotNull(best)
        assertEquals("COVER.PNG", best?.fileName)
    }

    @Test
    fun testUnsupportedExtensionsIgnored() {
        val candidates = listOf(
            FolderImageCandidate("cover.bmp", "uri://bmp"),
            FolderImageCandidate("cover.gif", "uri://gif"),
            FolderImageCandidate("cover.txt", "uri://txt"),
            FolderImageCandidate("random_photo.jpg", "uri://jpg")
        )
        val best = CoverArtResolver.findBestFolderImage(candidates)
        assertNull(best)

        val result = resolver.resolveCover(
            manualOverrideUri = null,
            embeddedArtPath = null,
            folderCandidates = candidates
        )
        assertEquals(CoverSource.LETTER_AVATAR, result.source)
        assertNull(result.path)
    }

    @Test
    fun testTieBreakingByExtensionAndFilename() {
        // Same priority: cover.jpg vs cover.png (jpg preferred over png)
        val candidates = listOf(
            FolderImageCandidate("cover.png", "uri://png"),
            FolderImageCandidate("cover.jpg", "uri://jpg")
        )
        val best = CoverArtResolver.findBestFolderImage(candidates)
        assertEquals("cover.jpg", best?.fileName)
    }

    @Test
    fun testFallbackToLetterAvatar() {
        val result = resolver.resolveCover(
            manualOverrideUri = null,
            embeddedArtPath = null,
            folderCandidates = emptyList()
        )
        assertEquals(CoverSource.LETTER_AVATAR, result.source)
        assertNull(result.path)
    }

    @Test
    fun testDiskCachingAndInvalidation() {
        val candidate1 = FolderImageCandidate(
            fileName = "cover.jpg",
            uriString = "content://saf/tree/book1/cover.jpg",
            lastModified = 1000L,
            sizeBytes = 500L
        )

        val initialData = "INITIAL_COVER_BYTES".toByteArray()
        val path1 = resolver.getOrCacheFolderImage(candidate1) {
            ByteArrayInputStream(initialData)
        }

        assertNotNull(path1)
        val file1 = File(path1!!)
        assertTrue(file1.exists())
        assertEquals(initialData.size.toLong(), file1.length())

        // Cache hit test with same timestamp and size
        val path1Cached = resolver.getOrCacheFolderImage(candidate1) {
            throw AssertionError("Should not read stream on cache hit")
        }
        assertEquals(path1, path1Cached)

        // Invalidation test: candidate modified with new timestamp and size
        val candidateModified = FolderImageCandidate(
            fileName = "cover.jpg",
            uriString = "content://saf/tree/book1/cover.jpg",
            lastModified = 2000L,
            sizeBytes = 800L
        )
        val updatedData = ByteArray(800) { 1 }
        val path2 = resolver.getOrCacheFolderImage(candidateModified) {
            ByteArrayInputStream(updatedData)
        }

        assertNotNull(path2)
        val file2 = File(path2!!)
        assertTrue(file2.exists())
        assertEquals(800L, file2.length())

        // The old cache file should be deleted upon invalidation
        assertFalse("Old cache file must be deleted upon invalidation", file1.exists())
    }
}
