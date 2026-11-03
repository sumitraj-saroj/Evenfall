package com.evenfall.app.ui.screens.fixlibrary

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.evenfall.app.R
import com.evenfall.app.data.scanner.MatchConfidence
import com.evenfall.app.data.scanner.NewBookMatchInfo
import com.evenfall.app.data.scanner.RelinkMatchResult

private val SerifFont = FontFamily.Serif

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixLibraryScreen(
    onBack: () -> Unit,
    hasMiniPlayer: Boolean = false,
    viewModel: FixLibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (_: Exception) {}
            viewModel.onFolderSelected(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.fix_library_title),
                        fontFamily = SerifFont,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        bottomBar = {
            val hasMatches = uiState.selectedMatches.isNotEmpty()
            if (uiState.analysis != null && !uiState.isComplete) {
                Surface(
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                            .padding(bottom = if (hasMiniPlayer) 80.dp else 0.dp)
                    ) {
                        Button(
                            onClick = viewModel::applyRelink,
                            enabled = hasMatches && !uiState.isApplying,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (uiState.isApplying) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${stringResource(R.string.confirm_relink)} (${uiState.selectedMatches.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                bottom = if (hasMiniPlayer) 90.dp else 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info Card
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Library Recovery",
                                fontFamily = SerifFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.fix_library_subtitle),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { folderPicker.launch(null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.select_new_folder), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Scanning state
            if (uiState.isScanning) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = uiState.scanMessage ?: "Scanning...",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Error message
            uiState.errorMessage?.let { error ->
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Success state
            if (uiState.isComplete) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.relink_success, uiState.relinkedCount),
                                fontFamily = SerifFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = onBack,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.back_to_library), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // REVIEW SECTIONS: Matched, Ambiguous, Unmatched
            uiState.analysis?.let { analysis ->
                // 1. Matched Books Section
                if (analysis.matched.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.matched_books_section, analysis.matched.size),
                            fontFamily = SerifFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(analysis.matched, key = { it.oldBook.id }) { match ->
                        val isChecked = uiState.selectedMatches.containsKey(match.oldBook.id)
                        val candidate = match.matchedCandidate
                        MatchedBookCard(
                            match = match,
                            isChecked = isChecked,
                            onToggle = { checked ->
                                if (candidate != null) {
                                    viewModel.toggleMatch(match.oldBook.id, checked, candidate)
                                }
                            }
                        )
                    }
                }

                // 2. Ambiguous Books Section
                if (analysis.ambiguous.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.ambiguous_books_section, analysis.ambiguous.size),
                            fontFamily = SerifFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }

                    items(analysis.ambiguous, key = { it.oldBook.id }) { match ->
                        val selectedCandidate = uiState.selectedMatches[match.oldBook.id]
                        AmbiguousBookCard(
                            match = match,
                            selectedCandidate = selectedCandidate,
                            onSelectCandidate = { candidate ->
                                if (candidate != null) {
                                    viewModel.toggleMatch(match.oldBook.id, true, candidate)
                                } else {
                                    viewModel.toggleMatch(match.oldBook.id, false, match.candidates.first().newBook)
                                }
                            }
                        )
                    }
                }

                // 3. Unmatched Books Section
                if (analysis.unmatchedOld.isNotEmpty() || analysis.unmatchedNew.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.unmatched_books_section, analysis.unmatchedOld.size + analysis.unmatchedNew.size),
                            fontFamily = SerifFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }

                    items(analysis.unmatchedOld, key = { "unmatched_old_${it.oldBook.id}" }) { match ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = match.oldBook.title,
                                    fontFamily = SerifFont,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = match.oldBook.author,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Not found in new folder. Book is preserved in library as unavailable.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchedBookCard(
    match: RelinkMatchResult,
    isChecked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = onToggle,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = match.oldBook.title,
                    fontFamily = SerifFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = match.oldBook.author,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = match.matchedCandidate?.title ?: "Matched folder",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun AmbiguousBookCard(
    match: RelinkMatchResult,
    selectedCandidate: NewBookMatchInfo?,
    onSelectCandidate: (NewBookMatchInfo?) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = match.oldBook.title,
                fontFamily = SerifFont,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                text = match.oldBook.author,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Choose match:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            match.candidates.forEach { candidate ->
                val isChosen = selectedCandidate?.identifier == candidate.newBook.identifier
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isChosen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clickable { onSelectCandidate(candidate.newBook) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = candidate.newBook.title,
                            fontSize = 13.sp,
                            fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                            color = if (isChosen) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${candidate.score}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Skip option
            OutlinedButton(
                onClick = { onSelectCandidate(null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(stringResource(R.string.skip_book), fontSize = 12.sp)
            }
        }
    }
}
