package com.jeppe.radm.application.analysis

import com.jeppe.radm.data.repository.ActivityMetadataUpdate
import com.jeppe.radm.data.repository.ActivityRepository
import com.jeppe.radm.domain.analysis.AnalysisCoordinateMode
import com.jeppe.radm.domain.analysis.AnalysisProcessorStatus
import com.jeppe.radm.domain.analysis.AnalysisSeriesAvailability
import com.jeppe.radm.domain.model.AbsoluteTimestampUtcMillis
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
import com.jeppe.radm.domain.model.LatitudeDegrees
import com.jeppe.radm.domain.model.LongitudeDegrees
import com.jeppe.radm.domain.model.PaceSecondsPerKilometre
import com.jeppe.radm.domain.model.PositionSample
import com.jeppe.radm.domain.model.ProcessorDefinition
import com.jeppe.radm.domain.model.ProcessorName
import com.jeppe.radm.domain.model.ProcessorStatus
import com.jeppe.radm.domain.model.ProvenanceType
import com.jeppe.radm.domain.model.RecordingEvent
import com.jeppe.radm.domain.model.RouteSegmentIndex
import com.jeppe.radm.domain.model.SampleIndex
import com.jeppe.radm.domain.model.SpeedMetresPerSecond
import com.jeppe.radm.domain.model.StepCounterEpoch
import com.jeppe.radm.domain.model.StepSample
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class LoadActivityAnalysisTest {
    @Test
    fun vvmAn001_loadsRepresentativeActivityAndInitializesFullDistanceState() = runBlocking {
        val repository = analysisRepository()

        val data = LoadActivityAnalysis(repository)(repository.activity.id)

        assertSame(repository.activity, data.activity)
        assertSame(repository.summary, data.summary)
        assertEquals(1, data.routeSegments.size)
        assertEquals(3, data.routeSegments.single().samples.size)
        assertEquals(AnalysisSeriesAvailability.AVAILABLE, data.distance.availability)
        assertEquals(AnalysisSeriesAvailability.AVAILABLE, data.pace.availability)
        assertEquals(AnalysisSeriesAvailability.UNAVAILABLE_NOT_APPLICABLE, data.speed.availability)
        assertEquals(AnalysisSeriesAvailability.AVAILABLE, data.elevation.availability)
        assertEquals(AnalysisSeriesAvailability.AVAILABLE, data.cadence.availability)
        assertEquals(
            AnalysisProcessorStatus.NOT_APPLICABLE,
            data.processorValidity.getValue(ProcessorName.SPEED).status,
        )
        assertEquals(AnalysisCoordinateMode.DISTANCE, data.initialState.coordinateMode)
        assertEquals(ActiveElapsedTimeMillis.ZERO, data.initialState.selectedPosition.activeElapsedTime)
        assertEquals(DistanceMetres.ZERO, data.initialState.selectedPosition.cumulativeDistance)
        assertEquals(ActiveElapsedTimeMillis(20_000L), data.initialState.range.endInclusive.activeElapsedTime)
        assertEquals(DistanceMetres(200.0), data.initialState.range.endInclusive.cumulativeDistance)
        assertEquals(ElevationMetres(10.0), data.initialInspector.elevation)
        assertNull(data.initialInspector.pace)
        assertNull(data.initialInspector.cadence)

        repository.failReads = true
        assertEquals(3, data.distance.points.size)
        assertEquals(1, data.cadence.points.size)
    }

    @Test
    fun staleDerivedStreamIsNotExposedAsCurrent() = runBlocking {
        val repository = analysisRepository()
        repository.definitions[ProcessorName.PACE] = ProcessorDefinition(ProcessorName.PACE, 2)

        val data = LoadActivityAnalysis(repository)(repository.activity.id)

        assertEquals(AnalysisProcessorStatus.STALE, data.processorValidity.getValue(ProcessorName.PACE).status)
        assertEquals(AnalysisSeriesAvailability.UNAVAILABLE_NOT_CURRENT, data.pace.availability)
        assertEquals(emptyList<Any>(), data.pace.points)
        assertEquals(AnalysisSeriesAvailability.AVAILABLE, data.distance.availability)
        assertEquals(AnalysisSeriesAvailability.AVAILABLE, data.elevation.availability)
    }

    @Test
    fun vvmAn010_missingRunningCadenceIsExplicitAndDoesNotCollapseOtherMetrics() = runBlocking {
        val repository = analysisRepository().apply { cadence = emptyList() }

        val data = LoadActivityAnalysis(repository)(repository.activity.id)

        assertEquals(AnalysisSeriesAvailability.UNAVAILABLE_NO_SOURCE, data.cadence.availability)
        assertEquals(AnalysisSeriesAvailability.AVAILABLE, data.pace.availability)
        assertEquals(AnalysisSeriesAvailability.AVAILABLE, data.elevation.availability)
        assertEquals(1, data.routeSegments.size)
    }

    @Test
    fun vvmAn011_noRouteKeepsCadenceMetadataAndElapsedCoordinateUsable() = runBlocking {
        val repository = analysisRepository().apply {
            positions = emptyList()
            trackMetrics = emptyList()
            summary = summary.copy(
                distance = null,
                averagePace = null,
                minimumElevation = null,
                maximumElevation = null,
            )
        }

        val data = LoadActivityAnalysis(repository)(repository.activity.id)

        assertEquals(emptyList<Any>(), data.routeSegments)
        assertEquals(AnalysisSeriesAvailability.UNAVAILABLE_NO_SOURCE, data.distance.availability)
        assertEquals(AnalysisSeriesAvailability.UNAVAILABLE_NO_SOURCE, data.pace.availability)
        assertEquals(AnalysisSeriesAvailability.UNAVAILABLE_NO_SOURCE, data.elevation.availability)
        assertEquals(AnalysisSeriesAvailability.AVAILABLE, data.cadence.availability)
        assertEquals(AnalysisCoordinateMode.ACTIVE_ELAPSED_TIME, data.initialState.coordinateMode)
        assertSame(repository.activity, data.activity)
        assertSame(repository.summary, data.summary)
    }

    @Test
    fun activityTypeControlsPaceSpeedAndCadenceApplicability() = runBlocking {
        val cycling = analysisRepository().apply {
            activity = activity.copy(type = ActivityType.CYCLING)
            summary = summary.copy(
                averagePace = null,
                averageSpeed = SpeedMetresPerSecond(3.0),
            )
        }

        val cyclingData = LoadActivityAnalysis(cycling)(cycling.activity.id)

        assertEquals(AnalysisSeriesAvailability.UNAVAILABLE_NOT_APPLICABLE, cyclingData.pace.availability)
        assertEquals(AnalysisSeriesAvailability.AVAILABLE, cyclingData.speed.availability)
        assertEquals(AnalysisSeriesAvailability.UNAVAILABLE_NOT_APPLICABLE, cyclingData.cadence.availability)

        val skiing = analysisRepository().apply {
            activity = activity.copy(type = ActivityType.CROSS_COUNTRY_SKIING)
        }

        val skiingData = LoadActivityAnalysis(skiing)(skiing.activity.id)

        assertEquals(AnalysisSeriesAvailability.AVAILABLE, skiingData.pace.availability)
        assertEquals(AnalysisSeriesAvailability.UNAVAILABLE_NOT_APPLICABLE, skiingData.speed.availability)
        assertEquals(AnalysisSeriesAvailability.UNAVAILABLE_NOT_APPLICABLE, skiingData.cadence.availability)
    }

    @Test
    fun distanceCoordinateIsNotInterpolatedAcrossRouteDiscontinuity() = runBlocking {
        val repository = analysisRepository().apply {
            positions = positions.mapIndexed { index, position ->
                if (index == positions.lastIndex) {
                    position.copy(routeSegmentIndex = RouteSegmentIndex(1))
                } else {
                    position
                }
            }
            cadence = listOf(
                CadenceSample(
                    activity.id,
                    SampleIndex(1),
                    ActiveElapsedTimeMillis(15_000L),
                    CadenceStepsPerMinute(170.0),
                ),
            )
        }

        val data = LoadActivityAnalysis(repository)(repository.activity.id)

        assertEquals(2, data.routeSegments.size)
        assertNull(data.cadence.points.single().position.cumulativeDistance)
    }
}

