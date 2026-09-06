package com.jeppe.radm.domain.model

data class PositionSample(
    val activityId: ActivityId,
    val sampleIndex: SampleIndex,
    val routeSegmentIndex: RouteSegmentIndex,
    val timestamp: AbsoluteTimestampUtcMillis,
    val activeElapsedTime: ActiveElapsedTimeMillis,
    val latitude: LatitudeDegrees,
    val longitude: LongitudeDegrees,
    val elevation: ElevationMetres? = null,
    val horizontalAccuracy: AccuracyMetres? = null,
    val verticalAccuracy: AccuracyMetres? = null,
)

data class StepSample(
    val activityId: ActivityId,
    val sampleIndex: SampleIndex,
    val counterEpoch: StepCounterEpoch,
    val timestamp: AbsoluteTimestampUtcMillis,
    val activeElapsedTime: ActiveElapsedTimeMillis,
    val cumulativeSteps: Long,
) {
    init {
        require(cumulativeSteps >= 0L) { "Cumulative steps must be non-negative" }
    }
}

/** A continuous geographical section. Separate instances are never implicitly connected. */
data class RouteSegment(
    val activityId: ActivityId,
    val index: RouteSegmentIndex,
    val samples: List<PositionSample>,
) {
    init {
        require(samples.isNotEmpty()) { "A route segment must contain at least one sample" }
        require(samples.all { it.activityId == activityId }) {
            "All route samples must belong to the segment activity"
        }
        require(samples.all { it.routeSegmentIndex == index }) {
            "All route samples must use the segment index"
        }
        require(samples.zipWithNext().all { (left, right) -> left.sampleIndex < right.sampleIndex }) {
            "Route samples must have strictly increasing sample indexes"
        }
    }
}

enum class RecordingEventType {
    START,
    PAUSE,
    RESUME,
    FINISH,
    RECOVERY_RESUME,
}

data class RecordingEvent(
    val activityId: ActivityId,
    val eventIndex: EventIndex,
    val type: RecordingEventType,
    val occurredAt: AbsoluteTimestampUtcMillis,
    val activeElapsedTime: ActiveElapsedTimeMillis,
)
