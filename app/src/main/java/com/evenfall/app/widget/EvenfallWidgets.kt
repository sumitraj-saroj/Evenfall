package com.evenfall.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.evenfall.app.MainActivity
import com.evenfall.app.R

class NowPlayingWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            NowPlayingWidgetContent()
        }
    }
}

class ContinueListeningWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            ContinueListeningWidgetContent()
        }
    }
}

@androidx.compose.runtime.Composable
private fun NowPlayingWidgetContent() {
    val darkBlue = Color(0xFF0F3254)
    val lightCyan = Color(0xFF7DD3FC)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(ColorProvider(darkBlue))
            .clickable(actionStartActivity<MainActivity>())
            .padding(16.dp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cookie-cropped cover thumbnail on left
            Box(
                modifier = GlanceModifier
                    .size(80.dp)
                    .cornerRadius(20.dp)
                    .background(ColorProvider(Color(0xFF0C1D33))),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_launcher_foreground),
                    contentDescription = "Cover",
                    modifier = GlanceModifier.size(60.dp)
                )
            }

            Spacer(modifier = GlanceModifier.width(14.dp))

            Column(modifier = GlanceModifier.defaultWeight()) {
                // Book Title
                Text(
                    text = "Echoes of Tomorrow",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )

                Spacer(modifier = GlanceModifier.height(2.dp))

                // Sleep timer status
                Text(
                    text = "🌙 Stops at 11:11 PM",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF93C5FD)),
                        fontSize = 11.sp
                    ),
                    maxLines = 1
                )

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Progress line
                Row(modifier = GlanceModifier.fillMaxWidth().height(4.dp)) {
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight()
                            .cornerRadius(2.dp)
                            .background(ColorProvider(lightCyan))
                    ) {}
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight()
                            .cornerRadius(2.dp)
                            .background(ColorProvider(Color(0xFF1E4068)))
                    ) {}
                }

                Spacer(modifier = GlanceModifier.height(4.dp))

                // Chapter info & time left
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    Text(
                        text = "Ch. 12 of 24",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF94A3B8)),
                            fontSize = 10.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = "7 hr, 33 min left",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF94A3B8)),
                            fontSize = 10.sp
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(10.dp))

                // Media controls row (Skip back, Play/Pause squircle, Skip forward)
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rewind
                    Box(
                        modifier = GlanceModifier
                            .size(32.dp)
                            .cornerRadius(16.dp)
                            .background(ColorProvider(Color(0xFF1E4976))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "↺ 20",
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(16.dp))

                    // Play/Pause squircle
                    Box(
                        modifier = GlanceModifier
                            .size(38.dp)
                            .cornerRadius(12.dp)
                            .background(ColorProvider(lightCyan)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "❚❚",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF0F172A)),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(16.dp))

                    // Forward
                    Box(
                        modifier = GlanceModifier
                            .size(32.dp)
                            .cornerRadius(16.dp)
                            .background(ColorProvider(Color(0xFF1E4976))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "↻ 20",
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun ContinueListeningWidgetContent() {
    val lightSurface = Color(0xFFD6E4F8)
    val darkBlue = Color(0xFF0F1E36)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(ColorProvider(lightSurface))
            .clickable(actionStartActivity<MainActivity>())
            .padding(14.dp)
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            // Header
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Continue listening",
                    style = TextStyle(
                        color = ColorProvider(darkBlue),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = "🎧",
                    style = TextStyle(fontSize = 12.sp)
                )
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            // Horizontal row of book covers
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Book 1: Echoes (active, scalloped)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = GlanceModifier.defaultWeight()
                ) {
                    Box(
                        modifier = GlanceModifier
                            .size(48.dp)
                            .cornerRadius(16.dp)
                            .background(ColorProvider(Color(0xFF0C1F38))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🪐", style = TextStyle(fontSize = 18.sp))
                    }
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = "Echoes of T...",
                        style = TextStyle(color = ColorProvider(darkBlue), fontSize = 10.sp),
                        maxLines = 1
                    )
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Box(
                        modifier = GlanceModifier
                            .width(36.dp)
                            .height(3.dp)
                            .cornerRadius(2.dp)
                            .background(ColorProvider(Color(0xFF1E40AF)))
                    ) {}
                }

                Spacer(modifier = GlanceModifier.width(8.dp))

                // Book 2: Horizon
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = GlanceModifier.defaultWeight()
                ) {
                    Box(
                        modifier = GlanceModifier
                            .size(48.dp)
                            .cornerRadius(12.dp)
                            .background(ColorProvider(Color(0xFFD97706))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🧭", style = TextStyle(fontSize = 18.sp))
                    }
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = "Beyond the...",
                        style = TextStyle(color = ColorProvider(darkBlue), fontSize = 10.sp),
                        maxLines = 1
                    )
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Box(
                        modifier = GlanceModifier
                            .width(36.dp)
                            .height(3.dp)
                            .cornerRadius(2.dp)
                            .background(ColorProvider(Color(0xFFB45309)))
                    ) {}
                }

                Spacer(modifier = GlanceModifier.width(8.dp))

                // Book 3: Bookshop
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = GlanceModifier.defaultWeight()
                ) {
                    Box(
                        modifier = GlanceModifier
                            .size(48.dp)
                            .cornerRadius(12.dp)
                            .background(ColorProvider(Color(0xFF78350F))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📚", style = TextStyle(fontSize = 18.sp))
                    }
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = "The Booksh...",
                        style = TextStyle(color = ColorProvider(darkBlue), fontSize = 10.sp),
                        maxLines = 1
                    )
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Box(
                        modifier = GlanceModifier
                            .width(36.dp)
                            .height(3.dp)
                            .cornerRadius(2.dp)
                            .background(ColorProvider(Color(0xFF92400E)))
                    ) {}
                }

                Spacer(modifier = GlanceModifier.width(8.dp))

                // Book 4: Empty placeholder slot
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = GlanceModifier.defaultWeight()
                ) {
                    Box(
                        modifier = GlanceModifier
                            .size(48.dp)
                            .cornerRadius(16.dp)
                            .background(ColorProvider(Color(0xFFBFDBFE).copy(alpha = 0.5f)))
                    ) {}
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(text = " ", style = TextStyle(fontSize = 10.sp))
                }
            }
        }
    }
}
