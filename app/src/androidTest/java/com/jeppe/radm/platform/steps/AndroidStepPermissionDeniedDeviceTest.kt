package com.jeppe.radm.platform.steps

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jeppe.radm.RadmApplication
import com.jeppe.radm.data.repository.RoomActivityRepository
import com.jeppe.radm.domain.model.ActivityType
import com.jeppe.radm.domain.recording.RecordingState
import com.jeppe.radm.platform.recording.RecordingForegroundService
import com.jeppe.radm.platform.recording.RecordingServiceAction
import com.jeppe.radm.platform.recording.RecordingServiceState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Explicit opt-in DEVICE permission test. The host revokes permission before instrumentation. */
@RunWith(AndroidJUnit4::class)
class AndroidStepPermissionDeniedDeviceTest {
    @Test
    fun vvmPerm002_realPermissionDenialKeepsRunningAvailableWithoutSteps() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString(TEST_ARGUMENT) == "true")
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        assumeTrue(
            context.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) ==
                PackageManager.PERMISSION_DENIED,
        )
        val automation = instrumentation.uiAutomation
        automation.grantRuntimePermission(context.packageName, Manifest.permission.ACCESS_COARSE_LOCATION)
        automation.grantRuntimePermission(context.packageName, Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            automation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        val application = context.applicationContext as RadmApplication

        application.container.recordingRepository.discardSession()
        application.container.recordingStateStore.publish(RecordingServiceState.Idle)
        try {
            application.container.recordingServiceClient.start(ActivityType.RUNNING).getOrThrow()
            val started = waitForState(application) {
                it.activeSnapshot()?.state == RecordingState.RECORDING
            }.activeSnapshot()!!
            SystemClock.sleep(500L)
            val active = application.container.recordingStateStore.state.value.activeSnapshot()!!
            application.container.recordingServiceClient.finish().getOrThrow()
            waitForState(application) { it.activeSnapshot()?.state == RecordingState.FINALIZING }

            assertEquals(started.activityId, active.activityId)
            assertEquals(RecordingState.RECORDING, active.state)
            assertTrue(active.activeElapsedTime.value > 0L)
            assertTrue(
                RoomActivityRepository(application.container.database)
                    .getSteps(requireNotNull(started.activityId))
                    .isEmpty(),
            )
        } finally {
            val active = application.container.recordingStateStore.state.value.activeSnapshot()
            if (active != null && active.state != RecordingState.FINALIZING) {
                application.container.recordingServiceClient.finish()
                waitForState(application) { it.activeSnapshot()?.state == RecordingState.FINALIZING }
            }
            if (application.container.recordingStateStore.state.value.activeSnapshot() != null) {
                application.container.recordingServiceClient.discard()
                waitForState(application) { it is RecordingServiceState.Idle }
            }
            application.container.recordingRepository.discardSession()
            context.stopService(RecordingForegroundService.intent(context, RecordingServiceAction.DISCARD))
            application.container.recordingStateStore.publish(RecordingServiceState.Idle)
        }
    }

    private fun waitForState(
        application: RadmApplication,
        predicate: (RecordingServiceState) -> Boolean,
    ): RecordingServiceState {
        val deadline = SystemClock.elapsedRealtime() + STATE_TIMEOUT_MS
        do {
            val state = application.container.recordingStateStore.state.value
            if (predicate(state)) return state
            SystemClock.sleep(25L)
        } while (SystemClock.elapsedRealtime() < deadline)
        error("Timed out waiting for recording state")
    }

    private fun RecordingServiceState.activeSnapshot() =
        (this as? RecordingServiceState.Active)?.snapshot

    private companion object {
        const val TEST_ARGUMENT = "radmPermissionDenied"
        const val STATE_TIMEOUT_MS = 10_000L
    }
}
