package com.evenfall.app

import com.evenfall.app.data.db.dao.ScanIssueDao
import com.evenfall.app.data.db.entity.ScanIssueEntity
import com.evenfall.app.data.db.entity.ScanIssueReason
import com.evenfall.app.data.scanner.ScanProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanLogTest {

    private class FakeScanIssueDao : ScanIssueDao {
        val issues = mutableListOf<ScanIssueEntity>()
        private val flow = MutableStateFlow<List<ScanIssueEntity>>(emptyList())

        override suspend fun insert(issue: ScanIssueEntity): Long {
            issues.add(issue)
            flow.value = issues.toList()
            return issues.size.toLong()
        }

        override suspend fun insertAll(issues: List<ScanIssueEntity>) {
            this.issues.addAll(issues)
            flow.value = this.issues.toList()
        }

        override fun getAllScanIssues(): Flow<List<ScanIssueEntity>> = flow

        override suspend fun getAllScanIssuesSync(): List<ScanIssueEntity> = issues.toList()

        override fun getIssueCount(): Flow<Int> = MutableStateFlow(issues.size)

        override suspend fun clearAll() {
            issues.clear()
            flow.value = emptyList()
        }
    }

    @Test
    fun testScanIssueLoggingAndReasons() = runTest {
        val dao = FakeScanIssueDao()

        // 1. Unreadable / Corrupt file
        dao.insert(
            ScanIssueEntity(
                filePath = "content://audiobooks/corrupt_file.mp3",
                fileName = "corrupt_file.mp3",
                reason = ScanIssueReason.UNREADABLE,
                details = "Corrupt ID3 header and truncated data stream"
            )
        )

        // 2. Timeout
        dao.insert(
            ScanIssueEntity(
                filePath = "content://audiobooks/hanging_file.m4b",
                fileName = "hanging_file.m4b",
                reason = ScanIssueReason.TIMEOUT,
                details = "Metadata extraction timed out after 5000ms"
            )
        )

        // 3. No audio track
        dao.insert(
            ScanIssueEntity(
                filePath = "content://audiobooks/silent_empty.mp3",
                fileName = "silent_empty.mp3",
                reason = ScanIssueReason.NO_AUDIO_TRACK,
                details = "No audio track detected or 0ms duration"
            )
        )

        // 4. Unsupported codec
        dao.insert(
            ScanIssueEntity(
                filePath = "content://audiobooks/unsupported.flac",
                fileName = "unsupported.flac",
                reason = ScanIssueReason.UNSUPPORTED_CODEC,
                details = "Unsupported file extension '.flac'"
            )
        )

        // 5. Permission denied
        dao.insert(
            ScanIssueEntity(
                filePath = "content://audiobooks/restricted.mp3",
                fileName = "restricted.mp3",
                reason = ScanIssueReason.PERMISSION_DENIED,
                details = "Permission denied by system storage"
            )
        )

        val recorded = dao.getAllScanIssuesSync()
        assertEquals(5, recorded.size)
        assertEquals(ScanIssueReason.UNREADABLE, recorded[0].reason)
        assertEquals(ScanIssueReason.TIMEOUT, recorded[1].reason)
        assertEquals(ScanIssueReason.NO_AUDIO_TRACK, recorded[2].reason)
        assertEquals(ScanIssueReason.UNSUPPORTED_CODEC, recorded[3].reason)
        assertEquals(ScanIssueReason.PERMISSION_DENIED, recorded[4].reason)

        // Verify clearAll
        dao.clearAll()
        assertTrue(dao.getAllScanIssuesSync().isEmpty())
    }

    @Test
    fun testScanProgressModel() {
        val progress = ScanProgress.Scanning(
            message = "Scanning 120 of 480",
            current = 120,
            total = 480
        )

        assertEquals("Scanning 120 of 480", progress.message)
        assertEquals(120, progress.current)
        assertEquals(480, progress.total)

        val cancelled = ScanProgress.Cancelled
        assertTrue(cancelled is ScanProgress)
    }
}
