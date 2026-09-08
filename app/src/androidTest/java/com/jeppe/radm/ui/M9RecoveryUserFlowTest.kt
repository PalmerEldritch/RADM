package com.jeppe.radm.ui

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jeppe.radm.MainActivity
import com.jeppe.radm.RadmApplication
import com.jeppe.radm.application.recording.ActivityIdSource
import com.jeppe.radm.application.recording.RecordingController
import com.jeppe.radm.domain.model.AbsoluteTimestampUtcMillis
import com.jeppe.radm.domain.model.ActivityId
import com.jeppe.radm.domain.model.ActivityType
import com.jeppe.radm.domain.model.MonotonicTimeMillis
import com.jeppe.radm.domain.model.RecordingEventType
import com.jeppe.radm.domain.recording.RecordingState
import com.jeppe.radm.platform.fakes.FakeClockSource
import com.jeppe.radm.platform.fakes.FakeLocationSource
import com.jeppe.radm.platform.fakes.FakeStepSource
import com.jeppe.radm.platform.recording.RecordingForegroundService
import com.jeppe.radm.platform.recording.RecordingServiceAction
import com.jeppe.radm.platform.recording.RecordingServiceState
import com.jeppe.radm.ui.library.ActivityLibraryViewModel
import com.jeppe.radm.ui.recording.RecordingTestTags
import com.jeppe.radm.ui.recording.RecordingViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class M9RecoveryUserFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private lateinit var application: RadmApplication

    @Before
    fun prepare() = runBlocking {
        application = composeRule.activity.application as RadmApplication
        cleanData()
        grantRuntimePermissions()
    }

    @After
    fun cleanUp() = runBlocking {
        val state = application.container.recordingStateStore.state.value
        if (state is RecordingServiceState.Active) {
            if (state.snapshot.state != RecordingState.FINALIZING) {
                application.container.recordingServiceClient.finish()
                waitForState { it.activeState() == RecordingState.FINALIZING }
            }
            application.container.recordingServiceClient.discard()
            waitForState { it is RecordingServiceState.Idle }
        }
        cleanData()
    }

    @Test
    fun uxAt009_recoveryTakesPrecedenceAndDiscardRequiresConfirmation() = runBlocking {
        createInterruptedRecording(ActivityType.RUNNING)
        publishAndAwaitRecovery()

        composeRule.onNodeWithTag(RecordingTestTags.RECOVERY).assertIsDisplayed()
        composeRule.onNodeWithText("Interrupted activity").assertIsDisplayed()
        composeRule.onNodeWithText("Running").assertIsDisplayed()
        val recoverable = application.container.recordingStateStore.state.value as RecordingServiceState.Recoverable
        application.container.recordingStateStore.publish(
            recoverable.copy(criticalMessage = "Reliable recording cannot continue."),
        )
        composeRule.onNodeWithText("Reliable recording cannot continue.").assertIsDisplayed()
        composeRule.onNodeWithTag(RecordingTestTags.RECOVERY_DISCARD).performScrollTo().performClick()
        composeRule.onNodeWithTag(RecordingTestTags.RECOVERY_DISCARD_CONFIRM).assertIsDisplayed()
        assertNotNull(application.container.recordingRepository.loadActiveSession())

        composeRule.onNodeWithTag(RecordingTestTags.RECOVERY_DISCARD_CONFIRM).performClick()
        waitForState { it is RecordingServiceState.Idle }

        assertNull(application.container.recordingRepository.loadActiveSession())
        assertNull(application.container.activityRepository.get(ACTIVITY_ID))
    }

    @Test
    fun vvmRecov002_resumeUsesForegroundServiceAndPreservesIdentity() = runBlocking {
        createInterruptedRecording(ActivityType.CYCLING)
        publishAndAwaitRecovery()

        composeRule.onNodeWithTag(RecordingTestTags.RECOVERY_RESUME).performClick()
        val active = waitForState { it.activeState() == RecordingState.RECORDING }
            as RecordingServiceState.Active

        assertEquals(ACTIVITY_ID, active.snapshot.activityId)
        org.junit.Assert.assertTrue(active.snapshot.activeElapsedTime.value >= 5_000L)
        assertEquals(1L, active.snapshot.routeSegmentIndex?.value)
        val recoveryEvent = application.container.activityRepository.getRecordingEvents(ACTIVITY_ID).last()
        assertEquals(RecordingEventType.RECOVERY_RESUME, recoveryEvent.type)
        assertEquals(5_000L, recoveryEvent.activeElapsedTime.value)
    }

    @Test
    fun vvmRecov003_finishSavePublishesCapturedPortionToLibrary() = runBlocking {
        createInterruptedRecording(ActivityType.CYCLING)
        publishAndAwaitRecovery()

        composeRule.activity.recordingViewModel().finishAndSaveRecovery()
        waitForState { it is RecordingServiceState.Idle }
        composeRule.waitUntil(10_000L) {
            composeRule.activity.libraryViewModel().state.value.items.any { it.activityId == ACTIVITY_ID }
        }

        val saved = application.container.activityRepository.get(ACTIVITY_ID)
        assertNotNull(saved?.savedAt)
        assertEquals(5_000L, saved?.activeDuration?.value)
        assertNull(application.container.recordingRepository.loadActiveSession())
        Unit
    }

    private suspend fun createInterruptedRecording(activityType: ActivityType) {
        val clock = FakeClockSource(
            absoluteTime = AbsoluteTimestampUtcMillis(1_788_379_200_000L),
            monotonicTime = MonotonicTimeMillis(10_000L),
        )
        val controller = RecordingController(
            recordingRepository = application.container.recordingRepository,
            locationSource = FakeLocationSource(),
            stepSource = FakeStepSource(),
            clockSource = clock,
            activityIdSource = ActivityIdSource { ACTIVITY_ID },
        )
        controller.start(activityType)
        clock.advance(5_000L)
        controller.checkpointIfDue()
    }

    private fun publishAndAwaitRecovery() {
        application.container.refreshRecoveryState()
        waitForState { it is RecordingServiceState.Recoverable }
    }

    private suspend fun cleanData() {
        composeRule.activity.applicationContext.stopService(
            RecordingForegroundService.intent(
                composeRule.activity.applicationContext,
                RecordingServiceAction.DISCARD,
            ),
        )
        application.container.recordingRepository.discardSession()
        application.container.activityRepository.listSaved().forEach {
            application.container.activityRepository.delete(it.id)
        }
        application.container.recordingStateStore.publish(RecordingServiceState.Idle)
        composeRule.activity.libraryViewModel().refresh()
    }

    private fun waitForState(predicate: (RecordingServiceState) -> Boolean): RecordingServiceState {
        composeRule.waitUntil(10_000L) {
            predicate(application.container.recordingStateStore.state.value)
        }
        return application.container.recordingStateStore.state.value
    }

    private fun RecordingServiceState.activeState(): RecordingState? =
        (this as? RecordingServiceState.Active)?.snapshot?.state

    private fun MainActivity.libraryViewModel(): ActivityLibraryViewModel =
        ViewModelProvider(this)[ActivityLibraryViewModel::class.java]

    private fun MainActivity.recordingViewModel(): RecordingViewModel =
        ViewModelProvider(this)[RecordingViewModel::class.java]

    private fun grantRuntimePermissions() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val packageName = composeRule.activity.packageName
        automation.grantRuntimePermission(packageName, Manifest.permission.ACCESS_COARSE_LOCATION)
        automation.grantRuntimePermission(packageName, Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            automation.grantRuntimePermission(packageName, Manifest.permission.ACTIVITY_RECOGNITION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            automation.grantRuntimePermission(packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private companion object {
        val ACTIVITY_ID: ActivityId = ActivityId.parse("40000000-0000-4000-8000-000000000909")
    }
}
