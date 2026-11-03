package com.evenfall.app.data.backup

import java.util.Calendar
import java.util.Locale

data class BackupFileInfo(
    val name: String,
    val uriString: String,
    val lastModified: Long
)

object BackupRotator {
    const val MAX_BACKUPS_TO_KEEP = 7
    private val BACKUP_FILENAME_REGEX = Regex("^evenfall-backup-\\d{4}-\\d{2}-\\d{2}.*\\.json$")

    fun isBackupFile(name: String): Boolean {
        return BACKUP_FILENAME_REGEX.matches(name)
    }

    /**
     * Given a list of backup files, sorts them by name/date descending,
     * and returns the list of files that exceed `keepCount` (i.e. files that must be pruned).
     */
    fun findFilesToDelete(
        files: List<BackupFileInfo>,
        keepCount: Int = MAX_BACKUPS_TO_KEEP
    ): List<BackupFileInfo> {
        val backupFiles = files
            .filter { isBackupFile(it.name) }
            .sortedWith(
                compareByDescending<BackupFileInfo> { it.name }
                    .thenByDescending { it.lastModified }
            )

        if (backupFiles.size <= keepCount) {
            return emptyList()
        }

        return backupFiles.drop(keepCount)
    }

    /**
     * Formats the standard backup filename for a given timestamp.
     * e.g. evenfall-backup-2026-11-03.json
     */
    fun formatBackupFileName(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
        }
        val year = calendar.get(Calendar.YEAR)
        val month = String.format(Locale.US, "%02d", calendar.get(Calendar.MONTH) + 1)
        val day = String.format(Locale.US, "%02d", calendar.get(Calendar.DAY_OF_MONTH))
        return "evenfall-backup-$year-$month-$day.json"
    }
}
