package com.jeppe.radm.ui.analysis

import com.jeppe.radm.domain.analysis.AnalysisCoordinateMode
import com.jeppe.radm.domain.analysis.AnalysisPoint
import com.jeppe.radm.domain.analysis.AnalysisPosition
import com.jeppe.radm.domain.analysis.AnalysisRange
import com.jeppe.radm.domain.analysis.AnalysisRouteCoordinate
import com.jeppe.radm.domain.analysis.AnalysisRoutePoint
import com.jeppe.radm.domain.analysis.AnalysisRouteSegment
import com.jeppe.radm.domain.model.ActiveElapsedTimeMillis
import com.jeppe.radm.domain.model.DistanceMetres
import com.jeppe.radm.domain.model.RouteSegmentIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RenderingDecimationTest {
    @Test
    fun vvmPerf009_chartRenderingIsBoundedAndPreservesEndpointsExtremaAndLogicalSource() {
        val source = List(100_000) { index ->
            AnalysisPoint(
                position = AnalysisPosition(
                    ActiveElapsedTimeMillis(index * 1_000L),
                    DistanceMetres(index.toDouble()),
                ),
                value = when (index) {
                    12_345 -> -1_000.0
                    54_321 -> 1_000.0
                    else -> (index % 20).toDouble()
                },
                continuityGroup = 0,
            )
        }
        val originalFirst = source.first()
        val originalLast = source.last()

        val rendered = chartSegments(
            points = source,
            coordinateMode = AnalysisCoordinateMode.DISTANCE,
            range = AnalysisRange(source.first().position, source.last().position),
            valueToY = { it },
        ).single()

        assertTrue(rendered.x.size <= MAX_RENDERED_CHART_POINTS)
        assertEquals(0.0, rendered.x.first(), 0.0)
        assertEquals(99.999, rendered.x.last(), 0.0)
        assertTrue(rendered.y.contains(-1_000.0))
        assertTrue(rendered.y.contains(1_000.0))
        assertEquals(100_000, source.size)
        assertSame(originalFirst, source.first())
        assertSame(originalLast, source.last())
    }

    @Test
    fun vvmPerf009_routeRenderingIsBoundedWithoutJoiningSegmentsOrChangingFullLookupData() {
        val source = listOf(
            routeSegment(0, 60_000),
            routeSegment(1, 40_000),
        )

        val rendered = routeSegmentsForRendering(source)

        assertTrue(rendered.sumOf { it.points.size } <= MAX_RENDERED_ROUTE_POINTS)
        assertEquals(listOf(RouteSegmentIndex(0), RouteSegmentIndex(1)), rendered.map { it.index })
        source.zip(rendered).forEach { (full, simplified) ->
            assertEquals(full.points.first(), simplified.points.first())
            assertEquals(full.points.last(), simplified.points.last())
        }
        assertEquals(100_000, source.sumOf { it.points.size })
    }

    private fun routeSegment(index: Long, count: Int): AnalysisRouteSegment =
        AnalysisRouteSegment(
            RouteSegmentIndex(index),
            List(count) { point ->
                AnalysisRoutePoint(
                    AnalysisRouteCoordinate(59.0 + point * 0.000001, 18.0 + point * 0.000001),
                    ActiveElapsedTimeMillis((index * 100_000L + point) * 1_000L),
                )
            },
        )
}
