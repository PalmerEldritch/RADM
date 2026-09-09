package com.jeppe.radm.application.analysis

import com.jeppe.radm.data.repository.ActivityRepository
import com.jeppe.radm.domain.analysis.ActivityAnalysisData
import com.jeppe.radm.domain.analysis.ActivityAnalysisState
import com.jeppe.radm.domain.analysis.AnalysisCoordinateMode
import com.jeppe.radm.domain.analysis.AnalysisInspectorValues
import com.jeppe.radm.domain.analysis.AnalysisPoint
import com.jeppe.radm.domain.analysis.AnalysisPosition
import com.jeppe.radm.domain.analysis.AnalysisProcessorStatus
import com.jeppe.radm.domain.analysis.AnalysisProcessorValidity
import com.jeppe.radm.domain.analysis.AnalysisRange
import com.jeppe.radm.domain.analysis.AnalysisSeries
import com.jeppe.radm.domain.analysis.AnalysisSeriesAvailability
import com.jeppe.radm.domain.model.ActiveElapsedTimeMillis
import com.jeppe.radm.domain.model.ActivityProcessorState
import com.jeppe.radm.domain.model.ActivityType
import com.jeppe.radm.domain.model.DistanceMetres
import com.jeppe.radm.domain.model.PositionSample
import com.jeppe.radm.domain.model.ProcessorDefinition
import com.jeppe.radm.domain.model.ProcessorName
import com.jeppe.radm.domain.model.ProcessorStatus
import com.jeppe.radm.domain.model.RouteSegment

