package com.evenfall.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightEvenfallColorScheme = lightColorScheme(
    primary = EvenfallBluePrimaryLight,
    onPrimary = EvenfallBlueOnPrimaryLight,
    primaryContainer = EvenfallBluePrimaryContainerLight,
    onPrimaryContainer = EvenfallBlueOnPrimaryContainerLight,
    secondary = EvenfallBlueSecondaryLight,
    surface = EvenfallBlueSurfaceLight,
    onSurface = EvenfallBlueOnSurfaceLight,
    surfaceVariant = EvenfallBlueSurfaceVariantLight,
    onSurfaceVariant = EvenfallBlueOnSurfaceVariantLight,
    background = EvenfallBlueSurfaceLight,
    onBackground = EvenfallBlueOnSurfaceLight
)

private val DarkEvenfallColorScheme = darkColorScheme(
    primary = EvenfallBluePrimaryDark,
    onPrimary = EvenfallBlueOnPrimaryDark,
    primaryContainer = EvenfallBluePrimaryContainerDark,
    onPrimaryContainer = EvenfallBlueOnPrimaryContainerDark,
    secondary = EvenfallBlueSecondaryDark,
    surface = EvenfallBlueSurfaceDark,
    onSurface = EvenfallBlueOnSurfaceDark,
    surfaceVariant = EvenfallBlueSurfaceVariantDark,
    onSurfaceVariant = EvenfallBlueOnSurfaceVariantDark,
    background = EvenfallBlueSurfaceDark,
    onBackground = EvenfallBlueOnSurfaceDark
)

@Composable
fun EvenfallTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    colorSchemeOption: ColorSchemeOption = ColorSchemeOption.EVENFALL_BLUE,
    customColorScheme: ColorScheme? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val baseColorScheme = customColorScheme ?: when {
        colorSchemeOption == ColorSchemeOption.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> when (colorSchemeOption) {
            ColorSchemeOption.TEAL -> darkColorScheme(primary = Color(0xFF4DB6AC), surface = EvenfallBlueSurfaceDark)
            ColorSchemeOption.GREEN -> darkColorScheme(primary = Color(0xFF81C784), surface = EvenfallBlueSurfaceDark)
            ColorSchemeOption.ORANGE -> darkColorScheme(primary = Color(0xFFFFB74D), surface = EvenfallBlueSurfaceDark)
            ColorSchemeOption.PINK -> darkColorScheme(primary = Color(0xFFF48FB1), surface = EvenfallBlueSurfaceDark)
            ColorSchemeOption.PURPLE -> darkColorScheme(primary = Color(0xFFCE93D8), surface = EvenfallBlueSurfaceDark)
            else -> DarkEvenfallColorScheme
        }
        else -> when (colorSchemeOption) {
            ColorSchemeOption.TEAL -> lightColorScheme(primary = Color(0xFF00796B), surface = EvenfallBlueSurfaceLight)
            ColorSchemeOption.GREEN -> lightColorScheme(primary = Color(0xFF388E3C), surface = EvenfallBlueSurfaceLight)
            ColorSchemeOption.ORANGE -> lightColorScheme(primary = Color(0xFFE65100), surface = EvenfallBlueSurfaceLight)
            ColorSchemeOption.PINK -> lightColorScheme(primary = Color(0xFFC2185B), surface = EvenfallBlueSurfaceLight)
            ColorSchemeOption.PURPLE -> lightColorScheme(primary = Color(0xFF7B1FA2), surface = EvenfallBlueSurfaceLight)
            else -> LightEvenfallColorScheme
        }
    }

    val finalColorScheme = if (darkTheme && pureBlack) {
        baseColorScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color(0xFF161616)
        )
    } else {
        baseColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = finalColorScheme,
        typography = EvenfallTypography,
        shapes = EvenfallShapes,
        content = content
    )
}
