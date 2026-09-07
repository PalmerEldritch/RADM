package com.jeppe.radm.domain.processing

import com.jeppe.radm.domain.location.GeoDistance
import com.jeppe.radm.domain.model.Activity
import com.jeppe.radm.domain.model.ActivityId
import com.jeppe.radm.domain.model.ActivitySummary
import com.jeppe.radm.domain.model.ActivityType
import com.jeppe.radm.domain.model.CadenceSample
import com.jeppe.radm.domain.model.CadenceStepsPerMinute
import com.jeppe.radm.domain.model.DerivedTrackMetric
import com.jeppe.radm.domain.model.DistanceMetres
import com.jeppe.radm.domain.model.DistanceSample
import com.jeppe.radm.domain.model.ElevationMetres
import com.jeppe.radm.domain.model.PaceSecondsPerKilometre
import com.jeppe.radm.domain.model.PositionSample
import com.jeppe.radm.domain.model.SpeedMetresPerSecond
import com.jeppe.radm.domain.model.StepSample

object R00FinalProcessingDefaults {
    const val MOVEMENT_WINDOW_MILLIS = 10_000L
    const val CADENCE_WINDOW_MILLIS = 10_000L
}

fun interface DistanceProcessor {
    fun process(positions: List<PositionSample>): List<DistanceSample>
}

fun interface MovementProcessor {
    fun process(
        activityType: ActivityType,
        positions: List<PositionSample>,
        distance: List<DistanceSample>,
    ): List<DerivedTrackMetric>
}

fun interface CadenceProcessor {
    fun process(activityType: ActivityType, steps: List<StepSample>): List<CadenceSample>
}

fun interface SummaryProcessor {
    fun process(
        activity: Activity,
        positions: List<PositionSample>,
        trackMetrics: List<DerivedTrackMetric>,
    ): ActivitySummary
}

/** REC 62-63: deterministic final distance from retained accepted positions. */
class R00DistanceProcessor : DistanceProcessor {
    override fun process(positions: List<PositionSample>): List<DistanceSample> {
        validateOrderedPositions(positions)
        var cumulativeMetres = 0.0
        return positions.mapIndexed { index, position ->
            val previous = positions.getOrNull(index - 1)
            if (previous != null && previous.routeSegmentIndex == position.routeSegmentIndex) {
                cumulativeMetres += GeoDistance.haversine(
                    previous.latitude,
                    previous.longitude,
                    position.latitude,
                    position.longitude,
                ).value
            }
            DistanceSample(
                activityId = position.activityId,
                positionSampleIndex = position.sampleIndex,
                activeElapsedTime = position.activeElapsedTime,
                cumulativeDistance = DistanceMetres(cumulativeMetres),
            )
        }
    }
}

/** SRS PACE/SPEED and REC 64-65: position-aligned centred movement windows. */
class R00MovementProcessor(
    private val windowMillis: Long = R00FinalProcessingDefaults.MOVEMENT_WINDOW_MILLIS,
) : MovementProcessor {
    init {
        require(windowMillis > 0L) { "Movement window must be positive" }
    }

    override fun process(
        activityType: ActivityType,
        positions: List<PositionSample>,
        distance: List<DistanceSample>,
    ): List<DerivedTrackMetric> {
        validateOrderedPositions(positions)
        require(distance.size == positions.size) { "Distance and position series must align" }
        require(distance.indices.all { index ->
            val position = positions[index]
            val sample = distance[index]
            sample.activityId == position.activityId &&
                sample.positionSampleIndex == position.sampleIndex &&
                sample.activeElapsedTime == position.activeElapsedTime
        }) { "Distance and position series must have identical sample coordinates" }

        val segmentRanges = contiguousSegmentRanges(positions)
        return positions.mapIndexed { index, position ->
            val segment = segmentRanges.getValue(position.routeSegmentIndex.value to index)
            val rate = movementRateAt(index, segment, distance)
            DerivedTrackMetric(
                activityId = position.activityId,
                positionSampleIndex = position.sampleIndex,
                cumulativeDistance = distance[index].cumulativeDistance,
                pace = if (activityType == ActivityType.RUNNING ||
                    activityType == ActivityType.CROSS_COUNTRY_SKIING
                ) {
                    rate?.takeIf { it > 0.0 }?.let { PaceSecondsPerKilometre(1_000.0 / it) }
                } else {
                    null
                },
                speed = if (activityType == ActivityType.CYCLING) {
                    rate?.let(::SpeedMetresPerSecond)
                } else {
                    null
                },
            )
        }
    }

    private fun movementRateAt(
        centreIndex: Int,
        segment: IntRange,
        distance: List<DistanceSample>,
    ): Double? {
        if (segment.first == segment.last || !hasStrictlyIncreasingTime(segment, distance)) return null
        val halfWindow = windowMillis / 2.0
        val centreTime = distance[centreIndex].activeElapsedTime.value.toDouble()
        val startTime = maxOf(
            distance[segment.first].activeElapsedTime.value.toDouble(),
            centreTime - halfWindow,
        )
        val endTime = minOf(
            distance[segment.last].activeElapsedTime.value.toDouble(),
            centreTime + halfWindow,
        )
        val durationSeconds = (endTime - startTime) / 1_000.0
        if (durationSeconds <= 0.0) return null
        val startDistance = interpolateDistance(startTime, segment, distance) ?: return null
        val endDistance = interpolateDistance(endTime, segment, distance) ?: return null
        val displacement = endDistance - startDistance
        if (!displacement.isFinite() || displacement <= 0.0) return null
        return (displacement / durationSeconds).takeIf { it.isFinite() && it >= 0.0 }
    }
}