class LoadActivityAnalysis(
    private val repository: ActivityRepository,
) {
    suspend operator fun invoke(activityId: com.jeppe.radm.domain.model.ActivityId): ActivityAnalysisData {
        val activity = requireNotNull(repository.get(activityId)) { "Activity does not exist: $activityId" }
        require(activity.savedAt != null) { "Activity is not saved" }

        val positions = repository.getPositions(activityId)
        val steps = repository.getSteps(activityId)
        val trackMetrics = repository.getTrackMetrics(activityId)
        val cadenceSamples = repository.getCadence(activityId)
        val states = repository.getProcessorStates(activityId).associateBy { it.processorName }
        val definitions = PROCESSORS.associateWith { repository.getProcessorDefinition(it) }
        val validity = PROCESSORS.associateWith { processor ->
            processorValidity(
                processor = processor,
                applicable = processor.isApplicableTo(activity.type),
                state = states[processor],
                definition = definitions[processor],
            )
        }
        val isCurrent: (ProcessorName) -> Boolean = {
            validity.getValue(it).status == AnalysisProcessorStatus.CURRENT
        }

        val positionByIndex = positions.associateBy { it.sampleIndex }
        val distanceBySampleIndex = if (isCurrent(ProcessorName.DISTANCE)) {
            trackMetrics.associate { it.positionSampleIndex to it.cumulativeDistance }
        } else {
            emptyMap()
        }
        val distancePoints = if (isCurrent(ProcessorName.DISTANCE)) {
            trackMetrics.mapNotNull { metric ->
                positionByIndex[metric.positionSampleIndex]?.let { position ->
                    AnalysisPoint(
                        position = AnalysisPosition(position.activeElapsedTime, metric.cumulativeDistance),
                        value = metric.cumulativeDistance,
                        continuityGroup = position.routeSegmentIndex.value,
                    )
                }
            }
        } else {
            emptyList()
        }
        val distance = series(
            applicable = true,
            current = isCurrent(ProcessorName.DISTANCE),
            points = distancePoints,
        )

        val paceApplicable = activity.type != ActivityType.CYCLING
        val pace = series(
            applicable = paceApplicable,
            current = isCurrent(ProcessorName.PACE),
            points = if (paceApplicable && isCurrent(ProcessorName.PACE)) {
                trackMetrics.mapNotNull { metric ->
                    positionByIndex[metric.positionSampleIndex]?.let { position ->
                        AnalysisPoint(
                            position = AnalysisPosition(
                                position.activeElapsedTime,
                                distanceBySampleIndex[metric.positionSampleIndex],
                            ),
                            value = metric.pace,
                            continuityGroup = position.routeSegmentIndex.value,
                        )
                    }
                }
            } else {
                emptyList()
            },
        )

        val speedApplicable = activity.type == ActivityType.CYCLING
        val speed = series(
            applicable = speedApplicable,
            current = isCurrent(ProcessorName.SPEED),
            points = if (speedApplicable && isCurrent(ProcessorName.SPEED)) {
                trackMetrics.mapNotNull { metric ->
                    positionByIndex[metric.positionSampleIndex]?.let { position ->
                        AnalysisPoint(
                            position = AnalysisPosition(
                                position.activeElapsedTime,
                                distanceBySampleIndex[metric.positionSampleIndex],
                            ),
                            value = metric.speed,
                            continuityGroup = position.routeSegmentIndex.value,
                        )
                    }
                }
            } else {
                emptyList()
            },
        )

        val elevation = series(
            applicable = true,
            current = true,
            points = positions.map { position ->
                AnalysisPoint(
                    position = AnalysisPosition(
                        position.activeElapsedTime,
                        distanceBySampleIndex[position.sampleIndex],
                    ),
                    value = position.elevation,
                    continuityGroup = position.routeSegmentIndex.value,
                )
            },
        )

        val stepEpochByIndex = steps.associate { it.sampleIndex to it.counterEpoch.value }
        val cadenceApplicable = activity.type == ActivityType.RUNNING
        val cadence = series(
            applicable = cadenceApplicable,
            current = isCurrent(ProcessorName.CADENCE),
            points = if (cadenceApplicable && isCurrent(ProcessorName.CADENCE)) {
                cadenceSamples.map { sample ->
                    AnalysisPoint(
                        position = AnalysisPosition(
                            sample.activeElapsedTime,
                            interpolateDistance(distancePoints, sample.activeElapsedTime),
                        ),
                        value = sample.cadence,
                        continuityGroup = stepEpochByIndex[sample.sampleIndex] ?: 0L,
                    )
                }
            } else {
                emptyList()
            },
        )

        val routeSegments = positions.toRouteSegments()
        val endDistance = distancePoints.lastOrNull()?.value
        val start = AnalysisPosition(ActiveElapsedTimeMillis.ZERO, distanceAtStart(distancePoints))
        val end = AnalysisPosition(activity.activeDuration, endDistance)
        val initialState = ActivityAnalysisState(
            selectedPosition = start,
            range = AnalysisRange(start, end),
            coordinateMode = if (distance.availability == AnalysisSeriesAvailability.AVAILABLE) {
                AnalysisCoordinateMode.DISTANCE
            } else {
                AnalysisCoordinateMode.ACTIVE_ELAPSED_TIME
            },
        )

        return ActivityAnalysisData(
            activity = activity,
            summary = repository.getSummary(activityId).takeIf { isCurrent(ProcessorName.SUMMARY) },
            routeSegments = routeSegments,
            distance = distance,
            pace = pace,
            speed = speed,
            elevation = elevation,
            cadence = cadence,
            processorValidity = validity,
            initialState = initialState,
            initialInspector = AnalysisInspectorValues(
                position = start,
                pace = pace.valueAtOrBefore(start.activeElapsedTime),
                speed = speed.valueAtOrBefore(start.activeElapsedTime),
                elevation = elevation.valueAtOrBefore(start.activeElapsedTime),
                cadence = cadence.valueAtOrBefore(start.activeElapsedTime),
            ),
        )
    }
}

private val PROCESSORS = listOf(
    ProcessorName.DISTANCE,
    ProcessorName.PACE,
    ProcessorName.SPEED,
    ProcessorName.CADENCE,
    ProcessorName.SUMMARY,
)

