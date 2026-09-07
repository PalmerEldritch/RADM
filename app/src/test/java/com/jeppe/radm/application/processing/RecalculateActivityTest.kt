package com.jeppe.radm.application.processing

import com.jeppe.radm.data.repository.ActivityMetadataUpdate
import com.jeppe.radm.data.repository.ActivityRepository
import com.jeppe.radm.domain.model.AbsoluteTimestampUtcMillis
import com.jeppe.radm.domain.model.ActiveElapsedTimeMillis
import com.jeppe.radm.domain.model.Activity
import com.jeppe.radm.domain.model.ActivityId
import com.jeppe.radm.domain.model.ActivityLibraryItem
import com.jeppe.radm.domain.model.ActivityProcessorState
import com.jeppe.radm.domain.model.ActivitySummary
import com.jeppe.radm.domain.model.ActivityType
import com.jeppe.radm.domain.model.CadenceSample
import com.jeppe.radm.domain.model.DerivedTrackMetric
import com.jeppe.radm.domain.model.LatitudeDegrees
import com.jeppe.radm.domain.model.LongitudeDegrees
import com.jeppe.radm.domain.model.PositionSample
import com.jeppe.radm.domain.model.ProcessorDefinition
import com.jeppe.radm.domain.model.ProcessorName
import com.jeppe.radm.domain.model.ProcessorStatus
import com.jeppe.radm.domain.model.ProvenanceType
import com.jeppe.radm.domain.model.RecordingEvent
import com.jeppe.radm.domain.model.RouteSegmentIndex
import com.jeppe.radm.domain.model.SampleIndex
import com.jeppe.radm.domain.model.StepCounterEpoch
import com.jeppe.radm.domain.model.StepSample
import com.jeppe.radm.domain.processing.CadenceProcessor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecalculateActivityTest {
    @Test
    fun `VVM PROC 009 recalculation regenerates deterministic output from retained source`() = runBlocking {
        val repository = FakeActivityRepository(runningActivity(), positions(), steps())
        val useCase = RecalculateActivity(repository)
        val originalPositions = repository.positions.toList()
        val originalSteps = repository.steps.toList()

        val first = useCase(ACTIVITY_ID, PROCESSED_AT)
        val expectedTrack = repository.trackMetrics
        val expectedCadence = repository.cadence
        val expectedSummary = repository.summary
        repository.deleteDerivedOutputs(ACTIVITY_ID)
        val second = useCase(ACTIVITY_ID, PROCESSED_AT)

        assertTrue(first.successful)
        assertTrue(second.successful)
        assertEquals(expectedTrack, repository.trackMetrics)
        assertEquals(expectedCadence, repository.cadence)
        assertEquals(expectedSummary, repository.summary)
        assertEquals(originalPositions, repository.positions)
        assertEquals(originalSteps, repository.steps)
        assertTrue(repository.states.values.all { it.status == ProcessorStatus.CURRENT })
    }

    @Test
    fun `VVM REL 002 optional cadence failure preserves source and unrelated derived metrics`() = runBlocking {
        val repository = FakeActivityRepository(runningActivity(), positions(), steps())
        val failingCadence = CadenceProcessor { _, _ -> error("injected cadence failure") }

        val result = RecalculateActivity(repository, cadenceProcessor = failingCadence)(
            ACTIVITY_ID,
            PROCESSED_AT,
        )

        assertFalse(result.successful)
        assertEquals(ProcessorStatus.FAILED, result.processorStates[ProcessorName.CADENCE])
        assertEquals(ProcessorStatus.CURRENT, result.processorStates[ProcessorName.DISTANCE])
        assertEquals(ProcessorStatus.CURRENT, result.processorStates[ProcessorName.SUMMARY])
        assertTrue(repository.positions.isNotEmpty())
        assertTrue(repository.steps.isNotEmpty())
        assertTrue(repository.trackMetrics.isNotEmpty())
        assertNotNull(repository.summary)
        assertTrue(repository.cadence.isEmpty())
    }

    @Test
    fun `VVM PROC 010 Running to Cycling reprocessing changes applicability without source edits`() = runBlocking {
        val repository = FakeActivityRepository(runningActivity(), positions(), steps())
        val useCase = RecalculateActivity(repository)
        useCase(ACTIVITY_ID, PROCESSED_AT)
        val originalPositions = repository.positions.toList()
        val originalSteps = repository.steps.toList()

        repository.updateMetadata(
            ACTIVITY_ID,
            ActivityMetadataUpdate(ActivityType.CYCLING, null, null, PROCESSED_AT),
        )
        val result = useCase(ACTIVITY_ID, PROCESSED_AT)

        assertTrue(result.successful)
        assertEquals(originalPositions, repository.positions)
        assertEquals(originalSteps, repository.steps)
        assertTrue(repository.trackMetrics.all { it.pace == null })
        assertTrue(repository.trackMetrics.any { it.speed != null })
        assertTrue(repository.cadence.isEmpty())
        assertNull(repository.summary?.averagePace)
        assertNotNull(repository.summary?.averageSpeed)
    }

    @Test
    fun `DMS processor currentness requires current status and matching definition version`() = runBlocking {
        val repository = FakeActivityRepository(runningActivity(), positions(), steps())
        RecalculateActivity(repository)(ACTIVITY_ID, PROCESSED_AT)

        assertTrue(repository.isProcessorCurrent(ACTIVITY_ID, ProcessorName.PACE))
        repository.updateProcessorVersion(ProcessorName.PACE, 2)

        assertFalse(repository.isProcessorCurrent(ACTIVITY_ID, ProcessorName.PACE))
        assertTrue(repository.trackMetrics.isNotEmpty())
    }

    private fun runningActivity() = Activity(
        id = ACTIVITY_ID,
        provenanceType = ProvenanceType.RADM_NATIVE,
        type = ActivityType.RUNNING,
        startedAt = AbsoluteTimestampUtcMillis(BASE_TIME),
        endedAt = AbsoluteTimestampUtcMillis(BASE_TIME + 20_000),
        savedAt = AbsoluteTimestampUtcMillis(BASE_TIME + 21_000),
        activeDuration = ActiveElapsedTimeMillis(20_000),
        createdAt = AbsoluteTimestampUtcMillis(BASE_TIME),
        updatedAt = AbsoluteTimestampUtcMillis(BASE_TIME),
    )

    private fun positions() = (0L..4L).map { index ->
        PositionSample(
            activityId = ACTIVITY_ID,
            sampleIndex = SampleIndex(index),
            routeSegmentIndex = RouteSegmentIndex(0),
            timestamp = AbsoluteTimestampUtcMillis(BASE_TIME + index * 5_000),
            activeElapsedTime = ActiveElapsedTimeMillis(index * 5_000),
            latitude = LatitudeDegrees(59.0),
            longitude = LongitudeDegrees(18.0 + index * 0.0001),
        )
    }

    private fun steps() = listOf(
        step(0, 0, 0, 100),
        step(1, 0, 10_000, 120),
        step(2, 0, 20_000, 140),
    )

    private fun step(index: Long, epoch: Long, elapsed: Long, count: Long) = StepSample(
        activityId = ACTIVITY_ID,
        sampleIndex = SampleIndex(index),
        counterEpoch = StepCounterEpoch(epoch),
        timestamp = AbsoluteTimestampUtcMillis(BASE_TIME + elapsed),
        activeElapsedTime = ActiveElapsedTimeMillis(elapsed),
        cumulativeSteps = count,
    )

    private companion object {
        const val BASE_TIME = 1_788_379_200_000L
        val ACTIVITY_ID = ActivityId.parse("50000000-0000-4000-8000-000000000001")
        val PROCESSED_AT = AbsoluteTimestampUtcMillis(BASE_TIME + 30_000)
    }
}

