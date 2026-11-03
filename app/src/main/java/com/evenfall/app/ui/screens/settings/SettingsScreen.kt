package com.evenfall.app.ui.screens.settings

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import com.evenfall.app.playback.HeadsetAction
import java.util.Locale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.res.stringResource
import com.evenfall.app.R
import com.evenfall.app.data.backup.RestoreMergeMode
import com.evenfall.app.util.TimeUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.evenfall.app.data.datastore.ThemeMode
import com.evenfall.app.ui.components.SparkleDecorations
import com.evenfall.app.ui.theme.ColorSchemeOption
import com.evenfall.app.ui.theme.CookieShape
import com.evenfall.app.ui.theme.SerifFont

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenScanLog: () -> Unit = {},
    onOpenManageCollections: () -> Unit = {},
    onOpenStats: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    var showExportAllDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var exportAllFormat by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(com.evenfall.app.util.ExportFormat.MARKDOWN) }

    var showBtRationaleDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var btPermissionDeniedMessage by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            btPermissionDeniedMessage = null
            viewModel.setAutoResumeBluetooth(true)
        } else {
            btPermissionDeniedMessage = context.getString(R.string.bluetooth_permission_denied)
            viewModel.setAutoResumeBluetooth(false)
        }
    }

    val backupFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setBackupFolder(uri)
        }
    }

    val restoreFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onRestoreFileSelected(uri)
        }
    }

    val saveAllBookmarksLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/*")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val content = viewModel.exportAllBookmarks(exportAllFormat)
                com.evenfall.app.util.BookmarkExporter.writeToUri(context, uri, content)
            }
        }
    }

    // Appearance card palette adapting cleanly to dark and light modes
    val appearanceCardBg = if (isDark) Color(0xFF1E2D4A) else Color(0xFFB4D2F7).copy(alpha = 0.55f)
    val appearanceTextPrimary = if (isDark) Color(0xFFE2EDFD) else Color(0xFF0F1E36)
    val appearanceTextSecondary = if (isDark) Color(0xFFBFD8F8) else Color(0xFF1E3A5F)
    val appearanceTextMuted = if (isDark) Color(0xFF90B4EC) else Color(0xFF1E3A5F).copy(alpha = 0.85f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        SparkleDecorations()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Settings",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = SerifFont,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "Make Evenfall yours",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    modifier = Modifier.statusBarsPadding()
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // APPEARANCE CARD (matching Image 7)
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = appearanceCardBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Appearance",
                            fontFamily = SerifFont,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = appearanceTextPrimary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Theme",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = appearanceTextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // 3 Theme Preview Cards (Follow system, Light, Dark)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ThemePreviewCard(
                                title = "Follow system",
                                isSelected = uiState.themeMode == ThemeMode.FOLLOW_SYSTEM,
                                isSplit = true,
                                titleColor = appearanceTextSecondary,
                                onClick = { viewModel.setThemeMode(ThemeMode.FOLLOW_SYSTEM) },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            ThemePreviewCard(
                                title = "Light",
                                isSelected = uiState.themeMode == ThemeMode.LIGHT,
                                isDark = false,
                                titleColor = appearanceTextSecondary,
                                onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            ThemePreviewCard(
                                title = "Dark",
                                isSelected = uiState.themeMode == ThemeMode.DARK,
                                isDark = true,
                                titleColor = appearanceTextSecondary,
                                onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Pure Black Toggle Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CookieShape(lobes = 10, depth = 0.16f))
                                    .background(Color(0xFF1E40AF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Contrast,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pure black",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = appearanceTextPrimary
                                )
                                Text(
                                    text = "Saves battery on OLED screens in the dark theme.",
                                    fontSize = 12.sp,
                                    color = appearanceTextMuted
                                )
                            }

                            Switch(
                                checked = uiState.pureBlack,
                                onCheckedChange = viewModel::setPureBlack
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Color Scheme Swatches
                        Text(
                            text = "Color scheme",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = appearanceTextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Row 1: 5 swatches (Evenfall blue, Dynamic, Teal, Green, Orange)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            listOf(
                                ColorSchemeOption.EVENFALL_BLUE,
                                ColorSchemeOption.DYNAMIC,
                                ColorSchemeOption.TEAL,
                                ColorSchemeOption.GREEN,
                                ColorSchemeOption.ORANGE
                            ).forEach { option ->
                                ColorSchemeSwatch(
                                    option = option,
                                    isSelected = uiState.colorSchemeOption == option,
                                    onClick = { viewModel.setColorScheme(option) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Row 2: 2 swatches (Pink, Purple)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            listOf(
                                ColorSchemeOption.PINK,
                                ColorSchemeOption.PURPLE
                            ).forEach { option ->
                                ColorSchemeSwatch(
                                    option = option,
                                    isSelected = uiState.colorSchemeOption == option,
                                    onClick = { viewModel.setColorScheme(option) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = uiState.colorSchemeOption.displayName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = appearanceTextSecondary
                        )
                    }
                }

                // LISTENING CARD (matching Image 7)
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Listening",
                            fontFamily = SerifFont,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Skip amount",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "How far rewind and fast forward jump",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stepper row with live preview of skip buttons (matching Image 7)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Minus button
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(44.dp)
                                    .clickable { viewModel.updateSkipAmount(-5) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Decrease skip amount",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(20.dp))

                            // Live preview of buttons pill matching Image 7
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = if (isDark) Color(0xFF1E293B) else Color(0xFF334155).copy(alpha = 0.15f),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Replay,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CookieShape(lobes = 10, depth = 0.16f))
                                            .background(Color(0xFF2563EB)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(20.dp))

                            // Plus button
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(44.dp)
                                    .clickable { viewModel.updateSkipAmount(5) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Increase skip amount",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${uiState.skipAmountSeconds} seconds",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Auto-rewind after pause toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Auto-rewind after pause", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Rewinds a few seconds based on pause length",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.autoRewindEnabled,
                                onCheckedChange = viewModel::setAutoRewindEnabled
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Shake to extend sleep timer toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Shake to extend sleep timer", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Gently shake phone to add 15 minutes before sleep timer ends",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.shakeToExtendEnabled,
                                onCheckedChange = viewModel::setShakeToExtendEnabled
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Compact player toggle (default on)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Compact player", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Calm, streamlined layout with simplified controls",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.compactPlayer,
                                onCheckedChange = viewModel::setCompactPlayer
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Auto-mark finished toggle (default on)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.auto_mark_finished_title), fontWeight = FontWeight.SemiBold)
                                Text(
                                    stringResource(R.string.auto_mark_finished_desc),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.autoMarkFinished,
                                onCheckedChange = viewModel::setAutoMarkFinished
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Auto-play next in series toggle (default off)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.auto_play_next_in_series), fontWeight = FontWeight.SemiBold)
                                Text(
                                    stringResource(R.string.auto_play_next_in_series_desc),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.autoPlayNextInSeries,
                                onCheckedChange = viewModel::setAutoPlayNextInSeries
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // CONTROLS & HEADSET CARD
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = stringResource(R.string.controls_title),
                            fontFamily = SerifFont,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Player Gestures Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.player_gestures_title), fontWeight = FontWeight.SemiBold)
                                Text(
                                    stringResource(R.string.player_gestures_desc),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.playerGesturesEnabled,
                                onCheckedChange = viewModel::setPlayerGesturesEnabled
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Haptic Feedback Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.haptic_feedback_title), fontWeight = FontWeight.SemiBold)
                                Text(
                                    stringResource(R.string.haptic_feedback_desc),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.hapticFeedbackEnabled,
                                onCheckedChange = viewModel::setHapticFeedbackEnabled
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Headset Controls Header
                        Text(
                            text = stringResource(R.string.headset_controls_title),
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Headset Double-press Mapping
                        HeadsetActionRow(
                            title = stringResource(R.string.headset_double_press),
                            currentAction = uiState.headsetDoublePress,
                            onActionSelected = viewModel::setHeadsetDoublePress
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Headset Triple-press Mapping
                        HeadsetActionRow(
                            title = stringResource(R.string.headset_triple_press),
                            currentAction = uiState.headsetTriplePress,
                            onActionSelected = viewModel::setHeadsetTriplePress
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Headset Long-press Mapping
                        HeadsetActionRow(
                            title = stringResource(R.string.headset_long_press),
                            currentAction = uiState.headsetLongPress,
                            onActionSelected = viewModel::setHeadsetLongPress
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Auto-resume on Bluetooth / Car connect
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.bluetooth_autoresume_title), fontWeight = FontWeight.SemiBold)
                                Text(
                                    stringResource(R.string.bluetooth_autoresume_desc),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.autoResumeBluetoothEnabled,
                                onCheckedChange = { enable ->
                                    if (enable) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            val permissionCheck = ContextCompat.checkSelfPermission(
                                                context,
                                                android.Manifest.permission.BLUETOOTH_CONNECT
                                            )
                                            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                                                showBtRationaleDialog = true
                                            } else {
                                                viewModel.setAutoResumeBluetooth(true)
                                            }
                                        } else {
                                            viewModel.setAutoResumeBluetooth(true)
                                        }
                                    } else {
                                        viewModel.setAutoResumeBluetooth(false)
                                    }
                                }
                            )
                        }

                        if (btPermissionDeniedMessage != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = btPermissionDeniedMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // BEDTIME SCHEDULE CARD
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.bedtime_title),
                                    fontFamily = SerifFont,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.bedtime_desc),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.bedtimeEnabled,
                                onCheckedChange = viewModel::setBedtimeEnabled
                            )
                        }

                        if (uiState.bedtimeEnabled) {
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(14.dp))

                            // Bedtime Start Time
                            Text(
                                text = stringResource(R.string.bedtime_start_time),
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(21 to 0, 22 to 0, 22 to 30, 23 to 0, 23 to 30).forEach { (h, m) ->
                                    val isSelected = uiState.bedtimeStartHour == h && uiState.bedtimeStartMinute == m
                                    val label = String.format(Locale.US, "%02d:%02d", h, m)
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.clickable {
                                            viewModel.setBedtimeStartHour(h)
                                            viewModel.setBedtimeStartMinute(m)
                                        }
                                    ) {
                                        Text(
                                            text = label,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Timer Duration
                            Text(
                                text = stringResource(R.string.bedtime_duration),
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(15, 30, 45, 60, -1).forEach { dur ->
                                    val isSelected = uiState.bedtimeDurationMinutes == dur
                                    val label = if (dur == -1) "End of ch." else "${dur}m"
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.clickable { viewModel.setBedtimeDurationMinutes(dur) }
                                    ) {
                                        Text(
                                            text = label,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Mode (Auto-start vs Prompt)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.bedtime_auto_start),
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = if (uiState.bedtimeAutoStart) "Starts timer automatically upon playback" else "Shows one-tap prompt upon playback",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = uiState.bedtimeAutoStart,
                                    onCheckedChange = viewModel::setBedtimeAutoStart
                                )
                            }
                        }
                    }
                }

                // BACKUP & LIBRARY FOLDERS CARD
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = stringResource(R.string.backup_and_library),
                            fontFamily = SerifFont,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Active scanning progress indicator or cancelled resume banner
                        val currentProgress = scanProgress
                        if (currentProgress is com.evenfall.app.data.scanner.ScanProgress.Scanning) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (currentProgress.total > 0) {
                                                stringResource(R.string.scanning_progress, currentProgress.current, currentProgress.total)
                                            } else {
                                                currentProgress.message
                                            },
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = viewModel::cancelScan,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = stringResource(R.string.cancel_scan),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    com.evenfall.app.ui.components.WavyProgressLine(
                                        progress = if (currentProgress.total > 0) currentProgress.current.toFloat() / currentProgress.total else 0.5f,
                                        isPlaying = true,
                                        activeColor = MaterialTheme.colorScheme.primary,
                                        inactiveColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        } else if (currentProgress is com.evenfall.app.data.scanner.ScanProgress.Cancelled) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Scan paused",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = viewModel::rescanLibrary) {
                                        Text(stringResource(R.string.resume_scan))
                                    }
                                }
                            }
                        }

                        // Rescan library button
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = viewModel::rescanLibrary)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Rescan audiobooks", fontWeight = FontWeight.SemiBold)
                                    Text("Detect new, renamed, or removed files", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Listening stats button
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onOpenStats)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.QueryStats, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(stringResource(R.string.stats_title), fontWeight = FontWeight.SemiBold)
                                    Text(
                                        stringResource(R.string.stats_nav_desc),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Manage collections button
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onOpenManageCollections)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Bookmarks, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(stringResource(R.string.manage_collections), fontWeight = FontWeight.SemiBold)
                                    Text(
                                        stringResource(R.string.collections_title),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Scan log button
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onOpenScanLog)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(stringResource(R.string.scan_log), fontWeight = FontWeight.SemiBold)
                                    Text(
                                        stringResource(R.string.scan_log_desc),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Backup folder picker row
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { backupFolderLauncher.launch(null) }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource(R.string.backup_folder_title), fontWeight = FontWeight.SemiBold)
                                    val folderText = if (uiState.backupFolderUri != null) {
                                        Uri.parse(uiState.backupFolderUri).lastPathSegment?.substringAfterLast(':') ?: uiState.backupFolderUri
                                    } else {
                                        stringResource(R.string.backup_folder_none)
                                    }
                                    Text(
                                        folderText ?: "",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )
                                }
                                if (uiState.backupFolderUri != null) {
                                    IconButton(onClick = viewModel::clearBackupFolder) {
                                        Icon(
                                            Icons.Default.Remove,
                                            contentDescription = "Clear backup folder",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Back up now button
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    enabled = !uiState.backupFolderUri.isNullOrBlank() && !uiState.isBackingUp,
                                    onClick = viewModel::backupNow
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (uiState.isBackingUp) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(
                                        Icons.Default.Save,
                                        contentDescription = null,
                                        tint = if (!uiState.backupFolderUri.isNullOrBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        stringResource(R.string.backup_now),
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (!uiState.backupFolderUri.isNullOrBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                    val lastBackupText = uiState.lastBackupTimestamp?.let {
                                        stringResource(R.string.last_backup_format, "${TimeUtils.formatDate(it)} ${TimeUtils.formatTimeOfDay(it)}")
                                    } ?: stringResource(R.string.never_backed_up)
                                    Text(
                                        lastBackupText,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Restore from backup button
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { restoreFileLauncher.launch(arrayOf("application/json", "*/*")) }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Replay, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(stringResource(R.string.restore_from_backup), fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "Restore positions, bookmarks, and settings from a JSON backup file",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Export all bookmarks button
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showExportAllDialog = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(stringResource(R.string.export_all_bookmarks), fontWeight = FontWeight.SemiBold)
                                    Text(
                                        stringResource(R.string.export_all_bookmarks_desc),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // ABOUT & PRIVACY CARD
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "About Evenfall",
                            fontFamily = SerifFont,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Evenfall is 100% offline. Zero network permission is requested or used. All bookmarks, listening history, and playback positions stay strictly on your device.",
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Version 1.0.0 · Apache 2.0 License",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (com.evenfall.app.BuildConfig.DEBUG) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Developer & Stress Testing",
                                fontFamily = SerifFont,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Generate synthetic library of 500+ books with 20+ hour M4B files (100+ chapters) to verify smooth scrolling, stable memory, and position persistence.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.generateSyntheticLibrary() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Generate 500+", fontSize = 12.sp)
                                }
                                androidx.compose.material3.OutlinedButton(
                                    onClick = { viewModel.clearSyntheticLibrary() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Clear synthetic", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Bluetooth Permission Rationale Dialog
        if (showBtRationaleDialog) {
            AlertDialog(
                onDismissRequest = { showBtRationaleDialog = false },
                title = { Text(stringResource(R.string.bluetooth_autoresume_title)) },
                text = { Text(stringResource(R.string.bluetooth_permission_rationale)) },
                confirmButton = {
                    Button(
                        onClick = {
                            showBtRationaleDialog = false
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                btPermissionLauncher.launch(android.Manifest.permission.BLUETOOTH_CONNECT)
                            } else {
                                viewModel.setAutoResumeBluetooth(true)
                            }
                        }
                    ) {
                        Text(stringResource(R.string.grant_permission))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBtRationaleDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        // Restore Review Dialog
        if (uiState.pendingRestoreBackup != null) {
            val backup = uiState.pendingRestoreBackup!!
            val analysis = uiState.restoreAnalysis
            AlertDialog(
                onDismissRequest = {
                    if (!uiState.isRestoring) viewModel.dismissRestoreReview()
                },
                title = {
                    Text(
                        stringResource(R.string.restore_dialog_title),
                        fontFamily = SerifFont,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Backup file created: ${TimeUtils.formatDate(backup.createdAt)} ${TimeUtils.formatTimeOfDay(backup.createdAt)}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val totalBackupBooks = backup.books.size
                        val matchedCount = analysis?.matched?.size ?: 0
                        Text(
                            text = stringResource(R.string.restore_dialog_books_found, totalBackupBooks, matchedCount),
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )

                        if (analysis != null) {
                            if (analysis.ambiguous.isNotEmpty()) {
                                Text(
                                    text = "${analysis.ambiguous.size} books matched with lower confidence.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            if (analysis.unmatchedOld.isNotEmpty()) {
                                Text(
                                    text = "${analysis.unmatchedOld.size} books in backup not found in current library.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Text(
                            text = stringResource(R.string.restore_merge_mode_title),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )

                        // Mode 1: MERGE
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.setRestoreMergeMode(RestoreMergeMode.MERGE) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.restoreMergeMode == RestoreMergeMode.MERGE,
                                onClick = { viewModel.setRestoreMergeMode(RestoreMergeMode.MERGE) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(stringResource(R.string.restore_mode_merge), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(stringResource(R.string.restore_mode_merge_desc), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Mode 2: KEEP_NEWER
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.setRestoreMergeMode(RestoreMergeMode.KEEP_NEWER) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.restoreMergeMode == RestoreMergeMode.KEEP_NEWER,
                                onClick = { viewModel.setRestoreMergeMode(RestoreMergeMode.KEEP_NEWER) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(stringResource(R.string.restore_mode_keep_newer), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(stringResource(R.string.restore_mode_keep_newer_desc), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Mode 3: KEEP_BACKUP
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.setRestoreMergeMode(RestoreMergeMode.KEEP_BACKUP) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.restoreMergeMode == RestoreMergeMode.KEEP_BACKUP,
                                onClick = { viewModel.setRestoreMergeMode(RestoreMergeMode.KEEP_BACKUP) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(stringResource(R.string.restore_mode_keep_backup), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(stringResource(R.string.restore_mode_keep_backup_desc), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = viewModel::confirmRestore,
                        enabled = !uiState.isRestoring
                    ) {
                        if (uiState.isRestoring) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(stringResource(R.string.apply_restore))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = viewModel::dismissRestoreReview,
                        enabled = !uiState.isRestoring
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        // Restore Success Dialog
        if (uiState.restoreSuccessMessage != null) {
            AlertDialog(
                onDismissRequest = viewModel::clearStatusMessage,
                title = { Text("Restore Complete", fontFamily = SerifFont, fontWeight = FontWeight.Bold) },
                text = { Text(uiState.restoreSuccessMessage ?: "") },
                confirmButton = {
                    TextButton(onClick = viewModel::clearStatusMessage) {
                        Text("OK")
                    }
                }
            )
        }

        // Restore Error Dialog
        if (uiState.restoreErrorMessage != null) {
            AlertDialog(
                onDismissRequest = viewModel::clearStatusMessage,
                title = { Text("Restore Failed", fontFamily = SerifFont, fontWeight = FontWeight.Bold) },
                text = { Text(uiState.restoreErrorMessage ?: "") },
                confirmButton = {
                    TextButton(onClick = viewModel::clearStatusMessage) {
                        Text("OK")
                    }
                }
            )
        }

        // Export All Bookmarks Dialog
        if (showExportAllDialog) {
            AlertDialog(
                onDismissRequest = { showExportAllDialog = false },
                title = {
                    Text(
                        stringResource(R.string.export_all_bookmarks),
                        fontFamily = SerifFont,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Choose export format:",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { exportAllFormat = com.evenfall.app.util.ExportFormat.MARKDOWN }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = exportAllFormat == com.evenfall.app.util.ExportFormat.MARKDOWN,
                                onClick = { exportAllFormat = com.evenfall.app.util.ExportFormat.MARKDOWN }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.export_format_markdown), fontSize = 14.sp)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { exportAllFormat = com.evenfall.app.util.ExportFormat.TEXT }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = exportAllFormat == com.evenfall.app.util.ExportFormat.TEXT,
                                onClick = { exportAllFormat = com.evenfall.app.util.ExportFormat.TEXT }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.export_format_text), fontSize = 14.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val content = viewModel.exportAllBookmarks(exportAllFormat)
                                com.evenfall.app.util.BookmarkExporter.shareExport(
                                    context,
                                    content,
                                    "Evenfall - All Bookmarks"
                                )
                                showExportAllDialog = false
                            }
                        }
                    ) {
                        Text(stringResource(R.string.share_export))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            val ext = if (exportAllFormat == com.evenfall.app.util.ExportFormat.MARKDOWN) "md" else "txt"
                            saveAllBookmarksLauncher.launch("evenfall-all-bookmarks.$ext")
                            showExportAllDialog = false
                        }
                    ) {
                        Text(stringResource(R.string.save_export))
                    }
                }
            )
        }
    }
}

@Composable
fun ThemePreviewCard(
    title: String,
    isSelected: Boolean,
    isDark: Boolean = false,
    isSplit: Boolean = false,
    titleColor: Color = Color(0xFF1E3A5F),
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = if (isSelected) 2.5.dp else 1.dp,
                    color = if (isSelected) Color(0xFF1E3A8A) else Color.White.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(16.dp)
                )
                .background(
                    if (isSplit) Color.White
                    else if (isDark) Color(0xFF0F172A)
                    else Color.White
                )
        ) {
            if (isSplit) {
                // Diagonal dark split half
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        color = Color(0xFF0F172A),
                        topLeft = Offset(size.width * 0.45f, 0f),
                        size = Size(size.width * 0.55f, size.height)
                    )
                }
            }

            // Mini player mockup inside preview card
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF60A5FA))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(3.dp)
                        .background(if (isDark) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.5f))
                )
                Spacer(modifier = Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(2.5.dp)
                        .background(Color(0xFF94A3B8))
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CookieShape(lobes = 8, depth = 0.16f))
                        .background(Color(0xFF1E3A8A))
                )
            }

            // Checkmark badge if selected
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E3A8A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = titleColor
        )
    }
}

@Composable
fun ColorSchemeSwatch(
    option: ColorSchemeOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val size = 38.dp
    val shape = if (isSelected) CookieShape(lobes = 10, depth = 0.16f) else CircleShape

    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when (option) {
            ColorSchemeOption.EVENFALL_BLUE -> {
                // Quadrant swatch matching screenshot
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = this.size.width
                    val h = this.size.height
                    drawRect(Color(0xFF1E3A8A), topLeft = Offset.Zero, size = Size(w / 2f, h / 2f))
                    drawRect(Color(0xFF93C5FD), topLeft = Offset(w / 2f, 0f), size = Size(w / 2f, h / 2f))
                    drawRect(Color(0xFFE9D5FF), topLeft = Offset(0f, h / 2f), size = Size(w / 2f, h / 2f))
                    drawRect(Color(0xFF38BDF8), topLeft = Offset(w / 2f, h / 2f), size = Size(w / 2f, h / 2f))
                }
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            ColorSchemeOption.DYNAMIC -> {
                // 4 pastel quadrants with sparkles
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = this.size.width
                    val h = this.size.height
                    drawRect(Color(0xFF94A3B8), topLeft = Offset.Zero, size = Size(w / 2f, h / 2f))
                    drawRect(Color(0xFFBAE6FD), topLeft = Offset(w / 2f, 0f), size = Size(w / 2f, h / 2f))
                    drawRect(Color(0xFFFBCFE8), topLeft = Offset(0f, h / 2f), size = Size(w / 2f, h / 2f))
                    drawRect(Color(0xFFFEF08A), topLeft = Offset(w / 2f, h / 2f), size = Size(w / 2f, h / 2f))
                }
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            ColorSchemeOption.TEAL -> {
                SplitColorSwatch(
                    topColor = Color(0xFF00796B),
                    bottomColor = Color(0xFFB2DFDB),
                    isSelected = isSelected
                )
            }
            ColorSchemeOption.GREEN -> {
                SplitColorSwatch(
                    topColor = Color(0xFF388E3C),
                    bottomColor = Color(0xFFDCEDC8),
                    isSelected = isSelected
                )
            }
            ColorSchemeOption.ORANGE -> {
                SplitColorSwatch(
                    topColor = Color(0xFFD84315),
                    bottomColor = Color(0xFFFFE0B2),
                    isSelected = isSelected
                )
            }
            ColorSchemeOption.PINK -> {
                SplitColorSwatch(
                    topColor = Color(0xFFC2185B),
                    bottomColor = Color(0xFFF8BBD0),
                    isSelected = isSelected
                )
            }
            ColorSchemeOption.PURPLE -> {
                SplitColorSwatch(
                    topColor = Color(0xFF7B1FA2),
                    bottomColor = Color(0xFFE1BEE7),
                    isSelected = isSelected
                )
            }
        }
    }
}

@Composable
fun SplitColorSwatch(
    topColor: Color,
    bottomColor: Color,
    isSelected: Boolean
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(topColor, topLeft = Offset.Zero, size = Size(size.width, size.height / 2f))
            drawRect(bottomColor, topLeft = Offset(0f, size.height / 2f), size = Size(size.width, size.height / 2f))
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun HeadsetActionRow(
    title: String,
    currentAction: String,
    onActionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentEnum = try {
        HeadsetAction.valueOf(currentAction)
    } catch (_: Exception) {
        HeadsetAction.SKIP_FORWARD
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )

        Box {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.clickable { expanded = true }
            ) {
                Text(
                    text = stringResource(currentEnum.labelResId),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                HeadsetAction.values().forEach { action ->
                    DropdownMenuItem(
                        text = { Text(stringResource(action.labelResId)) },
                        onClick = {
                            onActionSelected(action.name)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
