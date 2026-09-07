package com.jeppe.radm.domain.recording

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/** VVM-STEP-001..003 and VVM-STEP-007 at the Android-independent acquisition boundary. */
class StepCounterProcessorTest {
    @Test
    fun `VVM STEP 001 first cumulative value is epoch-zero baseline rather than activity steps`() {
        val processor = StepCounterProcessor()

        val accepted = processor.accept(candidate(time = 1_000L, steps = 28_451L)).accepted()

        assertEquals(0L, accepted.measurement.counterEpoch.value)
        assertEquals(28_451L, accepted.measurement.cumulativeSteps)
        assertNull(accepted.stepDelta)
    }

    @Test
    fun `VVM STEP 002 increasing values expose only within-epoch deltas`() {
        val processor = StepCounterProcessor()
        processor.accept(candidate(time = 1_000L, steps = 28_451L))

        val accepted = processor.accept(candidate(time = 11_000L, steps = 28_469L)).accepted()

        assertEquals(0L, accepted.measurement.counterEpoch.value)
        assertEquals(18L, accepted.stepDelta)
    }

    @Test
    fun `VVM STEP 003 counter decrease starts epoch and never emits negative delta`() {
        val processor = StepCounterProcessor()
        processor.accept(candidate(time = 1_000L, steps = 28_510L))

        val reset = processor.accept(candidate(time = 2_000L, steps = 15L)).accepted()
        val continued = processor.accept(candidate(time = 3_000L, steps = 19L)).accepted()

        assertEquals(1L, reset.measurement.counterEpoch.value)
        assertNull(reset.stepDelta)
        assertEquals(1L, continued.measurement.counterEpoch.value)
        assertEquals(4L, continued.stepDelta)
    }

    @Test
    fun `duplicate and reordered events cannot create a false reset epoch`() {
        val processor = StepCounterProcessor()
        processor.accept(candidate(time = 2_000L, steps = 100L))

        assertSame(
            StepCounterResult.RejectedDuplicateOrReordered,
            processor.accept(candidate(time = 2_000L, steps = 100L)),
        )
        assertSame(
            StepCounterResult.RejectedDuplicateOrReordered,
            processor.accept(candidate(time = 1_000L, steps = 1L)),
        )
        val next = processor.accept(candidate(time = 3_000L, steps = 102L)).accepted()
        assertEquals(0L, next.measurement.counterEpoch.value)
        assertEquals(2L, next.stepDelta)
    }

    @Test
    fun `VVM STEP 007 resumed first value is a new baseline excluding paused steps`() {
        val processor = StepCounterProcessor()
        processor.accept(candidate(time = 1_000L, steps = 500L))
        processor.accept(candidate(time = 2_000L, steps = 510L))

        processor.beginNewBaseline()
        val resumedBaseline = processor.accept(candidate(time = 10_000L, steps = 560L)).accepted()
        val resumedDelta = processor.accept(candidate(time = 11_000L, steps = 563L)).accepted()

        assertEquals(1L, resumedBaseline.measurement.counterEpoch.value)
        assertNull(resumedBaseline.stepDelta)
        assertEquals(3L, resumedDelta.stepDelta)
    }

    private fun candidate(time: Long, steps: Long) = StepCounterCandidate(
        timestampUtcMillis = BASE_UTC + time,
        monotonicTimestampMillis = time,
        cumulativeSteps = steps,
    )

    private fun StepCounterResult.accepted() = this as StepCounterResult.Accepted

    private companion object {
        const val BASE_UTC = 1_788_379_200_000L
    }
}
