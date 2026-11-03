package com.evenfall.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evenfall.app.R
import com.evenfall.app.data.datastore.LibrarySortBy
import com.evenfall.app.data.db.entity.CollectionEntity
import com.evenfall.app.ui.screens.home.FilterState
import com.evenfall.app.ui.theme.SerifFont

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SortFilterBottomSheet(
    currentSortBy: LibrarySortBy,
    currentSortAscending: Boolean,
    currentFilterState: FilterState,
    collections: List<CollectionEntity>,
    onApplySortAndFilter: (LibrarySortBy, Boolean, FilterState) -> Unit,
    onResetFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var tempSortBy by remember { mutableStateOf(currentSortBy) }
    var tempSortAscending by remember { mutableStateOf(currentSortAscending) }
    var tempFilterStatuses by remember { mutableStateOf(currentFilterState.statuses) }
    var tempCollectionId by remember { mutableStateOf(currentFilterState.collectionId) }
    var tempSeriesOnly by remember { mutableStateOf(currentFilterState.seriesOnly) }
    var tempUnavailableOnly by remember { mutableStateOf(currentFilterState.unavailableOnly) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.sort_and_filter),
                    fontFamily = SerifFont,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(
                    onClick = {
                        tempSortBy = LibrarySortBy.RECENTLY_PLAYED
                        tempSortAscending = false
                        tempFilterStatuses = emptySet()
                        tempCollectionId = null
                        tempSeriesOnly = false
                        tempUnavailableOnly = false
                        onResetFilters()
                    }
                ) {
                    Text(stringResource(R.string.reset_filters))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SORT BY SECTION
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.sort_by),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { tempSortAscending = !tempSortAscending }
                ) {
                    Icon(
                        imageVector = if (tempSortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = if (tempSortAscending) stringResource(R.string.sort_order_ascending) else stringResource(R.string.sort_order_descending),
                        modifier = Modifier.padding(end = 4.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (tempSortAscending) stringResource(R.string.sort_order_ascending) else stringResource(R.string.sort_order_descending),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val sortOptions = listOf(
                    LibrarySortBy.RECENTLY_PLAYED to stringResource(R.string.sort_recently_played),
                    LibrarySortBy.TITLE to stringResource(R.string.sort_title),
                    LibrarySortBy.AUTHOR to stringResource(R.string.sort_author),
                    LibrarySortBy.DURATION to stringResource(R.string.sort_duration),
                    LibrarySortBy.PROGRESS to stringResource(R.string.sort_progress),
                    LibrarySortBy.DATE_ADDED to stringResource(R.string.sort_date_added)
                )

                for ((sort, label) in sortOptions) {
                    FilterChip(
                        selected = tempSortBy == sort,
                        onClick = { tempSortBy = sort },
                        label = { Text(label, fontSize = 13.sp) },
                        leadingIcon = if (tempSortBy == sort) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // FILTER BY STATUS
            Text(
                text = stringResource(R.string.filter_status),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val statusOptions = listOf(
                    "IN_PROGRESS" to stringResource(R.string.status_in_progress),
                    "NOT_STARTED" to stringResource(R.string.status_not_started),
                    "FINISHED" to stringResource(R.string.status_finished)
                )

                for ((code, label) in statusOptions) {
                    val isSelected = tempFilterStatuses.contains(code)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            tempFilterStatuses = if (isSelected) {
                                tempFilterStatuses - code
                            } else {
                                tempFilterStatuses + code
                            }
                        },
                        label = { Text(label, fontSize = 13.sp) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null
                    )
                }
            }

            // FILTER BY COLLECTION
            if (collections.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.filter_collections),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (col in collections) {
                        val isSelected = tempCollectionId == col.id
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                tempCollectionId = if (isSelected) null else col.id
                            },
                            label = { Text(col.name, fontSize = 13.sp) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // OTHER FILTERS (Series, Unavailable)
            Text(
                text = "Attributes",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = tempSeriesOnly,
                    onClick = { tempSeriesOnly = !tempSeriesOnly },
                    label = { Text(stringResource(R.string.filter_series_only), fontSize = 13.sp) },
                    leadingIcon = if (tempSeriesOnly) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null
                )

                FilterChip(
                    selected = tempUnavailableOnly,
                    onClick = { tempUnavailableOnly = !tempUnavailableOnly },
                    label = { Text(stringResource(R.string.filter_unavailable), fontSize = 13.sp) },
                    leadingIcon = if (tempUnavailableOnly) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Bottom Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
                Button(
                    onClick = {
                        val newFilterState = FilterState(
                            statuses = tempFilterStatuses,
                            collectionId = tempCollectionId,
                            seriesOnly = tempSeriesOnly,
                            unavailableOnly = tempUnavailableOnly
                        )
                        onApplySortAndFilter(tempSortBy, tempSortAscending, newFilterState)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.apply_filters))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
