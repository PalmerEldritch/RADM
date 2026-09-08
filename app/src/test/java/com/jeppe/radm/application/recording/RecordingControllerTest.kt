package com.jeppe.radm.application.recording

import com.jeppe.radm.domain.model.AbsoluteTimestampUtcMillis
import com.jeppe.radm.domain.model.ActivityId
import com.jeppe.radm.domain.model.ActivityType
import com.jeppe.radm.domain.model.MonotonicTimeMillis
import com.jeppe.radm.domain.model.RecordingEventType
import com.jeppe.radm.domain.model.StepCounterEpoch
import com.jeppe.radm.domain.location.LocationAvailability
import com.jeppe.radm.domain.recording.LocationCandidate
import com.jeppe.radm.domain.recording.RecordingState
import com.jeppe.radm.domain.recording.StepMeasurement
import com.jeppe.radm.platform.fakes.FakeClockSource
import com.jeppe.radm.platform.fakes.FakeLocationSource
import com.jeppe.radm.platform.fakes.FakeStepSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingControllerTest {
    @Test
    fun `M3 fake lifecycle covers VVM REC 002 and 005 through 008 plus time and durability`() = runBlocking {
        val fixture = Fixture()

        val started = fixture.controller.start(ActivityType.RUNNING)

        assertEquals(RecordingState.RECORDING, started.state)
        assertEquals(fixture.activityId, started.activityId)
        assertEquals(0L, fixture.repository.session?.activeElapsedTime?.value)
        assertEquals(listOf(RecordingEventType.START), fixture.repository.events.map { it.type })
        assertTrue(fixture.location.isStarted)
        assertTrue(fixture.steps.isStarted)

        fixture.clock.advance(TWENTY_MINUTES)
        fixture.emitLocation(59.3293, 18.0686)
        fixture.emitSteps(28_451L)
        val paused = fixture.controller.pause()

        assertEquals(RecordingState.PAUSED, paused.state)
        assertEquals(TWENTY_MINUTES, paused.activeElapsedTime.value)
        assertEquals(1, fixture.repository.positions.size)
        assertEquals(1, fixture.repository.steps.size)
        assertEquals(RecordingState.PAUSED, fixture.repository.session?.state)
        assertFalse(fixture.location.isStarted)
        assertFalse(fixture.steps.isStarted)

        fixture.clock.advance(FIVE_MINUTES)
        val pausedLocation = fixture.locationMeasurement(60.0, 19.0)
        val pausedSteps = fixture.stepMeasurement(29_000L)
        fixture.location.emitDelayed(pausedLocation)
        fixture.steps.emitDelayed(pausedSteps)
        assertEquals(1, fixture.repository.positions.size)
        assertEquals(1, fixture.repository.steps.size)
        assertEquals(TWENTY_MINUTES, fixture.controller.snapshot().activeElapsedTime.value)

        fixture.clock.advance(1L)
        val resumed = fixture.controller.resume()
        assertEquals(RecordingState.RECORDING, resumed.state)
        assertEquals(1L, resumed.routeSegmentIndex?.value)
        fixture.location.emitDelayed(pausedLocation)
        fixture.steps.emitDelayed(pausedSteps)

        // Civil time can move independently; active time remains monotonic (VVM-TIME-002).
        fixture.clock.adjustCivilTimeTo(AbsoluteTimestampUtcMillis(BASE_UTC + 1_000L))
        fixture.clock.advance(TWENTY_MINUTES, TWENTY_MINUTES)
        fixture.emitLocation(59.3310, 18.0710)
        fixture.emitSteps(28_600L)
        val finalizing = fixture.controller.finish()

        assertEquals(RecordingState.FINALIZING, finalizing.state)
        assertEquals(FORTY_MINUTES, finalizing.activeElapsedTime.value)
        assertEquals(listOf(0L, 1L), fixture.repository.positions.map { it.routeSegmentIndex.value })
        assertEquals(2, fixture.repository.positions.size)
        assertEquals(2, fixture.repository.steps.size)
        assertEquals(0.0, requireNotNull(finalizing.liveDistance).value, 0.0)
        assertEquals(
            listOf(
                RecordingEventType.START,
                RecordingEventType.PAUSE,
                RecordingEventType.RESUME,
                RecordingEventType.FINISH,
            ),
            fixture.repository.events.map { it.type },
        )
        assertFalse(fixture.location.isStarted)
        assertFalse(fixture.steps.isStarted)

        fixture.location.emitDelayed(fixture.locationMeasurement(61.0, 20.0))
        assertEquals(2, fixture.repository.positions.size)

        fixture.clock.advance(1_000L)
        val saved = fixture.controller.save(SaveRecordingMetadata(title = "M3 simulated activity"))

        assertEquals(FORTY_MINUTES, saved.activeDuration.value)
        assertEquals("M3 simulated activity", saved.title)
        assertNotNull(saved.endedAt)
        assertNotNull(saved.savedAt)
        assertNull(fixture.repository.session)
        assertEquals(RecordingState.IDLE, fixture.controller.snapshot().state)
    }

    @Test
    fun `VVM REC 009 discard removes the simulated activity and every dependent source`() = runBlocking {
        val fixture = Fixture()
        fixture.controller.start(ActivityType.RUNNING)
        fixture.clock.advance(1_000L)
        fixture.emitLocation(59.3293, 18.0686)
        fixture.controller.finish()

        assertTrue(fixture.controller.discard())

        assertNull(fixture.repository.activity)
        assertNull(fixture.repository.session)
        assertTrue(fixture.repository.events.isEmpty())
        assertTrue(fixture.repository.positions.isEmpty())
        assertTrue(fixture.repository.steps.isEmpty())
    }

    @Test
    fun `VVM REL 005 failed save leaves finalizing state authoritative`() = runBlocking {
        val fixture = Fixture()
        fixture.controller.start(ActivityType.CYCLING)
        fixture.clock.advance(1_000L)
        fixture.controller.finish()
        fixture.repository.failFinalization = true

        assertThrows(IllegalStateException::class.java) {
            runBlocking { fixture.controller.save() }
        }

        assertEquals(RecordingState.FINALIZING, fixture.controller.snapshot().state)
        assertEquals(RecordingState.FINALIZING, fixture.repository.session?.state)
        assertNull(fixture.repository.activity?.savedAt)
    }

    @Test
    fun `non Running recording never starts or retains fake step input`() = runBlocking {
        listOf(ActivityType.CYCLING, ActivityType.CROSS_COUNTRY_SKIING).forEach { activityType ->
            val fixture = Fixture()
            fixture.controller.start(activityType)

            assertFalse(fixture.steps.isStarted)
            assertThrows(IllegalStateException::class.java) {
                runBlocking { fixture.steps.emitDelayed(fixture.stepMeasurement(100L)) }
            }
            fixture.controller.finish()
            assertTrue(fixture.repository.steps.isEmpty())
        }
    }

    @Test
    fun `VVM STEP 004 and PERM 002 unavailable step source does not affect Running`() = runBlocking {
        val fixture = Fixture()
        fixture.steps.startFailure = SecurityException("Activity recognition denied")

        val started = fixture.controller.start(ActivityType.RUNNING)
        fixture.clock.advance(1_000L)
        fixture.emitLocation(59.3293, 18.0686)
        fixture.controller.finish()

        assertEquals(RecordingState.RECORDING, started.state)
        assertEquals(1, fixture.repository.positions.size)
        assertTrue(fixture.repository.steps.isEmpty())
    }

    @Test
    fun `VVM REL 003 step acquisition failure preserves source data and location recording`() = runBlocking {
        val fixture = Fixture()
        fixture.controller.start(ActivityType.RUNNING)
        fixture.clock.advance(1_000L)
        fixture.emitSteps(100L)

        fixture.steps.stop()
        fixture.clock.advance(1_000L)
        fixture.emitLocation(59.3293, 18.0686)
        val active = fixture.controller.snapshot()
        fixture.controller.finish()

        assertEquals(RecordingState.RECORDING, active.state)
        assertEquals(1, fixture.repository.positions.size)
        assertEquals(1, fixture.repository.steps.size)
    }

    @Test
    fun `VVM REC 003 and 004 start without fix then acquire the first route sample`() = runBlocking {
        val fixture = Fixture()

        val started = fixture.controller.start(ActivityType.RUNNING)
        assertEquals(LocationAvailability.ACQUIRING, started.locationAvailability)
        assertNull(started.liveDistance)

        fixture.clock.advance(15_000L)
        val unavailable = fixture.controller.snapshot()
        assertEquals(RecordingState.RECORDING, unavailable.state)
        assertEquals(15_000L, unavailable.activeElapsedTime.value)
        assertEquals(LocationAvailability.UNAVAILABLE, unavailable.locationAvailability)
        assertNull(unavailable.liveDistance)

        fixture.emitLocation(59.3293, 18.0686)
        val acquired = fixture.controller.snapshot()
        assertEquals(LocationAvailability.AVAILABLE, acquired.locationAvailability)
        assertEquals(0.0, requireNotNull(acquired.liveDistance).value, 0.0)

        fixture.controller.finish()
        assertEquals(1, fixture.repository.positions.size)
        assertEquals(0L, fixture.repository.positions.single().sampleIndex.value)
    }

    @Test
    fun `VVM LOC 001 through 005 rejected candidates consume no index or distance`() = runBlocking {
        val fixture = Fixture()
        fixture.controller.start(ActivityType.CYCLING)
        val first = fixture.locationMeasurement(0.0, 0.0)
        assertTrue(fixture.location.emit(first))
        assertTrue(fixture.location.emit(first))

        fixture.clock.advance(1_000L)
        assertTrue(fixture.location.emit(fixture.locationMeasurement(91.0, 0.0)))
        assertTrue(
            fixture.location.emit(
                fixture.locationMeasurement(0.0, 0.0001).copy(horizontalAccuracyMetres = 31.0),
            ),
        )
        assertTrue(fixture.location.emit(fixture.locationMeasurement(1.0, 0.0)))
        assertTrue(fixture.location.emit(fixture.locationMeasurement(0.0, 0.0001)))

        fixture.controller.finish()

        assertEquals(listOf(0L, 1L), fixture.repository.positions.map { it.sampleIndex.value })
        assertEquals(2, fixture.repository.positions.size)
        assertTrue(requireNotNull(fixture.controller.snapshot().liveDistance).value > 0.0)
    }

    @Test
    fun `VVM LOC 006 through 008 gap persists a new segment without adding distance`() = runBlocking {
        val fixture = Fixture()
        fixture.controller.start(ActivityType.CYCLING)
        fixture.emitLocation(0.0, 0.0)
        fixture.clock.advance(1_000L)
        fixture.emitLocation(0.0, 0.0001)
        val beforeGap = requireNotNull(fixture.controller.snapshot().liveDistance)

        fixture.clock.advance(15_000L)
        assertEquals(RecordingState.RECORDING, fixture.controller.snapshot().state)
        fixture.emitLocation(1.0, 1.0)
        val afterGap = requireNotNull(fixture.controller.snapshot().liveDistance)

        assertEquals(beforeGap.value, afterGap.value, 0.0)
        fixture.controller.finish()
        val saved = fixture.controller.save()
        assertNotNull(saved.savedAt)
        assertEquals(listOf(0L, 0L, 1L), fixture.repository.positions.map { it.routeSegmentIndex.value })
    }

    @Test
    fun `VVM LOC 009 no-route activity remains saveable without synthetic positions`() = runBlocking {
        val fixture = Fixture()
        fixture.controller.start(ActivityType.CYCLING)
        fixture.clock.advance(20_000L)

        fixture.controller.finish()
        val saved = fixture.controller.save()

        assertEquals(20_000L, saved.activeDuration.value)
        assertTrue(fixture.repository.positions.isEmpty())
    }

    @Test
    fun `VVM REL 004 location failure leaves recording time and independent steps operational`() = runBlocking {
        val fixture = Fixture()
        fixture.controller.start(ActivityType.RUNNING)
        fixture.emitLocation(59.3293, 18.0686)

        fixture.location.becomeUnavailable()
        fixture.clock.advance(5_000L)
        fixture.emitSteps(100L)
        val snapshot = fixture.controller.snapshot()

        assertEquals(RecordingState.RECORDING, snapshot.state)
        assertEquals(5_000L, snapshot.activeElapsedTime.value)
        assertEquals(LocationAvailability.UNAVAILABLE, snapshot.locationAvailability)
        assertEquals(0.0, requireNotNull(snapshot.liveDistance).value, 0.0)
        fixture.controller.finish()
        assertEquals(1, fixture.repository.positions.size)
        assertEquals(1, fixture.repository.steps.size)
    }

    @Test
    fun `location source startup failure degrades only location acquisition`() = runBlocking {
        val fixture = Fixture()
        fixture.location.startFailure = IllegalStateException("Injected location failure")

        val started = fixture.controller.start(ActivityType.RUNNING)
        fixture.clock.advance(1_000L)

        assertEquals(RecordingState.RECORDING, started.state)
        assertEquals(LocationAvailability.UNAVAILABLE, started.locationAvailability)
        assertEquals(1_000L, fixture.controller.snapshot().activeElapsedTime.value)
        assertTrue(fixture.steps.isStarted)
    }

    @Test
    fun `VVM DUR 001 periodic checkpoint persists active time without source callbacks`() = runBlocking {
        val fixture = Fixture()
        fixture.controller.start(ActivityType.CYCLING)

        fixture.clock.advance(4_999L)
        fixture.controller.checkpointIfDue()
        assertEquals(0, fixture.repository.checkpointAttempts)
        assertEquals(0L, fixture.repository.session?.activeElapsedTime?.value)

        fixture.clock.advance(1L)
        fixture.controller.checkpointIfDue()
        assertEquals(1, fixture.repository.checkpointAttempts)
        assertEquals(5_000L, fixture.repository.session?.activeElapsedTime?.value)
        assertTrue(fixture.repository.positions.isEmpty())
    }

    @Test
    fun `VVM DUR 001 process loss at varied pre flush phases loses only uncommitted tail`() = runBlocking {
        listOf(1L, 1_000L, 4_999L).forEach { tailMillis ->
            val before = Fixture()
            before.controller.start(ActivityType.CYCLING)
            before.clock.advance(5_000L)
            before.emitLocation(59.3293, 18.0686)
            assertEquals(1, before.repository.positions.size)

            before.clock.advance(tailMillis)
            before.emitLocation(59.3294, 18.0687)
            assertEquals(1, before.repository.positions.size)

            val afterProcessLoss = RecordingController(
                recordingRepository = before.repository,
                locationSource = FakeLocationSource(),
                stepSource = FakeStepSource(),
                clockSource = before.clock,
                persistencePolicy = RecordingPersistencePolicy(backoff = {}),
            )
            val recovered = afterProcessLoss.recoverAndResume()

            assertEquals(before.activityId, recovered.activityId)
            assertEquals(1L, before.repository.session?.positionSampleCount)
            assertEquals(5_000L, before.repository.events.last().activeElapsedTime.value)
        }
    }

    @Test
    fun `VVM DUR 001 twenty accepted samples flush before five seconds`() = runBlocking {
        val fixture = Fixture()
        fixture.controller.start(ActivityType.CYCLING)

        repeat(20) { index ->
            fixture.clock.advance(100L)
            fixture.emitLocation(0.0, index * 0.000001)
        }

        assertEquals(1, fixture.repository.checkpointAttempts)
        assertEquals(20, fixture.repository.positions.size)
        assertEquals(2_000L, fixture.repository.session?.activeElapsedTime?.value)
    }

    @Test
    fun `VVM DUR 004 transient persistence failure retries and commits the batch once`() = runBlocking {
        val fixture = Fixture()
        fixture.controller.start(ActivityType.CYCLING)
        fixture.repository.checkpointFailuresRemaining = 2
        fixture.clock.advance(5_000L)

        fixture.emitLocation(59.3293, 18.0686)

        assertEquals(3, fixture.repository.checkpointAttempts)
        assertEquals(1, fixture.repository.positions.size)
        assertEquals(1L, fixture.repository.session?.positionSampleCount)
    }

    @Test
    fun `VVM DUR 005 persistent failure stops after three retries and retains committed data`() = runBlocking {
        var criticalFailure: RecordingPersistenceException? = null
        val fixture = Fixture(
            onCriticalPersistenceFailure = { criticalFailure = it },
        )
        fixture.controller.start(ActivityType.CYCLING)
        fixture.repository.checkpointFailuresRemaining = 10
        fixture.clock.advance(5_000L)

        fixture.emitLocation(59.3293, 18.0686)

        assertEquals(4, fixture.repository.checkpointAttempts)
        assertEquals(4, criticalFailure?.attempts)
        assertTrue(fixture.repository.positions.isEmpty())
        assertNotNull(fixture.repository.session)
    }

    @Test
    fun `VVM RECOV 001 and 002 recovery preserves identity time sources and discontinuities`() = runBlocking {
        val before = Fixture()
        before.controller.start(ActivityType.RUNNING)
        before.clock.advance(4_999L)
        before.emitLocation(59.3293, 18.0686)
        before.emitSteps(28_451L)
        before.clock.advance(1L)
        before.controller.checkpointIfDue()
        assertEquals(5_000L, before.repository.session?.activeElapsedTime?.value)

        before.clock.advance(600_000L)
        val recoveredLocation = FakeLocationSource()
        val recoveredSteps = FakeStepSource()
        val recoveredController = RecordingController(
            recordingRepository = before.repository,
            locationSource = recoveredLocation,
            stepSource = recoveredSteps,
            clockSource = before.clock,
        )

        val recovered = recoveredController.recoverAndResume()

        assertEquals(before.activityId, recovered.activityId)
        assertEquals(5_000L, recovered.activeElapsedTime.value)
        assertEquals(1L, recovered.routeSegmentIndex?.value)
        assertEquals(RecordingEventType.RECOVERY_RESUME, before.repository.events.last().type)
        assertEquals(5_000L, before.repository.events.last().activeElapsedTime.value)

        before.clock.advance(1_000L)
        recoveredLocation.emit(before.locationMeasurement(59.3300, 18.0690))
        recoveredSteps.emit(before.stepMeasurement(100L))
        recoveredController.finish()

        assertEquals(listOf(0L, 1L), before.repository.positions.map { it.routeSegmentIndex.value })
        assertEquals(listOf(0L, 1L), before.repository.steps.map { it.counterEpoch.value })
        assertEquals(6_000L, before.repository.session?.activeElapsedTime?.value)
    }

    private class Fixture(
        onCriticalPersistenceFailure: suspend (RecordingPersistenceException) -> Unit = { throw it },
    ) {
        val activityId = ActivityId.parse("40000000-0000-4000-8000-000000000003")
        val repository = FakeRecordingRepository()
        val location = FakeLocationSource()
        val steps = FakeStepSource()
        val clock = FakeClockSource(
            absoluteTime = AbsoluteTimestampUtcMillis(BASE_UTC),
            monotonicTime = MonotonicTimeMillis(10_000L),
        )
        val controller = RecordingController(
            recordingRepository = repository,
            locationSource = location,
            stepSource = steps,
            clockSource = clock,
            activityIdSource = ActivityIdSource { activityId },
            persistencePolicy = RecordingPersistencePolicy(backoff = {}),
            onCriticalPersistenceFailure = onCriticalPersistenceFailure,
        )

        suspend fun emitLocation(latitude: Double, longitude: Double) {
            assertTrue(location.emit(locationMeasurement(latitude, longitude)))
        }

        suspend fun emitSteps(cumulativeSteps: Long) {
            assertTrue(steps.emit(stepMeasurement(cumulativeSteps)))
        }

        fun locationMeasurement(latitude: Double, longitude: Double) = LocationCandidate(
            timestampUtcMillis = clock.absoluteNow().value,
            monotonicTimestampMillis = clock.monotonicNow().value,
            latitudeDegrees = latitude,
            longitudeDegrees = longitude,
            horizontalAccuracyMetres = 4.0,
        )

        fun stepMeasurement(cumulativeSteps: Long) = StepMeasurement(
            timestamp = clock.absoluteNow(),
            monotonicTimestamp = clock.monotonicNow(),
            counterEpoch = StepCounterEpoch(0L),
            cumulativeSteps = cumulativeSteps,
        )
    }

    private companion object {
        const val BASE_UTC = 1_788_379_200_000L
        const val FIVE_MINUTES = 300_000L
        const val TWENTY_MINUTES = 1_200_000L
        const val FORTY_MINUTES = 2_400_000L
    }
}
