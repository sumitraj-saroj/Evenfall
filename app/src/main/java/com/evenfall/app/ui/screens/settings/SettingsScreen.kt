package com.evenfall.app.ui.screens.settings

import android.net.Uri
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.res.stringResource
import com.evenfall.app.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportBackup(uri)
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
                            text = "Library & Backup",
                            fontFamily = SerifFont,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(14.dp))

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

                        // Export backup button
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { exportLauncher.launch("evenfall_backup.json") }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Export bookmarks and progress", fontWeight = FontWeight.SemiBold)
                                    Text("Save a local JSON backup file to device", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

                Spacer(modifier = Modifier.height(24.dp))
            }
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
