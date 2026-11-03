package com.evenfall.app.data.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.evenfall.app.data.datastore.PreferencesManager
import com.evenfall.app.data.db.dao.BookDao
import com.evenfall.app.data.db.dao.BookmarkDao
import com.evenfall.app.data.db.dao.ChapterDao
import com.evenfall.app.data.db.dao.HistoryDao
import com.evenfall.app.data.db.entity.BookEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val bookDao: BookDao,
    private val chapterDao: ChapterDao,
    private val bookmarkDao: BookmarkDao,
    private val historyDao: HistoryDao,
    private val preferencesManager: PreferencesManager
) {
    companion object {
        const val BACKUP_WORK_NAME = "evenfall_daily_backup"
    }

    suspend fun getBackupFolderUri(): String? {
        return preferencesManager.backupFolderUri.first()
    }

    suspend fun createBackup(): EvenfallBackup = withContext(Dispatchers.IO) {
        val books = bookDao.getAllBooksSync()
        val backupBooks = books.map { book ->
            val chapters = chapterDao.getChaptersForBookSync(book.id).map { ch ->
                BackupChapter(
                    chapterIndex = ch.chapterIndex,
                    title = ch.title,
                    titleOverride = ch.titleOverride,
                    startMs = ch.startMs,
                    durationMs = ch.durationMs,
                    fileUri = ch.fileUri
                )
            }
            val bookmarks = bookmarkDao.getBookmarksForBookSync(book.id).map { bm ->
                BackupBookmark(
                    chapterIndex = bm.chapterIndex,
                    chapterTitle = bm.chapterTitle,
                    positionMs = bm.positionMs,
                    title = bm.title,
                    type = bm.type,
                    note = bm.note,
                    timestamp = bm.timestamp
                )
            }
            val history = historyDao.getHistoryForBookSync(book.id).map { h ->
                BackupHistory(
                    eventType = h.eventType,
                    timestamp = h.timestamp,
                    positionMs = h.positionMs,
                    chapterIndex = h.chapterIndex,
                    durationInterruptedMs = h.durationInterruptedMs
                )
            }
            BackupBook(
                title = book.title,
                author = book.author,
                folderUri = book.folderUri,
                folderIdentifier = book.folderIdentifier,
                totalDurationMs = book.totalDurationMs,
                fileCount = book.fileCount,
                currentPositionMs = book.currentPositionMs,
                currentChapterIndex = book.currentChapterIndex,
                chapterPositionMs = book.chapterPositionMs,
                playbackSpeed = book.playbackSpeed,
                isFinished = book.isFinished,
                finishedAt = book.finishedAt,
                lastPlayedTimestamp = book.lastPlayedTimestamp,
                titleOverride = book.titleOverride,
                authorOverride = book.authorOverride,
                narrator = book.narrator,
                narratorOverride = book.narratorOverride,
                seriesName = book.seriesName,
                seriesOrder = book.seriesOrder,
                coverOverrideUri = book.coverOverrideUri,
                chapters = chapters,
                bookmarks = bookmarks,
                history = history
            )
        }

        val settings = BackupSettings(
            themeMode = preferencesManager.themeMode.first().name,
            pureBlack = preferencesManager.pureBlack.first(),
            colorScheme = preferencesManager.colorSchemeOption.first().name,
            skipAmountSeconds = preferencesManager.skipAmountSeconds.first(),
            autoRewindEnabled = preferencesManager.autoRewindEnabled.first(),
            shakeToExtendEnabled = preferencesManager.shakeToExtendEnabled.first(),
            fadeOutDurationSeconds = preferencesManager.fadeOutDurationSeconds.first(),
            silenceSkippingEnabled = preferencesManager.silenceSkippingEnabled.first(),
            volumeBoostLevel = preferencesManager.volumeBoostLevel.first(),
            defaultPlaybackSpeed = preferencesManager.defaultPlaybackSpeed.first(),
            scanMode = preferencesManager.scanMode.first().name,
            compactPlayer = preferencesManager.compactPlayer.first(),
            autoMarkFinished = preferencesManager.autoMarkFinished.first()
        )

        EvenfallBackup(
            schemaVersion = BackupSerializer.CURRENT_SCHEMA_VERSION,
            app = BackupSerializer.APP_IDENTIFIER,
            createdAt = System.currentTimeMillis(),
            settings = settings,
            books = backupBooks
        )
    }

    suspend fun writeBackupToFolder(treeUri: Uri, backup: EvenfallBackup): Result<String> = withContext(Dispatchers.IO) {
        try {
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
                ?: return@withContext Result.failure(IllegalStateException("Cannot open backup folder"))

            val fileName = BackupRotator.formatBackupFileName(backup.createdAt)
            val existing = rootDoc.findFile(fileName)
            val targetDoc = existing ?: rootDoc.createFile("application/json", fileName)
                ?: return@withContext Result.failure(IllegalStateException("Cannot create backup file"))

            val jsonString = BackupSerializer.serialize(backup)
            context.contentResolver.openOutputStream(targetDoc.uri, "wt")?.use { os ->
                OutputStreamWriter(os).use { writer ->
                    writer.write(jsonString)
                }
            } ?: return@withContext Result.failure(IllegalStateException("Cannot write to backup file"))

            // Rotate: keep last 7
            val files = rootDoc.listFiles().map { doc ->
                BackupFileInfo(
                    name = doc.name ?: "",
                    uriString = doc.uri.toString(),
                    lastModified = doc.lastModified()
                )
            }
            val toDelete = BackupRotator.findFilesToDelete(files)
            for (item in toDelete) {
                rootDoc.findFile(item.name)?.delete()
            }

            preferencesManager.setLastBackupTimestamp(backup.createdAt)
            Result.success(fileName)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun readBackupFromUri(fileUri: Uri): Result<EvenfallBackup> = withContext(Dispatchers.IO) {
        try {
            val content = context.contentResolver.openInputStream(fileUri)?.use { input ->
                BufferedReader(InputStreamReader(input)).use { reader ->
                    reader.readText()
                }
            } ?: return@withContext Result.failure(IllegalStateException("Cannot open backup file"))

            val backup = BackupSerializer.deserialize(content)
            Result.success(backup)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun applyRestore(
        backup: EvenfallBackup,
        matchedBooks: List<Pair<BookEntity, BackupBook>>,
        mode: RestoreMergeMode
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var restoredCount = 0
            for ((currentBook, backupBook) in matchedBooks) {
                val currentBookmarks = bookmarkDao.getBookmarksForBookSync(currentBook.id)
                val currentHistory = historyDao.getHistoryForBookSync(currentBook.id)

                val mergeResult = BackupMergeEngine.mergeBook(
                    currentBook = currentBook,
                    currentBookmarks = currentBookmarks,
                    currentHistory = currentHistory,
                    backupBook = backupBook,
                    mode = mode
                )

                bookDao.update(mergeResult.updatedBook)

                for (bm in mergeResult.newBookmarksToInsert) {
                    bookmarkDao.insert(bm)
                }

                for (h in mergeResult.newHistoryToInsert) {
                    historyDao.insert(h)
                }

                restoredCount++
            }

            // Restore global settings
            preferencesManager.setPureBlack(backup.settings.pureBlack)
            preferencesManager.setSkipAmountSeconds(backup.settings.skipAmountSeconds)
            preferencesManager.setAutoRewindEnabled(backup.settings.autoRewindEnabled)
            preferencesManager.setShakeToExtendEnabled(backup.settings.shakeToExtendEnabled)
            preferencesManager.setFadeOutDurationSeconds(backup.settings.fadeOutDurationSeconds)
            preferencesManager.setSilenceSkippingEnabled(backup.settings.silenceSkippingEnabled)
            preferencesManager.setVolumeBoostLevel(backup.settings.volumeBoostLevel)
            preferencesManager.setDefaultPlaybackSpeed(backup.settings.defaultPlaybackSpeed)
            preferencesManager.setCompactPlayer(backup.settings.compactPlayer)
            preferencesManager.setAutoMarkFinished(backup.settings.autoMarkFinished)

            Result.success(restoredCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun scheduleDailyBackup() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresDeviceIdle(true)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<BackupWorker>(
            1, TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            BACKUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }

    fun cancelDailyBackup() {
        WorkManager.getInstance(context).cancelUniqueWork(BACKUP_WORK_NAME)
    }
}
