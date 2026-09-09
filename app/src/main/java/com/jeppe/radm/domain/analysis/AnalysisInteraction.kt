package com.jeppe.radm.domain.analysis

import com.jeppe.radm.domain.model.ActiveElapsedTimeMillis
import com.jeppe.radm.domain.model.CadenceStepsPerMinute
import com.jeppe.radm.domain.model.DistanceMetres
import com.jeppe.radm.domain.model.ElevationMetres
import com.jeppe.radm.domain.model.PaceSecondsPerKilometre
import com.jeppe.radm.domain.model.SpeedMetresPerSecond
import kotlin.math.abs
import kotlin.math.roundToLong

/** Numeric extent in metres for Distance mode and milliseconds for Active Elapsed Time mode. */
data class AnalysisCoordinateExtent(
    val start: Double,
    val endInclusive: Double,
) {
    init {
        require(start.isFinite() && endInclusive.isFinite())
        require(start <= endInclusive)
    }
}

data class ActivityAnalysisInteractionSnapshot(
    val state: ActivityAnalysisState,
    val inspector: AnalysisInspectorValues,
    val fullRange: AnalysisRange,
    val visibleCoordinateExtent: AnalysisCoordinateExtent,
    val rangeStartFraction: Double,
    val rangeEndFraction: Double,
    val cursorFraction: Double?,
    val route: ActivityAnalysisRouteSnapshot,
)

/**
 * Precomputed, persistence-independent indexes for synchronized analysis lookup.
 * Every query is local and uses binary search; no Android, Room, or renderer type is involved.
 */
class ActivityAnalysisLookup(data: ActivityAnalysisData) {
    private val distanceByTime = data.distance.points.sortedByTime()
    private val distanceByDistance = distanceByTime
        .mapNotNull { point -> point.value?.let { DistanceEntry(it.value, point.time, point.group) } }
        .sortedWith(compareBy<DistanceEntry> { it.distance }.thenBy { it.time })
    private val paceByTime = data.pace.points.sortedByTime()
    private val speedByTime = data.speed.points.sortedByTime()
    private val elevationByTime = data.elevation.points.sortedByTime()
    private val cadenceByTime = data.cadence.points.sortedByTime()

    val hasDistance: Boolean get() = distanceByDistance.isNotEmpty()

    fun distanceAt(time: ActiveElapsedTimeMillis): DistanceMetres? = interpolateAtTime(
        entries = distanceByTime,
        time = time.value,
        value = { it.value?.value },
        create = ::DistanceMetres,
    )

    fun nearestDistanceAt(time: ActiveElapsedTimeMillis): DistanceMetres? {
        if (distanceByTime.isEmpty()) return null
        val insertion = distanceByTime.lowerBound(time.value.toDouble()) { it.time.toDouble() }
        val candidates = listOfNotNull(
            distanceByTime.getOrNull(insertion - 1),
            distanceByTime.getOrNull(insertion),
        ).filter { it.value != null }
        return candidates.minByOrNull { abs(it.time - time.value) }?.value
    }

    fun elapsedAtDistance(distance: DistanceMetres): ActiveElapsedTimeMillis? {
        if (distanceByDistance.isEmpty()) return null
        val target = distance.value
        val insertion = distanceByDistance.lowerBound(target) { it.distance }
        if (insertion < distanceByDistance.size && distanceByDistance[insertion].distance == target) {
            return ActiveElapsedTimeMillis(distanceByDistance[insertion].time)
        }
        if (insertion == 0) return ActiveElapsedTimeMillis(distanceByDistance.first().time)
        if (insertion == distanceByDistance.size) return ActiveElapsedTimeMillis(distanceByDistance.last().time)
        val left = distanceByDistance[insertion - 1]
        val right = distanceByDistance[insertion]
        if (left.group != right.group || right.distance <= left.distance) {
            return ActiveElapsedTimeMillis(
                if (abs(target - left.distance) <= abs(right.distance - target)) left.time else right.time,
            )
        }
        val fraction = (target - left.distance) / (right.distance - left.distance)
        return ActiveElapsedTimeMillis((left.time + (right.time - left.time) * fraction).roundToLong())
    }

    fun paceAt(time: ActiveElapsedTimeMillis): PaceSecondsPerKilometre? = interpolateAtTime(
        entries = paceByTime,
        time = time.value,
        value = { it.value?.value },
        create = ::PaceSecondsPerKilometre,
    )

    fun speedAt(time: ActiveElapsedTimeMillis): SpeedMetresPerSecond? = interpolateAtTime(
        entries = speedByTime,
        time = time.value,
        value = { it.value?.value },
        create = ::SpeedMetresPerSecond,
    )

    fun elevationAt(time: ActiveElapsedTimeMillis): ElevationMetres? = interpolateAtTime(
        entries = elevationByTime,
        time = time.value,
        value = { it.value?.value },
        create = ::ElevationMetres,
    )

