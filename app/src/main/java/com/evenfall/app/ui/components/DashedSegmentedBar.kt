package com.evenfall.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Segmented progress bar where each segment corresponds to a chapter.
 */
@Composable
fun DashedSegmentedBar(
    totalSegments: Int,
    currentSegment: Int, // 0-based
    progressWithinCurrent: Float = 0f, // 0f .. 1f
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
    barHeight: Dp = 6.dp,
    gapWidth: Dp = 4.dp,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
    ) {
        val total = totalSegments.coerceAtLeast(1)
        val gapPx = gapWidth.toPx()
        val totalGapsWidth = (total - 1) * gapPx
        val availableWidth = (size.width - totalGapsWidth).coerceAtLeast(0f)
        val segmentWidth = availableWidth / total
        val h = size.height
        val corner = CornerRadius(h / 2f, h / 2f)

        for (i in 0 until total) {
            val startX = i * (segmentWidth + gapPx)
            when {
                i < currentSegment -> {
                    // Fully completed chapter segment
                    drawRoundRect(
                        color = activeColor,
                        topLeft = Offset(startX, 0f),
                        size = Size(segmentWidth, h),
                        cornerRadius = corner
                    )
                }
                i == currentSegment -> {
                    // Current chapter segment: partially filled
                    // Draw inactive background first
                    drawRoundRect(
                        color = inactiveColor,
                        topLeft = Offset(startX, 0f),
                        size = Size(segmentWidth, h),
                        cornerRadius = corner
                    )
                    // Draw active fill
                    val activeW = segmentWidth * progressWithinCurrent.coerceIn(0f, 1f)
                    if (activeW > 0f) {
                        drawRoundRect(
                            color = activeColor,
                            topLeft = Offset(startX, 0f),
                            size = Size(activeW, h),
                            cornerRadius = corner
                        )
                    }
                }
                else -> {
                    // Future chapter segment
                    drawRoundRect(
                        color = inactiveColor,
                        topLeft = Offset(startX, 0f),
                        size = Size(segmentWidth, h),
                        cornerRadius = corner
                    )
                }
            }
        }
    }
}