private fun ProcessorName.isApplicableTo(type: ActivityType): Boolean = when (this) {
    ProcessorName.PACE -> type != ActivityType.CYCLING
    ProcessorName.SPEED -> type == ActivityType.CYCLING
    ProcessorName.CADENCE -> type == ActivityType.RUNNING
    else -> true
}

private fun processorValidity(
    processor: ProcessorName,
    applicable: Boolean,
    state: ActivityProcessorState?,
    definition: ProcessorDefinition?,
): AnalysisProcessorValidity {
    val status = when {
        !applicable -> AnalysisProcessorStatus.NOT_APPLICABLE
        state == null -> AnalysisProcessorStatus.UNPROCESSED
        state.status == ProcessorStatus.FAILED -> AnalysisProcessorStatus.FAILED
        state.status == ProcessorStatus.UNPROCESSED -> AnalysisProcessorStatus.UNPROCESSED
        definition == null || !state.isCurrentAgainst(definition) -> AnalysisProcessorStatus.STALE
        else -> AnalysisProcessorStatus.CURRENT
    }
    return AnalysisProcessorValidity(
        processorName = processor,
        status = status,
        storedVersion = state?.processorVersion,
        currentVersion = definition?.currentVersion,
    )
}

private fun <T> series(
    applicable: Boolean,
    current: Boolean,
    points: List<AnalysisPoint<T>>,
): AnalysisSeries<T> {
    val availability = when {
        !applicable -> AnalysisSeriesAvailability.UNAVAILABLE_NOT_APPLICABLE
        !current -> AnalysisSeriesAvailability.UNAVAILABLE_NOT_CURRENT
        points.any { it.value != null } -> AnalysisSeriesAvailability.AVAILABLE
        else -> AnalysisSeriesAvailability.UNAVAILABLE_NO_SOURCE
    }
    return AnalysisSeries(
        availability = availability,
        points = points.takeIf { availability == AnalysisSeriesAvailability.AVAILABLE }.orEmpty(),
    )
}

private fun List<PositionSample>.toRouteSegments(): List<RouteSegment> =
    groupBy { it.routeSegmentIndex }.map { (index, samples) ->
        RouteSegment(checkNotNull(samples.firstOrNull()).activityId, index, samples)
    }

private fun distanceAtStart(points: List<AnalysisPoint<DistanceMetres>>): DistanceMetres? =
    points.firstOrNull()?.takeIf {
        it.position.activeElapsedTime == ActiveElapsedTimeMillis.ZERO
    }?.value

private fun interpolateDistance(
    points: List<AnalysisPoint<DistanceMetres>>,
    elapsedTime: ActiveElapsedTimeMillis,
): DistanceMetres? {
    if (points.isEmpty()) return null
    val exact = points.binarySearchBy(elapsedTime.value) { it.position.activeElapsedTime.value }
    if (exact >= 0) return points[exact].value
    val insertion = -exact - 1
    if (insertion == 0 || insertion == points.size) return null
    val left = points[insertion - 1]
    val right = points[insertion]
    if (left.continuityGroup != right.continuityGroup) return null
    val leftDistance = left.value ?: return null
    val rightDistance = right.value ?: return null
    val duration = right.position.activeElapsedTime.value - left.position.activeElapsedTime.value
    if (duration <= 0L) return leftDistance
    val fraction = (elapsedTime.value - left.position.activeElapsedTime.value).toDouble() / duration
    return DistanceMetres(leftDistance.value + (rightDistance.value - leftDistance.value) * fraction)
}

private fun <T> AnalysisSeries<T>.valueAtOrBefore(time: ActiveElapsedTimeMillis): T? =
    points.asSequence()
        .takeWhile { it.position.activeElapsedTime <= time }
        .mapNotNull { it.value }
        .lastOrNull()
