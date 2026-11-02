package com.evenfall.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

@Composable
fun SparkleDecorations(
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFF9FA8DA).copy(alpha = 0.5f)
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Pre-positioned calm sparkles matching the screenshot
        val sparkles = listOf(
            Offset(w * 0.12f, h * 0.28f) to 12f,
            Offset(w * 0.88f, h * 0.25f) to 14f,
            Offset(w * 0.10f, h * 0.65f) to 16f,
            Offset(w * 0.90f, h * 0.58f) to 12f,
            Offset(w * 0.60f, h * 0.55f) to 8f,
            Offset(w * 0.28f, h * 0.55f) to 8f,
            Offset(w * 0.65f, h * 0.11f) to 8f,
            Offset(w * 0.17f, h * 0.94f) to 10f,
            Offset(w * 0.82f, h * 0.93f) to 10f
        )

        for ((center, radius) in sparkles) {
            val path = Path().apply {
                moveTo(center.x, center.y - radius)
                quadraticTo(center.x, center.y, center.x + radius, center.y)
                quadraticTo(center.x, center.y, center.x, center.y + radius)
                quadraticTo(center.x, center.y, center.x - radius, center.y)
                quadraticTo(center.x, center.y, center.x, center.y - radius)
                close()
            }
            drawPath(path, color = tint)
        }
    }
}