private class FakeActivityRepository(
    initialActivity: Activity,
    val positions: List<PositionSample>,
    val steps: List<StepSample>,
) : ActivityRepository {
    private var activity: Activity? = initialActivity
    var summary: ActivitySummary? = null
    var trackMetrics: List<DerivedTrackMetric> = emptyList()
    var cadence: List<CadenceSample> = emptyList()
    val definitions = mutableMapOf<ProcessorName, ProcessorDefinition>()
    val states = mutableMapOf<ProcessorName, ActivityProcessorState>()

    init {
        listOf(
            ProcessorName.DISTANCE,
            ProcessorName.PACE,
            ProcessorName.SPEED,
            ProcessorName.CADENCE,
            ProcessorName.SUMMARY,
        ).forEach { definitions[it] = ProcessorDefinition(it, 1) }
    }

    override suspend fun insert(activity: Activity) {
        this.activity = activity
    }

    override suspend fun get(activityId: ActivityId): Activity? = activity?.takeIf { it.id == activityId }
    override suspend fun listSaved(): List<Activity> = listOfNotNull(activity)
    override suspend fun listLibraryItems(): List<ActivityLibraryItem> = listOfNotNull(
        activity?.takeIf { it.savedAt != null }?.let { saved ->
            ActivityLibraryItem(
                activityId = saved.id,
                activityType = saved.type,
                title = saved.title,
                startedAt = saved.startedAt,
                activeDuration = saved.activeDuration,
                distance = summary?.distance,
                averagePace = summary?.averagePace,
                averageSpeed = summary?.averageSpeed,
            )
        },
    )

    override suspend fun updateMetadata(activityId: ActivityId, update: ActivityMetadataUpdate): Boolean {
        val current = activity?.takeIf { it.id == activityId } ?: return false
        activity = current.copy(
            type = update.type,
            title = update.title,
            notes = update.notes,
            updatedAt = update.updatedAt,
        )
        return true
    }

    override suspend fun delete(activityId: ActivityId): Boolean {
        if (activity?.id != activityId) return false
        activity = null
        return true
    }

    override suspend fun putSummary(summary: ActivitySummary) {
        this.summary = summary
    }

    override suspend fun getSummary(activityId: ActivityId): ActivitySummary? =
        summary?.takeIf { it.activityId == activityId }

    override suspend fun getPositions(activityId: ActivityId) = positions.filter { it.activityId == activityId }
    override suspend fun getSteps(activityId: ActivityId) = steps.filter { it.activityId == activityId }
    override suspend fun getRecordingEvents(activityId: ActivityId): List<RecordingEvent> = emptyList()
    override suspend fun getTrackMetrics(activityId: ActivityId) =
        trackMetrics.filter { it.activityId == activityId }

    override suspend fun getCadence(activityId: ActivityId) = cadence.filter { it.activityId == activityId }

    override suspend fun putProcessorDefinition(definition: ProcessorDefinition) {
        definitions[definition.name] = definition
    }

    override suspend fun getProcessorDefinition(name: ProcessorName) = definitions[name]

    override suspend fun updateProcessorVersion(name: ProcessorName, version: Int): Boolean {
        val current = definitions[name] ?: return false
        definitions[name] = current.copy(currentVersion = version)
        return true
    }

    override suspend fun getProcessorStates(activityId: ActivityId) =
        states.values.filter { it.activityId == activityId }.sortedBy { it.processorName.value }

    override suspend fun isProcessorCurrent(activityId: ActivityId, name: ProcessorName): Boolean {
        val state = states[name]?.takeIf { it.activityId == activityId } ?: return false
        val definition = definitions[name] ?: return false
        return state.isCurrentAgainst(definition)
    }

    override suspend fun putProcessorStates(states: List<ActivityProcessorState>) {
        states.forEach { this.states[it.processorName] = it }
    }

    override suspend fun replaceTrackMetrics(
        activityId: ActivityId,
        metrics: List<DerivedTrackMetric>,
        processorStates: List<ActivityProcessorState>,
    ) {
        trackMetrics = metrics
        putProcessorStates(processorStates)
    }

    override suspend fun replaceCadence(
        activityId: ActivityId,
        samples: List<CadenceSample>,
        processorState: ActivityProcessorState,
    ) {
        cadence = samples
        putProcessorStates(listOf(processorState))
    }

    override suspend fun replaceSummary(
        activityId: ActivityId,
        summary: ActivitySummary,
        processorState: ActivityProcessorState,
    ) {
        this.summary = summary
        putProcessorStates(listOf(processorState))
    }

    override suspend fun deleteDerivedOutputs(activityId: ActivityId) {
        trackMetrics = emptyList()
        cadence = emptyList()
        summary = null
        states.clear()
    }
}
