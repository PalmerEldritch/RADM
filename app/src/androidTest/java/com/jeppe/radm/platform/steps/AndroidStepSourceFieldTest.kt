package com.jeppe.radm.platform.steps

import android.Manifest
import android.os.Build
import android.os.SystemClock
import android.util.Log
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
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Explicit opt-in DEVICE/FIELD test. Run with instrumentation argument radmFieldStep=true. */
@RunWith(AndroidJUnit4::class)
class AndroidStepSourceFieldTest {
    @Test
    fun vvmField006_realRunningPersistsIncreasingPhoneStepCounterSamples() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assumeTrue(InstrumentationRegistry.getArguments().getString(FIELD_ARGUMENT) == "true")
        assumeTrue(Build.MODEL.equals("SM-S921B", ignoreCase = true))
        val context = instrumentation.targetContext
        val application = context.applicationContext as RadmApplication
        val automation = instrumentation.uiAutomation
        automation.grantRuntimePermission(context.packageName, Manifest.permission.ACCESS_COARSE_LOCATION)
        automation.grantRuntimePermission(context.packageName, Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            automation.grantRuntimePermission(context.packageName, Manifest.permission.ACTIVITY_RECOGNITION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            automation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }

        application.container.recordingRepository.discardSession()
        application.container.recordingStateStore.publish(RecordingServiceState.Idle)
        try {
            application.container.recordingServiceClient.start(ActivityType.RUNNING).getOrThrow()
            val started = waitForState(application) {
                it.activeSnapshot()?.state == RecordingState.RECORDING
            }.activeSnapshot()!!
            val activityId = requireNotNull(started.activityId)

            // The operator walks a short, manually counted segment while this window is open.
            SystemClock.sleep(FIELD_MOVEMENT_WINDOW_MS)
            application.container.recordingServiceClient.finish().getOrThrow()
            waitForState(application) { it.activeSnapshot()?.state == RecordingState.FINALIZING }

            val samples = RoomActivityRepository(application.container.database).getSteps(activityId)
            val withinEpochDelta = samples.zipWithNext()
                .filter { (left, right) -> left.counterEpoch == right.counterEpoch }
                .sumOf { (left, right) -> right.cumulativeSteps - left.cumulativeSteps }
            Log.i(
                LOG_TAG,
                "VVM-FIELD-006 samples=${samples.size} observedWithinEpochDelta=$withinEpochDelta",
            )
            assertTrue("Expected at least two retained real step events, got ${samples.size}", samples.size >= 2)
            assertTrue("Expected a positive within-epoch step delta, got $withinEpochDelta", withinEpochDelta > 0L)
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
        const val FIELD_ARGUMENT = "radmFieldStep"
        const val FIELD_MOVEMENT_WINDOW_MS = 45_000L
        const val STATE_TIMEOUT_MS = 10_000L
        const val LOG_TAG = "RADM_FIELD_STEP"
    }
}