    fun cadenceAt(time: ActiveElapsedTimeMillis): CadenceStepsPerMinute? = interpolateAtTime(
        entries = cadenceByTime,
        time = time.value,
        value = { it.value?.value },
        create = ::CadenceStepsPerMinute,
    )

    fun positionAt(time: ActiveElapsedTimeMillis): AnalysisPosition =
        AnalysisPosition(time, distanceAt(time))

    fun inspectorAt(time: ActiveElapsedTimeMillis): AnalysisInspectorValues =
        AnalysisInspectorValues(
            position = positionAt(time),
            pace = paceAt(time),
            speed = speedAt(time),
            elevation = elevationAt(time),
            cadence = cadenceAt(time),
        )

    fun coordinateAt(
        time: ActiveElapsedTimeMillis,
        mode: AnalysisCoordinateMode,
    ): Double? = when (mode) {
        AnalysisCoordinateMode.DISTANCE -> distanceAt(time)?.value
        AnalysisCoordinateMode.ACTIVE_ELAPSED_TIME -> time.value.toDouble()
    }

    fun elapsedAt(
        coordinate: Double,
        mode: AnalysisCoordinateMode,
    ): ActiveElapsedTimeMillis? = when (mode) {
        AnalysisCoordinateMode.DISTANCE -> elapsedAtDistance(DistanceMetres(coordinate))
        AnalysisCoordinateMode.ACTIVE_ELAPSED_TIME ->
            ActiveElapsedTimeMillis(coordinate.coerceAtLeast(0.0).roundToLong())
    }
}

/** One authoritative selected-time/range state shared by all graph adapters. */
class ActivityAnalysisInteraction(
    private val data: ActivityAnalysisData,
) {
    val lookup = ActivityAnalysisLookup(data)
    val routeLookup = ActivityRouteLookup(data.routeSegments)
    private val fullRange = data.initialState.range
    private var state = data.initialState
    private var highlightedRouteSegments = routeLookup.segmentsInRange(state.range)

    var snapshot: ActivityAnalysisInteractionSnapshot = buildSnapshot()
        private set

    fun selectVisibleFraction(fraction: Double): ActivityAnalysisInteractionSnapshot {
        val extent = visibleExtent(state.coordinateMode, state.range)
        val coordinate = extent.start + extent.length * fraction.coerceIn(0.0, 1.0)
        val elapsed = lookup.elapsedAt(coordinate, state.coordinateMode) ?: return snapshot
        state = state.copy(selectedPosition = lookup.positionAt(elapsed.coerceTo(state.range)))
        return refresh()
    }

    fun setCoordinateMode(mode: AnalysisCoordinateMode): ActivityAnalysisInteractionSnapshot {
        if (mode == AnalysisCoordinateMode.DISTANCE && !lookup.hasDistance) return snapshot
        state = state.copy(coordinateMode = mode)
        return refresh()
    }

    fun setRangeFractions(
        startFraction: Double,
        endFraction: Double,
    ): ActivityAnalysisInteractionSnapshot {
        val start = startFraction.coerceIn(0.0, 1.0).coerceAtMost(endFraction.coerceIn(0.0, 1.0))
        val end = endFraction.coerceIn(0.0, 1.0).coerceAtLeast(start)
        val fullExtent = fullExtent(state.coordinateMode)
        val startCoordinate = fullExtent.start + fullExtent.length * start
        val endCoordinate = fullExtent.start + fullExtent.length * end
        val startTime = checkNotNull(lookup.elapsedAt(startCoordinate, state.coordinateMode))
        val endTime = checkNotNull(lookup.elapsedAt(endCoordinate, state.coordinateMode))
        val orderedStart = minOf(startTime, endTime)
        val orderedEnd = maxOf(startTime, endTime)
        val range = AnalysisRange(
            lookup.positionAt(orderedStart),
            lookup.positionAt(orderedEnd),
        )
        state = state.copy(
            selectedPosition = lookup.positionAt(state.selectedPosition.activeElapsedTime.coerceTo(range)),
            range = range,
        )
        highlightedRouteSegments = routeLookup.segmentsInRange(range)
        return refresh()
    }

    fun restoreFullRange(): ActivityAnalysisInteractionSnapshot {
        state = state.copy(
            selectedPosition = lookup.positionAt(state.selectedPosition.activeElapsedTime.coerceTo(fullRange)),
            range = fullRange,
        )
        highlightedRouteSegments = routeLookup.segmentsInRange(fullRange)
        return refresh()
    }

    fun selectRouteCoordinate(
        coordinate: AnalysisRouteCoordinate,
        toleranceMetres: Double,
    ): ActivityAnalysisInteractionSnapshot {
        val candidate = routeLookup.nearestPosition(coordinate, toleranceMetres, state.range)
            ?: return snapshot
        state = state.copy(selectedPosition = lookup.positionAt(candidate.activeElapsedTime))
        return refresh()
    }

    private fun refresh(): ActivityAnalysisInteractionSnapshot = buildSnapshot().also { snapshot = it }

    private fun buildSnapshot(): ActivityAnalysisInteractionSnapshot {
        val visibleExtent = visibleExtent(state.coordinateMode, state.range)
        val fullExtent = fullExtent(state.coordinateMode)
        val selectedCoordinate = lookup.coordinateAt(state.selectedPosition.activeElapsedTime, state.coordinateMode)
        return ActivityAnalysisInteractionSnapshot(
            state = state,
            inspector = lookup.inspectorAt(state.selectedPosition.activeElapsedTime),
            fullRange = fullRange,
            visibleCoordinateExtent = visibleExtent,
            rangeStartFraction = fullExtent.fractionOf(visibleExtent.start),
            rangeEndFraction = fullExtent.fractionOf(visibleExtent.endInclusive),
            cursorFraction = selectedCoordinate?.let(visibleExtent::fractionOf)?.coerceIn(0.0, 1.0),
            route = ActivityAnalysisRouteSnapshot(
                fullSegments = routeLookup.fullSegments,
                highlightedSegments = highlightedRouteSegments,
                selectedCoordinate = routeLookup.coordinateAt(state.selectedPosition.activeElapsedTime),
            ),
        )
    }

    private fun fullExtent(mode: AnalysisCoordinateMode): AnalysisCoordinateExtent =
        visibleExtent(mode, fullRange)

    private fun visibleExtent(
        mode: AnalysisCoordinateMode,
        range: AnalysisRange,
    ): AnalysisCoordinateExtent = when (mode) {
        AnalysisCoordinateMode.ACTIVE_ELAPSED_TIME -> AnalysisCoordinateExtent(
            range.start.activeElapsedTime.value.toDouble(),
            range.endInclusive.activeElapsedTime.value.toDouble(),
        )
        AnalysisCoordinateMode.DISTANCE -> AnalysisCoordinateExtent(
            boundaryDistance(range.start, first = true),
            boundaryDistance(range.endInclusive, first = false),
        )
    }

    private fun boundaryDistance(position: AnalysisPosition, first: Boolean): Double =
        position.cumulativeDistance?.value
            ?: lookup.distanceAt(position.activeElapsedTime)?.value
            ?: lookup.nearestDistanceAt(position.activeElapsedTime)?.value
            ?: data.distance.points
                .mapNotNull { it.value?.value }
                .let { values -> if (first) values.first() else values.last() }
}

