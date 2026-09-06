package com.jeppe.radm.domain.recording

import com.jeppe.radm.domain.model.AbsoluteTimestampUtcMillis
import com.jeppe.radm.domain.model.MonotonicTimeMillis
import com.jeppe.radm.domain.model.StepCounterEpoch

/** Android-independent boundary implemented by the platform location adapter. */
interface LocationSource {
    suspend fun start(consumer: suspend (LocationSourceEvent) -> Unit)
    suspend fun stop()
}

/** Android-independent boundary implemented by the later platform step adapter. */
interface StepSource {
    suspend fun start(consumer: suspend (StepMeasurement) -> Unit)
    suspend fun stop()
}

/** Separate civil and monotonic time sources preserve RECM-TIME-004. */
interface ClockSource {
    fun absoluteNow(): AbsoluteTimestampUtcMillis
    fun monotonicNow(): MonotonicTimeMillis
}

/**
 * Raw provider candidate. Primitive numerical values deliberately remain unvalidated so the
 * Android-independent acceptance policy can reject malformed platform input deterministically.
 */
data class LocationCandidate(
    val timestampUtcMillis: Long,
    val monotonicTimestampMillis: Long,
    val latitudeDegrees: Double,
    val longitudeDegrees: Double,
    val elevationMetres: Double? = null,
    val horizontalAccuracyMetres: Double? = null,
    val verticalAccuracyMetres: Double? = null,
)

sealed interface LocationSourceEvent {
    data class Candidate(val value: LocationCandidate) : LocationSourceEvent
    data object ProviderAvailable : LocationSourceEvent
    data object ProviderUnavailable : LocationSourceEvent
}

/**
 * A normalized cumulative step measurement. Counter reset/epoch detection is
 * owned by the M6 step-acquisition adapter, so the controller receives the resolved epoch.
 */
data class StepMeasurement(
    val timestamp: AbsoluteTimestampUtcMillis,
    val monotonicTimestamp: MonotonicTimeMillis,
    val counterEpoch: StepCounterEpoch,
    val cumulativeSteps: Long,
) {
    init {
        require(cumulativeSteps >= 0L) { "Cumulative steps must be non-negative" }
    }
}
