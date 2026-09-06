package com.jeppe.radm.domain.recording

import com.jeppe.radm.domain.model.ActiveElapsedTimeMillis
import com.jeppe.radm.domain.model.MonotonicTimeMillis

/**
 * Immutable active-time state based only on monotonic readings.
 *
 * A recovered recording is deliberately given a new monotonic origin while
 * retaining its durable active-time checkpoint, so recovery downtime cannot be
 * counted accidentally.
 */
class ActiveTimeTracker private constructor(
    val recordingState: RecordingState,
    private val completedActiveTime: ActiveElapsedTimeMillis,
    private val activeIntervalStartedAt: MonotonicTimeMillis?,
) {
    fun elapsedAt(now: MonotonicTimeMillis): ActiveElapsedTimeMillis = when (recordingState) {
        RecordingState.RECORDING -> completedActiveTime.plus(intervalDuration(now))
        RecordingState.IDLE,
        RecordingState.PAUSED,
        RecordingState.FINALIZING,
        -> completedActiveTime
    }

    fun pause(at: MonotonicTimeMillis): ActiveTimeTracker {
        require(recordingState == RecordingState.RECORDING) { "Only a recording tracker can pause" }
        return ActiveTimeTracker(
            recordingState = RecordingState.PAUSED,
            completedActiveTime = elapsedAt(at),
            activeIntervalStartedAt = null,
        )
    }

    fun resume(at: MonotonicTimeMillis): ActiveTimeTracker {
        require(recordingState == RecordingState.PAUSED) { "Only a paused tracker can resume" }
        return ActiveTimeTracker(
            recordingState = RecordingState.RECORDING,
            completedActiveTime = completedActiveTime,
            activeIntervalStartedAt = at,
        )
    }

    fun finish(at: MonotonicTimeMillis): ActiveTimeTracker {
        require(recordingState == RecordingState.RECORDING || recordingState == RecordingState.PAUSED) {
            "Only a recording or paused tracker can finish"
        }
        return ActiveTimeTracker(
            recordingState = RecordingState.FINALIZING,
            completedActiveTime = elapsedAt(at),
            activeIntervalStartedAt = null,
        )
    }

    private fun intervalDuration(now: MonotonicTimeMillis): Long {
        val startedAt = checkNotNull(activeIntervalStartedAt) {
            "Recording tracker requires an active interval origin"
        }
        require(now >= startedAt) { "Monotonic time must not move backwards" }
        return Math.subtractExact(now.value, startedAt.value)
    }

    private fun ActiveElapsedTimeMillis.plus(deltaMillis: Long): ActiveElapsedTimeMillis =
        ActiveElapsedTimeMillis(Math.addExact(value, deltaMillis))

    companion object {
        fun idle(): ActiveTimeTracker = ActiveTimeTracker(
            recordingState = RecordingState.IDLE,
            completedActiveTime = ActiveElapsedTimeMillis.ZERO,
            activeIntervalStartedAt = null,
        )

        fun start(at: MonotonicTimeMillis): ActiveTimeTracker = ActiveTimeTracker(
            recordingState = RecordingState.RECORDING,
            completedActiveTime = ActiveElapsedTimeMillis.ZERO,
            activeIntervalStartedAt = at,
        )

        fun recoverRecording(
            checkpoint: ActiveElapsedTimeMillis,
            resumedAt: MonotonicTimeMillis,
        ): ActiveTimeTracker = ActiveTimeTracker(
            recordingState = RecordingState.RECORDING,
            completedActiveTime = checkpoint,
            activeIntervalStartedAt = resumedAt,
        )

        fun recoverPaused(checkpoint: ActiveElapsedTimeMillis): ActiveTimeTracker = ActiveTimeTracker(
            recordingState = RecordingState.PAUSED,
            completedActiveTime = checkpoint,
            activeIntervalStartedAt = null,
        )
    }
}
