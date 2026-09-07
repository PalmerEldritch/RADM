package com.jeppe.radm.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jeppe.radm.application.library.EditSavedActivity
import com.jeppe.radm.application.library.LoadSavedActivity
import com.jeppe.radm.application.processing.RecalculateActivity
import com.jeppe.radm.data.db.RadmDatabase
import com.jeppe.radm.data.db.RadmDatabaseFactory
import com.jeppe.radm.data.repository.RoomActivityRepository
import com.jeppe.radm.data.repository.RoomRecordingRepository
import com.jeppe.radm.domain.model.AbsoluteTimestampUtcMillis
import com.jeppe.radm.domain.model.ActivityType
import com.jeppe.radm.domain.model.ProcessorName
import com.jeppe.radm.domain.model.ProcessorStatus
import com.jeppe.radm.domain.model.RouteSegmentIndex
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
class ActivityLibraryRoomTest {
    private lateinit var database: RadmDatabase
    private lateinit var activities: RoomActivityRepository
    private lateinit var recordings: RoomRecordingRepository
    private lateinit var context: Context

    @Before
    fun createDatabase() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(DATABASE_NAME)
        database = RadmDatabaseFactory.create(context, DATABASE_NAME)
        activities = RoomActivityRepository(database)
        recordings = RoomRecordingRepository(database)
    }

    @After
    fun closeDatabase() {
        database.close()
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun vvmLib001And002AndPerf006_libraryProjectionIsNewestFirstSummaryOnlyAndSavedOnly() = runBlocking {
        val oldest = M2TestFixtures.activity(901)
        val newest = M2TestFixtures.activity(903).copy(type = ActivityType.CYCLING)
        val middle = M2TestFixtures.activity(902).copy(type = ActivityType.CROSS_COUNTRY_SKIING)
        val unresolved = M2TestFixtures.activity(904, saved = false)
        listOf(oldest, newest, middle, unresolved).forEach { activities.insert(it) }
        listOf(oldest, newest, middle).forEach {
            activities.replaceSummary(
                it.id,
                M2TestFixtures.summary(it),
                M2TestFixtures.processorState(it, ProcessorName.SUMMARY),
            )
        }
        recordings.appendPositions(M2TestFixtures.positions(oldest))
        recordings.appendSteps(M2TestFixtures.steps(oldest))

        val library = activities.listLibraryItems()

        assertEquals(listOf(newest.id, middle.id, oldest.id), library.map { it.activityId })
        assertEquals(ActivityType.CYCLING, library.first().activityType)
        assertEquals(M2TestFixtures.summary(newest).distance, library.first().distance)
        assertTrue(library.none { it.activityId == unresolved.id })
        assertEquals(M2TestFixtures.positions(oldest), activities.getPositions(oldest.id))

        activities.updateProcessorVersion(ProcessorName.SUMMARY, 2)
        assertTrue(activities.listLibraryItems().all { it.distance == null })
        assertNull(LoadSavedActivity(activities)(oldest.id).summary)
    }

    @Test
    fun vvmLib003_typeEditReprocessesApplicabilityAndPreservesIdentityAndSource() = runBlocking {
        val activity = M2TestFixtures.activity(905)
        activities.insert(activity)
        recordings.appendPositions(
            M2TestFixtures.positions(activity).map { it.copy(routeSegmentIndex = RouteSegmentIndex(0)) },
        )
        recordings.appendSteps(M2TestFixtures.steps(activity))
        RecalculateActivity(activities)(activity.id, PROCESSED_AT)
        val originalPositions = activities.getPositions(activity.id)
        val originalSteps = activities.getSteps(activity.id)

        val edited = EditSavedActivity(activities, RecalculateActivity(activities))(
            activityId = activity.id,
            activityType = ActivityType.CYCLING,
            title = "  Evening ride  ",
            notes = "  Dry roads  ",
            updatedAt = PROCESSED_AT,
        )

        assertEquals(activity.id, edited.activity.id)
        assertEquals(ActivityType.CYCLING, edited.activity.type)
        assertEquals("Evening ride", edited.activity.title)
        assertEquals("Dry roads", edited.activity.notes)
        assertEquals(originalPositions, activities.getPositions(activity.id))
        assertEquals(originalSteps, activities.getSteps(activity.id))
        assertTrue(activities.getTrackMetrics(activity.id).all { it.pace == null })
        assertTrue(activities.getTrackMetrics(activity.id).any { it.speed != null })
        assertTrue(activities.getCadence(activity.id).isEmpty())
        assertNull(activities.getSummary(activity.id)?.averagePace)
        assertNotNull(activities.getSummary(activity.id)?.averageSpeed)
        assertTrue(PROCESSORS.all { activities.isProcessorCurrent(activity.id, it) })

        database.close()
        database = RadmDatabaseFactory.create(context, DATABASE_NAME)
        activities = RoomActivityRepository(database)
        recordings = RoomRecordingRepository(database)
        val reloaded = requireNotNull(activities.get(activity.id))
        assertEquals(ActivityType.CYCLING, reloaded.type)
        assertEquals("Evening ride", reloaded.title)
        assertEquals("Dry roads", reloaded.notes)
        assertEquals(originalPositions, activities.getPositions(activity.id))
        assertEquals(originalSteps, activities.getSteps(activity.id))
    }

    @Test
    fun typeEditInvalidatesCurrentProcessorStateBeforeRecalculation() = runBlocking {
        val activity = M2TestFixtures.activity(906)
        activities.insert(activity)
        recordings.appendPositions(M2TestFixtures.positions(activity))
        RecalculateActivity(activities)(activity.id, PROCESSED_AT)

        activities.updateMetadata(
            activity.id,
            com.jeppe.radm.data.repository.ActivityMetadataUpdate(
                ActivityType.CYCLING,
                activity.title,
                activity.notes,
                PROCESSED_AT,
            ),
        )

        assertTrue(activities.getProcessorStates(activity.id).all {
            it.status == ProcessorStatus.UNPROCESSED && it.processorVersion == null
        })
        assertTrue(PROCESSORS.none { activities.isProcessorCurrent(activity.id, it) })
    }

    private companion object {
        const val DATABASE_NAME = "radm-library-test.db"
        val PROCESSED_AT = AbsoluteTimestampUtcMillis(M2TestFixtures.baseTime.value + 200_000_000L)
        val PROCESSORS = listOf(
            ProcessorName.DISTANCE,
            ProcessorName.PACE,
            ProcessorName.SPEED,
            ProcessorName.CADENCE,
            ProcessorName.SUMMARY,
        )
    }
}
