package com.jeppe.radm.application.recording

import com.jeppe.radm.domain.model.AbsoluteTimestampUtcMillis
import com.jeppe.radm.domain.model.ActivityId
import com.jeppe.radm.domain.model.ActivityType
import com.jeppe.radm.domain.model.MonotonicTimeMillis
import com.jeppe.radm.domain.model.RecordingEventType
import com.jeppe.radm.domain.recording.RecordingState
import com.jeppe.radm.platform.fakes.FakeClockSource
import com.jeppe.radm.platform.fakes.FakeLocationSource
import com.jeppe.radm.platform.fakes.FakeStepSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingRecoveryTest {
    @Test
    fun `VVM RECOV 001 detection describes retained unresolved source`() = runBlocking {
        val fixture = Fixture()
        fixture.controller.start(ActivityType.CYCLING)
        fixture.clock.advance(5_000L)
        fixture.controller.checkpointIfDue()

        val recovery = fixture.recovery.detect()

        assertNotNull(recovery)
        assertEquals(fixture.activityId, recovery?.activityId)
        assertEquals(ActivityType.CYCLING, recovery?.activityType)
        assertEquals(5_000L, recovery?.retainedActiveTime?.value)
        assertEquals(RecordingState.RECORDING, recovery?.durableState)
    }

    @Test
    fun `VVM RECOV 003 finish save excludes downtime and clears unresolved state`() = runBlocking {
        val fixture = Fixture()
        fixture.controller.start(ActivityType.CYCLING)
        fixture.clock.advance(5_000L)
        fixture.controller.checkpointIfDue()
        val trustworthyEnd = fixture.repository.session?.lastCheckpointAt
        fixture.clock.advance(600_000L)

        val savedId = fixture.recovery.finishAndSave()

        assertEquals(fixture.activityId, savedId)
        assertNull(fixture.repository.session)
        assertEquals(5_000L, fixture.repository.activity?.activeDuration?.value)
        assertEquals(trustworthyEnd, fixture.repository.activity?.endedAt)
        assertEquals(RecordingEventType.FINISH, fixture.repository.events.last().type)
        assertTrue(fixture.recalculated)
    }

    @Test
    fun `VVM RECOV 004 discard removes interrupted activity only after explicit workflow`() = runBlocking {
        val fixture = Fixture()
        fixture.controller.start(ActivityType.RUNNING)
        assertNotNull(fixture.recovery.detect())

        assertTrue(fixture.recovery.discard())

        assertNull(fixture.repository.activity)
        assertNull(fixture.repository.session)
        assertNull(fixture.recovery.detect())
    }

    @Test
    fun `finalizing interrupted session can save but cannot be resumed`() = runBlocking {
        val fixture = Fixture()
        fixture.controller.start(ActivityType.RUNNING)
        fixture.clock.advance(1_000L)
        fixture.controller.finish()
        val finalizing = fixture.recovery.detect()

        assertEquals(RecordingState.FINALIZING, finalizing?.durableState)
        val newController = fixture.newController()
        val failure = runCatching { newController.recoverAndResume() }.exceptionOrNull()
        assertNotNull(failure)
        assertFalse(fixture.recalculated)

        fixture.recovery.finishAndSave()
        assertTrue(fixture.recalculated)
        assertNull(fixture.repository.session)
    }

    @Test
    fun `VVM REL 005 recovery save failure remains finalizing and resolvable`() = runBlocking {
        val fixture = Fixture()
        fixture.controller.start(ActivityType.CYCLING)
        fixture.clock.advance(5_000L)
        fixture.controller.checkpointIfDue()
        fixture.repository.failFinalization = true

        val failure = runCatching { fixture.recovery.finishAndSave() }.exceptionOrNull()

        assertNotNull(failure)
        assertEquals(RecordingState.FINALIZING, fixture.repository.session?.state)
        assertNull(fixture.repository.activity?.savedAt)
        assertNotNull(fixture.recovery.detect())
    }

    private class Fixture {
        val activityId = ActivityId.parse("40000000-0000-4000-8000-000000000009")
        val repository = FakeRecordingRepository()
        val clock = FakeClockSource(
            absoluteTime = AbsoluteTimestampUtcMillis(1_788_379_200_000L),
            monotonicTime = MonotonicTimeMillis(10_000L),
        )
        var recalculated = false
        val controller = newController()
        val recovery = RecordingRecovery(
            recordingRepository = repository,
            clockSource = clock,
            recalculateActivity = { id, _ ->
                assertEquals(activityId, id)
                recalculated = true
            },
            persistencePolicy = RecordingPersistencePolicy(backoff = {}),
        )

        fun newController() = RecordingController(
            recordingRepository = repository,
            locationSource = FakeLocationSource(),
            stepSource = FakeStepSource(),
            clockSource = clock,
            activityIdSource = ActivityIdSource { activityId },
            persistencePolicy = RecordingPersistencePolicy(backoff = {}),
        )
    }
}
