package com.jeppe.radm.application.recording

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
        val fixture = Fixture()
        fixture.controller.start(ActivityType.CYCLING)

        assertFalse(fixture.steps.isStarted)
        assertThrows(IllegalStateException::class.java) {
            runBlocking { fixture.steps.emitDelayed(fixture.stepMeasurement(100L)) }
        }
        assertTrue(fixture.repository.steps.isEmpty())
    }

    private class Fixture {
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
        )

        suspend fun emitLocation(latitude: Double, longitude: Double) {
            assertTrue(location.emit(locationMeasurement(latitude, longitude)))
        }

        suspend fun emitSteps(cumulativeSteps: Long) {
            assertTrue(steps.emit(stepMeasurement(cumulativeSteps)))
        }

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
        const val BASE_UTC = 1_788_379_200_000L
        const val FIVE_MINUTES = 300_000L
        const val TWENTY_MINUTES = 1_200_000L
        const val FORTY_MINUTES = 2_400_000L
    }
}
