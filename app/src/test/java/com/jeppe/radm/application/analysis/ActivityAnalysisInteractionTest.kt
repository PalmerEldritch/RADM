package com.jeppe.radm.application.analysis

import com.jeppe.radm.domain.analysis.ActivityAnalysisInteraction
import com.jeppe.radm.domain.analysis.ActivityAnalysisLookup
import com.jeppe.radm.domain.analysis.AnalysisCoordinateMode
import com.jeppe.radm.domain.model.AbsoluteTimestampUtcMillis
import com.jeppe.radm.domain.model.ActiveElapsedTimeMillis
import com.jeppe.radm.domain.model.DistanceMetres
import com.jeppe.radm.domain.model.ElevationMetres
import com.jeppe.radm.domain.model.PaceSecondsPerKilometre
import com.jeppe.radm.domain.model.RouteSegmentIndex
import com.jeppe.radm.domain.model.SampleIndex
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActivityAnalysisInteractionTest {
    @Test
    fun lookupInterpolatesTimeDistanceAndMetricsInMemory() = runBlocking {
        val repository = analysisRepository()
        val data = LoadActivityAnalysis(repository)(repository.activity.id)
        val lookup = ActivityAnalysisLookup(data)

        assertEquals(DistanceMetres(50.0), lookup.distanceAt(ActiveElapsedTimeMillis(5_000L)))
        assertEquals(ActiveElapsedTimeMillis(15_000L), lookup.elapsedAtDistance(DistanceMetres(150.0)))
        assertEquals(
            PaceSecondsPerKilometre(330.0),
            lookup.paceAt(ActiveElapsedTimeMillis(15_000L)),
        )
        assertEquals(ElevationMetres(10.5), lookup.elevationAt(ActiveElapsedTimeMillis(5_000L)))
        assertNull(lookup.elevationAt(ActiveElapsedTimeMillis(15_000L)))
    }

    @Test
    fun vvmAn002And005_oneCanonicalSelectionUpdatesInspectorAndPersistsAfterRelease() = runBlocking {
        val repository = analysisRepository()
        val data = LoadActivityAnalysis(repository)(repository.activity.id)
        val interaction = ActivityAnalysisInteraction(data)

        val selected = interaction.selectVisibleFraction(0.75)

        assertEquals(ActiveElapsedTimeMillis(15_000L), selected.state.selectedPosition.activeElapsedTime)
        assertEquals(DistanceMetres(150.0), selected.inspector.position.cumulativeDistance)
        assertEquals(PaceSecondsPerKilometre(330.0), selected.inspector.pace)
        assertEquals(0.75, selected.cursorFraction ?: Double.NaN, 0.0001)
        assertEquals(selected, interaction.snapshot)
    }

    @Test
    fun vvmAn006_coordinateSwitchPreservesCanonicalSelectionAndRange() = runBlocking {
        val repository = analysisRepository()
        val data = LoadActivityAnalysis(repository)(repository.activity.id)
        val interaction = ActivityAnalysisInteraction(data)
        interaction.setRangeFractions(0.25, 0.75)
        val distanceSelection = interaction.selectVisibleFraction(0.5)

        val elapsedSelection = interaction.setCoordinateMode(AnalysisCoordinateMode.ACTIVE_ELAPSED_TIME)

        assertEquals(
            distanceSelection.state.selectedPosition.activeElapsedTime,
            elapsedSelection.state.selectedPosition.activeElapsedTime,
        )
        assertEquals(distanceSelection.state.range, elapsedSelection.state.range)
        assertEquals(AnalysisCoordinateMode.ACTIVE_ELAPSED_TIME, elapsedSelection.state.coordinateMode)
        assertEquals(0.5, elapsedSelection.cursorFraction ?: Double.NaN, 0.0001)
    }

    @Test
    fun vvmAn007_commonRangeClampsSelectionAndRestoresFullExtent() = runBlocking {
        val repository = analysisRepository()
        val data = LoadActivityAnalysis(repository)(repository.activity.id)
        val interaction = ActivityAnalysisInteraction(data)

        val restricted = interaction.setRangeFractions(0.25, 0.75)

        assertEquals(ActiveElapsedTimeMillis(5_000L), restricted.state.range.start.activeElapsedTime)
        assertEquals(ActiveElapsedTimeMillis(15_000L), restricted.state.range.endInclusive.activeElapsedTime)
        assertEquals(ActiveElapsedTimeMillis(5_000L), restricted.state.selectedPosition.activeElapsedTime)
        assertEquals(0.25, restricted.rangeStartFraction, 0.0001)
        assertEquals(0.75, restricted.rangeEndFraction, 0.0001)

        val restored = interaction.restoreFullRange()

        assertEquals(data.initialState.range, restored.state.range)
        assertEquals(0.0, restored.rangeStartFraction, 0.0001)
        assertEquals(1.0, restored.rangeEndFraction, 0.0001)
    }

    @Test
    fun distanceToTimeDoesNotInterpolateAcrossRouteDiscontinuity() = runBlocking {
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
        val lookup = ActivityAnalysisLookup(data)

        assertNull(lookup.distanceAt(ActiveElapsedTimeMillis(15_000L)))
        assertEquals(ActiveElapsedTimeMillis(10_000L), lookup.elapsedAtDistance(DistanceMetres(150.0)))

        val restricted = ActivityAnalysisInteraction(data).setRangeFractions(0.75, 1.0)
        assertEquals(ActiveElapsedTimeMillis(10_000L), restricted.state.range.start.activeElapsedTime)
        assertEquals(DistanceMetres(100.0), restricted.state.range.start.cumulativeDistance)
    }

    @Test
    fun highFrequencySelectionUsesLoadedDataAfterRepositoryBecomesUnavailable() = runBlocking {
        val repository = analysisRepository()
        val data = LoadActivityAnalysis(repository)(repository.activity.id)
        repository.failReads = true
        val interaction = ActivityAnalysisInteraction(data)

        repeat(10_000) { index ->
            interaction.selectVisibleFraction((index % 1_001) / 1_000.0)
        }

        assertEquals(ActiveElapsedTimeMillis(19_800L), interaction.snapshot.state.selectedPosition.activeElapsedTime)
    }

    @Test
    fun vvmPerf001And002Profiling_largeLoadedIndexSupportsRepeatedLocalSelection() = runBlocking {
        val repository = analysisRepository()
        val firstPosition = repository.positions.first()
        val firstMetric = repository.trackMetrics[1]
        val lastElapsed = 99_999_000L
        repository.activity = repository.activity.copy(
            endedAt = AbsoluteTimestampUtcMillis(repository.activity.startedAt.value + lastElapsed),
            savedAt = AbsoluteTimestampUtcMillis(repository.activity.startedAt.value + lastElapsed + 1_000L),
            activeDuration = ActiveElapsedTimeMillis(lastElapsed),
        )
        repository.positions = List(100_000) { index ->
            firstPosition.copy(
                sampleIndex = SampleIndex(index.toLong()),
                timestamp = AbsoluteTimestampUtcMillis(repository.activity.startedAt.value + index * 1_000L),
                activeElapsedTime = ActiveElapsedTimeMillis(index * 1_000L),
                elevation = ElevationMetres(10.0 + index % 20),
            )
        }
        repository.trackMetrics = List(100_000) { index ->
            firstMetric.copy(
                positionSampleIndex = SampleIndex(index.toLong()),
                cumulativeDistance = DistanceMetres(index * 2.5),
                pace = PaceSecondsPerKilometre(300.0 + index % 30),
            )
        }
        repository.cadence = emptyList()
        val data = LoadActivityAnalysis(repository)(repository.activity.id)
        repository.failReads = true
        val interaction = ActivityAnalysisInteraction(data)

        repeat(10_000) { index ->
            interaction.selectVisibleFraction((index % 1_001) / 1_000.0)
        }

        assertEquals(100_000, data.distance.points.size)
        assertEquals(ActiveElapsedTimeMillis(98_999_010L), interaction.snapshot.state.selectedPosition.activeElapsedTime)
    }
}
