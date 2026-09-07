package com.jeppe.radm.platform.recording

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jeppe.radm.MainActivity
import com.jeppe.radm.RadmApplication
import com.jeppe.radm.domain.model.ActivityType
import com.jeppe.radm.domain.recording.RecordingState
import com.jeppe.radm.ui.recording.RecordingViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecordingForegroundServiceTest {
    private lateinit var context: Context
    private lateinit var application: RadmApplication

    @Before
    fun prepareCleanRecordingState() = runBlocking {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        application = context.applicationContext as RadmApplication
        context.stopService(RecordingForegroundService.intent(context, RecordingServiceAction.DISCARD))
        application.container.recordingRepository.discardSession()
        application.container.recordingStateStore.publish(RecordingServiceState.Idle)
        grantRuntimePermissions()
    }

    @After
    fun cleanUpRecordingState() = runBlocking {
        val state = application.container.recordingStateStore.state.value
        if (state is RecordingServiceState.Active) {
            if (state.snapshot.state != RecordingState.FINALIZING) {
                application.container.recordingServiceClient.finish()
                waitForState { it.activeSnapshot()?.state == RecordingState.FINALIZING }
            }
            application.container.recordingServiceClient.discard()
            waitForState { it is RecordingServiceState.Idle }
        }
        application.container.recordingRepository.discardSession()
        context.stopService(RecordingForegroundService.intent(context, RecordingServiceAction.DISCARD))
        application.container.recordingStateStore.publish(RecordingServiceState.Idle)
    }

    @Test
    fun vvmFgs001To003AndCompat004_visibleStartSurvivesBackgroundAndRecreation() {
        assertTrue(application.container.recordingCapabilityChecker.current().canStartLocationForegroundService)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.recordingViewModel().start(ActivityType.RUNNING)
            }
            val initial = waitForState { it.activeSnapshot()?.state == RecordingState.RECORDING }
                .activeSnapshot()!!
            val activityId = requireNotNull(initial.activityId)
            assertForegroundNotificationContains("Recording")

            val elapsedBeforeBackground = initial.activeElapsedTime.value
            scenario.moveToState(Lifecycle.State.CREATED)
            SystemClock.sleep(1_100L)
            val backgroundSnapshot = waitForState {
                val snapshot = it.activeSnapshot()
                snapshot?.let {
                    it.activityId == activityId && it.activeElapsedTime.value > elapsedBeforeBackground
                } == true
            }.activeSnapshot()!!
            assertEquals(activityId, backgroundSnapshot.activityId)

            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.recreate()
            scenario.onActivity { recreatedActivity ->
                val reconnected = recreatedActivity.recordingViewModel().recordingState.value.activeSnapshot()
                assertEquals(activityId, reconnected?.activityId)
            }
            assertEquals(
                activityId,
                runBlocking { application.container.recordingRepository.loadActiveSession()?.activityId },
            )

            scenario.onActivity { activity -> activity.recordingViewModel().pause() }
            waitForState { it.activeSnapshot()?.state == RecordingState.PAUSED }
            assertForegroundNotificationContains("Paused")

            scenario.recreate()
            scenario.onActivity { recreatedActivity ->
                val reconnected = recreatedActivity.recordingViewModel().recordingState.value.activeSnapshot()
                assertEquals(activityId, reconnected?.activityId)
                assertEquals(RecordingState.PAUSED, reconnected?.state)
                recreatedActivity.recordingViewModel().resume()
            }
            waitForState { it.activeSnapshot()?.state == RecordingState.RECORDING }
            assertEquals(
                activityId,
                runBlocking { application.container.recordingRepository.loadActiveSession()?.activityId },
            )

            scenario.onActivity { activity -> activity.recordingViewModel().finish() }
            waitForState { it.activeSnapshot()?.state == RecordingState.FINALIZING }
            application.container.recordingServiceClient.discard().getOrThrow()
            waitForState { it is RecordingServiceState.Idle }

            assertNull(runBlocking { application.container.recordingRepository.loadActiveSession() })
            waitForNotificationToDisappear()
        }
    }

    @Test
    fun srsLocstart001_disabledLocationServicesBlockStartBeforeDurableSession() = runBlocking {
        val initiallyEnabled = application.container.recordingCapabilityChecker.current().locationServicesEnabled
        try {
            setLocationEnabled(false)
            waitForCapability { !it.locationServicesEnabled }

            val result = application.container.recordingServiceClient.start(ActivityType.CYCLING)
            val error = waitForState { it is RecordingServiceState.CriticalError }

            assertTrue(result.isFailure)
            assertTrue((error as RecordingServiceState.CriticalError).message.contains("location", ignoreCase = true))
            assertNull(application.container.recordingRepository.loadActiveSession())
        } finally {
            setLocationEnabled(initiallyEnabled)
        }
    }

    @Test
    fun vvmRel004_locationServicesLossAfterStartDoesNotInvalidateRecording() {
        val initiallyEnabled = application.container.recordingCapabilityChecker.current().locationServicesEnabled
        try {
            setLocationEnabled(true)
            waitForCapability { it.locationServicesEnabled }
            application.container.recordingServiceClient.start(ActivityType.CYCLING).getOrThrow()
            val started = waitForState { it.activeSnapshot()?.state == RecordingState.RECORDING }
                .activeSnapshot()!!

            setLocationEnabled(false)
            val unavailable = waitForState {
                it.activeSnapshot()?.locationAvailability ==
                    com.jeppe.radm.domain.location.LocationAvailability.UNAVAILABLE
            }.activeSnapshot()!!

            assertEquals(started.activityId, unavailable.activityId)
            assertEquals(RecordingState.RECORDING, unavailable.state)
            assertTrue(unavailable.activeElapsedTime >= started.activeElapsedTime)

            setLocationEnabled(true)
            val resumed = waitForState {
                val availability = it.activeSnapshot()?.locationAvailability
                availability == com.jeppe.radm.domain.location.LocationAvailability.ACQUIRING ||
                    availability == com.jeppe.radm.domain.location.LocationAvailability.AVAILABLE
            }.activeSnapshot()!!
            assertEquals(started.activityId, resumed.activityId)
            assertEquals(RecordingState.RECORDING, resumed.state)
        } finally {
            setLocationEnabled(initiallyEnabled)
        }
    }

    private fun assertForegroundNotificationContains(expectedText: String) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val notification = waitForNotification(manager, expectedText)
        val text = notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        assertTrue("Expected notification text to contain $expectedText, was $text", text.contains(expectedText))
        assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
    }

    private fun waitForNotification(
        manager: NotificationManager,
        expectedText: String,
    ): Notification {
        val deadline = SystemClock.elapsedRealtime() + 10_000L
        do {
            manager.activeNotifications
                .firstOrNull { it.id == RecordingForegroundService.NOTIFICATION_ID }
                ?.notification
                ?.takeIf {
                    it.extras.getCharSequence(Notification.EXTRA_TEXT)
                        ?.toString()
                        .orEmpty()
                        .contains(expectedText)
                }
                ?.let { return it }
            SystemClock.sleep(50L)
        } while (SystemClock.elapsedRealtime() < deadline)
        error("Recording foreground notification did not show $expectedText")
    }

    private fun waitForNotificationToDisappear() {
        val manager = context.getSystemService(NotificationManager::class.java)
        val deadline = SystemClock.elapsedRealtime() + 10_000L
        do {
            if (manager.activeNotifications.none { it.id == RecordingForegroundService.NOTIFICATION_ID }) return
            SystemClock.sleep(50L)
        } while (SystemClock.elapsedRealtime() < deadline)
        error("Recording foreground notification remained after the service stopped")
    }

    private fun waitForState(predicate: (RecordingServiceState) -> Boolean): RecordingServiceState {
        val deadline = SystemClock.elapsedRealtime() + 10_000L
        do {
            val state = application.container.recordingStateStore.state.value
            if (predicate(state)) return state
            SystemClock.sleep(25L)
        } while (SystemClock.elapsedRealtime() < deadline)
        error("Timed out waiting for recording state; current=${application.container.recordingStateStore.state.value}")
    }

    private fun waitForCapability(
        predicate: (com.jeppe.radm.platform.permissions.RecordingCapabilities) -> Boolean,
    ) {
        val deadline = SystemClock.elapsedRealtime() + 10_000L
        do {
            if (predicate(application.container.recordingCapabilityChecker.current())) return
            SystemClock.sleep(50L)
        } while (SystemClock.elapsedRealtime() < deadline)
        error("Timed out waiting for recording capability state")
    }

    private fun setLocationEnabled(enabled: Boolean) {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
            "cmd location set-location-enabled $enabled",
        )
        ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
    }

    private fun RecordingServiceState.activeSnapshot() =
        (this as? RecordingServiceState.Active)?.snapshot

    private fun MainActivity.recordingViewModel(): RecordingViewModel =
        ViewModelProvider(this)[RecordingViewModel::class.java]

    private fun grantRuntimePermissions() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val packageName = context.packageName
        automation.grantRuntimePermission(packageName, Manifest.permission.ACCESS_COARSE_LOCATION)
        automation.grantRuntimePermission(packageName, Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            automation.grantRuntimePermission(packageName, Manifest.permission.ACTIVITY_RECOGNITION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            automation.grantRuntimePermission(packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
