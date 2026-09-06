package com.jeppe.radm.domain.recording

import com.jeppe.radm.domain.model.AbsoluteTimestampUtcMillis
import com.jeppe.radm.domain.model.AccuracyMetres
import com.jeppe.radm.domain.model.ElevationMetres
import com.jeppe.radm.domain.model.LatitudeDegrees
import com.jeppe.radm.domain.model.LongitudeDegrees
import com.jeppe.radm.domain.model.MonotonicTimeMillis
import com.jeppe.radm.domain.model.StepCounterEpoch

/** Android-independent boundary implemented by the later platform location adapter. */
interface LocationSource {
    suspend fun start(consumer: suspend (LocationMeasurement) -> Unit)
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
 * A normalized, already accepted geographical source measurement.
 *
 * Location validation and route-gap detection are introduced in M5. The M3
 * fake source supplies this boundary type directly so the recording lifecycle
 * can be proven without Android Location or prematurely implementing M5.
 */
data class LocationMeasurement(
    val timestamp: AbsoluteTimestampUtcMillis,
    val monotonicTimestamp: MonotonicTimeMillis,
    val latitude: LatitudeDegrees,
    val longitude: LongitudeDegrees,
    val elevation: ElevationMetres? = null,
    val horizontalAccuracy: AccuracyMetres? = null,
    val verticalAccuracy: AccuracyMetres? = null,
)

/**
 * A normalized cumulative step measurement. Counter reset/epoch detection is
 * owned by the M6 step-acquisition adapter, so M3 receives the resolved epoch.
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
