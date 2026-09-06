package com.jeppe.radm.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jeppe.radm.data.db.RadmDatabase
import com.jeppe.radm.data.db.RadmDatabaseFactory
import com.jeppe.radm.data.db.toEntity
import com.jeppe.radm.data.db.entity.RecordingSessionEntity
import com.jeppe.radm.data.repository.ActivityMetadataUpdate
import com.jeppe.radm.data.repository.RoomActivityRepository
import com.jeppe.radm.data.repository.RoomRecordingRepository
import com.jeppe.radm.domain.model.AbsoluteTimestampUtcMillis
import com.jeppe.radm.domain.model.Activity
import com.jeppe.radm.domain.model.ActivityType
import com.jeppe.radm.domain.model.ProcessorName
import com.jeppe.radm.domain.model.SampleIndex
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RadmDatabaseTest {
    private lateinit var database: RadmDatabase
    private lateinit var activityRepository: RoomActivityRepository
    private lateinit var recordingRepository: RoomRecordingRepository

    @Before
    fun createDatabase() {
        database = RadmDatabaseFactory.createInMemory(ApplicationProvider.getApplicationContext())
        activityRepository = RoomActivityRepository(database)
        recordingRepository = RoomRecordingRepository(database)
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun vvmDb001And002_cleanSchemaContainsEveryRequiredTableAndIndex() = runBlocking {
        val expectedTables = setOf(
            "activities",
            "recording_sessions",
            "recording_events",
            "position_samples",
            "step_samples",
            "derived_track_metrics",
            "derived_cadence",
            "activity_summaries",
            "processor_definitions",
            "activity_processor_state",
        )
        val expectedIndexes = setOf(
            "idx_activities_saved_start",
            "idx_activities_type_start",
            "idx_position_activity_elapsed",
            "idx_position_activity_segment_sample",
            "idx_step_activity_elapsed",
            "idx_cadence_activity_elapsed",
            "idx_recording_event_activity_elapsed",
        )
        val expectedTriggers = setOf(
            "trg_recording_sessions_singleton_insert",
            "trg_recording_sessions_singleton_update",
            "trg_activities_uuid_insert",
            "trg_activities_uuid_update",
        )

        assertTrue(database.schemaDao().listSchemaObjects("table").containsAll(expectedTables))
        assertTrue(database.schemaDao().listSchemaObjects("index").containsAll(expectedIndexes))
        assertTrue(database.schemaDao().listSchemaObjects("trigger").containsAll(expectedTriggers))
        assertEquals(0, database.schemaDao().recordingSessionCount())
    }

    @Test
    fun vvmDb003_uuidIdentityIsValidUniqueStableAndDatabaseGuarded() = runBlocking {
        val activities = (1..3).map { M2TestFixtures.activity(it) }
        activities.forEach { activityRepository.insert(it) }

        assertEquals(activities.map { it.id }, activities.map { activityRepository.get(it.id)?.id })
        expectFailure { activityRepository.insert(activities.first()) }

        val first = activities.first()
        activityRepository.updateMetadata(
            first.id,
            ActivityMetadataUpdate(
                type = ActivityType.CYCLING,
                title = "Renamed",
                notes = "Updated",
                updatedAt = AbsoluteTimestampUtcMillis(M2TestFixtures.baseTime.value + 1_000L),
            ),
        )
        assertEquals(first.id, activityRepository.get(first.id)?.id)

        expectFailure {
            database.activityDao().insertActivity(first.toEntity().copy(activityId = "INVALID-ID"))
        }
    }

    @Test
    fun vvmDb004_onlyOneUnresolvedRecordingCanExist() = runBlocking {
        val first = M2TestFixtures.activity(10, saved = false)
        val second = M2TestFixtures.activity(11, saved = false)
        recordingRepository.createSession(first, M2TestFixtures.session(first), M2TestFixtures.events(first).single())

        expectFailure {
            recordingRepository.createSession(
                second,
                M2TestFixtures.session(second),
                M2TestFixtures.events(second).single(),
            )
        }

        assertEquals(first.id, recordingRepository.loadActiveSession()?.activityId)
        assertNull(activityRepository.get(second.id))
        expectFailure {
            database.recordingDao().insertSession(
                M2TestFixtures.session(first).toEntity().copy(singletonId = 2),
            )
        }
        assertEquals(RecordingSessionEntity.SINGLETON_ID, database.recordingDao().getSession()?.singletonId)
    }

    @Test
    fun vvmDb005_cascadingDeleteRemovesOnlySelectedActivityChildren() = runBlocking {
        val deleted = M2TestFixtures.activity(20)
        val retained = M2TestFixtures.activity(21)
        populateCompleteActivity(deleted)
        populateCompleteActivity(retained)

        assertTrue(activityRepository.delete(deleted.id))

        assertNull(activityRepository.get(deleted.id))
        assertTrue(activityRepository.getPositions(deleted.id).isEmpty())
        assertTrue(activityRepository.getSteps(deleted.id).isEmpty())
        assertTrue(activityRepository.getRecordingEvents(deleted.id).isEmpty())
        assertTrue(activityRepository.getTrackMetrics(deleted.id).isEmpty())
        assertTrue(activityRepository.getCadence(deleted.id).isEmpty())
        assertNull(activityRepository.getSummary(deleted.id))
        assertTrue(activityRepository.getProcessorStates(deleted.id).isEmpty())

        assertEquals(retained, activityRepository.get(retained.id))
        assertEquals(M2TestFixtures.positions(retained), activityRepository.getPositions(retained.id))
        assertEquals(M2TestFixtures.steps(retained), activityRepository.getSteps(retained.id))
        assertEquals(M2TestFixtures.events(retained), activityRepository.getRecordingEvents(retained.id))
        assertEquals(M2TestFixtures.trackMetrics(retained), activityRepository.getTrackMetrics(retained.id))
        assertEquals(M2TestFixtures.cadence(retained), activityRepository.getCadence(retained.id))
        assertEquals(M2TestFixtures.summary(retained), activityRepository.getSummary(retained.id))
    }

    @Test
    fun vvmDb006_deletingDerivedOutputPreservesSourceStreamsUnchanged() = runBlocking {
        val activity = M2TestFixtures.activity(30)
        populateCompleteActivity(activity)
        val positions = activityRepository.getPositions(activity.id)
        val steps = activityRepository.getSteps(activity.id)
        val events = activityRepository.getRecordingEvents(activity.id)

        activityRepository.deleteDerivedOutputs(activity.id)

        assertEquals(positions, activityRepository.getPositions(activity.id))
        assertEquals(steps, activityRepository.getSteps(activity.id))
        assertEquals(events, activityRepository.getRecordingEvents(activity.id))
        assertTrue(activityRepository.getTrackMetrics(activity.id).isEmpty())
        assertTrue(activityRepository.getCadence(activity.id).isEmpty())
        assertNull(activityRepository.getSummary(activity.id))
        assertTrue(activityRepository.getProcessorStates(activity.id).isEmpty())
    }

    @Test
    fun vvmDb007_metadataEditsLeaveEverySourceStreamUnchanged() = runBlocking {
        val activity = M2TestFixtures.activity(40)
        populateCompleteActivity(activity)
        val positions = activityRepository.getPositions(activity.id)
        val steps = activityRepository.getSteps(activity.id)
        val events = activityRepository.getRecordingEvents(activity.id)

        assertTrue(
            activityRepository.updateMetadata(
                activity.id,
                ActivityMetadataUpdate(
                    type = ActivityType.CYCLING,
                    title = "Changed title",
                    notes = null,
                    updatedAt = AbsoluteTimestampUtcMillis(M2TestFixtures.baseTime.value + 5_000L),
                ),
            ),
        )

        assertEquals(positions, activityRepository.getPositions(activity.id))
        assertEquals(steps, activityRepository.getSteps(activity.id))
        assertEquals(events, activityRepository.getRecordingEvents(activity.id))
        assertEquals(activity.id, activityRepository.get(activity.id)?.id)
    }

    @Test
    fun vvmDb008_versionMismatchMakesRetainedOutputStale() = runBlocking {
        val activity = M2TestFixtures.activity(50)
        activityRepository.insert(activity)
        recordingRepository.appendPositions(M2TestFixtures.positions(activity))
        activityRepository.replaceTrackMetrics(
            activity.id,
            M2TestFixtures.trackMetrics(activity),
            listOf(M2TestFixtures.processorState(activity, ProcessorName.DISTANCE)),
        )

        assertTrue(activityRepository.isProcessorCurrent(activity.id, ProcessorName.DISTANCE))
        assertTrue(activityRepository.updateProcessorVersion(ProcessorName.DISTANCE, 2))
        assertFalse(activityRepository.isProcessorCurrent(activity.id, ProcessorName.DISTANCE))
        assertEquals(M2TestFixtures.trackMetrics(activity), activityRepository.getTrackMetrics(activity.id))
    }

    @Test
    fun vvmDb009_failedReplacementRollsBackOutputAndCurrentState() = runBlocking {
        val activity = M2TestFixtures.activity(60)
        activityRepository.insert(activity)
        recordingRepository.appendPositions(M2TestFixtures.positions(activity))
        val originalMetrics = M2TestFixtures.trackMetrics(activity)
        val originalState = M2TestFixtures.processorState(activity, ProcessorName.DISTANCE)
        activityRepository.replaceTrackMetrics(activity.id, originalMetrics, listOf(originalState))

        val invalidMetrics = originalMetrics.mapIndexed { index, metric ->
            if (index == 1) metric.copy(positionSampleIndex = SampleIndex(999)) else metric
        }
        expectFailure {
            activityRepository.replaceTrackMetrics(
                activity.id,
                invalidMetrics,
                listOf(originalState.copy(processorVersion = 2)),
            )
        }

        assertEquals(originalMetrics, activityRepository.getTrackMetrics(activity.id))
        assertEquals(listOf(originalState), activityRepository.getProcessorStates(activity.id))
        assertTrue(activityRepository.isProcessorCurrent(activity.id, ProcessorName.DISTANCE))
        assertEquals(M2TestFixtures.positions(activity), activityRepository.getPositions(activity.id))
    }

    @Test
    fun repositoryRoundTrips_preserveNullableSourceValuesSegmentsAndEpochs() = runBlocking {
        val activity = M2TestFixtures.activity(70)
        populateCompleteActivity(activity)

        assertEquals(activity, activityRepository.get(activity.id))
        assertEquals(listOf(activity), activityRepository.listSaved())
        assertNull(activityRepository.getPositions(activity.id)[1].elevation)
        assertEquals(1L, activityRepository.getPositions(activity.id)[1].routeSegmentIndex.value)
        assertEquals(1L, activityRepository.getSteps(activity.id)[1].counterEpoch.value)
        assertNotNull(activityRepository.getProcessorDefinition(ProcessorName.SUMMARY))
    }

    private suspend fun populateCompleteActivity(activity: Activity) {
        activityRepository.insert(activity)
        recordingRepository.appendEvents(M2TestFixtures.events(activity))
        recordingRepository.appendPositions(M2TestFixtures.positions(activity))
        recordingRepository.appendSteps(M2TestFixtures.steps(activity))
        activityRepository.putSummary(M2TestFixtures.summary(activity))
        activityRepository.replaceTrackMetrics(
            activity.id,
            M2TestFixtures.trackMetrics(activity),
            listOf(M2TestFixtures.processorState(activity, ProcessorName.DISTANCE)),
        )
        activityRepository.replaceCadence(
            activity.id,
            M2TestFixtures.cadence(activity),
            M2TestFixtures.processorState(activity, ProcessorName.CADENCE),
        )
    }

    private suspend fun expectFailure(block: suspend () -> Unit) {
        var failure: Throwable? = null
        try {
            block()
        } catch (caught: Throwable) {
            failure = caught
        }
        assertNotNull("Expected persistence operation to fail", failure)
    }
}
