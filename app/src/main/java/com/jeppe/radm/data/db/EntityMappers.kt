package com.jeppe.radm.data.db

import com.jeppe.radm.data.db.entity.ActivityEntity
import com.jeppe.radm.data.db.entity.ActivityLibraryItemEntity
import com.jeppe.radm.data.db.entity.ActivityProcessorStateEntity
import com.jeppe.radm.data.db.entity.ActivitySummaryEntity
import com.jeppe.radm.data.db.entity.DerivedCadenceEntity
import com.jeppe.radm.data.db.entity.DerivedTrackMetricEntity
import com.jeppe.radm.data.db.entity.PositionSampleEntity
import com.jeppe.radm.data.db.entity.ProcessorDefinitionEntity
import com.jeppe.radm.data.db.entity.RecordingEventEntity
import com.jeppe.radm.data.db.entity.RecordingSessionEntity
import com.jeppe.radm.data.db.entity.StepSampleEntity
import com.jeppe.radm.domain.model.AbsoluteTimestampUtcMillis
import com.jeppe.radm.domain.model.AccuracyMetres
import com.jeppe.radm.domain.model.ActiveElapsedTimeMillis
import com.jeppe.radm.domain.model.Activity
import com.jeppe.radm.domain.model.ActivityId
import com.jeppe.radm.domain.model.ActivityLibraryItem
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
import com.jeppe.radm.domain.model.ProcessorDefinition
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

fun Activity.toEntity() = ActivityEntity(
    activityId = id.value,
    provenanceType = provenanceType.value,
    provenanceExternalId = provenanceExternalId,
    activityType = type.name,
    title = title,
    notes = notes,
    startedAtUtcMs = startedAt.value,
    endedAtUtcMs = endedAt?.value,
    savedAtUtcMs = savedAt?.value,
    activeDurationMs = activeDuration.value,
    createdAtUtcMs = createdAt.value,
    updatedAtUtcMs = updatedAt.value,
)

fun ActivityEntity.toDomain() = Activity(
    id = ActivityId.parse(activityId),
    provenanceType = ProvenanceType(provenanceType),
    provenanceExternalId = provenanceExternalId,
    type = ActivityType.valueOf(activityType),
    title = title,
    notes = notes,
    startedAt = AbsoluteTimestampUtcMillis(startedAtUtcMs),
    endedAt = endedAtUtcMs?.let(::AbsoluteTimestampUtcMillis),
    savedAt = savedAtUtcMs?.let(::AbsoluteTimestampUtcMillis),
    activeDuration = ActiveElapsedTimeMillis(activeDurationMs),
    createdAt = AbsoluteTimestampUtcMillis(createdAtUtcMs),
    updatedAt = AbsoluteTimestampUtcMillis(updatedAtUtcMs),
)

fun ActivityLibraryItemEntity.toDomain() = ActivityLibraryItem(
    activityId = ActivityId.parse(activityId),
    activityType = ActivityType.valueOf(activityType),
    title = title,
    startedAt = AbsoluteTimestampUtcMillis(startedAtUtcMs),
    activeDuration = ActiveElapsedTimeMillis(activeDurationMs),
    distance = distanceM?.let(::DistanceMetres),
    averagePace = averagePaceSPerKm?.let(::PaceSecondsPerKilometre),
    averageSpeed = averageSpeedMps?.let(::SpeedMetresPerSecond),
)

fun ActivitySummary.toEntity() = ActivitySummaryEntity(
    activityId = activityId.value,
    distanceM = distance?.value,
    averagePaceSPerKm = averagePace?.value,
    averageSpeedMps = averageSpeed?.value,
    minElevationM = minimumElevation?.value,
    maxElevationM = maximumElevation?.value,
    totalAscentM = totalAscent?.value,
)

fun ActivitySummaryEntity.toDomain() = ActivitySummary(
    activityId = ActivityId.parse(activityId),
    distance = distanceM?.let(::DistanceMetres),
    averagePace = averagePaceSPerKm?.let(::PaceSecondsPerKilometre),
    averageSpeed = averageSpeedMps?.let(::SpeedMetresPerSecond),
    minimumElevation = minElevationM?.let(::ElevationMetres),
    maximumElevation = maxElevationM?.let(::ElevationMetres),
    totalAscent = totalAscentM?.let(::DistanceMetres),
)

fun RecordingSession.toEntity() = RecordingSessionEntity(
    activityId = activityId.value,
    state = state.name,
    activeElapsedMs = activeElapsedTime.value,
    stateEnteredAtUtcMs = stateEnteredAt.value,
    lastCheckpointAtUtcMs = lastCheckpointAt.value,
    routeSegmentIndex = routeSegmentIndex.value,
    positionSampleCount = positionSampleCount,
    stepSampleCount = stepSampleCount,
)

fun RecordingSessionEntity.toDomain() = RecordingSession(
    activityId = ActivityId.parse(activityId),
    state = RecordingState.valueOf(state),
    activeElapsedTime = ActiveElapsedTimeMillis(activeElapsedMs),
    stateEnteredAt = AbsoluteTimestampUtcMillis(stateEnteredAtUtcMs),
    lastCheckpointAt = AbsoluteTimestampUtcMillis(lastCheckpointAtUtcMs),
    routeSegmentIndex = RouteSegmentIndex(routeSegmentIndex),
    positionSampleCount = positionSampleCount,
    stepSampleCount = stepSampleCount,
)

