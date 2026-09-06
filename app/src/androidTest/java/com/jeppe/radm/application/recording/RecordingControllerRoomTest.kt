package com.jeppe.radm.application.recording

import android.content.Context
import androidx.room3.executeSQL
import androidx.room3.useWriterConnection
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jeppe.radm.data.db.RadmDatabase
import com.jeppe.radm.data.db.RadmDatabaseFactory
import com.jeppe.radm.data.repository.RoomActivityRepository
import com.jeppe.radm.data.repository.RoomRecordingRepository
import com.jeppe.radm.domain.model.AbsoluteTimestampUtcMillis
import com.jeppe.radm.domain.model.AccuracyMetres
import com.jeppe.radm.domain.model.ActivityId
import com.jeppe.radm.domain.model.ActivityType
import com.jeppe.radm.domain.model.LatitudeDegrees
import com.jeppe.radm.domain.model.LongitudeDegrees
import com.jeppe.radm.domain.model.MonotonicTimeMillis
import com.jeppe.radm.domain.model.RecordingEventType
import com.jeppe.radm.domain.model.StepCounterEpoch
import com.jeppe.radm.domain.recording.LocationMeasurement
import com.jeppe.radm.domain.recording.RecordingState
import com.jeppe.radm.domain.recording.StepMeasurement
import com.jeppe.radm.platform.fakes.FakeClockSource
import com.jeppe.radm.platform.fakes.FakeLocationSource
import com.jeppe.radm.platform.fakes.FakeStepSource
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecordingControllerRoomTest {
    private lateinit var context: Context
    private lateinit var database: RadmDatabase

    @Before
    fun createDatabase() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(DATABASE_NAME)
        database = RadmDatabaseFactory.create(context, DATABASE_NAME)
    }

    @After
    fun closeDatabase() {
        database.close()
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun m3Lifecycle_startPauseResumeFinishSaveAndReloadFromRoom() = runBlocking {
        val fixture = Fixture(database)
        val activityRepository = RoomActivityRepository(database)

        fixture.controller.start(ActivityType.RUNNING)
        assertEquals(RecordingState.RECORDING, RoomRecordingRepository(database).loadActiveSession()?.state)
        assertEquals(
            listOf(RecordingEventType.START),
            activityRepository.getRecordingEvents(ACTIVITY_ID).map { it.type },
        )

        fixture.clock.advance(1_000L)
        fixture.location.emit(fixture.locationMeasurement(59.3293, 18.0686))
        fixture.steps.emit(fixture.stepMeasurement(28_451L))
        fixture.controller.pause()

        assertEquals(RecordingState.PAUSED, RoomRecordingRepository(database).loadActiveSession()?.state)
        assertEquals(1, activityRepository.getPositions(ACTIVITY_ID).size)
        assertEquals(1, activityRepository.getSteps(ACTIVITY_ID).size)

        fixture.clock.advance(5_000L)
        fixture.location.emitDelayed(fixture.locationMeasurement(60.0, 19.0))
        fixture.steps.emitDelayed(fixture.stepMeasurement(30_000L))
        fixture.controller.resume()
        fixture.clock.advance(2_000L)
        fixture.location.emit(fixture.locationMeasurement(59.3310, 18.0710))
        fixture.steps.emit(fixture.stepMeasurement(28_460L))
        fixture.controller.finish()

        val finalizing = RoomRecordingRepository(database).loadActiveSession()
        assertEquals(RecordingState.FINALIZING, finalizing?.state)
        assertEquals(3_000L, finalizing?.activeElapsedTime?.value)
        assertEquals(listOf(0L, 1L), activityRepository.getPositions(ACTIVITY_ID).map { it.routeSegmentIndex.value })
        assertEquals(
            listOf(
                RecordingEventType.START,
                RecordingEventType.PAUSE,
                RecordingEventType.RESUME,
                RecordingEventType.FINISH,
            ),
            activityRepository.getRecordingEvents(ACTIVITY_ID).map { it.type },
        )

        fixture.clock.advance(1_000L)
        fixture.controller.save(SaveRecordingMetadata(title = "Room lifecycle"))
        assertNull(RoomRecordingRepository(database).loadActiveSession())

        database.close()
        database = RadmDatabaseFactory.create(context, DATABASE_NAME)
        val reloadedActivities = RoomActivityRepository(database)
        val saved = reloadedActivities.get(ACTIVITY_ID)

        assertNotNull(saved)
        assertEquals("Room lifecycle", saved?.title)
        assertEquals(3_000L, saved?.activeDuration?.value)
        assertEquals(listOf(saved), reloadedActivities.listSaved())
        assertEquals(2, reloadedActivities.getPositions(ACTIVITY_ID).size)
        assertEquals(2, reloadedActivities.getSteps(ACTIVITY_ID).size)
        assertEquals(4, reloadedActivities.getRecordingEvents(ACTIVITY_ID).size)
        assertNull(RoomRecordingRepository(database).loadActiveSession())
    }

    @Test
    fun vvmRec009_discardCascadesAndLeavesNoRecoverySession() = runBlocking {
        val fixture = Fixture(database)
        val activities = RoomActivityRepository(database)

        fixture.controller.start(ActivityType.RUNNING)
        fixture.clock.advance(1_000L)
        fixture.location.emit(fixture.locationMeasurement(59.3293, 18.0686))
        fixture.controller.finish()
        fixture.controller.discard()

        assertNull(activities.get(ACTIVITY_ID))
        assertTrue(activities.getPositions(ACTIVITY_ID).isEmpty())
        assertTrue(activities.getRecordingEvents(ACTIVITY_ID).isEmpty())
        assertNull(RoomRecordingRepository(database).loadActiveSession())
    }

    @Test
    fun vvmRel005_saveFailureRollsBackLibraryStateAndSessionRemoval() = runBlocking {
        val fixture = Fixture(database)
        val activities = RoomActivityRepository(database)
        fixture.controller.start(ActivityType.CYCLING)
        fixture.clock.advance(1_000L)
        fixture.controller.finish()
        database.useWriterConnection { connection ->
            connection.executeSQL(
                """
                CREATE TRIGGER fail_m3_session_delete
                BEFORE DELETE ON recording_sessions
                BEGIN
                    SELECT RAISE(ABORT, 'injected finalization failure');
                END
                """.trimIndent(),
            )
        }

        val failure = runCatching { fixture.controller.save() }.exceptionOrNull()

        assertNotNull(failure)
        assertNull(activities.get(ACTIVITY_ID)?.savedAt)
        assertTrue(activities.listSaved().isEmpty())
        assertEquals(RecordingState.FINALIZING, RoomRecordingRepository(database).loadActiveSession()?.state)
        assertEquals(RecordingState.FINALIZING, fixture.controller.snapshot().state)
    }

    private class Fixture(database: RadmDatabase) {
        val clock = FakeClockSource(
            absoluteTime = AbsoluteTimestampUtcMillis(BASE_UTC),
            monotonicTime = MonotonicTimeMillis(10_000L),
        )
        val location = FakeLocationSource()
        val steps = FakeStepSource()
        val controller = RecordingController(
            recordingRepository = RoomRecordingRepository(database),
            locationSource = location,
            stepSource = steps,
            clockSource = clock,
            activityIdSource = ActivityIdSource { ACTIVITY_ID },
        )

        fun locationMeasurement(latitude: Double, longitude: Double) = LocationMeasurement(
            timestamp = clock.absoluteNow(),
            monotonicTimestamp = clock.monotonicNow(),
            latitude = LatitudeDegrees(latitude),
            longitude = LongitudeDegrees(longitude),
            horizontalAccuracy = AccuracyMetres(4.0),
        )

        fun stepMeasurement(cumulativeSteps: Long) = StepMeasurement(
            timestamp = clock.absoluteNow(),
            monotonicTimestamp = clock.monotonicNow(),
            counterEpoch = StepCounterEpoch(0L),
            cumulativeSteps = cumulativeSteps,
        )
    }

    private companion object {
        const val DATABASE_NAME = "radm-m3-recording-test.db"
        const val BASE_UTC = 1_788_379_200_000L
        val ACTIVITY_ID: ActivityId = ActivityId.parse("40000000-0000-4000-8000-000000000303")
    }
}
