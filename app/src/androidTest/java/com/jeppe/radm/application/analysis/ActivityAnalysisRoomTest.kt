package com.jeppe.radm.application.analysis

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jeppe.radm.data.M2TestFixtures
import com.jeppe.radm.data.db.RadmDatabase
import com.jeppe.radm.data.db.RadmDatabaseFactory
import com.jeppe.radm.data.repository.RoomActivityRepository
import com.jeppe.radm.data.repository.RoomRecordingRepository
import com.jeppe.radm.domain.analysis.AnalysisProcessorStatus
import com.jeppe.radm.domain.analysis.AnalysisSeriesAvailability
import com.jeppe.radm.domain.model.ProcessorName
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActivityAnalysisRoomTest {
    private lateinit var database: RadmDatabase
    private lateinit var activities: RoomActivityRepository
    private lateinit var recordings: RoomRecordingRepository

    @Before
    fun createDatabase() {
        database = RadmDatabaseFactory.createInMemory(ApplicationProvider.getApplicationContext())
        activities = RoomActivityRepository(database)
        recordings = RoomRecordingRepository(database)
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun vvmAn001_roomBackedLoaderReturnsCurrentLocalAnalysisData() = runBlocking {
        val activity = M2TestFixtures.activity(1_001)
        activities.insert(activity)
        recordings.appendPositions(M2TestFixtures.positions(activity))
        recordings.appendSteps(M2TestFixtures.steps(activity))
        activities.replaceTrackMetrics(
            activity.id,
            M2TestFixtures.trackMetrics(activity),
            listOf(
                M2TestFixtures.processorState(activity, ProcessorName.DISTANCE),
                M2TestFixtures.processorState(activity, ProcessorName.PACE),
            ),
        )
        activities.replaceCadence(
            activity.id,
            M2TestFixtures.cadence(activity),
            M2TestFixtures.processorState(activity, ProcessorName.CADENCE),
        )
        activities.putSummary(M2TestFixtures.summary(activity))
        activities.putProcessorStates(
            listOf(M2TestFixtures.processorState(activity, ProcessorName.SUMMARY)),
        )

        val data = LoadActivityAnalysis(activities)(activity.id)

        assertEquals(activity, data.activity)
        assertEquals(M2TestFixtures.summary(activity), data.summary)
        assertEquals(2, data.routeSegments.size)
        assertEquals(AnalysisSeriesAvailability.AVAILABLE, data.distance.availability)
        assertEquals(AnalysisSeriesAvailability.AVAILABLE, data.pace.availability)
        assertEquals(AnalysisSeriesAvailability.AVAILABLE, data.elevation.availability)
        assertEquals(AnalysisSeriesAvailability.AVAILABLE, data.cadence.availability)
    }

    @Test
    fun staleSummaryAndPaceRowsRemainStoredButAreNotPresentedAsCurrent() = runBlocking {
        val activity = M2TestFixtures.activity(1_002)
        activities.insert(activity)
        recordings.appendPositions(M2TestFixtures.positions(activity))
        activities.replaceTrackMetrics(
            activity.id,
            M2TestFixtures.trackMetrics(activity),
            listOf(
                M2TestFixtures.processorState(activity, ProcessorName.DISTANCE),
                M2TestFixtures.processorState(activity, ProcessorName.PACE),
            ),
        )
        activities.putSummary(M2TestFixtures.summary(activity))
        activities.putProcessorStates(
            listOf(M2TestFixtures.processorState(activity, ProcessorName.SUMMARY)),
        )
        activities.updateProcessorVersion(ProcessorName.PACE, 2)
        activities.updateProcessorVersion(ProcessorName.SUMMARY, 2)

        val data = LoadActivityAnalysis(activities)(activity.id)

        assertEquals(AnalysisProcessorStatus.STALE, data.processorValidity.getValue(ProcessorName.PACE).status)
        assertEquals(AnalysisSeriesAvailability.UNAVAILABLE_NOT_CURRENT, data.pace.availability)
        assertNull(data.summary)
        assertEquals(M2TestFixtures.trackMetrics(activity), activities.getTrackMetrics(activity.id))
        assertEquals(M2TestFixtures.summary(activity), activities.getSummary(activity.id))
    }
}
