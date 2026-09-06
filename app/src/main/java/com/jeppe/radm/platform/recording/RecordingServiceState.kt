package com.jeppe.radm.platform.recording

import com.jeppe.radm.domain.model.ActivityType
import com.jeppe.radm.application.recording.RecordingSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface RecordingServiceState {
    data object Idle : RecordingServiceState

    data class Starting(
        val activityType: ActivityType,
    ) : RecordingServiceState

    data class Active(
        val snapshot: RecordingSnapshot,
    ) : RecordingServiceState

    data class CriticalError(
        val message: String,
        val lastSnapshot: RecordingSnapshot? = null,
    ) : RecordingServiceState
}

/** Process-local observation mirror; authoritative mutation remains in the service controller. */
class RecordingServiceStateStore {
    private val mutableState = MutableStateFlow<RecordingServiceState>(RecordingServiceState.Idle)
    val state: StateFlow<RecordingServiceState> = mutableState.asStateFlow()

    fun publish(state: RecordingServiceState) {
        mutableState.value = state
    }
}
