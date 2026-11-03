package com.evenfall.app.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.evenfall.app.ui.theme.CookieShape

@Composable
fun MorphingPlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    iconSize: Dp = 38.dp,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    val morphProgress by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "morphProgress"
    )

    // Interpolate shape between CookieShape (paused) and RoundedCornerShape (playing)
    val shape = if (morphProgress > 0.5f) {
        RoundedCornerShape(24.dp)
    } else {
        CookieShape(lobes = 12, depth = 0.14f * (1f - morphProgress))
    }

    val haptics = com.evenfall.app.ui.util.rememberEvenfallHaptics()

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(containerColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                role = Role.Button,
                onClickLabel = if (isPlaying) "Pause" else "Play",
                onClick = {
                    haptics.playPause()
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(targetState = isPlaying, label = "iconCrossfade") { playing ->
            if (playing) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "Pause",
                    tint = contentColor,
                    modifier = Modifier.size(iconSize)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = contentColor,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}
