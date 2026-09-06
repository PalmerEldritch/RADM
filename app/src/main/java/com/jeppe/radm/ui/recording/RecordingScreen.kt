package com.jeppe.radm.ui.recording

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jeppe.radm.domain.model.ActivityType
import com.jeppe.radm.domain.model.DistanceMetres
import com.jeppe.radm.domain.location.LocationAvailability
import com.jeppe.radm.domain.recording.RecordingState
import com.jeppe.radm.platform.permissions.LocationPermissionCapability
import com.jeppe.radm.platform.recording.RecordingServiceState
import java.util.Locale

object RecordingTestTags {
    const val START = "recording_start"
    const val PAUSE_RESUME = "recording_pause_resume"
    const val FINISH = "recording_finish"
    const val STATE = "recording_state"
    const val ACTIVITY_ID = "recording_activity_id"
    const val ELAPSED = "recording_elapsed"
    const val DISTANCE = "recording_distance"
    const val LOCATION = "recording_location"
}

@Composable
fun RecordingScreen(viewModel: RecordingViewModel) {
    val recordingState by viewModel.recordingState.collectAsState()
    val capabilities by viewModel.capabilities.collectAsState()
    val message by viewModel.message.collectAsState()
    var selectedType by remember { mutableStateOf(ActivityType.RUNNING) }
    var pendingStart by remember { mutableStateOf<ActivityType?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        viewModel.refreshCapabilities()
        pendingStart?.let(viewModel::start)
        pendingStart = null
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (val state = recordingState) {
                RecordingServiceState.Idle,
                is RecordingServiceState.CriticalError,
                -> IdleRecordingContent(
                    selectedType = selectedType,
                    onTypeSelected = { selectedType = it },
                    locationCapability = capabilities.locationPermission,
                    locationServicesEnabled = capabilities.locationServicesEnabled,
                    error = (state as? RecordingServiceState.CriticalError)?.message ?: message,
                    onStart = {
                        val permissions = viewModel.permissionsForStart(selectedType)
                        if (permissions.isEmpty()) {
                            viewModel.start(selectedType)
                        } else {
                            pendingStart = selectedType
                            permissionLauncher.launch(permissions)
                        }
                    },
                )

                is RecordingServiceState.Starting -> {
                    Text(
                        text = "Starting ${state.activityType.displayName()}…",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.testTag(RecordingTestTags.STATE),
                    )
                }

                is RecordingServiceState.Active -> ActiveRecordingContent(
                    state = state,
                    locationCapability = capabilities.locationPermission,
                    onPause = viewModel::pause,
                    onResume = viewModel::resume,
                    onFinish = viewModel::finish,
                )
            }
        }
    }
}

@Composable
private fun IdleRecordingContent(
    selectedType: ActivityType,
    onTypeSelected: (ActivityType) -> Unit,
    locationCapability: LocationPermissionCapability,
    locationServicesEnabled: Boolean,
    error: String?,
    onStart: () -> Unit,
) {
    Text("New activity", style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(20.dp))
    ActivityType.entries.forEach { type ->
        FilterChip(
            selected = selectedType == type,
            onClick = { onTypeSelected(type) },
            label = { Text(type.displayName()) },
        )
    }
    Spacer(Modifier.height(16.dp))
    val capabilityText = when {
        !locationServicesEnabled -> "Device location services are off"
        locationCapability == LocationPermissionCapability.PRECISE -> "Precise location available"
        locationCapability == LocationPermissionCapability.APPROXIMATE -> "Approximate location only"
        else -> "Location permission needed"
    }
    Text(capabilityText, textAlign = TextAlign.Center)
    error?.let {
        Spacer(Modifier.height(12.dp))
        Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
    }
    Spacer(Modifier.height(24.dp))
    Button(
        onClick = onStart,
        modifier = Modifier.testTag(RecordingTestTags.START),
    ) {
        Text("Start recording")
    }
}

@Composable
private fun ActiveRecordingContent(
    state: RecordingServiceState.Active,
    locationCapability: LocationPermissionCapability,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
) {
    val snapshot = state.snapshot
    Text(snapshot.activityType?.displayName().orEmpty(), style = MaterialTheme.typography.titleLarge)
    Text(
        text = snapshot.state.displayName(),
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.testTag(RecordingTestTags.STATE),
    )
    Spacer(Modifier.height(28.dp))
    Text(
        text = formatElapsed(snapshot.activeElapsedTime.value),
        style = MaterialTheme.typography.displayMedium,
        modifier = Modifier.testTag(RecordingTestTags.ELAPSED),
    )
    Text("Active time")
    Spacer(Modifier.height(20.dp))
    Text(
        text = snapshot.liveDistance?.let(::formatDistance) ?: "—",
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.testTag(RecordingTestTags.DISTANCE),
    )
    Text("Distance")
    val locationText = snapshot.locationAvailability.displayName()
    Text(
        text = if (locationCapability == LocationPermissionCapability.APPROXIMATE) {
            "$locationText (approximate only)"
        } else {
            locationText
        },
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.testTag(RecordingTestTags.LOCATION),
    )
    Text(
        text = snapshot.activityId?.value.orEmpty(),
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.testTag(RecordingTestTags.ACTIVITY_ID),
    )
    Spacer(Modifier.height(32.dp))
    if (snapshot.state == RecordingState.RECORDING) {
        Button(
            onClick = onPause,
            modifier = Modifier.testTag(RecordingTestTags.PAUSE_RESUME),
        ) {
            Text("Pause")
        }
    } else if (snapshot.state == RecordingState.PAUSED) {
        Button(
            onClick = onResume,
            modifier = Modifier.testTag(RecordingTestTags.PAUSE_RESUME),
        ) {
            Text("Resume")
        }
    }
    if (snapshot.state != RecordingState.FINALIZING) {
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(RecordingTestTags.FINISH),
        ) {
            Text("Finish")
        }
    } else {
        Text("Activity complete", style = MaterialTheme.typography.titleLarge)
    }
}

private fun ActivityType.displayName(): String = when (this) {
    ActivityType.RUNNING -> "Running"
    ActivityType.CYCLING -> "Cycling"
    ActivityType.CROSS_COUNTRY_SKIING -> "Cross-country skiing"
}

private fun RecordingState.displayName(): String = when (this) {
    RecordingState.IDLE -> "Ready"
    RecordingState.RECORDING -> "Recording"
    RecordingState.PAUSED -> "Paused"
    RecordingState.FINALIZING -> "Finalizing"
}

private fun LocationAvailability.displayName(): String = when (this) {
    LocationAvailability.ACQUIRING -> "Acquiring location"
    LocationAvailability.AVAILABLE -> "Location available"
    LocationAvailability.DEGRADED -> "Location degraded"
    LocationAvailability.UNAVAILABLE -> "Location unavailable"
}

private fun formatDistance(distance: DistanceMetres): String =
    String.format(Locale.ROOT, "%.2f km", distance.value / 1_000.0)

private fun formatElapsed(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
    }
}
