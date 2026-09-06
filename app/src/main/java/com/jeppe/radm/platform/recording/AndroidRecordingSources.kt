package com.jeppe.radm.platform.recording

import android.os.SystemClock
import com.jeppe.radm.domain.model.AbsoluteTimestampUtcMillis
import com.jeppe.radm.domain.model.MonotonicTimeMillis
import com.jeppe.radm.domain.recording.ClockSource
import com.jeppe.radm.domain.recording.StepMeasurement
import com.jeppe.radm.domain.recording.StepSource

object AndroidClockSource : ClockSource {
    override fun absoluteNow() = AbsoluteTimestampUtcMillis(System.currentTimeMillis())

    override fun monotonicNow() = MonotonicTimeMillis(SystemClock.elapsedRealtime())
}

/** M5 retains an explicit no-op step adapter. Actual Android step acquisition begins in M6. */
class PendingStepSource : StepSource {
    override suspend fun start(consumer: suspend (StepMeasurement) -> Unit) = Unit
    override suspend fun stop() = Unit
}
