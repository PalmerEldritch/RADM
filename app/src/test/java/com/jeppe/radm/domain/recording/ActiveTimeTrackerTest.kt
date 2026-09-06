package com.jeppe.radm.domain.recording

import com.jeppe.radm.domain.model.ActiveElapsedTimeMillis
import com.jeppe.radm.domain.model.MonotonicTimeMillis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ActiveTimeTrackerTest {
    @Test
    fun `VVM TIME 001 excludes a five minute pause from forty five wall minutes`() {
        val started = ActiveTimeTracker.start(MonotonicTimeMillis(10_000L))
        val paused = started.pause(MonotonicTimeMillis(1_210_000L))

        assertEquals(1_200_000L, paused.elapsedAt(MonotonicTimeMillis(1_510_000L)).value)

        val resumed = paused.resume(MonotonicTimeMillis(1_510_000L))
        val finished = resumed.finish(MonotonicTimeMillis(2_710_000L))

        assertEquals(2_400_000L, finished.elapsedAt(MonotonicTimeMillis(9_999_999L)).value)
        assertEquals(RecordingState.FINALIZING, finished.recordingState)
    }

    @Test
    fun `RECM TIME 004 active time uses no wall clock input`() {
        val tracker = ActiveTimeTracker.start(MonotonicTimeMillis(500L))

        assertEquals(5_000L, tracker.elapsedAt(MonotonicTimeMillis(5_500L)).value)
    }

    @Test
    fun `recovery continues from checkpoint and excludes recovery downtime`() {
        val recovered = ActiveTimeTracker.recoverRecording(
            checkpoint = ActiveElapsedTimeMillis(300_000L),
            resumedAt = MonotonicTimeMillis(50L),
        )

        assertEquals(360_000L, recovered.elapsedAt(MonotonicTimeMillis(60_050L)).value)
    }

    @Test
    fun `paused recovery remains frozen until explicitly resumed`() {
        val paused = ActiveTimeTracker.recoverPaused(ActiveElapsedTimeMillis(300_000L))

        assertEquals(300_000L, paused.elapsedAt(MonotonicTimeMillis(999_999L)).value)
        assertEquals(301_000L, paused.resume(MonotonicTimeMillis(10L)).elapsedAt(MonotonicTimeMillis(1_010L)).value)
    }

    @Test
    fun `monotonic readings cannot move backwards within an active interval`() {
        val tracker = ActiveTimeTracker.start(MonotonicTimeMillis(1_000L))

        assertThrows(IllegalArgumentException::class.java) {
            tracker.elapsedAt(MonotonicTimeMillis(999L))
        }
    }
}
