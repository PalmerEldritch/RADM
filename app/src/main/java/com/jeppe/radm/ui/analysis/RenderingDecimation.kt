package com.jeppe.radm.ui.analysis

import com.jeppe.radm.domain.analysis.AnalysisCoordinateMode
import com.jeppe.radm.domain.analysis.AnalysisPoint
import com.jeppe.radm.domain.analysis.AnalysisRange
import com.jeppe.radm.domain.analysis.AnalysisRouteSegment
import kotlin.math.roundToInt

/** Presentation-only limits; complete logical series remain in ActivityAnalysisData. */
internal const val MAX_RENDERED_CHART_POINTS = 2_000
internal const val MAX_RENDERED_ROUTE_POINTS = 5_000

internal data class ChartSegment(
    val x: List<Double>,
    val y: List<Double>,
)

internal fun <T : Any> chartSegments(
    points: List<AnalysisPoint<T>>,
    coordinateMode: AnalysisCoordinateMode,
    range: AnalysisRange,
    valueToY: (T) -> Double,
    maxRenderedPoints: Int = MAX_RENDERED_CHART_POINTS,
): List<ChartSegment> {
    require(maxRenderedPoints >= 2)
    val exact = mutableListOf<ChartSegment>()
    var group: Long? = null
    var xValues = mutableListOf<Double>()
    var yValues = mutableListOf<Double>()

    fun flush() {
        if (xValues.isNotEmpty()) exact += ChartSegment(xValues, yValues)
        xValues = mutableListOf()
        yValues = mutableListOf()
    }

    points.forEach { point ->
        if (point.position.activeElapsedTime !in
            range.start.activeElapsedTime..range.endInclusive.activeElapsedTime
        ) {
            flush()
            group = null
            return@forEach
        }
        val x = when (coordinateMode) {
            AnalysisCoordinateMode.DISTANCE -> point.position.cumulativeDistance?.value?.div(1_000.0)
            AnalysisCoordinateMode.ACTIVE_ELAPSED_TIME -> point.position.activeElapsedTime.value / 60_000.0
        }
        val value = point.value
        if (x == null || value == null) {
            flush()
            group = null
            return@forEach
        }
        if (group != null && group != point.continuityGroup) flush()
        group = point.continuityGroup
        val y = valueToY(value)
        if (xValues.lastOrNull() == x) {
            yValues[yValues.lastIndex] = y
        } else if (xValues.lastOrNull()?.let { x > it } != false) {
            xValues += x
            yValues += y
        }
    }
    flush()

    val budgets = renderingBudgets(exact.map { it.x.size }, maxRenderedPoints)
    return exact.mapIndexed { index, segment -> decimateChartSegment(segment, budgets[index]) }
}

internal fun routeSegmentsForRendering(
    segments: List<AnalysisRouteSegment>,
    maxRenderedPoints: Int = MAX_RENDERED_ROUTE_POINTS,
): List<AnalysisRouteSegment> {
    require(maxRenderedPoints >= 2)
    val budgets = renderingBudgets(segments.map { it.points.size }, maxRenderedPoints)
    return segments.mapIndexed { index, segment ->
        val budget = budgets[index]
        if (segment.points.size <= budget) {
            segment
        } else {
            val selected = buildList(budget) {
                repeat(budget) { outputIndex ->
                    val sourceIndex = (
                        outputIndex.toLong() * (segment.points.lastIndex) / (budget - 1)
                        ).toInt()
                    val point = segment.points[sourceIndex]
                    if (lastOrNull() != point) add(point)
                }
            }
            AnalysisRouteSegment(segment.index, selected)
        }
    }
}

private fun decimateChartSegment(segment: ChartSegment, budget: Int): ChartSegment {
    if (segment.x.size <= budget) return segment
    if (budget == 2) {
        return ChartSegment(
            x = listOf(segment.x.first(), segment.x.last()),
            y = listOf(segment.y.first(), segment.y.last()),
        )
    }

    val selectedIndexes = mutableListOf(0)
    val interiorSize = segment.x.size - 2
    val pairedBucketCount = (budget - 2) / 2
    repeat(pairedBucketCount) { bucket ->
        val start = 1 + bucket * interiorSize / pairedBucketCount
        val endExclusive = 1 + (bucket + 1) * interiorSize / pairedBucketCount
        if (start < endExclusive) {
            var minimum = start
            var maximum = start
            for (index in start + 1 until endExclusive) {
                if (segment.y[index] < segment.y[minimum]) minimum = index
                if (segment.y[index] > segment.y[maximum]) maximum = index
            }
            selectedIndexes += minOf(minimum, maximum)
            if (minimum != maximum) selectedIndexes += maxOf(minimum, maximum)
        }
    }
    if (selectedIndexes.size < budget - 1) selectedIndexes += segment.x.lastIndex / 2
    selectedIndexes += segment.x.lastIndex
    val ordered = selectedIndexes.distinct().sorted().take(budget)
    return ChartSegment(
        x = ordered.map(segment.x::get),
        y = ordered.map(segment.y::get),
    )
}

private fun renderingBudgets(sizes: List<Int>, requestedMaximum: Int): IntArray {
    if (sizes.isEmpty()) return IntArray(0)
    val total = sizes.sum()
    if (total <= requestedMaximum) return sizes.toIntArray()
    val budgets = IntArray(sizes.size) { index -> minOf(2, sizes[index]) }
    val minimumTotal = budgets.sum()
    val target = requestedMaximum.coerceAtLeast(minimumTotal).coerceAtMost(total)
    var remaining = target - minimumTotal
    while (remaining > 0) {
        val capacities = sizes.indices.map { index -> sizes[index] - budgets[index] }
        val capacityTotal = capacities.sum()
        if (capacityTotal == 0) break
        var allocated = 0
        capacities.forEachIndexed { index, capacity ->
            if (capacity == 0 || remaining == 0) return@forEachIndexed
            val share = maxOf(1, (remaining.toDouble() * capacity / capacityTotal).roundToInt())
                .coerceAtMost(capacity)
                .coerceAtMost(remaining)
            budgets[index] += share
            remaining -= share
            allocated += share
        }
        if (allocated == 0) break
    }
    return budgets
}
