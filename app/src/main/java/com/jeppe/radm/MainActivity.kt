package com.jeppe.radm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.jeppe.radm.platform.recording.RecordingServiceState
import com.jeppe.radm.ui.library.ActivityLibraryScreen
import com.jeppe.radm.ui.library.ActivityLibraryViewModel
import com.jeppe.radm.ui.recording.RecordingScreen
import com.jeppe.radm.ui.recording.RecordingViewModel
import com.jeppe.radm.ui.theme.RADMTheme

class MainActivity : ComponentActivity() {
    private val recordingViewModel by viewModels<RecordingViewModel> {
        RecordingViewModel.factory((application as RadmApplication).container)
    }
    private val libraryViewModel by viewModels<ActivityLibraryViewModel> {
        ActivityLibraryViewModel.factory((application as RadmApplication).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RADMTheme {
                RadmApp(recordingViewModel, libraryViewModel)
            }
        }
    }
}

@Composable
private fun RadmApp(
    recordingViewModel: RecordingViewModel,
    libraryViewModel: ActivityLibraryViewModel,
) {
    val serviceState by recordingViewModel.recordingState.collectAsState()
    var recordingFlowRequested by rememberSaveable { mutableStateOf(false) }
    var observedRecording by rememberSaveable { mutableStateOf(false) }
    val serviceOwnsVisibleFlow = serviceState is RecordingServiceState.Starting ||
        serviceState is RecordingServiceState.Active

    LaunchedEffect(serviceState) {
        if (serviceOwnsVisibleFlow) {
            observedRecording = true
        } else if (serviceState is RecordingServiceState.Idle && observedRecording) {
            observedRecording = false
            recordingFlowRequested = false
            libraryViewModel.refresh()
        }
    }

    if (serviceOwnsVisibleFlow || recordingFlowRequested) {
        RecordingScreen(
            viewModel = recordingViewModel,
            onBackToLibrary = {
                recordingFlowRequested = false
                libraryViewModel.refresh()
            },
        )
    } else {
        ActivityLibraryScreen(
            viewModel = libraryViewModel,
            onStartActivity = { recordingFlowRequested = true },
        )
    }
}
