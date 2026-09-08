package com.jeppe.radm.platform.recording

import android.content.Context
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jeppe.radm.RadmApplication
import com.jeppe.radm.RadmContainer
import com.jeppe.radm.application.recording.ActivityIdSource
import com.jeppe.radm.application.recording.RecordingController
import com.jeppe.radm.domain.model.AbsoluteTimestampUtcMillis
import com.jeppe.radm.domain.model.ActivityId
import com.jeppe.radm.domain.model.ActivityType
import com.jeppe.radm.domain.model.MonotonicTimeMillis
import com.jeppe.radm.platform.fakes.FakeClockSource
import com.jeppe.radm.platform.fakes.FakeLocationSource
import com.jeppe.radm.platform.fakes.FakeStepSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecordingStartupRecoveryTest {
    private lateinit var application: RadmApplication

    @Before
    fun cleanBefore() = runBlocking {
        application = ApplicationProvider.getApplicationContext<Context>().applicationContext as RadmApplication
        application.container.recordingRepository.discardSession()
        application.container.recordingStateStore.publish(RecordingServiceState.Idle)
    }

    @After
    fun cleanAfter() = runBlocking {
        application.container.recordingRepository.discardSession()
        application.container.recordingStateStore.publish(RecordingServiceState.Idle)
    }

    @Test
    fun srsRecov001_freshApplicationContainerDetectsDurableUnresolvedSession() = runBlocking {
        val clock = FakeClockSource(
            AbsoluteTimestampUtcMillis(1_788_379_200_000L),
            MonotonicTimeMillis(10_000L),
        )
        val controller = RecordingController(
            recordingRepository = application.container.recordingRepository,
            locationSource = FakeLocationSource(),
            stepSource = FakeStepSource(),
            clockSource = clock,
            activityIdSource = ActivityIdSource { ACTIVITY_ID },
        )
        controller.start(ActivityType.CYCLING)
        clock.advance(5_000L)
        controller.checkpointIfDue()

        val reconstructed = RadmContainer(application)
        val state = waitForRecovery(reconstructed)

        assertNotNull(state)
        assertEquals(ACTIVITY_ID, state?.recording?.activityId)
        assertEquals(5_000L, state?.recording?.retainedActiveTime?.value)
        reconstructed.database.close()
    }

    private fun waitForRecovery(container: RadmContainer): RecordingServiceState.Recoverable? {
        val deadline = SystemClock.elapsedRealtime() + 10_000L
        do {
            (container.recordingStateStore.state.value as? RecordingServiceState.Recoverable)?.let {
                return it
            }
            SystemClock.sleep(25L)
        } while (SystemClock.elapsedRealtime() < deadline)
        return null
    }

    private companion object {
        val ACTIVITY_ID: ActivityId = ActivityId.parse("40000000-0000-4000-8000-000000000910")
    }
}
