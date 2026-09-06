package com.jeppe.radm.domain.fixtures

import com.jeppe.radm.domain.model.AbsoluteTimestampUtcMillis
import com.jeppe.radm.domain.model.AccuracyMetres
import com.jeppe.radm.domain.model.ActiveElapsedTimeMillis
import com.jeppe.radm.domain.model.Activity
import com.jeppe.radm.domain.model.ActivityId
import com.jeppe.radm.domain.model.ActivitySummary
import com.jeppe.radm.domain.model.ActivityType
import com.jeppe.radm.domain.model.DistanceMetres
import com.jeppe.radm.domain.model.ElevationMetres
import com.jeppe.radm.domain.model.EventIndex
import com.jeppe.radm.domain.model.LatitudeDegrees
import com.jeppe.radm.domain.model.LongitudeDegrees
import com.jeppe.radm.domain.model.PositionSample
import com.jeppe.radm.domain.model.ProvenanceType
import com.jeppe.radm.domain.model.RecordingEvent
import com.jeppe.radm.domain.model.RecordingEventType
import com.jeppe.radm.domain.model.RouteSegmentIndex
import com.jeppe.radm.domain.model.SampleIndex
import com.jeppe.radm.domain.model.StepCounterEpoch
import com.jeppe.radm.domain.model.StepSample

/** Fixed source fixtures shared by M1 JVM verification and later milestone tests. */
object DeterministicFixtures {
    val runningActivityId = ActivityId.parse("10000000-0000-4000-8000-000000000001")
    val cyclingActivityId = ActivityId.parse("10000000-0000-4000-8000-000000000002")
    val skiingActivityId = ActivityId.parse("10000000-0000-4000-8000-000000000003")
    val baseTimestamp = AbsoluteTimestampUtcMillis(1_788_379_200_000L)

    val runningActivity = activity(runningActivityId, ActivityType.RUNNING)
    val cyclingActivity = activity(cyclingActivityId, ActivityType.CYCLING)
    val skiingActivity = activity(skiingActivityId, ActivityType.CROSS_COUNTRY_SKIING)

    val continuousRoute: List<PositionSample> = listOf(
        position(0, 0, 0, 59.32930, 18.06860, 22.0),
        position(1, 0, 1_000, 59.32935, 18.06869, 22.2),
        position(2, 0, 2_000, 59.32940, 18.06878, 22.4),
        position(3, 0, 3_000, 59.32945, 18.06887, 22.5),
        position(4, 0, 4_000, 59.32950, 18.06896, 22.7),
    )

    val irregularTimestampRoute: List<PositionSample> = listOf(
        position(0, 0, 0, 59.32930, 18.06860, 22.0),
        position(1, 0, 750, 59.32934, 18.06867, 22.1),
        position(2, 0, 2_300, 59.32942, 18.06880, 22.4),
        position(3, 0, 7_000, 59.32960, 18.06910, 22.9),
    )

    val initialTwentySecondsWithoutLocation: List<PositionSample> = listOf(
        position(0, 0, 20_000, 59.32930, 18.06860, 22.0),
        position(1, 0, 21_000, 59.32935, 18.06869, 22.2),
    )

    val routeGap: List<PositionSample> = listOf(
        position(0, 0, 0, 59.32930, 18.06860, 22.0),
        position(1, 0, 1_000, 59.32935, 18.06869, 22.2),
        position(2, 1, 21_000, 59.33100, 18.07100, 24.0),
        position(3, 1, 22_000, 59.33105, 18.07109, 24.2),
    )

    val missingElevationRoute: List<PositionSample> = listOf(
        position(0, 0, 0, 59.32930, 18.06860, elevationMetres = null),
        position(1, 0, 1_000, 59.32935, 18.06869, elevationMetres = null),
    )

    val noRoute: List<PositionSample> = emptyList()

    val runningSteps: List<StepSample> = listOf(
        step(0, 0, 0, 28_451),
        step(1, 0, 9_000, 28_468),
        step(2, 0, 18_000, 28_484),
    )

    val runningWithoutSteps: List<StepSample> = emptyList()

    val stepCounterReset: List<StepSample> = listOf(
        step(0, 0, 0, 28_451),
        step(1, 0, 9_000, 28_468),
        step(2, 1, 18_000, 4),
        step(3, 1, 27_000, 20),
    )

    val manualPauseEvents: List<RecordingEvent> = listOf(
        event(0, RecordingEventType.START, wallOffsetMillis = 0, activeMillis = 0),
        event(1, RecordingEventType.PAUSE, wallOffsetMillis = 1_200_000, activeMillis = 1_200_000),
        event(2, RecordingEventType.RESUME, wallOffsetMillis = 1_500_000, activeMillis = 1_200_000),
        event(3, RecordingEventType.FINISH, wallOffsetMillis = 2_700_000, activeMillis = 2_400_000),
    )

    val recoveryBoundaryEvents: List<RecordingEvent> = listOf(
        event(0, RecordingEventType.START, wallOffsetMillis = 0, activeMillis = 0),
        event(1, RecordingEventType.RECOVERY_RESUME, wallOffsetMillis = 900_000, activeMillis = 300_000),
        event(2, RecordingEventType.FINISH, wallOffsetMillis = 1_200_000, activeMillis = 600_000),
    )

