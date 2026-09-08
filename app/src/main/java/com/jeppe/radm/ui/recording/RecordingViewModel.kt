package com.jeppe.radm.ui.recording

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jeppe.radm.RadmContainer
import com.jeppe.radm.application.recording.SaveRecordingMetadata
import com.jeppe.radm.domain.model.ActivityType
import com.jeppe.radm.platform.permissions.RecordingCapabilities
import com.jeppe.radm.platform.recording.RecordingServiceClient
import com.jeppe.radm.platform.recording.RecordingServiceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecordingViewModel(
    private val serviceClient: RecordingServiceClient,
    private val container: RadmContainer,
) : ViewModel() {
    val recordingState: StateFlow<RecordingServiceState> = container.recordingStateStore.state

    private val mutableCapabilities = MutableStateFlow(container.recordingCapabilityChecker.current())
    val capabilities: StateFlow<RecordingCapabilities> = mutableCapabilities.asStateFlow()

    private val mutableMessage = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = mutableMessage.asStateFlow()

    fun permissionsForStart(activityType: ActivityType): Array<String> =
        container.recordingCapabilityChecker.permissionsForUserVisibleStart(
            includeRunningSteps = activityType == ActivityType.RUNNING,
        )

    fun refreshCapabilities() {
        mutableCapabilities.value = container.recordingCapabilityChecker.current()
    }

    fun start(activityType: ActivityType) {
        refreshCapabilities()
        if (!capabilities.value.canStartLocationForegroundService) {
            mutableMessage.value = if (!capabilities.value.locationServicesEnabled) {
                "Turn on device location services before starting recording."
            } else {
                "Location permission is required by Android to start location recording."
            }
            return
        }
        mutableMessage.value = null
        serviceClient.start(activityType).onFailure(::publishFailure)
    }

    fun pause() {
        serviceClient.pause().onFailure(::publishFailure)
    }

    fun resume() {
        serviceClient.resume().onFailure(::publishFailure)
    }

    fun resumeRecovery() {
        refreshCapabilities()
        serviceClient.resumeRecovery().onFailure(::publishFailure)
    }

    fun finishAndSaveRecovery() {
        viewModelScope.launch {
            mutableMessage.value = null
            runCatching {
                withContext(Dispatchers.IO) { container.recordingRecovery.finishAndSave() }
            }.onSuccess {
                container.recordingStateStore.publish(RecordingServiceState.Idle)
            }.onFailure(::publishFailure)
        }
    }

    fun discardRecovery() {
        viewModelScope.launch {
            mutableMessage.value = null
            runCatching {
                withContext(Dispatchers.IO) { container.recordingRecovery.discard() }
            }.onSuccess {
                container.recordingStateStore.publish(RecordingServiceState.Idle)
            }.onFailure(::publishFailure)
        }
    }

    fun finish() {
        serviceClient.finish().onFailure(::publishFailure)
    }

    fun save(metadata: SaveRecordingMetadata) {
        serviceClient.save(metadata).onFailure(::publishFailure)
    }

    fun discard() {
        serviceClient.discard().onFailure(::publishFailure)
    }

    private fun publishFailure(failure: Throwable) {
        mutableMessage.value = failure.message ?: "Recording command failed."
    }

    companion object {
        fun factory(container: RadmContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    RecordingViewModel(container.recordingServiceClient, container) as T
            }
    }
}
