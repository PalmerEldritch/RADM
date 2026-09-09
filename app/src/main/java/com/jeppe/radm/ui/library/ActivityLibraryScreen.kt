package com.jeppe.radm.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.jeppe.radm.domain.model.ActivityLibraryItem
import com.jeppe.radm.domain.model.ActivityType
import com.jeppe.radm.ui.analysis.ActivityAnalysisScreen
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object ActivityLibraryTestTags {
    const val LIST = "activity_library_list"
    const val EMPTY = "activity_library_empty"
    const val START = "activity_library_start"
    const val DETAIL = "activity_detail"
    const val EDIT = "activity_edit"
    const val DELETE = "activity_delete"
    const val DELETE_CONFIRM = "activity_delete_confirm"
    const val EDIT_TITLE = "activity_edit_title"
    const val EDIT_NOTES = "activity_edit_notes"
    const val EDIT_SAVE = "activity_edit_save"
}

@Composable
fun ActivityLibraryScreen(
    viewModel: ActivityLibraryViewModel,
    onStartActivity: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val selected = state.selected
    val analysisInteraction = state.analysisInteraction
    if (selected == null || analysisInteraction == null) {
        LibraryListContent(state, viewModel::open, onStartActivity)
    } else {
        ActivityAnalysisScreen(
            data = selected,
            interaction = analysisInteraction,
            saving = state.saving,
            error = state.error,
            onBack = viewModel::closeActivity,
            onSelectFraction = viewModel::selectAnalysisFraction,
            onCoordinateMode = viewModel::setAnalysisCoordinateMode,
            onRange = viewModel::setAnalysisRange,
            onRestoreFullRange = viewModel::restoreFullAnalysisRange,
            onSelectRoute = viewModel::selectAnalysisRoutePosition,
            onEdit = viewModel::edit,
            onDelete = viewModel::deleteSelected,
        )
    }
}

@Composable
private fun LibraryListContent(
    state: ActivityLibraryUiState,
    onOpen: (com.jeppe.radm.domain.model.ActivityId) -> Unit,
    onStartActivity: () -> Unit,
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text("Activities", style = MaterialTheme.typography.headlineLarge)
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(12.dp))
            if (!state.loading && state.items.isEmpty()) {
                Text(
                    "No saved activities yet",
                    modifier = Modifier
                        .weight(1f)
                        .testTag(ActivityLibraryTestTags.EMPTY),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .testTag(ActivityLibraryTestTags.LIST),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.items, key = { it.activityId.value }) { item ->
                        ActivityLibraryRow(item, onOpen)
                    }
                }
            }
            Button(
                onClick = onStartActivity,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(ActivityLibraryTestTags.START),
            ) {
                Text("Start activity")
            }
        }
    }
}

@Composable
private fun ActivityLibraryRow(
    item: ActivityLibraryItem,
    onOpen: (com.jeppe.radm.domain.model.ActivityId) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(item.activityId) }
            .testTag("activity_${item.activityId.value}"),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(item.title ?: item.activityType.displayName(), style = MaterialTheme.typography.titleMedium)
            if (item.title != null) Text(item.activityType.displayName())
            Text("${formatDate(item.startedAt.value)} · ${item.distance?.value?.let(::formatDistance) ?: "Distance unavailable"}")
            Text(
                "${formatDuration(item.activeDuration.value)} · " +
                    (item.averagePace?.value?.let(::formatPace)
                        ?: item.averageSpeed?.value?.let(::formatSpeed)
                        ?: "Movement metric unavailable"),
            )
        }
    }
}

private fun ActivityType.displayName(): String = when (this) {
    ActivityType.RUNNING -> "Running"
    ActivityType.CYCLING -> "Cycling"
    ActivityType.CROSS_COUNTRY_SKIING -> "Cross-country skiing"
}

private fun formatDate(utcMillis: Long): String = DateTimeFormatter
    .ofLocalizedDate(FormatStyle.MEDIUM)
    .withLocale(Locale.getDefault())
    .format(Instant.ofEpochMilli(utcMillis).atZone(ZoneId.systemDefault()))

private fun formatDistance(metres: Double): String = String.format(Locale.getDefault(), "%.2f km", metres / 1_000.0)

private fun formatDuration(milliseconds: Long): String {
    val seconds = milliseconds / 1_000L
    return String.format(
        Locale.getDefault(),
        "%d:%02d:%02d",
        seconds / 3_600L,
        (seconds % 3_600L) / 60L,
        seconds % 60L,
    )
}

private fun formatPace(secondsPerKilometre: Double): String {
    val seconds = secondsPerKilometre.toLong()
    return String.format(Locale.getDefault(), "%d:%02d/km", seconds / 60L, seconds % 60L)
}

private fun formatSpeed(metresPerSecond: Double): String =
    String.format(Locale.getDefault(), "%.1f km/h", metresPerSecond * 3.6)
