package com.evenfall.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val EvenfallShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

/**
 * A scalloped/cookie shape used across Evenfall (play button, letter avatars, widgets, and dynamic badges).
 * @param lobes Number of lobes/scallops around the perimeter (defaults to 12).
 * @param depth Indentation depth of scallops (0.0f = circle, 0.12f = gentle cookie, 0.2f = deep scallops).
 */
class CookieShape(
    val lobes: Int = 12,
    val depth: Float = 0.12f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val maxRadius = minOf(centerX, centerY)
        val minRadius = maxRadius * (1f - depth)
        val totalPoints = lobes * 2
        val angleStep = (2.0 * PI / totalPoints).toFloat()

        for (i in 0 until totalPoints) {
            val isPeak = i % 2 == 0
            val radius = if (isPeak) maxRadius else minRadius
            val angle = i * angleStep - (PI / 2).toFloat()
            val x = centerX + radius * cos(angle)
            val y = centerY + radius * sin(angle)

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                // Smooth transition through the points using quadratic bezier
                val prevAngle = (i - 1) * angleStep - (PI / 2).toFloat()
                val prevRadius = if (!isPeak) maxRadius else minRadius
                val prevX = centerX + prevRadius * cos(prevAngle)
                val prevY = centerY + prevRadius * sin(prevAngle)

                val midAngle = angle - (angleStep / 2f)
                val midRadius = (radius + prevRadius) / 2f
                val ctrlX = centerX + midRadius * cos(midAngle)
                val ctrlY = centerY + midRadius * sin(midAngle)

                path.quadraticTo(ctrlX, ctrlY, x, y)
            }
        }
        path.close()
        return Outline.Generic(path)
    }
}