private fun analysisRepository(): AnalysisRepositoryFake {
    val id = ActivityId.parse("40000000-0000-4000-8000-000000000010")
    val startedAt = AbsoluteTimestampUtcMillis(1_788_379_200_000L)
    val activity = Activity(
        id = id,
        provenanceType = ProvenanceType.RADM_NATIVE,
        type = ActivityType.RUNNING,
        title = "Representative run",
        notes = null,
        startedAt = startedAt,
        endedAt = AbsoluteTimestampUtcMillis(startedAt.value + 20_000L),
        savedAt = AbsoluteTimestampUtcMillis(startedAt.value + 21_000L),
        activeDuration = ActiveElapsedTimeMillis(20_000L),
        createdAt = startedAt,
        updatedAt = startedAt,
    )
    val positions = listOf(0L, 10_000L, 20_000L).mapIndexed { index, elapsed ->
        PositionSample(
            activityId = id,
            sampleIndex = SampleIndex(index.toLong()),
            routeSegmentIndex = RouteSegmentIndex(0),
            timestamp = AbsoluteTimestampUtcMillis(startedAt.value + elapsed),
            activeElapsedTime = ActiveElapsedTimeMillis(elapsed),
            latitude = LatitudeDegrees(59.0 + index * 0.001),
            longitude = LongitudeDegrees(18.0 + index * 0.001),
            elevation = if (index < 2) ElevationMetres(10.0 + index) else null,
        )
    }
    val metrics = listOf(
        DerivedTrackMetric(id, SampleIndex(0), DistanceMetres.ZERO, null, null),
        DerivedTrackMetric(
            id,
            SampleIndex(1),
            DistanceMetres(100.0),
            PaceSecondsPerKilometre(300.0),
            SpeedMetresPerSecond(3.333),
        ),
        DerivedTrackMetric(
            id,
            SampleIndex(2),
            DistanceMetres(200.0),
            PaceSecondsPerKilometre(360.0),
            SpeedMetresPerSecond(2.778),
        ),
    )
    val steps = listOf(
        StepSample(
            id,
            SampleIndex(1),
            StepCounterEpoch(0),
            AbsoluteTimestampUtcMillis(startedAt.value + 10_000L),
            ActiveElapsedTimeMillis(10_000L),
            100,
        ),
    )
    val cadence = listOf(
        CadenceSample(
            id,
            SampleIndex(1),
            ActiveElapsedTimeMillis(10_000L),
            CadenceStepsPerMinute(168.0),
        ),
    )
    val summary = ActivitySummary(
        id,
        DistanceMetres(200.0),
        PaceSecondsPerKilometre(330.0),
        null,
        ElevationMetres(10.0),
        ElevationMetres(11.0),
        null,
    )
    return AnalysisRepositoryFake(activity, positions, steps, metrics, cadence, summary)
}

