package com.evenfall.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import com.evenfall.app.R
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evenfall.app.ui.theme.SerifFont
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Skip rewind/forward button with curved circular arrow and seconds label.
 * Supports click and long-press for rapid adjustment.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CircularSkipButton(
    seconds: Int,
    isForward: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    size: Dp = 58.dp,
    containerColor: Color = Color(0xFFF7D171),
    contentColor: Color = Color(0xFF5A350D)
) {
    val haptics = com.evenfall.app.ui.util.rememberEvenfallHaptics()

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                role = Role.Button,
                onClickLabel = if (isForward) "Forward $seconds seconds" else "Rewind $seconds seconds",
                onLongClickLabel = if (onLongClick != null) "Adjust skip duration" else null,
                onClick = onClick,
                onLongClick = if (onLongClick != null) {
                    {
                        haptics.longPressPopup()
                        onLongClick()
                    }
                } else null
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.65f)) {
            val w = this.size.width
            val h = this.size.height
            val center = Offset(w / 2f, h / 2f)
            val radius = w * 0.42f
            val strokeW = 2.4.dp.toPx()

            // Draw curved 270-degree arrow arc
            val startAngle = if (isForward) 210f else -30f
            val sweepAngle = if (isForward) 280f else -280f

            drawArc(
                color = contentColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )

            // Arrow head at the end
            val endAngleRad = (startAngle + sweepAngle) * (PI / 180.0)
            val tipX = (center.x + radius * cos(endAngleRad)).toFloat()
            val tipY = (center.y + radius * sin(endAngleRad)).toFloat()

            val arrowPath = Path()
            val arrowSize = 6.dp.toPx()
            if (isForward) {
                arrowPath.moveTo(tipX - arrowSize * 0.5f, tipY - arrowSize)
                arrowPath.lineTo(tipX + arrowSize * 0.5f, tipY)
                arrowPath.lineTo(tipX - arrowSize * 0.8f, tipY + arrowSize * 0.5f)
            } else {
                arrowPath.moveTo(tipX + arrowSize * 0.5f, tipY - arrowSize)
                arrowPath.lineTo(tipX - arrowSize * 0.5f, tipY)
                arrowPath.lineTo(tipX + arrowSize * 0.8f, tipY + arrowSize * 0.5f)
            }
            drawPath(path = arrowPath, color = contentColor)

            // Center number (e.g. "20")
            val paint = android.graphics.Paint().apply {
                color = contentColor.hashCode()
                textSize = (w * 0.32f)
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
            val textY = center.y - ((paint.descent() + paint.ascent()) / 2f)
            drawContext.canvas.nativeCanvas.drawText(
                seconds.toString(),
                center.x,
                textY,
                paint
            )
        }
    }
}

@Composable
fun QuickSkipAdjustDialog(
    isForward: Boolean,
    currentSeconds: Int,
    onSecondsChange: (Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.done))
            }
        },
        title = {
            Text(
                text = if (isForward) stringResource(R.string.book_skip_forward) else stringResource(R.string.book_skip_backward),
                fontFamily = SerifFont,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$currentSeconds seconds",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Slider(
                    value = currentSeconds.toFloat(),
                    onValueChange = { onSecondsChange(it.toInt()) },
                    valueRange = 5f..120f,
                    steps = 22
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Presets row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(10, 15, 20, 30, 45, 60).forEach { sec ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (currentSeconds == sec) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { onSecondsChange(sec) }
                        ) {
                            Text(
                                text = "${sec}s",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                fontSize = 12.sp,
                                fontWeight = if (currentSeconds == sec) FontWeight.Bold else FontWeight.Normal,
                                color = if (currentSeconds == sec) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    )
}
