package com.evenfall.app.ui.theme

import androidx.compose.ui.graphics.Color

// Evenfall Blue (Primary signature palette)
val EvenfallBluePrimaryLight = Color(0xFF1B4D7E)
val EvenfallBlueOnPrimaryLight = Color(0xFFFFFFFF)
val EvenfallBluePrimaryContainerLight = Color(0xFFD6E4F7)
val EvenfallBlueOnPrimaryContainerLight = Color(0xFF0D2845)
val EvenfallBlueSecondaryLight = Color(0xFF4A657E)
val EvenfallBlueSurfaceLight = Color(0xFFF7F9FC)
val EvenfallBlueOnSurfaceLight = Color(0xFF191C20)
val EvenfallBlueSurfaceVariantLight = Color(0xFFDFE4ED)
val EvenfallBlueOnSurfaceVariantLight = Color(0xFF42474E)

val EvenfallBluePrimaryDark = Color(0xFF96CCFF)
val EvenfallBlueOnPrimaryDark = Color(0xFF003355)
val EvenfallBluePrimaryContainerDark = Color(0xFF1A3D63)
val EvenfallBlueOnPrimaryContainerDark = Color(0xFFD6E4F7)
val EvenfallBlueSecondaryDark = Color(0xFFB4C8DE)
val EvenfallBlueSurfaceDark = Color(0xFF111418)
val EvenfallBlueOnSurfaceDark = Color(0xFFE2E2E6)
val EvenfallBlueSurfaceVariantDark = Color(0xFF222B36)
val EvenfallBlueOnSurfaceVariantDark = Color(0xFFC2C7CF)

// Accent Colors for Library Cards & Book Theming
val HeroCardGradientStart = Color(0xFF6C9BE8)
val HeroCardGradientEnd = Color(0xFFA5B8F8)
val AmberCardBackground = Color(0xFFF7D97B)
val AmberAccent = Color(0xFFD97706)
val DeepSpaceBlue = Color(0xFF0C1929)
val DeepSpaceSurface = Color(0xFF122238)

// Color Scheme Presets
enum class ColorSchemeOption(val displayName: String, val primaryColor: Color) {
    EVENFALL_BLUE("Evenfall blue", Color(0xFF1B4D7E)),
    DYNAMIC("Material You", Color(0xFF88A4C4)),
    TEAL("Teal", Color(0xFF00796B)),
    GREEN("Green", Color(0xFF388E3C)),
    ORANGE("Orange", Color(0xFFE65100)),
    PINK("Pink", Color(0xFFC2185B)),
    PURPLE("Purple", Color(0xFF7B1FA2))
}