    val grossJumpCandidates: List<LocationCandidateFixture> = listOf(
        LocationCandidateFixture(
            position(0, 0, 0, 59.32930, 18.06860, 22.0),
            expectedAccepted = true,
        ),
        LocationCandidateFixture(
            position(1, 0, 1_000, 59.42930, 18.16860, 22.0),
            expectedAccepted = false,
        ),
    )

    val poorQualityCandidates: List<LocationCandidateFixture> = listOf(
        LocationCandidateFixture(
            position(0, 0, 0, 59.32930, 18.06860, 22.0, horizontalAccuracyMetres = 4.0),
            expectedAccepted = true,
        ),
        LocationCandidateFixture(
            position(1, 0, 5_000, 59.32932, 18.06864, 22.1, horizontalAccuracyMetres = 250.0),
            expectedAccepted = false,
        ),
        LocationCandidateFixture(
            position(2, 0, 10_000, 59.32935, 18.06869, 22.2, horizontalAccuracyMetres = 250.0),
            expectedAccepted = false,
        ),
        LocationCandidateFixture(
            position(3, 0, 15_000, 59.32937, 18.06873, 22.3, horizontalAccuracyMetres = 250.0),
            expectedAccepted = false,
        ),
        LocationCandidateFixture(
            position(4, 0, 20_000, 59.32940, 18.06878, 22.4, horizontalAccuracyMetres = 4.0),
            expectedAccepted = true,
        ),
    )

    /** Deterministic generator; avoids committing a large generated artifact. */
    fun largeActivityPositions(count: Int = 100_000): Sequence<PositionSample> {
        require(count >= 0)
        return (0 until count).asSequence().map { index ->
            position(
                sampleIndex = index.toLong(),
                segmentIndex = 0,
                elapsedMillis = index * 1_000L,
                latitude = 59.0 + (index % 1_000) * 0.000001,
                longitude = 18.0 + (index % 1_000) * 0.000001,
                elevationMetres = 20.0 + (index % 20) * 0.1,
            )
        }
    }

    /** Deterministic summary-only generator for the 10,000-activity library fixture. */
    fun largeLibrarySummaries(count: Int = 10_000): Sequence<ActivitySummary> {
        require(count >= 0)
        return (0 until count).asSequence().map { index ->
            ActivitySummary(
                activityId = ActivityId.parse("20000000-0000-4000-8000-${index.toString(16).padStart(12, '0')}"),
                distance = DistanceMetres(5_000.0 + index),
                averagePace = null,
                averageSpeed = null,
                minimumElevation = null,
                maximumElevation = null,
                totalAscent = null,
            )
        }
    }

    private fun activity(id: ActivityId, type: ActivityType): Activity = Activity(
        id = id,
        provenanceType = ProvenanceType.RADM_NATIVE,
        type = type,
        startedAt = baseTimestamp,
        createdAt = baseTimestamp,
        updatedAt = baseTimestamp,
    )

    private fun position(
        sampleIndex: Long,
        segmentIndex: Long,
        elapsedMillis: Long,
        latitude: Double,
        longitude: Double,
        elevationMetres: Double?,
        horizontalAccuracyMetres: Double = 4.0,
    ): PositionSample = PositionSample(
        activityId = runningActivityId,
        sampleIndex = SampleIndex(sampleIndex),
        routeSegmentIndex = RouteSegmentIndex(segmentIndex),
        timestamp = AbsoluteTimestampUtcMillis(baseTimestamp.value + elapsedMillis),
        activeElapsedTime = ActiveElapsedTimeMillis(elapsedMillis),
        latitude = LatitudeDegrees(latitude),
        longitude = LongitudeDegrees(longitude),
        elevation = elevationMetres?.let(::ElevationMetres),
        horizontalAccuracy = AccuracyMetres(horizontalAccuracyMetres),
        verticalAccuracy = null,
    )

    private fun step(
        sampleIndex: Long,
        epoch: Long,
        elapsedMillis: Long,
        cumulativeSteps: Long,
    ): StepSample = StepSample(
        activityId = runningActivityId,
        sampleIndex = SampleIndex(sampleIndex),
        counterEpoch = StepCounterEpoch(epoch),
        timestamp = AbsoluteTimestampUtcMillis(baseTimestamp.value + elapsedMillis),
        activeElapsedTime = ActiveElapsedTimeMillis(elapsedMillis),
        cumulativeSteps = cumulativeSteps,
    )

    private fun event(
        index: Long,
        type: RecordingEventType,
        wallOffsetMillis: Long,
        activeMillis: Long,
    ): RecordingEvent = RecordingEvent(
        activityId = runningActivityId,
        eventIndex = EventIndex(index),
        type = type,
        occurredAt = AbsoluteTimestampUtcMillis(baseTimestamp.value + wallOffsetMillis),
        activeElapsedTime = ActiveElapsedTimeMillis(activeMillis),
    )
}

data class LocationCandidateFixture(
    val sample: PositionSample,
    val expectedAccepted: Boolean,
)
