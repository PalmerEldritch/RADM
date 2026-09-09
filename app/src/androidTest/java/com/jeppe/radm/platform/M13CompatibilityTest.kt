package com.jeppe.radm.platform

import android.Manifest
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jeppe.radm.MainActivity
import com.jeppe.radm.RadmApplication
import com.jeppe.radm.domain.recording.RecordingState
import com.jeppe.radm.platform.recording.RecordingForegroundService
import com.jeppe.radm.platform.recording.RecordingServiceAction
import com.jeppe.radm.platform.recording.RecordingServiceState
import com.jeppe.radm.ui.library.ActivityLibraryTestTags
import com.jeppe.radm.ui.library.ActivityLibraryViewModel
import com.jeppe.radm.ui.recording.RecordingTestTags
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class M13CompatibilityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private lateinit var application: RadmApplication

    @Before
    fun prepareCleanApplication() = runBlocking {
        application = composeRule.activity.application as RadmApplication
        cleanData()
        grantRuntimePermissions()
        composeRule.activity.libraryViewModel().refresh()
        composeRule.waitUntil(10_000L) { !composeRule.activity.libraryViewModel().state.value.loading }
    }

    @After
    fun cleanUp() = runBlocking {
        cleanData()
    }

    @Test
    fun vvmCompat001And002_api26LaunchScreensAndCompleteRecordingFlow() {
        assumeTrue(Build.VERSION.SDK_INT == Build.VERSION_CODES.O)

        composeRule.onNodeWithTag(ActivityLibraryTestTags.START).assertIsDisplayed().performClick()
        composeRule.onNodeWithTag(RecordingTestTags.START).assertIsDisplayed().performClick()
        waitForState { it.activeState() == RecordingState.RECORDING }
        composeRule.onNodeWithTag(RecordingTestTags.PAUSE_RESUME).performClick()
        waitForState { it.activeState() == RecordingState.PAUSED }
        composeRule.onNodeWithTag(RecordingTestTags.PAUSE_RESUME).performClick()
        waitForState { it.activeState() == RecordingState.RECORDING }
        composeRule.onNodeWithTag(RecordingTestTags.FINISH).performClick()
        waitForState { it.activeState() == RecordingState.FINALIZING }
        composeRule.onNodeWithTag(RecordingTestTags.SAVE).performScrollTo().performClick()
        waitForState { it is RecordingServiceState.Idle }
        composeRule.waitUntil(10_000L) {
            composeRule.activity.libraryViewModel().state.value.items.size == 1
        }
        val item = composeRule.activity.libraryViewModel().state.value.items.single()
        composeRule.onNodeWithTag("activity_${item.activityId.value}").assertIsDisplayed().performClick()
        composeRule.waitUntil(10_000L) {
            composeRule.activity.libraryViewModel().state.value.analysisInteraction != null
        }
        composeRule.onNodeWithTag(ActivityLibraryTestTags.DETAIL).assertIsDisplayed()
    }

    @Test
    fun vvmCompat003And004_currentTargetUserStartContinuesWhileUiBackgrounded() {
        val targetSdk = composeRule.activity.applicationInfo.targetSdkVersion
        assumeTrue(Build.VERSION.SDK_INT == targetSdk)

        composeRule.onNodeWithTag(ActivityLibraryTestTags.START).performClick()
        composeRule.onNodeWithTag(RecordingTestTags.START).performClick()
        val started = waitForState { it.activeState() == RecordingState.RECORDING }
        val before = (started as RecordingServiceState.Active).snapshot.activeElapsedTime

        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        SystemClock.sleep(2_200L)
        val background = waitForState { state ->
            val active = state as? RecordingServiceState.Active
            active?.snapshot?.state == RecordingState.RECORDING &&
                active.snapshot.activeElapsedTime.value >= before.value + 1_000L
        }
        assertTrue((background as RecordingServiceState.Active).snapshot.activeElapsedTime > before)

        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.onNodeWithTag(RecordingTestTags.STATE).assertIsDisplayed()
        composeRule.onNodeWithTag(RecordingTestTags.FINISH).performClick()
        waitForState { it.activeState() == RecordingState.FINALIZING }
        composeRule.onNodeWithTag(RecordingTestTags.DISCARD).performScrollTo().performClick()
        composeRule.onNodeWithTag(RecordingTestTags.DISCARD_CONFIRM).performClick()
        waitForState { it is RecordingServiceState.Idle }
        assertNotNull(application.container.recordingStateStore.state.value)
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

    private suspend fun cleanData() {
        composeRule.activity.applicationContext.stopService(
            RecordingForegroundService.intent(
                composeRule.activity.applicationContext,
                RecordingServiceAction.DISCARD,
            ),
        )
        application.container.recordingRepository.discardSession()
        application.container.database.activityDao().deleteAllActivities()
        application.container.recordingStateStore.publish(RecordingServiceState.Idle)
        composeRule.activity.libraryViewModel().closeActivity()
    }

    private fun grantRuntimePermissions() {
        grantRuntimePermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        grantRuntimePermission(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            grantRuntimePermission(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            grantRuntimePermission(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun grantRuntimePermission(permission: String) {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
            "pm grant ${composeRule.activity.packageName} $permission",
        )
        ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
    }

    private fun RecordingServiceState.activeState(): RecordingState? =
        (this as? RecordingServiceState.Active)?.snapshot?.state

    private fun MainActivity.libraryViewModel(): ActivityLibraryViewModel =
        ViewModelProvider(this)[ActivityLibraryViewModel::class.java]
}