private class AnalysisRepositoryFake(
    var activity: Activity,
    var positions: List<PositionSample>,
    private var steps: List<StepSample>,
    var trackMetrics: List<DerivedTrackMetric>,
    var cadence: List<CadenceSample>,
    var summary: ActivitySummary,
) : ActivityRepository {
    var failReads = false
    val definitions = listOf(
        ProcessorName.DISTANCE,
        ProcessorName.PACE,
        ProcessorName.SPEED,
        ProcessorName.CADENCE,
        ProcessorName.SUMMARY,
    ).associateWith { ProcessorDefinition(it, 1) }.toMutableMap()
    private val states = definitions.keys.associateWith {
        ActivityProcessorState(activity.id, it, 1, ProcessorStatus.CURRENT, activity.savedAt)
    }

    private fun checkReadable() = check(!failReads) { "Repository was read after analysis load" }

    override suspend fun get(activityId: ActivityId): Activity? = activity.also { checkReadable() }
    override suspend fun getSummary(activityId: ActivityId): ActivitySummary = summary.also { checkReadable() }
    override suspend fun getPositions(activityId: ActivityId) = positions.also { checkReadable() }
    override suspend fun getSteps(activityId: ActivityId) = steps.also { checkReadable() }
    override suspend fun getTrackMetrics(activityId: ActivityId) = trackMetrics.also { checkReadable() }
    override suspend fun getCadence(activityId: ActivityId) = cadence.also { checkReadable() }
    override suspend fun getProcessorDefinition(name: ProcessorName) = definitions[name].also { checkReadable() }
    override suspend fun getProcessorStates(activityId: ActivityId) = states.values.toList().also { checkReadable() }

    override suspend fun insert(activity: Activity): Unit = unsupported()
    override suspend fun listSaved(): List<Activity> = unsupported()
    override suspend fun listLibraryItems(): List<ActivityLibraryItem> = unsupported()
    override suspend fun updateMetadata(activityId: ActivityId, update: ActivityMetadataUpdate): Boolean = unsupported()
    override suspend fun delete(activityId: ActivityId): Boolean = unsupported()
    override suspend fun putSummary(summary: ActivitySummary): Unit = unsupported()
    override suspend fun getRecordingEvents(activityId: ActivityId): List<RecordingEvent> = unsupported()
    override suspend fun putProcessorDefinition(definition: ProcessorDefinition): Unit = unsupported()
    override suspend fun updateProcessorVersion(name: ProcessorName, version: Int): Boolean = unsupported()
    override suspend fun isProcessorCurrent(activityId: ActivityId, name: ProcessorName): Boolean = unsupported()
    override suspend fun putProcessorStates(states: List<ActivityProcessorState>): Unit = unsupported()
    override suspend fun replaceTrackMetrics(
        activityId: ActivityId,
        metrics: List<DerivedTrackMetric>,
        processorStates: List<ActivityProcessorState>,
    ): Unit = unsupported()
    override suspend fun replaceCadence(
        activityId: ActivityId,
        samples: List<CadenceSample>,
        processorState: ActivityProcessorState,
    ): Unit = unsupported()
    override suspend fun replaceSummary(
        activityId: ActivityId,
        summary: ActivitySummary,
        processorState: ActivityProcessorState,
    ): Unit = unsupported()
    override suspend fun deleteDerivedOutputs(activityId: ActivityId): Unit = unsupported()

    private fun <T> unsupported(): T = error("Not required by this test")
}
