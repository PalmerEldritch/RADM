package com.jeppe.radm.platform.recording

import android.os.SystemClock
import com.jeppe.radm.domain.model.AbsoluteTimestampUtcMillis
import com.jeppe.radm.domain.model.MonotonicTimeMillis
import com.jeppe.radm.domain.recording.ClockSource
import com.jeppe.radm.domain.recording.LocationMeasurement
import com.jeppe.radm.domain.recording.LocationSource
import com.jeppe.radm.domain.recording.StepMeasurement
import com.jeppe.radm.domain.recording.StepSource

object AndroidClockSource : ClockSource {
    override fun absoluteNow() = AbsoluteTimestampUtcMillis(System.currentTimeMillis())

    override fun monotonicNow() = MonotonicTimeMillis(SystemClock.elapsedRealtime())
}

/** M4 shell adapter. Actual Android location acquisition begins in M5. */
class PendingLocationSource : LocationSource {
    override suspend fun start(consumer: suspend (LocationMeasurement) -> Unit) = Unit
    override suspend fun stop() = Unit
}

/** M4 shell adapter. Actual Android step acquisition begins in M6. */
class PendingStepSource : StepSource {
    override suspend fun start(consumer: suspend (StepMeasurement) -> Unit) = Unit
    override suspend fun stop() = Unit
}
