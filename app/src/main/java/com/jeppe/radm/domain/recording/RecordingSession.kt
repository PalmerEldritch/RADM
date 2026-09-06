package com.jeppe.radm.domain.recording

import com.jeppe.radm.domain.model.AbsoluteTimestampUtcMillis
import com.jeppe.radm.domain.model.ActiveElapsedTimeMillis
import com.jeppe.radm.domain.model.ActivityId
import com.jeppe.radm.domain.model.RouteSegmentIndex

/** Durable checkpoint for the repository's single unresolved recording. */
data class RecordingSession(
    val activityId: ActivityId,
    val state: RecordingState,
    val activeElapsedTime: ActiveElapsedTimeMillis,
    val stateEnteredAt: AbsoluteTimestampUtcMillis,
    val lastCheckpointAt: AbsoluteTimestampUtcMillis,
    val routeSegmentIndex: RouteSegmentIndex,
    val positionSampleCount: Long,
    val stepSampleCount: Long,
) {
    init {
        require(state != RecordingState.IDLE) { "IDLE is represented by no recording-session row" }
        require(positionSampleCount >= 0L) { "Position sample count must be non-negative" }
        require(stepSampleCount >= 0L) { "Step sample count must be non-negative" }
    }
}
