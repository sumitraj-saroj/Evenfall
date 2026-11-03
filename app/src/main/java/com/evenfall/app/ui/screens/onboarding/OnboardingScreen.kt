package com.evenfall.app.ui.screens.onboarding

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.evenfall.app.R
import com.evenfall.app.data.backup.RestoreMergeMode
import com.evenfall.app.data.datastore.ScanMode
import com.evenfall.app.data.scanner.ScannedBookPreview
import com.evenfall.app.ui.components.SparkleDecorations
import com.evenfall.app.ui.components.WavyProgressLine
import com.evenfall.app.ui.theme.CookieShape
import com.evenfall.app.ui.theme.SerifFont
import com.evenfall.app.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onNavigateToHome: () -> Unit,
    onBack: () -> Unit = {},
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scanMode by viewModel.scanMode.collectAsState()
    val pendingRestoreBackup by viewModel.pendingRestoreBackup.collectAsState()
    val restoreAnalysis by viewModel.restoreAnalysis.collectAsState()
    val restoreMergeMode by viewModel.restoreMergeMode.collectAsState()
    val restoreMessage by viewModel.restoreMessage.collectAsState()
    val isRestoring by viewModel.isRestoring.collectAsState()
    var showScanModeSheet by remember { mutableStateOf(false) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onFolderSelected(uri)
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onRestoreBackupSelected(uri)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Floating sparkles decoration
        SparkleDecorations()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    modifier = Modifier.statusBarsPadding()
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (val state = uiState) {
                        is OnboardingUiState.Found -> {
                            Button(
                                onClick = { viewModel.confirmAddBooks(onNavigateToHome) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                enabled = !isRestoring
                            ) {
                                if (isRestoring) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                val label = if (pendingRestoreBackup != null) {
                                    val matchedCount = restoreAnalysis?.matched?.size ?: 0
                                    "Restore & Add ${state.books.size} books ($matchedCount matched)"
                                } else {
                                    "Add ${state.books.size} books"
                                }
                                Text(
                                    text = label,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        is OnboardingUiState.Scanning -> {
                            Button(
                                onClick = {},
                                enabled = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(28.dp)
                            ) {
                                Text(stringResource(R.string.scanning_progress_simple), fontSize = 16.sp)
                            }
                        }
                        else -> {
                            Button(
                                onClick = { folderPickerLauncher.launch(null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Select Audiobooks folder",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = { restoreBackupLauncher.launch(arrayOf("application/json", "*/*")) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(28.dp)
                            ) {
                                Icon(Icons.Default.Replay, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.restore_from_backup),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = { showScanModeSheet = true }) {
                        Text(
                            text = "Change how books are found",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top wavy line decoration
                WavyProgressLine(
                    progress = if (uiState is OnboardingUiState.Scanning) 0.6f else 0.45f,
                    isPlaying = true,
                    activeColor = MaterialTheme.colorScheme.primary,
                    inactiveColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    modifier = Modifier.padding(horizontal = 40.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (restoreMessage != null && pendingRestoreBackup != null && uiState !is OnboardingUiState.Found) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = restoreMessage ?: "",
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                when (val state = uiState) {
                    is OnboardingUiState.Found -> {
                        Text(
                            text = if (pendingRestoreBackup != null) "Restore & Add books" else "We found ${state.books.size} books",
                            style = MaterialTheme.typography.displaySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (pendingRestoreBackup != null) {
                                "${restoreAnalysis?.matched?.size ?: 0} of ${pendingRestoreBackup?.books?.size ?: 0} backup books matched to your library"
                            } else {
                                "Every folder or file in ${state.folderName} is a book"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (pendingRestoreBackup != null) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        stringResource(R.string.restore_merge_mode_title),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { viewModel.setRestoreMergeMode(RestoreMergeMode.MERGE) },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = restoreMergeMode == RestoreMergeMode.MERGE,
                                            onClick = { viewModel.setRestoreMergeMode(RestoreMergeMode.MERGE) }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.restore_mode_merge), fontSize = 13.sp)
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { viewModel.setRestoreMergeMode(RestoreMergeMode.KEEP_BACKUP) },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = restoreMergeMode == RestoreMergeMode.KEEP_BACKUP,
                                            onClick = { viewModel.setRestoreMergeMode(RestoreMergeMode.KEEP_BACKUP) }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.restore_mode_keep_backup), fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(state.books) { index, book ->
                                OnboardingBookItem(book = book, index = index)
                            }
                        }
                    }
                    is OnboardingUiState.Scanning -> {
                        Text(
                            text = "Scanning your library...",
                            style = MaterialTheme.typography.displaySmall,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                    is OnboardingUiState.Error -> {
                        Text(
                            text = "Could not find books",
                            style = MaterialTheme.typography.displaySmall,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                    OnboardingUiState.Idle -> {
                        Text(
                            text = "Add a folder. Evenfall does the rest.",
                            style = MaterialTheme.typography.displaySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Point Evenfall to your audiobooks folder. We'll extract titles, authors, and chapters automatically.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Bottom sheet for scan mode
        if (showScanModeSheet) {
            ModalBottomSheet(
                onDismissRequest = { showScanModeSheet = false },
                sheetState = rememberModalBottomSheetState()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "How books are organized",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.setScanMode(ScanMode.SUBFOLDER_IS_BOOK)
                                showScanModeSheet = false
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = scanMode == ScanMode.SUBFOLDER_IS_BOOK,
                            onClick = {
                                viewModel.setScanMode(ScanMode.SUBFOLDER_IS_BOOK)
                                showScanModeSheet = false
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(stringResource(R.string.subfolder_book_recommended), fontWeight = FontWeight.SemiBold)
                            Text(
                                "Multi-file and single-file audiobooks kept inside separate folders",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.setScanMode(ScanMode.FILE_IS_BOOK)
                                showScanModeSheet = false
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = scanMode == ScanMode.FILE_IS_BOOK,
                            onClick = {
                                viewModel.setScanMode(ScanMode.FILE_IS_BOOK)
                                showScanModeSheet = false
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(stringResource(R.string.single_file_book), fontWeight = FontWeight.SemiBold)
                            Text(
                                "Useful if you have standalone M4B or MP3 files in one folder",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        if (restoreMessage != null && pendingRestoreBackup == null) {
            AlertDialog(
                onDismissRequest = viewModel::clearRestoreMessage,
                title = { Text(stringResource(R.string.restore_from_backup), fontFamily = SerifFont, fontWeight = FontWeight.Bold) },
                text = { Text(restoreMessage ?: "") },
                confirmButton = {
                    TextButton(onClick = viewModel::clearRestoreMessage) {
                        Text(stringResource(R.string.ok))
                    }
                }
            )
        }
    }
}

@Composable
fun OnboardingBookItem(
    book: ScannedBookPreview,
    index: Int
) {
    // Pastel palette for avatars matching screenshot
    val avatarColors = listOf(
        Color(0xFF82B1FF) to Color(0xFF0D47A1),
        Color(0xFFE1BEE7) to Color(0xFF4A148C),
        Color(0xFFB3E5FC) to Color(0xFF01579B),
        Color(0xFFFFCCBC) to Color(0xFFBF360C)
    )
    val (bg, fg) = avatarColors[index % avatarColors.size]
    val initial = book.title.firstOrNull()?.uppercaseChar()?.toString() ?: "A"

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cookie-shaped avatar
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CookieShape(lobes = 10, depth = 0.16f))
                    .background(bg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    color = fg,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    fontFamily = SerifFont
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = SerifFont,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val durationText = TimeUtils.formatDurationWords(book.durationMs)
                val filesText = if (book.fileCount > 1) "${book.fileCount} audio files" else "1 file"
                Text(
                    text = "${book.author} · $durationText · $filesText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
