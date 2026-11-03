package com.evenfall.app.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evenfall.app.R
import com.evenfall.app.data.db.entity.BookEntity
import com.evenfall.app.ui.theme.CookieShape
import com.evenfall.app.ui.theme.SerifFont
import com.evenfall.app.util.CoverStorageHelper

@Composable
fun EditBookDetailsDialog(
    book: BookEntity,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        author: String,
        narrator: String?,
        seriesName: String?,
        seriesOrder: Float?,
        coverUri: String?
    ) -> Unit,
    onResetToOriginal: () -> Unit
) {
    val context = LocalContext.current

    var title by remember(book.id) { mutableStateOf(book.displayTitle) }
    var author by remember(book.id) { mutableStateOf(book.displayAuthor) }
    var narrator by remember(book.id) { mutableStateOf(book.displayNarrator ?: "") }
    var seriesName by remember(book.id) { mutableStateOf(book.seriesName ?: "") }
    var seriesOrderStr by remember(book.id) { mutableStateOf(book.seriesOrder?.toString() ?: "") }
    var coverUri by remember(book.id) { mutableStateOf(book.displayCoverUri) }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { pickedUri ->
        if (pickedUri != null) {
            val localPath = CoverStorageHelper.saveAndDownsampleCover(context, pickedUri, book.id)
            if (localPath != null) {
                coverUri = localPath
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.edit_details_title),
                fontFamily = SerifFont,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cover preview & picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BookCoverArt(
                        title = title,
                        author = author,
                        coverPath = coverUri,
                        shape = CookieShape(lobes = 12, depth = 0.16f),
                        modifier = Modifier.size(64.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        OutlinedButton(
                            onClick = {
                                pickMediaLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("btn_change_cover")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.change_cover), fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        TextButton(
                            onClick = {
                                title = book.title
                                author = book.author
                                narrator = ""
                                seriesName = ""
                                seriesOrderStr = ""
                                coverUri = book.coverUri
                                onResetToOriginal()
                                onDismiss()
                            },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("btn_reset_to_original")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                stringResource(R.string.reset_to_original),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.title_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_edit_title")
                )

                // Author
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text(stringResource(R.string.author_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_edit_author")
                )

                // Narrator
                OutlinedTextField(
                    value = narrator,
                    onValueChange = { narrator = it },
                    label = { Text(stringResource(R.string.narrator_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_edit_narrator")
                )

                // Series Name
                OutlinedTextField(
                    value = seriesName,
                    onValueChange = { seriesName = it },
                    label = { Text(stringResource(R.string.series_name_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_edit_series_name")
                )

                // Series Number
                OutlinedTextField(
                    value = seriesOrderStr,
                    onValueChange = { seriesOrderStr = it },
                    label = { Text(stringResource(R.string.series_number_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_edit_series_number")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val order = seriesOrderStr.toFloatOrNull()
                    onSave(
                        title.trim(),
                        author.trim(),
                        narrator.trim().ifEmpty { null },
                        seriesName.trim().ifEmpty { null },
                        order,
                        coverUri
                    )
                    onDismiss()
                },
                enabled = title.isNotBlank() && author.isNotBlank(),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("btn_save_edit_details")
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(48.dp)
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun EditChapterTitleDialog(
    currentTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newTitle by remember(currentTitle) { mutableStateOf(currentTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.edit_chapter_title),
                fontFamily = SerifFont,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            OutlinedTextField(
                value = newTitle,
                onValueChange = { newTitle = it },
                label = { Text(stringResource(R.string.chapter_title_label)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_chapter_title")
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(newTitle.trim())
                    onDismiss()
                },
                enabled = newTitle.isNotBlank(),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("btn_save_chapter_title")
            ) {
                Text(stringResource(R.string.rename))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(48.dp)
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