/** SRS CAD and REC 66: trailing 10-second, within-epoch cumulative-step deltas. */
class R00CadenceProcessor(
    private val windowMillis: Long = R00FinalProcessingDefaults.CADENCE_WINDOW_MILLIS,
) : CadenceProcessor {
    init {
        require(windowMillis > 0L) { "Cadence window must be positive" }
    }

    override fun process(activityType: ActivityType, steps: List<StepSample>): List<CadenceSample> {
        validateOrderedSteps(steps)
        if (activityType != ActivityType.RUNNING) return emptyList()
        val epochRanges = contiguousEpochRanges(steps)
        return steps.mapIndexed { index, step ->
            val epoch = epochRanges.getValue(step.counterEpoch.value to index)
            CadenceSample(
                activityId = step.activityId,
                sampleIndex = step.sampleIndex,
                activeElapsedTime = step.activeElapsedTime,
                cadence = cadenceAt(index, epoch, steps)?.let(::CadenceStepsPerMinute),
            )
        }
    }

    private fun cadenceAt(index: Int, epoch: IntRange, steps: List<StepSample>): Double? {
        if (epoch.first == epoch.last || !hasValidEpoch(epoch, steps)) return null
        val endTime = steps[index].activeElapsedTime.value.toDouble()
        val startTime = maxOf(
            steps[epoch.first].activeElapsedTime.value.toDouble(),
            endTime - windowMillis,
        )
        val durationMinutes = (endTime - startTime) / 60_000.0
        if (durationMinutes <= 0.0) return null
        val startSteps = interpolateSteps(startTime, epoch, steps) ?: return null
        val stepDelta = steps[index].cumulativeSteps.toDouble() - startSteps
        if (!stepDelta.isFinite() || stepDelta < 0.0) return null
        return (stepDelta / durationMinutes).takeIf { it.isFinite() && it >= 0.0 }
    }
}

/** SRS SUM: summary values from final derived movement and retained source elevation. */
class R00SummaryProcessor : SummaryProcessor {
    override fun process(
        activity: Activity,
        positions: List<PositionSample>,
        trackMetrics: List<DerivedTrackMetric>,
    ): ActivitySummary {
        require(positions.all { it.activityId == activity.id }) { "Positions must belong to the activity" }
        require(trackMetrics.all { it.activityId == activity.id }) {
            "Track metrics must belong to the activity"
        }
        val distance = trackMetrics.lastOrNull()?.cumulativeDistance
        val durationSeconds = activity.activeDuration.value / 1_000.0
        val averageRate = if (
            distance != null && distance.value > 0.0 && durationSeconds > 0.0
        ) {
            distance.value / durationSeconds
        } else {
            null
        }
        val elevations = positions.mapNotNull { it.elevation?.value }
        return ActivitySummary(
            activityId = activity.id,
            distance = distance,
            averagePace = if (
                activity.type == ActivityType.RUNNING ||
                activity.type == ActivityType.CROSS_COUNTRY_SKIING
            ) {
                averageRate?.let { PaceSecondsPerKilometre(1_000.0 / it) }
            } else {
                null
            },
            averageSpeed = if (activity.type == ActivityType.CYCLING) {
                averageRate?.let(::SpeedMetresPerSecond)
            } else {
                null
            },
            minimumElevation = elevations.minOrNull()?.let(::ElevationMetres),
            maximumElevation = elevations.maxOrNull()?.let(::ElevationMetres),
            // R00 has not approved an elevation-noise/ascent algorithm yet.
            totalAscent = null,
        )
    }
}

