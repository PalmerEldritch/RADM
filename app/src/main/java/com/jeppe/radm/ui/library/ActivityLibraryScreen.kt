package com.jeppe.radm.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.jeppe.radm.application.library.SavedActivityDetails
import com.jeppe.radm.domain.model.ActivityLibraryItem
import com.jeppe.radm.domain.model.ActivityType
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
    if (selected == null) {
        LibraryListContent(state, viewModel::open, onStartActivity)
    } else {
        ActivityDetailContent(
            details = selected,
            saving = state.saving,
            error = state.error,
            onBack = viewModel::closeActivity,
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

@Composable
private fun ActivityDetailContent(
    details: SavedActivityDetails,
    saving: Boolean,
    error: String?,
    onBack: () -> Unit,
    onEdit: (ActivityType, String?, String?) -> Unit,
    onDelete: () -> Unit,
) {
    val activity = details.activity
    val summary = details.summary
    var editing by rememberSaveable(activity.id.value) { mutableStateOf(false) }
    var deleteConfirmation by remember { mutableStateOf(false) }
    var type by rememberSaveable(activity.id.value, editing) { mutableStateOf(activity.type) }
    var title by rememberSaveable(activity.id.value, editing) { mutableStateOf(activity.title.orEmpty()) }
    var notes by rememberSaveable(activity.id.value, editing) { mutableStateOf(activity.notes.orEmpty()) }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp)
                .testTag(ActivityLibraryTestTags.DETAIL),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                TextButton(onClick = onBack) { Text("Back to activities") }
                Text("Activity analysis", style = MaterialTheme.typography.headlineLarge)
                Text(activity.title ?: activity.type.displayName(), style = MaterialTheme.typography.titleLarge)
                Text("${activity.type.displayName()} · ${formatDate(activity.startedAt.value)}")
                Text("Active time: ${formatDuration(activity.activeDuration.value)}")
                Text("Distance: ${summary?.distance?.value?.let(::formatDistance) ?: "Unavailable"}")
                activity.notes?.let { Text("Notes: $it") }
                summary?.averagePace?.value?.let { Text("Average pace: ${formatPace(it)}") }
                summary?.averageSpeed?.value?.let { Text("Average speed: ${formatSpeed(it)}") }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            if (editing) {
                item {
                    Text("Edit activity", style = MaterialTheme.typography.titleLarge)
                    ActivityType.entries.forEach { activityType ->
                        FilterChip(
                            selected = type == activityType,
                            onClick = { type = activityType },
                            label = { Text(activityType.displayName()) },
                            modifier = Modifier.testTag("activity_edit_type_${activityType.name}"),
                        )
                    }
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title (optional)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(ActivityLibraryTestTags.EDIT_TITLE),
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (optional)") },
                        minLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(ActivityLibraryTestTags.EDIT_NOTES),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                onEdit(type, title, notes)
                                editing = false
                            },
                            enabled = !saving,
                            modifier = Modifier.testTag(ActivityLibraryTestTags.EDIT_SAVE),
                        ) { Text("Save changes") }
                        TextButton(onClick = { editing = false }) { Text("Cancel") }
                    }
                }
            } else {
                item {
                    Button(
                        onClick = { editing = true },
                        modifier = Modifier.testTag(ActivityLibraryTestTags.EDIT),
                    ) { Text("Edit metadata") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { deleteConfirmation = true },
                        modifier = Modifier.testTag(ActivityLibraryTestTags.DELETE),
                    ) { Text("Delete activity") }
                }
            }
        }
    }

    if (deleteConfirmation) {
        AlertDialog(
            onDismissRequest = { deleteConfirmation = false },
            title = { Text("Delete activity?") },
            text = { Text("This permanently removes this activity and its recorded data.") },
            confirmButton = {
                Button(
                    onClick = {
                        deleteConfirmation = false
                        onDelete()
                    },
                    modifier = Modifier.testTag(ActivityLibraryTestTags.DELETE_CONFIRM),
                ) { Text("Delete permanently") }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmation = false }) { Text("Cancel") }
            },
        )
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
