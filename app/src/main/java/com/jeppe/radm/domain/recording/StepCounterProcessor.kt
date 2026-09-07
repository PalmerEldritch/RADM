package com.jeppe.radm.domain.recording

import com.jeppe.radm.domain.model.AbsoluteTimestampUtcMillis
import com.jeppe.radm.domain.model.MonotonicTimeMillis
import com.jeppe.radm.domain.model.StepCounterEpoch

/** Raw, source-neutral cumulative step-counter input. */
data class StepCounterCandidate(
    val timestampUtcMillis: Long,
    val monotonicTimestampMillis: Long,
    val cumulativeSteps: Long,
)

sealed interface StepCounterResult {
    data class Accepted(
        val measurement: StepMeasurement,
        /** Null for the first baseline of an epoch; otherwise the within-epoch delta. */
        val stepDelta: Long?,
    ) : StepCounterResult

    data object RejectedDuplicateOrReordered : StepCounterResult
    data object RejectedMalformed : StepCounterResult
}

/**
 * Resolves cumulative counter baselines, resets, and comparable epochs without Android types.
 * The first accepted value is epoch zero. A restart after at least one retained value makes the
 * next accepted value a new baseline so paused or interrupted steps cannot enter an active delta.
 */
class StepCounterProcessor {
    private var lastMeasurement: StepMeasurement? = null
    private var nextMeasurementStartsNewEpoch = false

    fun beginNewBaseline() {
        if (lastMeasurement != null) nextMeasurementStartsNewEpoch = true
    }

    fun accept(candidate: StepCounterCandidate): StepCounterResult {
        if (
            candidate.timestampUtcMillis < 0L ||
            candidate.monotonicTimestampMillis < 0L ||
            candidate.cumulativeSteps < 0L
        ) {
            return StepCounterResult.RejectedMalformed
        }

        val previous = lastMeasurement
        if (
            previous != null &&
            candidate.monotonicTimestampMillis <= previous.monotonicTimestamp.value
        ) {
            return StepCounterResult.RejectedDuplicateOrReordered
        }

        val startsNewEpoch = previous != null &&
            (nextMeasurementStartsNewEpoch || candidate.cumulativeSteps < previous.cumulativeSteps)
        val epoch = when {
            previous == null -> StepCounterEpoch(0L)
            startsNewEpoch -> StepCounterEpoch(Math.addExact(previous.counterEpoch.value, 1L))
            else -> previous.counterEpoch
        }
        val measurement = StepMeasurement(
            timestamp = AbsoluteTimestampUtcMillis(candidate.timestampUtcMillis),
            monotonicTimestamp = MonotonicTimeMillis(candidate.monotonicTimestampMillis),
            counterEpoch = epoch,
            cumulativeSteps = candidate.cumulativeSteps,
        )
        val delta = if (previous != null && !startsNewEpoch) {
            Math.subtractExact(measurement.cumulativeSteps, previous.cumulativeSteps)
        } else {
            null
        }

        lastMeasurement = measurement
        nextMeasurementStartsNewEpoch = false
        return StepCounterResult.Accepted(measurement, delta)
    }
}
