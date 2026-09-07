package com.jeppe.radm.domain.processing

import com.jeppe.radm.domain.model.AbsoluteTimestampUtcMillis
import com.jeppe.radm.domain.model.ActiveElapsedTimeMillis
import com.jeppe.radm.domain.model.Activity
import com.jeppe.radm.domain.model.ActivityId
import com.jeppe.radm.domain.model.ActivityType
import com.jeppe.radm.domain.model.DistanceMetres
import com.jeppe.radm.domain.model.DistanceSample
import com.jeppe.radm.domain.model.ElevationMetres
import com.jeppe.radm.domain.model.LatitudeDegrees
import com.jeppe.radm.domain.model.LongitudeDegrees
import com.jeppe.radm.domain.model.PositionSample
import com.jeppe.radm.domain.model.ProvenanceType
import com.jeppe.radm.domain.model.RouteSegmentIndex
import com.jeppe.radm.domain.model.SampleIndex
import com.jeppe.radm.domain.model.StepCounterEpoch
import com.jeppe.radm.domain.model.StepSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinalProcessorsTest {
    private val distanceProcessor = R00DistanceProcessor()
    private val movementProcessor = R00MovementProcessor()
    private val cadenceProcessor = R00CadenceProcessor()
    private val summaryProcessor = R00SummaryProcessor()

    @Test
    fun `VVM PROC 001 final distance matches deterministic WGS84 fixture`() {
        val positions = listOf(
            position(0, 0, 0, longitude = 0.0),
            position(1, 0, 1_000, longitude = 0.001),
            position(2, 0, 2_000, longitude = 0.002),
        )

        val result = distanceProcessor.process(positions)

        assertEquals(0.0, result[0].cumulativeDistance.value, TOLERANCE)
        assertEquals(111.195080, result[1].cumulativeDistance.value, 0.001)
        assertEquals(222.390160, result[2].cumulativeDistance.value, 0.001)
        assertTrue(result.zipWithNext().all { (left, right) ->
            left.cumulativeDistance <= right.cumulativeDistance
        })
    }

    @Test
    fun `VVM PROC 002 distance never crosses a route segment boundary`() {
        val positions = listOf(
            position(0, 0, 0, longitude = 0.0),
            position(1, 0, 1_000, longitude = 0.001),
            position(2, 1, 20_000, latitude = 1.0, longitude = 1.0),
            position(3, 1, 21_000, latitude = 1.0, longitude = 1.001),
        )

        val result = distanceProcessor.process(positions)

        assertEquals(result[1].cumulativeDistance, result[2].cumulativeDistance)
        assertEquals(222.373225, result.last().cumulativeDistance.value, 0.01)
    }

    @Test
    fun `VVM PROC 003 centred ten second pace interpolates final distance`() {
        val positions = movementPositions()
        val distance = distanceSeries(listOf(0.0, 10.0, 30.0, 60.0, 100.0))

        val result = movementProcessor.process(ActivityType.RUNNING, positions, distance)

        assertEquals(200.0, result[2].pace?.value ?: error("pace unavailable"), TOLERANCE)
        assertTrue(result.all { it.speed == null })
    }

    @Test
    fun `VVM PROC 004 centred ten second cycling speed uses metres per second`() {
        val positions = movementPositions()
        val distance = distanceSeries(listOf(0.0, 10.0, 30.0, 60.0, 100.0))

        val result = movementProcessor.process(ActivityType.CYCLING, positions, distance)

        assertEquals(5.0, result[2].speed?.value ?: error("speed unavailable"), TOLERANCE)
        assertTrue(result.all { it.pace == null })
    }

    @Test
    fun `VVM PROC 005 cadence uses ten second cumulative step delta`() {
        val steps = listOf(
            step(0, 0, 0, 100),
            step(1, 0, 5_000, 108),
            step(2, 0, 10_000, 116),
            step(3, 0, 15_000, 126),
        )

        val result = cadenceProcessor.process(ActivityType.RUNNING, steps)

        assertNull(result.first().cadence)
        assertEquals(108.0, result.last().cadence?.value ?: error("cadence unavailable"), TOLERANCE)
    }

    @Test
    fun `VVM PROC 006 cadence does not cross counter epochs`() {
        val steps = listOf(
            step(0, 0, 0, 100),
            step(1, 0, 5_000, 108),
            step(2, 1, 10_000, 3),
            step(3, 1, 15_000, 13),
        )

        val result = cadenceProcessor.process(ActivityType.RUNNING, steps)

        assertNull(result[2].cadence)
        assertEquals(120.0, result[3].cadence?.value ?: error("cadence unavailable"), TOLERANCE)
    }

    @Test
    fun `VVM PROC 007 invalid or stationary windows stay unavailable and finite`() {
        val positions = movementPositions()
        val stationary = distanceSeries(List(positions.size) { 0.0 })
        val invalidSteps = listOf(step(0, 0, 0, 100), step(1, 0, 0, 101))

        val movement = movementProcessor.process(ActivityType.RUNNING, positions, stationary)
        val cadence = cadenceProcessor.process(ActivityType.RUNNING, invalidSteps)

        assertTrue(movement.all { it.pace == null && it.speed == null })
        assertTrue(cadence.all { it.cadence == null })
    }

    @Test
    fun `VVM PROC 008 repeated final processing is reproducible`() {
        val positions = movementPositions()

        val firstDistance = distanceProcessor.process(positions)
        val firstMovement = movementProcessor.process(ActivityType.RUNNING, positions, firstDistance)
        val secondDistance = distanceProcessor.process(positions)
        val secondMovement = movementProcessor.process(ActivityType.RUNNING, positions, secondDistance)

        assertEquals(firstDistance, secondDistance)
        assertEquals(firstMovement, secondMovement)
    }

    @Test
    fun `SRS SUM summary uses active duration type applicability and available elevation`() {
        val positions = movementPositions().mapIndexed { index, sample ->
            sample.copy(elevation = if (index == 1) null else ElevationMetres(20.0 + index))
        }
        val activity = activity(ActivityType.RUNNING, activeDurationMillis = 20_000)
        val track = movementProcessor.process(
            ActivityType.RUNNING,
            positions,
            distanceSeries(listOf(0.0, 10.0, 30.0, 60.0, 100.0)),
        )

        val summary = summaryProcessor.process(activity, positions, track)

        assertEquals(100.0, summary.distance?.value ?: error("distance unavailable"), TOLERANCE)
        assertEquals(200.0, summary.averagePace?.value ?: error("pace unavailable"), TOLERANCE)
        assertNull(summary.averageSpeed)
        assertEquals(20.0, summary.minimumElevation?.value ?: error("min unavailable"), TOLERANCE)
        assertEquals(24.0, summary.maximumElevation?.value ?: error("max unavailable"), TOLERANCE)
        assertNull(summary.totalAscent)
    }

    @Test
    fun `SRS SUM no route and missing elevation remain unavailable rather than zero`() {
        val summary = summaryProcessor.process(activity(ActivityType.CYCLING, 60_000), emptyList(), emptyList())

        assertNull(summary.distance)
        assertNull(summary.averagePace)
        assertNull(summary.averageSpeed)
        assertNull(summary.minimumElevation)
        assertNull(summary.maximumElevation)
        assertNull(summary.totalAscent)
    }

    @Test
    fun `VVM PROC 010 cadence and movement applicability follow changed activity type`() {
        val positions = movementPositions()
        val distance = distanceSeries(listOf(0.0, 10.0, 30.0, 60.0, 100.0))
        val steps = listOf(step(0, 0, 0, 100), step(1, 0, 10_000, 120))

        val running = movementProcessor.process(ActivityType.RUNNING, positions, distance)
        val cycling = movementProcessor.process(ActivityType.CYCLING, positions, distance)

        assertTrue(running.any { it.pace != null } && running.all { it.speed == null })
        assertTrue(cycling.any { it.speed != null } && cycling.all { it.pace == null })
        assertTrue(cadenceProcessor.process(ActivityType.CYCLING, steps).isEmpty())
    }

    private fun movementPositions(): List<PositionSample> = (0L..4L).map { index ->
        position(index, 0, index * 5_000L, longitude = index * 0.0001)
    }

    private fun distanceSeries(values: List<Double>): List<DistanceSample> = values.mapIndexed { index, value ->
        DistanceSample(
            activityId = ACTIVITY_ID,
            positionSampleIndex = SampleIndex(index.toLong()),
            activeElapsedTime = ActiveElapsedTimeMillis(index * 5_000L),
            cumulativeDistance = DistanceMetres(value),
        )
    }

    private fun position(
        index: Long,
        segment: Long,
        elapsedMillis: Long,
        latitude: Double = 0.0,
        longitude: Double,
    ): PositionSample = PositionSample(
        activityId = ACTIVITY_ID,
        sampleIndex = SampleIndex(index),
        routeSegmentIndex = RouteSegmentIndex(segment),
        timestamp = AbsoluteTimestampUtcMillis(BASE_TIME + elapsedMillis),
        activeElapsedTime = ActiveElapsedTimeMillis(elapsedMillis),
        latitude = LatitudeDegrees(latitude),
        longitude = LongitudeDegrees(longitude),
    )

    private fun step(index: Long, epoch: Long, elapsedMillis: Long, count: Long): StepSample = StepSample(
        activityId = ACTIVITY_ID,
        sampleIndex = SampleIndex(index),
        counterEpoch = StepCounterEpoch(epoch),
        timestamp = AbsoluteTimestampUtcMillis(BASE_TIME + elapsedMillis),
        activeElapsedTime = ActiveElapsedTimeMillis(elapsedMillis),
        cumulativeSteps = count,
    )

    private fun activity(type: ActivityType, activeDurationMillis: Long): Activity = Activity(
        id = ACTIVITY_ID,
        provenanceType = ProvenanceType.RADM_NATIVE,
        type = type,
        startedAt = AbsoluteTimestampUtcMillis(BASE_TIME),
        activeDuration = ActiveElapsedTimeMillis(activeDurationMillis),
        createdAt = AbsoluteTimestampUtcMillis(BASE_TIME),
        updatedAt = AbsoluteTimestampUtcMillis(BASE_TIME),
    )

    private companion object {
        const val BASE_TIME = 1_788_379_200_000L
        const val TOLERANCE = 1e-9
        val ACTIVITY_ID = ActivityId.parse("40000000-0000-4000-8000-000000000001")
    }
}
