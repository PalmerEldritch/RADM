package com.jeppe.radm.ui

import android.Manifest
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jeppe.radm.MainActivity
import com.jeppe.radm.RadmApplication
import com.jeppe.radm.data.M2TestFixtures
import com.jeppe.radm.domain.model.Activity
import com.jeppe.radm.domain.model.ActivitySummary
import com.jeppe.radm.domain.model.ProcessorName
import com.jeppe.radm.domain.recording.RecordingState
import com.jeppe.radm.platform.recording.RecordingForegroundService
import com.jeppe.radm.platform.recording.RecordingServiceAction
import com.jeppe.radm.platform.recording.RecordingServiceState
import com.jeppe.radm.ui.analysis.ActivityAnalysisTestTags
import com.jeppe.radm.ui.library.ActivityLibraryTestTags
import com.jeppe.radm.ui.library.ActivityLibraryViewModel
import com.jeppe.radm.ui.recording.RecordingTestTags
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class M10AnalysisUserFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private lateinit var application: RadmApplication
    private var wifiWasEnabled = false
    private var mobileDataWasEnabled = false

    @Before
    fun prepareCleanApp() = runBlocking {
        application = composeRule.activity.application as RadmApplication
        wifiWasEnabled = shell("settings get global wifi_on").trim() == "1"
        mobileDataWasEnabled = shell("settings get global mobile_data").trim() == "1"
        cleanData()
        grantRuntimePermissions()
        composeRule.activity.libraryViewModel().closeActivity()
        composeRule.activity.libraryViewModel().refresh()
        composeRule.waitUntil(10_000) { !composeRule.activity.libraryViewModel().state.value.loading }
    }

    @After
    fun cleanUp() = runBlocking {
        restoreNetwork()
        val serviceState = application.container.recordingStateStore.state.value
        if (serviceState is RecordingServiceState.Active) {
            if (serviceState.snapshot.state != RecordingState.FINALIZING) {
                application.container.recordingServiceClient.finish()
                waitForServiceState { it.activeState() == RecordingState.FINALIZING }
            }
            application.container.recordingServiceClient.discard()
            waitForServiceState { it is RecordingServiceState.Idle }
        }
        cleanData()
    }

    @Test
    fun vvmAn001AndUxAt011_representativeLocalAnalysisShowsApplicableStaticViews() = runBlocking {
        val activity = M2TestFixtures.activity(1_101)
        seedAnalysis(activity, includeRoute = true, includeCadence = true)

        open(activity)

        composeRule.onNodeWithTag(ActivityAnalysisTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Horizontal coordinate: Distance").assertIsDisplayed()
        assertAnalysisItemDisplayed(ActivityAnalysisTestTags.ROUTE)
        assertAnalysisItemDisplayed(ActivityAnalysisTestTags.PACE_CHART)
        assertAnalysisItemDisplayed(ActivityAnalysisTestTags.ELEVATION_CHART)
        assertAnalysisItemDisplayed(ActivityAnalysisTestTags.CADENCE_CHART)
        assertAnalysisItemDisplayed(ActivityAnalysisTestTags.INSPECTOR)
        Unit
    }

    @Test
    fun vvmAn010_missingCadenceIsExplicitWhileRouteAndPaceRemainUsable() = runBlocking {
        val activity = M2TestFixtures.activity(1_102)
        seedAnalysis(activity, includeRoute = true, includeCadence = false)

        open(activity)

        assertAnalysisItemDisplayed(ActivityAnalysisTestTags.ROUTE)
        assertAnalysisItemDisplayed(ActivityAnalysisTestTags.PACE_CHART)
        assertAnalysisItemDisplayed(ActivityAnalysisTestTags.CADENCE_UNAVAILABLE)
        composeRule.onNodeWithText("Cadence unavailable — no suitable recorded data")
            .assertIsDisplayed()
        Unit
    }

    @Test
    fun vvmAn011_missingRouteKeepsMetadataCadenceAndInspectorUsable() = runBlocking {
        val activity = M2TestFixtures.activity(1_103)
        seedAnalysis(activity, includeRoute = false, includeCadence = true)

        open(activity)

        composeRule.onNodeWithText("Horizontal coordinate: Active Elapsed Time").assertIsDisplayed()
        assertAnalysisItemDisplayed(ActivityAnalysisTestTags.ROUTE_UNAVAILABLE)
        assertAnalysisItemDisplayed(ActivityAnalysisTestTags.CADENCE_CHART)
        assertAnalysisItemDisplayed(ActivityAnalysisTestTags.INSPECTOR)
        Unit
    }

    @Test
    fun vvmOff002_routeAndLocalGraphsRemainAvailableWithoutNetwork() = runBlocking {
        val activity = M2TestFixtures.activity(1_104)
        seedAnalysis(activity, includeRoute = true, includeCadence = true)
        disableNetwork()

        open(activity)

        assertAnalysisItemDisplayed(ActivityAnalysisTestTags.ROUTE)
        assertAnalysisItemDisplayed(ActivityAnalysisTestTags.PACE_CHART)
        assertAnalysisItemDisplayed(ActivityAnalysisTestTags.ELEVATION_CHART)
        Unit
    }

    @Test
    fun vvmOff001_coreRecordPauseResumeSaveBrowseAndAnalysisWorkOffline() {
        disableNetwork()
        composeRule.onNodeWithTag(ActivityLibraryTestTags.START).performClick()
        composeRule.onNodeWithTag(RecordingTestTags.START).performClick()
        waitForServiceState { it.activeState() == RecordingState.RECORDING }
        composeRule.onNodeWithTag(RecordingTestTags.PAUSE_RESUME).performClick()
        waitForServiceState { it.activeState() == RecordingState.PAUSED }
        composeRule.onNodeWithTag(RecordingTestTags.PAUSE_RESUME).performClick()
        waitForServiceState { it.activeState() == RecordingState.RECORDING }
        composeRule.onNodeWithTag(RecordingTestTags.FINISH).performClick()
        waitForServiceState { it.activeState() == RecordingState.FINALIZING }
        val activityId = requireNotNull(
            (application.container.recordingStateStore.state.value as RecordingServiceState.Active)
                .snapshot.activityId,
        )
        composeRule.onNodeWithTag(RecordingTestTags.SAVE).performScrollTo().performClick()
        waitForServiceState { it is RecordingServiceState.Idle }
        composeRule.waitUntil(10_000) {
            composeRule.activity.libraryViewModel().state.value.items.any { it.activityId == activityId }
        }

        composeRule.onNodeWithTag("activity_${activityId.value}").performClick()
        composeRule.waitUntil(10_000) {
            composeRule.activity.libraryViewModel().state.value.selected?.activity?.id == activityId
        }

        composeRule.onNodeWithTag(ActivityAnalysisTestTags.SCREEN).assertIsDisplayed()
        assertAnalysisItemDisplayed(ActivityAnalysisTestTags.ROUTE_UNAVAILABLE)
        assertAnalysisItemDisplayed(ActivityAnalysisTestTags.PACE_UNAVAILABLE)
    }

    private suspend fun seedAnalysis(
        activity: Activity,
        includeRoute: Boolean,
        includeCadence: Boolean,
    ) {
        val activities = application.container.activityRepository
        val recordings = application.container.recordingRepository
        activities.insert(activity)
        if (includeRoute) {
            recordings.appendPositions(M2TestFixtures.positions(activity))
            activities.replaceTrackMetrics(
                activity.id,
                M2TestFixtures.trackMetrics(activity),
                listOf(
                    M2TestFixtures.processorState(activity, ProcessorName.DISTANCE),
                    M2TestFixtures.processorState(activity, ProcessorName.PACE),
                ),
            )
        } else {
            activities.putProcessorStates(
                listOf(
                    M2TestFixtures.processorState(activity, ProcessorName.DISTANCE),
                    M2TestFixtures.processorState(activity, ProcessorName.PACE),
                ),
            )
        }
        recordings.appendSteps(M2TestFixtures.steps(activity))
        if (includeCadence) {
            activities.replaceCadence(
                activity.id,
                M2TestFixtures.cadence(activity),
                M2TestFixtures.processorState(activity, ProcessorName.CADENCE),
            )
        } else {
            activities.putProcessorStates(
                listOf(M2TestFixtures.processorState(activity, ProcessorName.CADENCE)),
            )
        }
        val summary: ActivitySummary = M2TestFixtures.summary(activity).let {
            if (includeRoute) it else it.copy(
                distance = null,
                averagePace = null,
                minimumElevation = null,
                maximumElevation = null,
                totalAscent = null,
            )
        }
        activities.putSummary(summary)
        activities.putProcessorStates(
            listOf(M2TestFixtures.processorState(activity, ProcessorName.SUMMARY)),
        )
        composeRule.activity.libraryViewModel().refresh()
        composeRule.waitUntil(10_000) {
            composeRule.activity.libraryViewModel().state.value.items.any { it.activityId == activity.id }
        }
    }

    private fun open(activity: Activity) {
        composeRule.onNodeWithTag("activity_${activity.id.value}").performClick()
        composeRule.waitUntil(10_000) {
            composeRule.activity.libraryViewModel().state.value.selected?.activity?.id == activity.id
        }
    }

    private fun assertAnalysisItemDisplayed(tag: String) {
        composeRule.onNodeWithTag(ActivityAnalysisTestTags.SCREEN)
            .performScrollToNode(hasTestTag(tag))
        composeRule.onNodeWithTag(tag).assertIsDisplayed()
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
    }

    private fun disableNetwork() {
        shell("svc wifi disable")
        shell("svc data disable")
        SystemClock.sleep(500L)
    }

    private fun restoreNetwork() {
        shell("svc wifi ${if (wifiWasEnabled) "enable" else "disable"}")
        shell("svc data ${if (mobileDataWasEnabled) "enable" else "disable"}")
    }

    private fun waitForServiceState(predicate: (RecordingServiceState) -> Boolean) {
        composeRule.waitUntil(10_000) {
            predicate(application.container.recordingStateStore.state.value)
        }
    }

    private fun RecordingServiceState.activeState(): RecordingState? =
        (this as? RecordingServiceState.Active)?.snapshot?.state

    private fun MainActivity.libraryViewModel(): ActivityLibraryViewModel =
        ViewModelProvider(this)[ActivityLibraryViewModel::class.java]

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

    private fun shell(command: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).use {
            it.readBytes().decodeToString()
        }
    }
}
