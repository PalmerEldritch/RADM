package com.jeppe.radm.domain.analysis

import com.jeppe.radm.domain.model.ActiveElapsedTimeMillis
import com.jeppe.radm.domain.model.RouteSegment
import com.jeppe.radm.domain.model.RouteSegmentIndex
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToLong

data class AnalysisRouteCoordinate(
    val latitude: Double,
    val longitude: Double,
)

data class AnalysisRoutePoint(
    val coordinate: AnalysisRouteCoordinate,
    val activeElapsedTime: ActiveElapsedTimeMillis,
)

data class AnalysisRouteSegment(
    val index: RouteSegmentIndex,
    val points: List<AnalysisRoutePoint>,
) {
    init {
        require(points.isNotEmpty())
    }
}

data class ActivityAnalysisRouteSnapshot(
    val fullSegments: List<AnalysisRouteSegment>,
    val highlightedSegments: List<AnalysisRouteSegment>,
    val selectedCoordinate: AnalysisRouteCoordinate?,
)

/**
 * Renderer-neutral geographical lookup. Interpolation and nearest-point projection
 * are confined to individual retained route segments, so known gaps are never joined.
 */
class ActivityRouteLookup(routeSegments: List<RouteSegment>) {
    val fullSegments: List<AnalysisRouteSegment> = routeSegments.map { segment ->
        AnalysisRouteSegment(
            index = segment.index,
            points = segment.samples.map { sample ->
                AnalysisRoutePoint(
                    coordinate = AnalysisRouteCoordinate(
                        latitude = sample.latitude.value,
                        longitude = sample.longitude.value,
                    ),
                    activeElapsedTime = sample.activeElapsedTime,
                )
            },
        )
    }

    fun coordinateAt(time: ActiveElapsedTimeMillis): AnalysisRouteCoordinate? {
        fullSegments.forEach { segment ->
            val points = segment.points
            val insertion = points.lowerBound(time.value)
            if (insertion < points.size && points[insertion].activeElapsedTime == time) {
                return points[insertion].coordinate
            }
            if (insertion in 1..<points.size) {
                return interpolate(points[insertion - 1], points[insertion], time.value).coordinate
            }
        }
        return null
    }

    fun segmentsInRange(range: AnalysisRange): List<AnalysisRouteSegment> = buildList {
        fullSegments.forEach { segment ->
            val clipped = clip(segment.points, range)
            if (clipped.isNotEmpty()) add(AnalysisRouteSegment(segment.index, clipped))
        }
    }

    fun nearestPosition(
        coordinate: AnalysisRouteCoordinate,
        toleranceMetres: Double,
        range: AnalysisRange,
    ): AnalysisRoutePoint? {
        require(toleranceMetres >= 0.0 && toleranceMetres.isFinite())
        var best: AnalysisRoutePoint? = null
        var bestDistance = Double.POSITIVE_INFINITY
        segmentsInRange(range).forEach { segment ->
            if (segment.points.size == 1) {
                val candidate = segment.points.single()
                val distance = planarDistanceMetres(coordinate, candidate.coordinate)
                if (distance < bestDistance) {
                    best = candidate
                    bestDistance = distance
                }
            } else {
                segment.points.zipWithNext().forEach { (start, end) ->
                    val (candidate, distance) = nearestOnEdge(coordinate, start, end)
                    if (distance < bestDistance) {
                        best = candidate
                        bestDistance = distance
                    }
                }
            }
        }
        return best?.takeIf { bestDistance <= toleranceMetres }
    }

    private fun clip(points: List<AnalysisRoutePoint>, range: AnalysisRange): List<AnalysisRoutePoint> {
        val start = range.start.activeElapsedTime.value
        val end = range.endInclusive.activeElapsedTime.value
        if (points.size == 1) {
            return points.filter { it.activeElapsedTime.value in start..end }
        }
        val result = mutableListOf<AnalysisRoutePoint>()
        points.zipWithNext().forEach { (left, right) ->
            val overlapStart = maxOf(start, left.activeElapsedTime.value)
            val overlapEnd = minOf(end, right.activeElapsedTime.value)
            if (overlapStart <= overlapEnd) {
                result.addDistinct(interpolate(left, right, overlapStart))
                result.addDistinct(interpolate(left, right, overlapEnd))
            }
        }
        return result
    }
}

private fun nearestOnEdge(
    touch: AnalysisRouteCoordinate,
    start: AnalysisRoutePoint,
    end: AnalysisRoutePoint,
): Pair<AnalysisRoutePoint, Double> {
    val startX = longitudeMetres(start.coordinate.longitude - touch.longitude, touch.latitude)
    val startY = latitudeMetres(start.coordinate.latitude - touch.latitude)
    val endX = longitudeMetres(end.coordinate.longitude - touch.longitude, touch.latitude)
    val endY = latitudeMetres(end.coordinate.latitude - touch.latitude)
    val edgeX = endX - startX
    val edgeY = endY - startY
    val squaredLength = edgeX * edgeX + edgeY * edgeY
    val fraction = if (squaredLength == 0.0) {
        0.0
    } else {
        (-(startX * edgeX + startY * edgeY) / squaredLength).coerceIn(0.0, 1.0)
    }
    val elapsed = (
        start.activeElapsedTime.value +
            (end.activeElapsedTime.value - start.activeElapsedTime.value) * fraction
        ).roundToLong()
    val point = interpolate(start, end, elapsed)
    return point to hypot(startX + edgeX * fraction, startY + edgeY * fraction)
}

private fun interpolate(
    start: AnalysisRoutePoint,
    end: AnalysisRoutePoint,
    elapsed: Long,
): AnalysisRoutePoint {
    if (elapsed <= start.activeElapsedTime.value) return start
    if (elapsed >= end.activeElapsedTime.value) return end
    val span = end.activeElapsedTime.value - start.activeElapsedTime.value
    if (span <= 0L) return start
    val fraction = (elapsed - start.activeElapsedTime.value).toDouble() / span
    return AnalysisRoutePoint(
        coordinate = AnalysisRouteCoordinate(
            latitude = start.coordinate.latitude +
                (end.coordinate.latitude - start.coordinate.latitude) * fraction,
            longitude = start.coordinate.longitude +
                (end.coordinate.longitude - start.coordinate.longitude) * fraction,
        ),
        activeElapsedTime = ActiveElapsedTimeMillis(elapsed),
    )
}

private fun planarDistanceMetres(
    left: AnalysisRouteCoordinate,
    right: AnalysisRouteCoordinate,
): Double = hypot(
    longitudeMetres(right.longitude - left.longitude, left.latitude),
    latitudeMetres(right.latitude - left.latitude),
)

private fun longitudeMetres(deltaDegrees: Double, latitude: Double): Double =
    deltaDegrees * DEGREES_TO_RADIANS * EARTH_RADIUS_METRES * cos(latitude * DEGREES_TO_RADIANS)

private fun latitudeMetres(deltaDegrees: Double): Double =
    deltaDegrees * DEGREES_TO_RADIANS * EARTH_RADIUS_METRES

private fun List<AnalysisRoutePoint>.lowerBound(time: Long): Int {
    var low = 0
    var high = size
    while (low < high) {
        val middle = (low + high) ushr 1
        if (this[middle].activeElapsedTime.value < time) low = middle + 1 else high = middle
    }
    return low
}

private fun MutableList<AnalysisRoutePoint>.addDistinct(point: AnalysisRoutePoint) {
    if (lastOrNull() != point) add(point)
}

private const val EARTH_RADIUS_METRES = 6_371_000.0
private const val DEGREES_TO_RADIANS = PI / 180.0
