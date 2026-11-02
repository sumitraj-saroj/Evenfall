package com.evenfall.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.evenfall.app.ui.theme.SerifFont
import java.io.File

@Composable
fun BookCoverArt(
    title: String,
    author: String,
    coverPath: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    elevation: Dp = 0.dp
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color(0xFF1E293B)),
        contentAlignment = Alignment.Center
    ) {
        if (!coverPath.isNullOrEmpty() && File(coverPath).exists()) {
            Image(
                painter = rememberAsyncImagePainter(model = File(coverPath)),
                contentDescription = "$title cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            ProceduralBookCover(title = title, author = author)
        }
    }
}

@Composable
private fun ProceduralBookCover(title: String, author: String) {
    val cleanTitle = title.lowercase()
    when {
        cleanTitle.contains("echoes") -> {
            // Echoes of Tomorrow (Deep space, planet, spaceship, stars)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF030712),
                                Color(0xFF0B192C),
                                Color(0xFF071220)
                            )
                        )
                    )
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Glowing celestial planet
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color(0xFF38BDF8).copy(alpha = 0.8f), Color.Transparent),
                            center = Offset(w * 0.72f, h * 0.52f),
                            radius = w * 0.42f
                        ),
                        radius = w * 0.42f,
                        center = Offset(w * 0.72f, h * 0.52f)
                    )
                    drawCircle(
                        color = Color(0xFF0C2440),
                        radius = w * 0.32f,
                        center = Offset(w * 0.72f, h * 0.52f)
                    )
                    drawArc(
                        color = Color(0xFFBAE6FD),
                        startAngle = 120f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = Offset(w * 0.40f, h * 0.20f),
                        size = Size(w * 0.64f, h * 0.64f),
                        style = Stroke(width = w * 0.025f)
                    )

                    // Spaceship triangle silhouette
                    val shipPath = Path().apply {
                        moveTo(w * 0.28f, h * 0.50f)
                        lineTo(w * 0.42f, h * 0.46f)
                        lineTo(w * 0.38f, h * 0.52f)
                        lineTo(w * 0.44f, h * 0.54f)
                        lineTo(w * 0.28f, h * 0.50f)
                        close()
                    }
                    drawPath(shipPath, color = Color(0xFFE2E8F0))

                    // Stars
                    drawCircle(Color.White, radius = w * 0.012f, center = Offset(w * 0.18f, h * 0.22f))
                    drawCircle(Color.White.copy(alpha = 0.7f), radius = w * 0.008f, center = Offset(w * 0.78f, h * 0.14f))
                    drawCircle(Color.White.copy(alpha = 0.85f), radius = w * 0.01f, center = Offset(w * 0.35f, h * 0.32f))
                    drawCircle(Color.White.copy(alpha = 0.6f), radius = w * 0.009f, center = Offset(w * 0.20f, h * 0.75f))
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ECHOES",
                        color = Color(0xFFF1F5F9),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = "OF TOMORROW",
                        color = Color(0xFF93C5FD),
                        fontSize = 7.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "DANIEL HARTWELL",
                        color = Color(0xFFCBD5E1),
                        fontSize = 7.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        cleanTitle.contains("horizon") -> {
            // Beyond the Horizon (Warm mountain sunrise & compass)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF93B3CE),
                                Color(0xFFFBBF24),
                                Color(0xFFB45309),
                                Color(0xFF451A03)
                            )
                        )
                    )
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Mountain ridge silhouette
                    val mountain = Path().apply {
                        moveTo(0f, h * 0.58f)
                        lineTo(w * 0.30f, h * 0.42f)
                        lineTo(w * 0.55f, h * 0.49f)
                        lineTo(w * 0.78f, h * 0.38f)
                        lineTo(w, h * 0.50f)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(mountain, color = Color(0xFF3E1D06).copy(alpha = 0.85f))

                    // Compass dial circle
                    drawCircle(
                        color = Color(0xFFD97706),
                        radius = w * 0.25f,
                        center = Offset(w * 0.52f, h * 0.74f),
                        style = Stroke(width = w * 0.02f)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "BEYOND",
                        color = Color(0xFF1E293B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        fontFamily = SerifFont
                    )
                    Text(
                        text = "THE HORIZON",
                        color = Color(0xFF334155),
                        fontSize = 7.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "ALEXANDER REEVES",
                        color = Color(0xFFFEF3C7),
                        fontSize = 7.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        cleanTitle.contains("bookshop") -> {
            // The Bookshop on Maple Street (Cozy bookstore window at dusk)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF1C1917),
                                Color(0xFF291A10),
                                Color(0xFF451A03),
                                Color(0xFF1C1917)
                            )
                        )
                    )
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Glowing window
                    drawRoundRect(
                        color = Color(0xFFF59E0B),
                        topLeft = Offset(w * 0.24f, h * 0.40f),
                        size = Size(w * 0.52f, h * 0.38f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.06f, w * 0.06f)
                    )
                    // Window panes
                    drawLine(
                        color = Color(0xFF291A10),
                        start = Offset(w * 0.50f, h * 0.40f),
                        end = Offset(w * 0.50f, h * 0.78f),
                        strokeWidth = w * 0.02f
                    )
                    drawLine(
                        color = Color(0xFF291A10),
                        start = Offset(w * 0.24f, h * 0.59f),
                        end = Offset(w * 0.76f, h * 0.59f),
                        strokeWidth = w * 0.02f
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "THE BOOKSHOP",
                        color = Color(0xFFFEF3C7),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontFamily = SerifFont
                    )
                    Text(
                        text = "ON MAPLE STREET",
                        color = Color(0xFFFDE68A),
                        fontSize = 6.5.sp,
                        letterSpacing = 0.5.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "LENA MORGAN",
                        color = Color(0xFFE7E5E4),
                        fontSize = 7.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        else -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF1E3A5F),
                                Color(0xFF3B82F6)
                            )
                        )
                    )
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SerifFont,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = author,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 8.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
