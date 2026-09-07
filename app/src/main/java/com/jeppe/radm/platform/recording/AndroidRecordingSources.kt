package com.jeppe.radm.platform.recording

import android.os.SystemClock
import com.jeppe.radm.domain.model.AbsoluteTimestampUtcMillis
import com.jeppe.radm.domain.model.MonotonicTimeMillis
import com.jeppe.radm.domain.recording.ClockSource

object AndroidClockSource : ClockSource {
    override fun absoluteNow() = AbsoluteTimestampUtcMillis(System.currentTimeMillis())

    override fun monotonicNow() = MonotonicTimeMillis(SystemClock.elapsedRealtime())
}