private data class TimeEntry<T>(
    val time: Long,
    val value: T?,
    val group: Long,
)

private data class DistanceEntry(
    val distance: Double,
    val time: Long,
    val group: Long,
)

private val AnalysisCoordinateExtent.length: Double get() = endInclusive - start

private fun AnalysisCoordinateExtent.fractionOf(value: Double): Double =
    if (length == 0.0) 0.0 else (value - start) / length

private fun <T> List<AnalysisPoint<T>>.sortedByTime(): List<TimeEntry<T>> =
    map { TimeEntry(it.position.activeElapsedTime.value, it.value, it.continuityGroup) }
        .sortedBy { it.time }

private inline fun <T, R> interpolateAtTime(
    entries: List<TimeEntry<T>>,
    time: Long,
    value: (TimeEntry<T>) -> Double?,
    create: (Double) -> R,
): R? {
    if (entries.isEmpty()) return null
    val insertion = entries.lowerBound(time.toDouble()) { it.time.toDouble() }
    if (insertion < entries.size && entries[insertion].time == time) {
        return value(entries[insertion])?.let(create)
    }
    if (insertion == 0 || insertion == entries.size) return null
    val left = entries[insertion - 1]
    val right = entries[insertion]
    if (left.group != right.group || right.time <= left.time) return null
    val leftValue = value(left) ?: return null
    val rightValue = value(right) ?: return null
    val fraction = (time - left.time).toDouble() / (right.time - left.time)
    return create(leftValue + (rightValue - leftValue) * fraction)
}

private inline fun <T> List<T>.lowerBound(target: Double, value: (T) -> Double): Int {
    var low = 0
    var high = size
    while (low < high) {
        val middle = (low + high) ushr 1
        if (value(this[middle]) < target) low = middle + 1 else high = middle
    }
    return low
}

private fun ActiveElapsedTimeMillis.coerceTo(range: AnalysisRange): ActiveElapsedTimeMillis =
    ActiveElapsedTimeMillis(
        value.coerceIn(range.start.activeElapsedTime.value, range.endInclusive.activeElapsedTime.value),
    )
