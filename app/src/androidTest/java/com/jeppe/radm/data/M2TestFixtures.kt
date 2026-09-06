package com.jeppe.radm.data

import com.jeppe.radm.domain.model.AbsoluteTimestampUtcMillis
import com.jeppe.radm.domain.model.AccuracyMetres
import com.jeppe.radm.domain.model.ActiveElapsedTimeMillis
import com.jeppe.radm.domain.model.Activity
import com.jeppe.radm.domain.model.ActivityId
import com.jeppe.radm.domain.model.ActivityProcessorState
import com.jeppe.radm.domain.model.ActivitySummary
import com.jeppe.radm.domain.model.ActivityType
import com.jeppe.radm.domain.model.CadenceSample
import com.jeppe.radm.domain.model.CadenceStepsPerMinute
import com.jeppe.radm.domain.model.DerivedTrackMetric
import com.jeppe.radm.domain.model.DistanceMetres
import com.jeppe.radm.domain.model.ElevationMetres
import com.jeppe.radm.domain.model.EventIndex
import com.jeppe.radm.domain.model.LatitudeDegrees
import com.jeppe.radm.domain.model.LongitudeDegrees
import com.jeppe.radm.domain.model.PaceSecondsPerKilometre
import com.jeppe.radm.domain.model.PositionSample
import com.jeppe.radm.domain.model.ProcessorName
import com.jeppe.radm.domain.model.ProcessorStatus
import com.jeppe.radm.domain.model.ProvenanceType
import com.jeppe.radm.domain.model.RecordingEvent
import com.jeppe.radm.domain.model.RecordingEventType
import com.jeppe.radm.domain.model.RouteSegmentIndex
import com.jeppe.radm.domain.model.SampleIndex
import com.jeppe.radm.domain.model.SpeedMetresPerSecond
import com.jeppe.radm.domain.model.StepCounterEpoch
import com.jeppe.radm.domain.model.StepSample
import com.jeppe.radm.domain.recording.RecordingSession
import com.jeppe.radm.domain.recording.RecordingState

object M2TestFixtures {
    val baseTime = AbsoluteTimestampUtcMillis(1_788_379_200_000L)

    fun id(suffix: Int): ActivityId =
        ActivityId.parse("30000000-0000-4000-8000-${suffix.toString().padStart(12, '0')}")

    fun activity(suffix: Int, saved: Boolean = true): Activity = Activity(
        id = id(suffix),
        provenanceType = ProvenanceType.RADM_NATIVE,
        type = ActivityType.RUNNING,
        title = "Activity $suffix",
        notes = "M2 fixture",
        startedAt = AbsoluteTimestampUtcMillis(baseTime.value + suffix * 10_000L),
        endedAt = if (saved) AbsoluteTimestampUtcMillis(baseTime.value + suffix * 10_000L + 60_000L) else null,
        savedAt = if (saved) AbsoluteTimestampUtcMillis(baseTime.value + suffix * 10_000L + 61_000L) else null,
        activeDuration = ActiveElapsedTimeMillis(60_000L),
        createdAt = baseTime,
        updatedAt = baseTime,
    )

    fun session(activity: Activity): RecordingSession = RecordingSession(
        activityId = activity.id,
        state = RecordingState.RECORDING,
        activeElapsedTime = ActiveElapsedTimeMillis.ZERO,
        stateEnteredAt = baseTime,
        lastCheckpointAt = baseTime,
        routeSegmentIndex = RouteSegmentIndex(0),
        positionSampleCount = 0,
        stepSampleCount = 0,
    )

    fun events(activity: Activity): List<RecordingEvent> = listOf(
        RecordingEvent(
            activityId = activity.id,
            eventIndex = EventIndex(0),
            type = RecordingEventType.START,
            occurredAt = activity.startedAt,
            activeElapsedTime = ActiveElapsedTimeMillis.ZERO,
        ),
    )

    fun positions(activity: Activity): List<PositionSample> = listOf(
        PositionSample(
            activityId = activity.id,
            sampleIndex = SampleIndex(0),
            routeSegmentIndex = RouteSegmentIndex(0),
            timestamp = activity.startedAt,
            activeElapsedTime = ActiveElapsedTimeMillis.ZERO,
            latitude = LatitudeDegrees(59.32930),
            longitude = LongitudeDegrees(18.06860),
            elevation = ElevationMetres(22.0),
            horizontalAccuracy = AccuracyMetres(4.0),
        ),
        PositionSample(
            activityId = activity.id,
            sampleIndex = SampleIndex(1),
            routeSegmentIndex = RouteSegmentIndex(1),
            timestamp = AbsoluteTimestampUtcMillis(activity.startedAt.value + 20_000L),
            activeElapsedTime = ActiveElapsedTimeMillis(20_000L),
            latitude = LatitudeDegrees(59.33100),
            longitude = LongitudeDegrees(18.07100),
            elevation = null,
            horizontalAccuracy = AccuracyMetres(5.0),
        ),
    )

    fun steps(activity: Activity): List<StepSample> = listOf(
        StepSample(
            activityId = activity.id,
            sampleIndex = SampleIndex(0),
            counterEpoch = StepCounterEpoch(0),
            timestamp = activity.startedAt,
            activeElapsedTime = ActiveElapsedTimeMillis.ZERO,
            cumulativeSteps = 28_451,
        ),
        StepSample(
            activityId = activity.id,
            sampleIndex = SampleIndex(1),
            counterEpoch = StepCounterEpoch(1),
            timestamp = AbsoluteTimestampUtcMillis(activity.startedAt.value + 20_000L),
            activeElapsedTime = ActiveElapsedTimeMillis(20_000L),
            cumulativeSteps = 4,
        ),
    )

    fun trackMetrics(activity: Activity): List<DerivedTrackMetric> = listOf(
        DerivedTrackMetric(
            activityId = activity.id,
            positionSampleIndex = SampleIndex(0),
            cumulativeDistance = DistanceMetres.ZERO,
            pace = null,
            speed = null,
        ),
        DerivedTrackMetric(
            activityId = activity.id,
            positionSampleIndex = SampleIndex(1),
            cumulativeDistance = DistanceMetres(200.0),
            pace = PaceSecondsPerKilometre(360.0),
            speed = SpeedMetresPerSecond(2.78),
        ),
    )

    fun cadence(activity: Activity): List<CadenceSample> = listOf(
        CadenceSample(
            activityId = activity.id,
            sampleIndex = SampleIndex(0),
            activeElapsedTime = ActiveElapsedTimeMillis(20_000L),
            cadence = CadenceStepsPerMinute(160.0),
        ),
    )

    fun summary(activity: Activity): ActivitySummary = ActivitySummary(
        activityId = activity.id,
        distance = DistanceMetres(200.0),
        averagePace = PaceSecondsPerKilometre(360.0),
        averageSpeed = null,
        minimumElevation = ElevationMetres(22.0),
        maximumElevation = ElevationMetres(24.0),
        totalAscent = DistanceMetres(2.0),
    )

    fun processorState(
        activity: Activity,
        name: ProcessorName,
        version: Int = 1,
    ): ActivityProcessorState = ActivityProcessorState(
        activityId = activity.id,
        processorName = name,
        processorVersion = version,
        status = ProcessorStatus.CURRENT,
        processedAt = baseTime,
    )
}
