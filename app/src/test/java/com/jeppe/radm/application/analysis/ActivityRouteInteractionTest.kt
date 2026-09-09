package com.jeppe.radm.application.analysis

import com.jeppe.radm.domain.analysis.ActivityAnalysisInteraction
import com.jeppe.radm.domain.analysis.ActivityRouteLookup
import com.jeppe.radm.domain.analysis.AnalysisRouteCoordinate
import com.jeppe.radm.domain.model.ActiveElapsedTimeMillis
import com.jeppe.radm.domain.model.DistanceMetres
import com.jeppe.radm.domain.model.RouteSegmentIndex
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActivityRouteInteractionTest {
    @Test
    fun selectedTimeMapsToInterpolatedCoordinateWithoutLeavingSegment() = runBlocking {
        val repository = analysisRepository()
        val data = LoadActivityAnalysis(repository)(repository.activity.id)
        val lookup = ActivityRouteLookup(data.routeSegments)

        val coordinate = lookup.coordinateAt(ActiveElapsedTimeMillis(5_000L))

        assertEquals(59.0005, coordinate?.latitude ?: Double.NaN, 0.0000001)
        assertEquals(18.0005, coordinate?.longitude ?: Double.NaN, 0.0000001)
        assertNull(lookup.coordinateAt(ActiveElapsedTimeMillis(25_000L)))
    }

    @Test
    fun vvmAn004_touchNearRouteMapsToCanonicalElapsedSelectionAndInspector() = runBlocking {
        val repository = analysisRepository()
        val data = LoadActivityAnalysis(repository)(repository.activity.id)
        val interaction = ActivityAnalysisInteraction(data)

        val selected = interaction.selectRouteCoordinate(
            coordinate = AnalysisRouteCoordinate(59.00105, 18.00105),
            toleranceMetres = 20.0,
        )

        assertEquals(ActiveElapsedTimeMillis(10_500L), selected.state.selectedPosition.activeElapsedTime)
        assertEquals(DistanceMetres(105.0), selected.inspector.position.cumulativeDistance)
        assertEquals(selected.route.selectedCoordinate, interaction.routeLookup.coordinateAt(ActiveElapsedTimeMillis(10_500L)))
    }

    @Test
    fun touchOutsideToleranceDoesNotChangeSelection() = runBlocking {
        val repository = analysisRepository()
        val data = LoadActivityAnalysis(repository)(repository.activity.id)
        val interaction = ActivityAnalysisInteraction(data)
        val initial = interaction.snapshot

        val result = interaction.selectRouteCoordinate(
            coordinate = AnalysisRouteCoordinate(60.0, 19.0),
            toleranceMetres = 20.0,
        )

        assertEquals(initial, result)
    }

    @Test
    fun vvmAn007_rangeHighlightClipsEachSegmentAndNeverConnectsKnownGap() = runBlocking {
        val repository = analysisRepository().apply {
            positions = positions.mapIndexed { index, position ->
                if (index == positions.lastIndex) {
                    position.copy(routeSegmentIndex = RouteSegmentIndex(1))
                } else {
                    position
                }
            }
        }
        val data = LoadActivityAnalysis(repository)(repository.activity.id)
        val interaction = ActivityAnalysisInteraction(data)
        val restricted = interaction.setRangeFractions(0.25, 0.75)

        assertEquals(2, restricted.route.fullSegments.size)
        assertNull(interaction.routeLookup.coordinateAt(ActiveElapsedTimeMillis(15_000L)))
        assertEquals(1, restricted.route.highlightedSegments.size)
        assertEquals(
            listOf(5_000L, 10_000L),
            restricted.route.highlightedSegments.single().points.map { it.activeElapsedTime.value },
        )
    }

    @Test
    fun nearestRouteSelectionIsRestrictedToTheCanonicalAnalysisRange() = runBlocking {
        val repository = analysisRepository()
        val data = LoadActivityAnalysis(repository)(repository.activity.id)
        val interaction = ActivityAnalysisInteraction(data)
        interaction.setRangeFractions(0.5, 1.0)

        val unchanged = interaction.selectRouteCoordinate(
            coordinate = AnalysisRouteCoordinate(59.0, 18.0),
            toleranceMetres = 20.0,
        )

        assertEquals(ActiveElapsedTimeMillis(10_000L), unchanged.state.selectedPosition.activeElapsedTime)
    }
}
