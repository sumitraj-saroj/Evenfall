package com.evenfall.app.ui.screens.player

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

data class PlayerBookColors(
    val backgroundBrush: Brush,
    val onBackground: Color,
    val onBackgroundMuted: Color,
    val cardBackground: Color,
    val playButtonContainer: Color,
    val playButtonContent: Color,
    val skipButtonBackground: Color,
    val skipButtonContent: Color,
    val bottomToolbarContainer: Color,
    val bottomToolbarContent: Color,
    val activeSliderColor: Color,
    val inactiveSliderColor: Color,
    val undoChipBackground: Color,
    val undoChipContent: Color
)

object PlayerThemeExtractor {
    fun getColorsForBook(title: String, isDark: Boolean): PlayerBookColors {
        val clean = title.lowercase()
        return when {
            clean.contains("horizon") -> {
                // Beyond the Horizon: Warm Golden Amber Palette (matching Image 2)
                PlayerBookColors(
                    backgroundBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF7D97A),
                            Color(0xFFF3CE5A),
                            Color(0xFFEFBE3D)
                        )
                    ),
                    onBackground = Color(0xFF451A03),
                    onBackgroundMuted = Color(0xFF78350F).copy(alpha = 0.85f),
                    cardBackground = Color(0xFFFBE49D).copy(alpha = 0.65f),
                    playButtonContainer = Color(0xFF6B4314),
                    playButtonContent = Color(0xFFFFFBEB),
                    skipButtonBackground = Color(0xFFF7D171),
                    skipButtonContent = Color(0xFF5A350D),
                    bottomToolbarContainer = Color(0xFFF59E0B),
                    bottomToolbarContent = Color(0xFF451A03),
                    activeSliderColor = Color(0xFF6B4314),
                    inactiveSliderColor = Color(0xFF6B4314).copy(alpha = 0.25f),
                    undoChipBackground = Color(0xFFFDE68A),
                    undoChipContent = Color(0xFF78350F)
                )
            }
            clean.contains("echoes") -> {
                // Echoes of Tomorrow: Deep Cosmic Blue Palette (matching Image 3)
                PlayerBookColors(
                    backgroundBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0D253E),
                            Color(0xFF091C31),
                            Color(0xFF071424)
                        )
                    ),
                    onBackground = Color(0xFFE2E8F0),
                    onBackgroundMuted = Color(0xFF94A3B8),
                    cardBackground = Color(0xFF132F4C).copy(alpha = 0.7f),
                    playButtonContainer = Color(0xFF7DD3FC),
                    playButtonContent = Color(0xFF082F49),
                    skipButtonBackground = Color(0xFF1E4976),
                    skipButtonContent = Color(0xFFE0F2FE),
                    bottomToolbarContainer = Color(0xFF0284C7),
                    bottomToolbarContent = Color(0xFFE0F2FE),
                    activeSliderColor = Color(0xFF38BDF8),
                    inactiveSliderColor = Color(0xFF38BDF8).copy(alpha = 0.25f),
                    undoChipBackground = Color(0xFF0369A1),
                    undoChipContent = Color(0xFFE0F2FE)
                )
            }
            else -> {
                // Default Calming Evenfall Theme
                if (isDark) {
                    PlayerBookColors(
                        backgroundBrush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF121E2C),
                                Color(0xFF0E1824)
                            )
                        ),
                        onBackground = Color(0xFFE2E8F0),
                        onBackgroundMuted = Color(0xFF94A3B8),
                        cardBackground = Color(0xFF1C2D42),
                        playButtonContainer = Color(0xFF93C5FD),
                        playButtonContent = Color(0xFF0F172A),
                        skipButtonBackground = Color(0xFF1E3A5F),
                        skipButtonContent = Color(0xFFE2E8F0),
                        bottomToolbarContainer = Color(0xFF1D4ED8),
                        bottomToolbarContent = Color(0xFFFFFFFF),
                        activeSliderColor = Color(0xFF60A5FA),
                        inactiveSliderColor = Color(0xFF60A5FA).copy(alpha = 0.25f),
                        undoChipBackground = Color(0xFF1E3A5F),
                        undoChipContent = Color(0xFF93C5FD)
                    )
                } else {
                    PlayerBookColors(
                        backgroundBrush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFF3F7FD),
                                Color(0xFFE2ECFA)
                            )
                        ),
                        onBackground = Color(0xFF0F172A),
                        onBackgroundMuted = Color(0xFF475569),
                        cardBackground = Color(0xFFDCE8F8),
                        playButtonContainer = Color(0xFF1E40AF),
                        playButtonContent = Color(0xFFFFFFFF),
                        skipButtonBackground = Color(0xFFD6E4F8),
                        skipButtonContent = Color(0xFF1E3A8A),
                        bottomToolbarContainer = Color(0xFF2563EB),
                        bottomToolbarContent = Color(0xFFFFFFFF),
                        activeSliderColor = Color(0xFF1E40AF),
                        inactiveSliderColor = Color(0xFF1E40AF).copy(alpha = 0.25f),
                        undoChipBackground = Color(0xFFDBEAFE),
                        undoChipContent = Color(0xFF1E40AF)
                    )
                }
            }
        }
    }
}