fun RecordingEvent.toEntity() = RecordingEventEntity(
    activityId = activityId.value,
    eventIndex = eventIndex.value,
    eventType = type.name,
    occurredAtUtcMs = occurredAt.value,
    activeElapsedMs = activeElapsedTime.value,
)

fun RecordingEventEntity.toDomain() = RecordingEvent(
    activityId = ActivityId.parse(activityId),
    eventIndex = EventIndex(eventIndex),
    type = RecordingEventType.valueOf(eventType),
    occurredAt = AbsoluteTimestampUtcMillis(occurredAtUtcMs),
    activeElapsedTime = ActiveElapsedTimeMillis(activeElapsedMs),
)

fun PositionSample.toEntity() = PositionSampleEntity(
    activityId = activityId.value,
    sampleIndex = sampleIndex.value,
    routeSegmentIndex = routeSegmentIndex.value,
    timestampUtcMs = timestamp.value,
    elapsedMs = activeElapsedTime.value,
    latitudeDeg = latitude.value,
    longitudeDeg = longitude.value,
    elevationM = elevation?.value,
    horizontalAccuracyM = horizontalAccuracy?.value,
    verticalAccuracyM = verticalAccuracy?.value,
)

fun PositionSampleEntity.toDomain() = PositionSample(
    activityId = ActivityId.parse(activityId),
    sampleIndex = SampleIndex(sampleIndex),
    routeSegmentIndex = RouteSegmentIndex(routeSegmentIndex),
    timestamp = AbsoluteTimestampUtcMillis(timestampUtcMs),
    activeElapsedTime = ActiveElapsedTimeMillis(elapsedMs),
    latitude = LatitudeDegrees(latitudeDeg),
    longitude = LongitudeDegrees(longitudeDeg),
    elevation = elevationM?.let(::ElevationMetres),
    horizontalAccuracy = horizontalAccuracyM?.let(::AccuracyMetres),
    verticalAccuracy = verticalAccuracyM?.let(::AccuracyMetres),
)

fun StepSample.toEntity() = StepSampleEntity(
    activityId = activityId.value,
    sampleIndex = sampleIndex.value,
    counterEpoch = counterEpoch.value,
    timestampUtcMs = timestamp.value,
    elapsedMs = activeElapsedTime.value,
    cumulativeSteps = cumulativeSteps,
)

fun StepSampleEntity.toDomain() = StepSample(
    activityId = ActivityId.parse(activityId),
    sampleIndex = SampleIndex(sampleIndex),
    counterEpoch = StepCounterEpoch(counterEpoch),
    timestamp = AbsoluteTimestampUtcMillis(timestampUtcMs),
    activeElapsedTime = ActiveElapsedTimeMillis(elapsedMs),
    cumulativeSteps = cumulativeSteps,
)

fun DerivedTrackMetric.toEntity() = DerivedTrackMetricEntity(
    activityId = activityId.value,
    sampleIndex = positionSampleIndex.value,
    cumulativeDistanceM = cumulativeDistance.value,
    paceSPerKm = pace?.value,
    speedMps = speed?.value,
)

fun DerivedTrackMetricEntity.toDomain() = DerivedTrackMetric(
    activityId = ActivityId.parse(activityId),
    positionSampleIndex = SampleIndex(sampleIndex),
    cumulativeDistance = DistanceMetres(cumulativeDistanceM),
    pace = paceSPerKm?.let(::PaceSecondsPerKilometre),
    speed = speedMps?.let(::SpeedMetresPerSecond),
)

fun CadenceSample.toEntity() = DerivedCadenceEntity(
    activityId = activityId.value,
    sampleIndex = sampleIndex.value,
    elapsedMs = activeElapsedTime.value,
    cadenceSpm = cadence?.value,
)

fun DerivedCadenceEntity.toDomain() = CadenceSample(
    activityId = ActivityId.parse(activityId),
    sampleIndex = SampleIndex(sampleIndex),
    activeElapsedTime = ActiveElapsedTimeMillis(elapsedMs),
    cadence = cadenceSpm?.let(::CadenceStepsPerMinute),
)

fun ProcessorDefinition.toEntity() = ProcessorDefinitionEntity(
    processorName = name.value,
    currentVersion = currentVersion,
)

fun ProcessorDefinitionEntity.toDomain() = ProcessorDefinition(
    name = ProcessorName(processorName),
    currentVersion = currentVersion,
)

fun ActivityProcessorState.toEntity() = ActivityProcessorStateEntity(
    activityId = activityId.value,
    processorName = processorName.value,
    processorVersion = processorVersion,
    status = status.name,
    processedAtUtcMs = processedAt?.value,
)

fun ActivityProcessorStateEntity.toDomain() = ActivityProcessorState(
    activityId = ActivityId.parse(activityId),
    processorName = ProcessorName(processorName),
    processorVersion = processorVersion,
    status = ProcessorStatus.valueOf(status),
    processedAt = processedAtUtcMs?.let(::AbsoluteTimestampUtcMillis),
)
