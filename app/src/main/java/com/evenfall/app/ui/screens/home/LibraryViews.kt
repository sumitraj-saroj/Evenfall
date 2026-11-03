package com.evenfall.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evenfall.app.R
import com.evenfall.app.data.db.entity.BookEntity
import com.evenfall.app.ui.components.BookCoverArt
import com.evenfall.app.ui.theme.CookieShape
import com.evenfall.app.ui.theme.SerifFont
import com.evenfall.app.util.TimeUtils

@Composable
fun BookCoverGridContent(
    books: List<BookEntity>,
    onOpenBook: (Long) -> Unit,
    onEditDetails: (BookEntity) -> Unit,
    onAddToCollection: (BookEntity) -> Unit,
    onMarkFinished: (BookEntity) -> Unit,
    onMarkUnfinished: (BookEntity) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    if (books.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.no_matching_books),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = contentPadding,
        modifier = modifier.fillMaxSize()
    ) {
        items(books, key = { it.id }) { book ->
            GridBookCard(
                book = book,
                onClick = { onOpenBook(book.id) },
                onEditDetails = { onEditDetails(book) },
                onAddToCollection = { onAddToCollection(book) },
                onMarkFinished = { onMarkFinished(book) },
                onMarkUnfinished = { onMarkUnfinished(book) }
            )
        }
    }
}

@Composable
fun GridBookCard(
    book: BookEntity,
    onClick: () -> Unit,
    onEditDetails: () -> Unit,
    onAddToCollection: () -> Unit,
    onMarkFinished: () -> Unit,
    onMarkUnfinished: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Cover Art Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                BookCoverArt(
                    title = book.displayTitle,
                    author = book.displayAuthor,
                    coverPath = book.displayCoverUri,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxSize()
                )

                // Bottom progress line or finished badge
                if (book.isFinished) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Finished",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else if (book.currentPositionMs > 0 && book.totalDurationMs > 0) {
                    val progress = (book.currentPositionMs.toFloat() / book.totalDurationMs).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Black.copy(alpha = 0.35f),
                        strokeCap = StrokeCap.Round
                    )
                }

                // Unavailable overlay
                if (book.isUnavailable) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = "Unavailable",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title & Overflow
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = book.displayTitle,
                        fontFamily = SerifFont,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = book.displayAuthor,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    book.displaySeriesName?.let { series ->
                        val orderText = book.displaySeriesOrder?.let { " #$it" } ?: ""
                        Text(
                            text = "$series$orderText",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Book menu",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit details") },
                            onClick = {
                                showMenu = false
                                onEditDetails()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.add_to_collection)) },
                            onClick = {
                                showMenu = false
                                onAddToCollection()
                            }
                        )
                        if (book.isFinished) {
                            DropdownMenuItem(
                                text = { Text("Mark as not started") },
                                onClick = {
                                    showMenu = false
                                    onMarkUnfinished()
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Mark as finished") },
                                onClick = {
                                    showMenu = false
                                    onMarkFinished()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SeriesViewContent(
    seriesGroups: List<SeriesGroup>,
    onOpenBook: (Long) -> Unit,
    onEditDetails: (BookEntity) -> Unit,
    onAddToCollection: (BookEntity) -> Unit,
    onMarkFinished: (BookEntity) -> Unit,
    onMarkUnfinished: (BookEntity) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    if (seriesGroups.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No series found. Audiobooks with series tags or manual series names will appear here.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
        return
    }

    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(seriesGroups, key = { it.seriesName }) { series ->
            SeriesGroupCard(
                series = series,
                onOpenBook = onOpenBook,
                onEditDetails = onEditDetails,
                onAddToCollection = onAddToCollection,
                onMarkFinished = onMarkFinished,
                onMarkUnfinished = onMarkUnfinished
            )
        }
    }
}

@Composable
fun SeriesGroupCard(
    series: SeriesGroup,
    onOpenBook: (Long) -> Unit,
    onEditDetails: (BookEntity) -> Unit,
    onAddToCollection: (BookEntity) -> Unit,
    onMarkFinished: (BookEntity) -> Unit,
    onMarkUnfinished: (BookEntity) -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Series Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = series.seriesName,
                        fontFamily = SerifFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "by ${series.author}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                ) {
                    Text(
                        text = "${series.finishedBooks}/${series.totalBooks} read",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Aggregate Progress Bar
            LinearProgressIndicator(
                progress = { series.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${(series.progressFraction * 100).toInt()}% complete",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = TimeUtils.formatDuration(series.totalDurationMs) + " total",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Ordered Books List in Series
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                series.books.forEachIndexed { index, book ->
                    val orderLabel = book.displaySeriesOrder?.let {
                        val formatted = if (it % 1.0f == 0f) it.toInt().toString() else it.toString()
                        "Book $formatted"
                    } ?: "Book ${index + 1} of ${series.totalBooks}"

                    SeriesBookRow(
                        book = book,
                        orderLabel = orderLabel,
                        onClick = { onOpenBook(book.id) },
                        onEditDetails = { onEditDetails(book) },
                        onAddToCollection = { onAddToCollection(book) },
                        onMarkFinished = { onMarkFinished(book) },
                        onMarkUnfinished = { onMarkUnfinished(book) }
                    )
                }
            }
        }
    }
}

@Composable
fun SeriesBookRow(
    book: BookEntity,
    orderLabel: String,
    onClick: () -> Unit,
    onEditDetails: () -> Unit,
    onAddToCollection: () -> Unit,
    onMarkFinished: () -> Unit,
    onMarkUnfinished: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BookCoverArt(
                title = book.displayTitle,
                author = book.displayAuthor,
                coverPath = book.displayCoverUri,
                shape = CookieShape(lobes = 10, depth = 0.16f),
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = orderLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = book.displayTitle,
                    fontFamily = SerifFont,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (book.isFinished) "Finished" else "${TimeUtils.formatDuration(book.totalDurationMs - book.currentPositionMs)} left",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (book.isFinished) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                Surface(
                    shape = CookieShape(8),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable(onClick = onClick)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Book menu",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit details") },
                        onClick = {
                            showMenu = false
                            onEditDetails()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.add_to_collection)) },
                        onClick = {
                            showMenu = false
                            onAddToCollection()
                        }
                    )
                    if (book.isFinished) {
                        DropdownMenuItem(
                            text = { Text("Mark as not started") },
                            onClick = {
                                showMenu = false
                                onMarkUnfinished()
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Mark as finished") },
                            onClick = {
                                showMenu = false
                                onMarkFinished()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AuthorViewContent(
    authorGroups: List<AuthorGroup>,
    onOpenBook: (Long) -> Unit,
    onEditDetails: (BookEntity) -> Unit,
    onAddToCollection: (BookEntity) -> Unit,
    onMarkFinished: (BookEntity) -> Unit,
    onMarkUnfinished: (BookEntity) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    if (authorGroups.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No authors found.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
        return
    }

    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(authorGroups, key = { it.author }) { author ->
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = author.author,
                            fontFamily = SerifFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${author.totalBooks} books",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        author.books.forEach { book ->
                            SeriesBookRow(
                                book = book,
                                orderLabel = book.displaySeriesName?.let { "$it ${book.displaySeriesOrder ?: ""}" } ?: "Standalone",
                                onClick = { onOpenBook(book.id) },
                                onEditDetails = { onEditDetails(book) },
                                onAddToCollection = { onAddToCollection(book) },
                                onMarkFinished = { onMarkFinished(book) },
                                onMarkUnfinished = { onMarkUnfinished(book) }
                            )
                        }
                    }
                }
            }
        }
    }
}
