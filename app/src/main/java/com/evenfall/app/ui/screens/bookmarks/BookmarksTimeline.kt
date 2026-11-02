package com.evenfall.app.ui.screens.bookmarks

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Note
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evenfall.app.data.db.entity.BookmarkEntity
import com.evenfall.app.data.db.entity.BookmarkType
import com.evenfall.app.util.TimeUtils

@Composable
fun BookmarksTimeline(
    totalSegments: Int,
    currentSegment: Int,
    progressWithinCurrent: Float,
    bookmarks: List<BookmarkEntity>,
    totalDurationMs: Long,
    currentPositionMs: Long,
    modifier: Modifier = Modifier
) {
    val progressFraction = if (totalDurationMs > 0) {
        (currentPositionMs.toFloat() / totalDurationMs).coerceIn(0f, 1f)
    } else 0f
    val progressPercent = (progressFraction * 100).toInt()
    val timeLeftMs = (totalDurationMs - currentPositionMs).coerceAtLeast(0L)

    Column(modifier = modifier.fillMaxWidth()) {
        // Marker indicators Canvas + Segmented Bar
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
        ) {
            val total = totalSegments.coerceAtLeast(1)
            val gapPx = 3.dp.toPx()
            val totalGapsWidth = (total - 1) * gapPx
            val availableWidth = (size.width - totalGapsWidth).coerceAtLeast(0f)
            val segmentWidth = availableWidth / total
            val barY = size.height - 6.dp.toPx()
            val barH = 5.dp.toPx()
            val corner = CornerRadius(barH / 2f, barH / 2f)

            // 1. Draw Chapter Segments
            for (i in 0 until total) {
                val startX = i * (segmentWidth + gapPx)
                when {
                    i < currentSegment -> {
                        drawRoundRect(
                            color = Color(0xFF1E4D7E),
                            topLeft = Offset(startX, barY),
                            size = Size(segmentWidth, barH),
                            cornerRadius = corner
                        )
                    }
                    i == currentSegment -> {
                        drawRoundRect(
                            color = Color(0xFF1E4D7E).copy(alpha = 0.25f),
                            topLeft = Offset(startX, barY),
                            size = Size(segmentWidth, barH),
                            cornerRadius = corner
                        )
                        val activeW = segmentWidth * progressWithinCurrent.coerceIn(0f, 1f)
                        if (activeW > 0f) {
                            drawRoundRect(
                                color = Color(0xFF1E4D7E),
                                topLeft = Offset(startX, barY),
                                size = Size(activeW, barH),
                                cornerRadius = corner
                            )
                        }
                    }
                    else -> {
                        drawRoundRect(
                            color = Color(0xFF1E4D7E).copy(alpha = 0.25f),
                            topLeft = Offset(startX, barY),
                            size = Size(segmentWidth, barH),
                            cornerRadius = corner
                        )
                    }
                }
            }

            // 2. Draw Bookmark Markers floating above the segments
            for (b in bookmarks) {
                val seg = b.chapterIndex.coerceIn(0, total - 1)
                val segX = seg * (segmentWidth + gapPx) + (segmentWidth / 2f)
                val markerY = 10.dp.toPx()

                when (b.type) {
                    BookmarkType.FAVORITE -> {
                        // Purple/lavender heart marker
                        drawCircle(
                            color = Color(0xFFBA68C8),
                            radius = 6.dp.toPx(),
                            center = Offset(segX, markerY)
                        )
                    }
                    BookmarkType.QUOTE -> {
                        // Light blue quote marker
                        drawRoundRect(
                            color = Color(0xFF81D4FA),
                            topLeft = Offset(segX - 5.dp.toPx(), markerY - 5.dp.toPx()),
                            size = Size(10.dp.toPx(), 10.dp.toPx()),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                    }
                    BookmarkType.NOTE -> {
                        // Teal scalloped marker
                        drawCircle(
                            color = Color(0xFF26A69A),
                            radius = 6.dp.toPx(),
                            center = Offset(segX, markerY)
                        )
                    }
                    else -> {
                        // Blue bookmark marker
                        drawRoundRect(
                            color = Color(0xFF42A5F5),
                            topLeft = Offset(segX - 5.dp.toPx(), markerY - 5.dp.toPx()),
                            size = Size(10.dp.toPx(), 10.dp.toPx()),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                    }
                }
            }

            // 3. Draw "You are here" ring at current position
            val curSegX = currentSegment.coerceIn(0, total - 1) * (segmentWidth + gapPx) + (segmentWidth * progressWithinCurrent.coerceIn(0f, 1f))
            drawCircle(
                color = Color(0xFF0284C7),
                radius = 7.dp.toPx(),
                center = Offset(curSegX, barY + (barH / 2f)),
                style = Stroke(width = 3.dp.toPx())
            )
            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = Offset(curSegX, barY + (barH / 2f))
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Time and Progress text
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$progressPercent% of book",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = TimeUtils.formatTimeLeft(timeLeftMs),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
