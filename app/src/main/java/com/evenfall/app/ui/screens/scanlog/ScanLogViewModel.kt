package com.evenfall.app.ui.screens.scanlog

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evenfall.app.data.db.dao.ScanIssueDao
import com.evenfall.app.data.db.entity.ScanIssueEntity
import com.evenfall.app.util.BookmarkExporter
import com.evenfall.app.util.TimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScanLogUiState(
    val issues: List<ScanIssueEntity> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class ScanLogViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val scanIssueDao: ScanIssueDao
) : ViewModel() {

    val issues: StateFlow<List<ScanIssueEntity>> = scanIssueDao.getAllScanIssues()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun clearLog() {
        viewModelScope.launch {
            scanIssueDao.clearAll()
        }
    }

    fun shareLog() {
        val list = issues.value
        if (list.isEmpty()) return

        val sb = StringBuilder()
        sb.append("==================================================\n")
        sb.append("Evenfall — Audiobook Scan Log (${list.size} issues)\n")
        sb.append("==================================================\n\n")

        for ((index, issue) in list.withIndex()) {
            val date = "${TimeUtils.formatDate(issue.timestamp)} ${TimeUtils.formatTimeOfDay(issue.timestamp)}"
            sb.append("${index + 1}. [${issue.reason}] ${issue.fileName}\n")
            sb.append("   Path: ${issue.filePath}\n")
            sb.append("   Reason: ${issue.details}\n")
            sb.append("   Logged: $date\n\n")
        }

        BookmarkExporter.shareExport(context, sb.toString(), "Evenfall Scan Log")
    }
}
