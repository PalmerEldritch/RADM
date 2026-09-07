package com.jeppe.radm.application.processing

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jeppe.radm.data.M2TestFixtures
import com.jeppe.radm.data.db.RadmDatabase
import com.jeppe.radm.data.db.RadmDatabaseFactory
import com.jeppe.radm.data.repository.ActivityMetadataUpdate
import com.jeppe.radm.data.repository.RoomActivityRepository
import com.jeppe.radm.data.repository.RoomRecordingRepository
import com.jeppe.radm.domain.model.AbsoluteTimestampUtcMillis
import com.jeppe.radm.domain.model.ActivityType
import com.jeppe.radm.domain.model.LatitudeDegrees
import com.jeppe.radm.domain.model.LongitudeDegrees
import com.jeppe.radm.domain.model.ProcessorName
import com.jeppe.radm.domain.model.ProcessorStatus
import com.jeppe.radm.domain.model.RouteSegmentIndex
import com.jeppe.radm.domain.model.StepCounterEpoch
import com.jeppe.radm.domain.processing.CadenceProcessor
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
class RecalculateActivityRoomTest {
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
    fun closeDatabase() = database.close()

    @Test
    fun vvmProc009_roomRecalculationReplacesDerivedDataAndPreservesSource() = runBlocking {
        val activity = M2TestFixtures.activity(801)
        val positions = M2TestFixtures.positions(activity).mapIndexed { index, sample ->
            sample.copy(
                routeSegmentIndex = RouteSegmentIndex(0),
                latitude = LatitudeDegrees(59.32930),
                longitude = LongitudeDegrees(18.06860 + index * 0.001),
            )
        }
        val steps = M2TestFixtures.steps(activity).map { it.copy(counterEpoch = StepCounterEpoch(0)) }
        activityRepository.insert(activity)
        recordingRepository.appendPositions(positions)
        recordingRepository.appendSteps(steps)
        val originalPositions = activityRepository.getPositions(activity.id)
        val originalSteps = activityRepository.getSteps(activity.id)

        val first = RecalculateActivity(activityRepository)(activity.id, PROCESSED_AT)
        val firstMetrics = activityRepository.getTrackMetrics(activity.id)
        val firstSummary = activityRepository.getSummary(activity.id)
        activityRepository.deleteDerivedOutputs(activity.id)
        val second = RecalculateActivity(activityRepository)(activity.id, PROCESSED_AT)

        assertTrue(first.successful)
        assertTrue(second.successful)
        assertEquals(firstMetrics, activityRepository.getTrackMetrics(activity.id))
        assertEquals(firstSummary, activityRepository.getSummary(activity.id))
        assertEquals(originalPositions, activityRepository.getPositions(activity.id))
        assertEquals(originalSteps, activityRepository.getSteps(activity.id))
        PROCESSORS.forEach { assertTrue(activityRepository.isProcessorCurrent(activity.id, it)) }
    }

    @Test
    fun vvmProc010_roomTypeChangeReplacesApplicabilityAndRetainsSource() = runBlocking {
        val activity = M2TestFixtures.activity(802)
        val positions = M2TestFixtures.positions(activity).mapIndexed { index, sample ->
            sample.copy(
                routeSegmentIndex = RouteSegmentIndex(0),
                latitude = LatitudeDegrees(59.32930),
                longitude = LongitudeDegrees(18.06860 + index * 0.001),
            )
        }
        activityRepository.insert(activity)
        recordingRepository.appendPositions(positions)
        recordingRepository.appendSteps(M2TestFixtures.steps(activity))
        RecalculateActivity(activityRepository)(activity.id, PROCESSED_AT)
        val originalPositions = activityRepository.getPositions(activity.id)

        activityRepository.updateMetadata(
            activity.id,
            ActivityMetadataUpdate(ActivityType.CYCLING, activity.title, activity.notes, PROCESSED_AT),
        )
        val result = RecalculateActivity(activityRepository)(activity.id, PROCESSED_AT)

        assertTrue(result.successful)
        assertEquals(originalPositions, activityRepository.getPositions(activity.id))
        assertTrue(activityRepository.getTrackMetrics(activity.id).all { it.pace == null })
        assertTrue(activityRepository.getTrackMetrics(activity.id).any { it.speed != null })
        assertTrue(activityRepository.getCadence(activity.id).isEmpty())
        assertNull(activityRepository.getSummary(activity.id)?.averagePace)
        assertNotNull(activityRepository.getSummary(activity.id)?.averageSpeed)
    }

    @Test
    fun vvmRel002_processorFailureIsPersistedWithoutDeletingSource() = runBlocking {
        val activity = M2TestFixtures.activity(803)
        activityRepository.insert(activity)
        recordingRepository.appendPositions(M2TestFixtures.positions(activity))
        recordingRepository.appendSteps(M2TestFixtures.steps(activity))
        val failure = CadenceProcessor { _, _ -> error("injected") }

        val result = RecalculateActivity(activityRepository, cadenceProcessor = failure)(
            activity.id,
            PROCESSED_AT,
        )

        assertFalse(result.successful)
        assertEquals(ProcessorStatus.FAILED, result.processorStates[ProcessorName.CADENCE])
        assertFalse(activityRepository.isProcessorCurrent(activity.id, ProcessorName.CADENCE))
        assertTrue(activityRepository.isProcessorCurrent(activity.id, ProcessorName.DISTANCE))
        assertTrue(activityRepository.isProcessorCurrent(activity.id, ProcessorName.SUMMARY))
        assertTrue(activityRepository.getPositions(activity.id).isNotEmpty())
        assertTrue(activityRepository.getSteps(activity.id).isNotEmpty())
    }

    private companion object {
        val PROCESSED_AT = AbsoluteTimestampUtcMillis(M2TestFixtures.baseTime.value + 100_000_000L)
        val PROCESSORS = listOf(
            ProcessorName.DISTANCE,
            ProcessorName.PACE,
            ProcessorName.SPEED,
            ProcessorName.CADENCE,
            ProcessorName.SUMMARY,
        )
    }
}
