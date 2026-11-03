package com.evenfall.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Material 3 Expressive wavy progress line.
 * When isPlaying is true, the played portion is rendered as an organic, gently oscillating wave.
 * When paused, it renders as a calm flat line.
 */
@Composable
fun WavyProgressLine(
    progress: Float, // 0f .. 1f
    onSeek: ((Float) -> Unit)? = null,
    isPlaying: Boolean = false,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
    thumbColor: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 4.dp,
    waveAmplitude: Dp = 5.dp,
    waveLength: Dp = 24.dp,
    modifier: Modifier = Modifier,
    accessibilityDescription: String? = null,
    customAccessibilityActions: List<CustomAccessibilityAction> = emptyList()
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val reducedMotion = com.evenfall.app.ui.util.rememberReducedMotion()
    val infiniteTransition = rememberInfiniteTransition(label = "waveAnimation")
    val animatedPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    val phase = if (reducedMotion) 0f else animatedPhase

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .semantics(mergeDescendants = true) {
                if (accessibilityDescription != null) {
                    contentDescription = accessibilityDescription
                    stateDescription = accessibilityDescription
                }
                progressBarRangeInfo = ProgressBarRangeInfo(current = progress.coerceIn(0f, 1f), range = 0f..1f)
                if (onSeek != null) {
                    setProgress { targetValue ->
                        onSeek(targetValue.coerceIn(0f, 1f))
                        true
                    }
                }
                if (customAccessibilityActions.isNotEmpty()) {
                    customActions = customAccessibilityActions
                }
            }
            .then(
                if (onSeek != null) {
                    Modifier.pointerInput(isRtl) {
                        detectTapGestures { offset ->
                            val rawProgress = (offset.x / size.width).coerceIn(0f, 1f)
                            val newProgress = if (isRtl) (1f - rawProgress) else rawProgress
                            onSeek(newProgress)
                        }
                    }.pointerInput(isRtl) {
                        detectHorizontalDragGestures { change, _ ->
                            change.consume()
                            val rawProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                            val newProgress = if (isRtl) (1f - rawProgress) else rawProgress
                            onSeek(newProgress)
                        }
                    }
                } else Modifier
            )
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val currentProgress = progress.coerceIn(0f, 1f)
        val splitX = if (isRtl) width * (1f - currentProgress) else width * currentProgress

        val strokePx = strokeWidth.toPx()
        val ampPx = waveAmplitude.toPx()
        val lengthPx = waveLength.toPx()

        if (!isRtl) {
            // LTR
            // 1. Draw Inactive / Remaining Line (flat line)
            if (splitX < width) {
                drawLine(
                    color = inactiveColor,
                    start = Offset(splitX, centerY),
                    end = Offset(width, centerY),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round
                )
            }

            // 2. Draw Active / Played Line (wavy if playing and motion allowed, flat if paused/reduced motion)
            if (splitX > 0f) {
                if (isPlaying && !reducedMotion && ampPx > 0f) {
                    val wavePath = Path()
                    wavePath.moveTo(0f, centerY)

                    val stepPx = 3f
                    var x = 0f
                    while (x <= splitX) {
                        val relativeX = x / lengthPx * (2 * PI).toFloat()
                        val y = centerY + ampPx * sin(relativeX - phase)
                        wavePath.lineTo(x, y)
                        x += stepPx
                    }
                    wavePath.lineTo(splitX, centerY)

                    drawPath(
                        path = wavePath,
                        color = activeColor,
                        style = Stroke(
                            width = strokePx,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                } else {
                    drawLine(
                        color = activeColor,
                        start = Offset(0f, centerY),
                        end = Offset(splitX, centerY),
                        strokeWidth = strokePx,
                        cap = StrokeCap.Round
                    )
                }

                // Draw thumb indicator at splitX
                val thumbRadius = strokePx * 1.5f
                drawCircle(
                    color = thumbColor,
                    radius = thumbRadius,
                    center = Offset(splitX, centerY)
                )
            }
        } else {
            // RTL: Played moves from right (width) towards left (0)
            // 1. Draw Inactive / Remaining Line (from 0 to splitX)
            if (splitX > 0f) {
                drawLine(
                    color = inactiveColor,
                    start = Offset(0f, centerY),
                    end = Offset(splitX, centerY),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round
                )
            }

            // 2. Draw Active / Played Line (from splitX to width)
            if (splitX < width) {
                if (isPlaying && ampPx > 0f) {
                    val wavePath = Path()
                    wavePath.moveTo(splitX, centerY)

                    val stepPx = 3f
                    var x = splitX
                    while (x <= width) {
                        val relativeX = x / lengthPx * (2 * PI).toFloat()
                        val y = centerY + ampPx * sin(relativeX - phase)
                        wavePath.lineTo(x, y)
                        x += stepPx
                    }
                    wavePath.lineTo(width, centerY)

                    drawPath(
                        path = wavePath,
                        color = activeColor,
                        style = Stroke(
                            width = strokePx,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                } else {
                    drawLine(
                        color = activeColor,
                        start = Offset(splitX, centerY),
                        end = Offset(width, centerY),
                        strokeWidth = strokePx,
                        cap = StrokeCap.Round
                    )
                }

                // Draw thumb indicator at splitX
                val thumbRadius = strokePx * 1.5f
                drawCircle(
                    color = thumbColor,
                    radius = thumbRadius,
                    center = Offset(splitX, centerY)
                )
            }
        }
    }
}