private fun validateOrderedPositions(positions: List<PositionSample>) {
    val activityId = positions.firstOrNull()?.activityId ?: return
    require(positions.all { it.activityId == activityId }) { "A position series must have one activity owner" }
    require(positions.zipWithNext().all { (left, right) -> left.sampleIndex < right.sampleIndex }) {
        "Position sample indexes must be strictly increasing"
    }
    require(positions.zipWithNext().all { (left, right) ->
        left.routeSegmentIndex <= right.routeSegmentIndex
    }) { "Route segment indexes must be non-decreasing" }
}

private fun validateOrderedSteps(steps: List<StepSample>) {
    val activityId: ActivityId = steps.firstOrNull()?.activityId ?: return
    require(steps.all { it.activityId == activityId }) { "A step series must have one activity owner" }
    require(steps.zipWithNext().all { (left, right) -> left.sampleIndex < right.sampleIndex }) {
        "Step sample indexes must be strictly increasing"
    }
    require(steps.zipWithNext().all { (left, right) -> left.counterEpoch <= right.counterEpoch }) {
        "Step counter epochs must be non-decreasing"
    }
}

private fun contiguousSegmentRanges(
    positions: List<PositionSample>,
): Map<Pair<Long, Int>, IntRange> = contiguousRanges(positions) { it.routeSegmentIndex.value }

private fun contiguousEpochRanges(
    steps: List<StepSample>,
): Map<Pair<Long, Int>, IntRange> = contiguousRanges(steps) { it.counterEpoch.value }

private fun <T> contiguousRanges(values: List<T>, key: (T) -> Long): Map<Pair<Long, Int>, IntRange> {
    val result = mutableMapOf<Pair<Long, Int>, IntRange>()
    var start = 0
    while (start < values.size) {
        val groupKey = key(values[start])
        var end = start
        while (end + 1 < values.size && key(values[end + 1]) == groupKey) end++
        for (index in start..end) result[groupKey to index] = start..end
        start = end + 1
    }
    return result
}

private fun hasStrictlyIncreasingTime(range: IntRange, samples: List<DistanceSample>): Boolean =
    (range.first until range.last).all { index ->
        samples[index].activeElapsedTime < samples[index + 1].activeElapsedTime
    }

private fun hasValidEpoch(range: IntRange, samples: List<StepSample>): Boolean =
    (range.first until range.last).all { index ->
        samples[index].activeElapsedTime < samples[index + 1].activeElapsedTime &&
            samples[index].cumulativeSteps <= samples[index + 1].cumulativeSteps
    }

private fun interpolateDistance(
    timeMillis: Double,
    range: IntRange,
    samples: List<DistanceSample>,
): Double? = interpolate(
    timeMillis = timeMillis,
    range = range,
    timeAt = { samples[it].activeElapsedTime.value.toDouble() },
    valueAt = { samples[it].cumulativeDistance.value },
)

private fun interpolateSteps(
    timeMillis: Double,
    range: IntRange,
    samples: List<StepSample>,
): Double? = interpolate(
    timeMillis = timeMillis,
    range = range,
    timeAt = { samples[it].activeElapsedTime.value.toDouble() },
    valueAt = { samples[it].cumulativeSteps.toDouble() },
)

private inline fun interpolate(
    timeMillis: Double,
    range: IntRange,
    timeAt: (Int) -> Double,
    valueAt: (Int) -> Double,
): Double? {
    if (timeMillis <= timeAt(range.first)) return valueAt(range.first)
    if (timeMillis >= timeAt(range.last)) return valueAt(range.last)
    var low = range.first
    var high = range.last
    while (low + 1 < high) {
        val middle = (low + high) ushr 1
        if (timeAt(middle) <= timeMillis) low = middle else high = middle
    }
    val leftTime = timeAt(low)
    val rightTime = timeAt(high)
    if (rightTime <= leftTime) return null
    val fraction = (timeMillis - leftTime) / (rightTime - leftTime)
    return valueAt(low) + (valueAt(high) - valueAt(low)) * fraction
}
