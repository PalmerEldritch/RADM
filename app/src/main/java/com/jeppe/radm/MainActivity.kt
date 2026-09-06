package com.jeppe.radm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.jeppe.radm.ui.recording.RecordingScreen
import com.jeppe.radm.ui.recording.RecordingViewModel
import com.jeppe.radm.ui.theme.RADMTheme

class MainActivity : ComponentActivity() {
    private val recordingViewModel by viewModels<RecordingViewModel> {
        RecordingViewModel.factory((application as RadmApplication).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RADMTheme {
                RecordingScreen(recordingViewModel)
            }
        }
    }
}
