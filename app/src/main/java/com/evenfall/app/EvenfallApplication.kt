package com.evenfall.app

import android.app.Application
import com.evenfall.app.data.backup.BackupManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class EvenfallApplication : Application() {

    @Inject
    lateinit var backupManager: BackupManager

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val backupFolder = backupManager.getBackupFolderUri()
                if (!backupFolder.isNullOrBlank()) {
                    backupManager.scheduleDailyBackup()
                }
            } catch (_: Exception) {}
        }
    }
}
