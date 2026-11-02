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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
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
            // Render styled procedural cover art matching screenshot artwork
            ProceduralBookCover(title = title, author = author)
        }
    }
}

@Composable
private fun ProceduralBookCover(title: String, author: String) {
    val cleanTitle = title.lowercase()
    when {
        cleanTitle.contains("echoes") -> {
            // Echoes of Tomorrow: Deep space, glowing blue planet & spaceship
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF040B14),
                                Color(0xFF091E36),
                                Color(0xFF081422)
                            )
                        )
                    )
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    // Blue celestial planet halo
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color(0xFF38BDF8).copy(alpha = 0.7f), Color.Transparent),
                            center = Offset(w * 0.75f, h * 0.55f),
                            radius = w * 0.45f
                        ),
                        radius = w * 0.45f,
                        center = Offset(w * 0.75f, h * 0.55f)
                    )
                    // Planet sphere
                    drawCircle(
                        color = Color(0xFF0C2744),
                        radius = w * 0.35f,
                        center = Offset(w * 0.75f, h * 0.55f)
                    )
                    // Horizon glow ring
                    drawArc(
                        color = Color(0xFF7DD3FC),
                        startAngle = 120f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = Offset(w * 0.38f, h * 0.20f),
                        size = androidx.compose.ui.geometry.Size(w * 0.72f, h * 0.72f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.5f)
                    )
                    // Star dots
                    drawCircle(Color.White.copy(alpha = 0.8f), radius = 2f, center = Offset(w * 0.2f, h * 0.2f))
                    drawCircle(Color.White.copy(alpha = 0.6f), radius = 1.5f, center = Offset(w * 0.8f, h * 0.15f))
                    drawCircle(Color.White.copy(alpha = 0.9f), radius = 2.5f, center = Offset(w * 0.35f, h * 0.32f))
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "E C H O E S",
                        color = Color(0xFFF1F5F9),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = "— OF —",
                        color = Color(0xFF94A3B8),
                        fontSize = 8.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "TOMORROW",
                        color = Color(0xFFF1F5F9),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "DANIEL HARTWELL",
                        color = Color(0xFFCBD5E1),
                        fontSize = 8.sp,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        cleanTitle.contains("horizon") -> {
            // Beyond the Horizon: Warm mountain sunrise and golden compass
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF81A0BA),
                                Color(0xFFFBBF24),
                                Color(0xFFB45309),
                                Color(0xFF451A03)
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "BEYOND",
                        color = Color(0xFF1E293B),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        fontFamily = FontFamily.Serif
                    )
                    Text(
                        text = "— THE —",
                        color = Color(0xFF334155),
                        fontSize = 7.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "HORIZON",
                        color = Color(0xFF1E293B),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        fontFamily = FontFamily.Serif
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "ALEXANDER REEVES",
                        color = Color(0xFFFDE68A),
                        fontSize = 8.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        cleanTitle.contains("bookshop") -> {
            // The Bookshop on Maple Street: Warm amber bookstore glow
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF1C1917),
                                Color(0xFF451A03),
                                Color(0xFF78350F),
                                Color(0xFF1C1917)
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "BOOKSHOP",
                        color = Color(0xFFFEF3C7),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Serif
                    )
                    Text(
                        text = "MAPLE STREET",
                        color = Color(0xFFFDE68A),
                        fontSize = 8.sp,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "LENA MORGAN",
                        color = Color(0xFFD6D3D1),
                        fontSize = 8.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
        else -> {
            // Calming generic book cover with pastel gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF2C3E50),
                                Color(0xFF4CA1AF)
                            )
                        )
                    )
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = author,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
