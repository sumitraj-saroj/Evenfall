package com.evenfall.app.data.backup

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class BackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BackupWorkerEntryPoint {
        fun backupManager(): BackupManager
    }

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                BackupWorkerEntryPoint::class.java
            )
            val backupManager = entryPoint.backupManager()
            val backupFolderUriStr = backupManager.getBackupFolderUri()
            if (backupFolderUriStr.isNullOrBlank()) {
                return Result.success()
            }
            val treeUri = Uri.parse(backupFolderUriStr)
            val backup = backupManager.createBackup()
            backupManager.writeBackupToFolder(treeUri, backup)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
