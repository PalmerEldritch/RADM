package com.jeppe.radm.domain.model

import com.jeppe.radm.domain.analysis.AnalysisPosition
import com.jeppe.radm.domain.analysis.AnalysisRange
import com.jeppe.radm.domain.fixtures.DeterministicFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainModelTest {
    @Test
    fun `DMSM ID 002 accepts only canonical lowercase UUID text`() {
        assertEquals(
            "10000000-0000-4000-8000-000000000001",
            DeterministicFixtures.runningActivityId.value,
        )
        assertThrows(IllegalArgumentException::class.java) {
            ActivityId.parse("10000000-0000-4000-8000-00000000000A")
        }
        assertThrows(IllegalArgumentException::class.java) {
            ActivityId.parse("not-a-uuid")
        }
    }

    @Test
    fun `DMSM POS coordinate and unit types reject invalid numeric values`() {
        assertThrows(IllegalArgumentException::class.java) { LatitudeDegrees(90.1) }
        assertThrows(IllegalArgumentException::class.java) { LongitudeDegrees(-180.1) }
        assertThrows(IllegalArgumentException::class.java) { DistanceMetres(-0.1) }
        assertThrows(IllegalArgumentException::class.java) { AccuracyMetres(Double.NaN) }
        assertThrows(IllegalArgumentException::class.java) { PaceSecondsPerKilometre(Double.POSITIVE_INFINITY) }
        assertThrows(IllegalArgumentException::class.java) { ActiveElapsedTimeMillis(-1L) }
    }

    @Test
    fun `missing measurements remain distinct from numeric zero`() {
        assertNull(DeterministicFixtures.missingElevationRoute.first().elevation)

        val unavailable = CadenceSample(
            activityId = DeterministicFixtures.runningActivityId,
            sampleIndex = SampleIndex(0),
            activeElapsedTime = ActiveElapsedTimeMillis.ZERO,
            cadence = null,
        )
        val stopped = unavailable.copy(cadence = CadenceStepsPerMinute(0.0))

        assertNull(unavailable.cadence)
        assertEquals(0.0, stopped.cadence?.value ?: -1.0, 0.0)
    }

    @Test
    fun `DMSM SEG route boundaries remain explicit in fixtures`() {
        assertEquals(listOf(0L, 0L, 1L, 1L), DeterministicFixtures.routeGap.map { it.routeSegmentIndex.value })
    }

    @Test
    fun `DMSM STEP reset starts a new counter epoch`() {
        val samples = DeterministicFixtures.stepCounterReset

        assertTrue(samples[2].cumulativeSteps < samples[1].cumulativeSteps)
        assertTrue(samples[2].counterEpoch > samples[1].counterEpoch)
    }

    @Test
    fun `analysis coordinate requires active elapsed time and validates its range`() {
        val start = AnalysisPosition(ActiveElapsedTimeMillis(1_000L), DistanceMetres(5.0))
        val end = AnalysisPosition(ActiveElapsedTimeMillis(2_000L), DistanceMetres(10.0))

        assertEquals(start, AnalysisRange(start, end).start)
        assertThrows(IllegalArgumentException::class.java) { AnalysisRange(end, start) }
    }

    @Test
    fun `processor versions begin at one`() {
        assertEquals(1, ProcessorVersion(ProcessorName.DISTANCE, 1).version)
        assertThrows(IllegalArgumentException::class.java) {
            ProcessorVersion(ProcessorName.DISTANCE, 0)
        }
    }

    @Test
    fun `VVM DB 008 processor currentness requires current status and matching version`() {
        val definition = ProcessorDefinition(ProcessorName.DISTANCE, 2)
        val current = ActivityProcessorState(
            activityId = DeterministicFixtures.runningActivityId,
            processorName = ProcessorName.DISTANCE,
            processorVersion = 2,
            status = ProcessorStatus.CURRENT,
            processedAt = AbsoluteTimestampUtcMillis(1_000L),
        )

        assertTrue(current.isCurrentAgainst(definition))
        assertTrue(!current.copy(processorVersion = 1).isCurrentAgainst(definition))
        assertTrue(!current.copy(status = ProcessorStatus.FAILED).isCurrentAgainst(definition))
        assertThrows(IllegalArgumentException::class.java) {
            current.copy(processorVersion = null)
        }
    }

    @Test
    fun `large fixtures are deterministic generators with specified capacities`() {
        val positions = DeterministicFixtures.largeActivityPositions()
        assertEquals(100_000L, positions.count().toLong())

        val summaries = DeterministicFixtures.largeLibrarySummaries()
        assertEquals(10_000L, summaries.count().toLong())
        assertEquals(
            DeterministicFixtures.largeLibrarySummaries().first(),
            DeterministicFixtures.largeLibrarySummaries().first(),
        )
    }
}
