package com.jeppe.radm.ui

import android.Manifest
import android.os.Build
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.jeppe.radm.MainActivity
import com.jeppe.radm.RadmApplication
import com.jeppe.radm.data.M2TestFixtures
import com.jeppe.radm.domain.model.ActivityType
import com.jeppe.radm.domain.recording.RecordingState
import com.jeppe.radm.platform.recording.RecordingForegroundService
import com.jeppe.radm.platform.recording.RecordingServiceAction
import com.jeppe.radm.platform.recording.RecordingServiceState
import com.jeppe.radm.ui.library.ActivityLibraryTestTags
import com.jeppe.radm.ui.library.ActivityLibraryViewModel
import com.jeppe.radm.ui.recording.RecordingTestTags
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class M8UserFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private lateinit var application: RadmApplication

    @Before
    fun prepareCleanApp() = runBlocking {
        application = composeRule.activity.application as RadmApplication
        cleanData()
        grantRuntimePermissions()
        composeRule.activity.libraryViewModel().refresh()
        composeRule.waitUntil(10_000) {
            !composeRule.activity.libraryViewModel().state.value.loading
        }
    }

    @After
    fun cleanUp() = runBlocking {
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
    fun uxAt006_finishMetadataSaveReturnsToPersistentLibrary() {
        composeRule.onNodeWithTag(ActivityLibraryTestTags.START).performClick()
        composeRule.onNodeWithTag(RecordingTestTags.START).performClick()
        waitForServiceState { it.activeState() == RecordingState.RECORDING }
        composeRule.onNodeWithTag(RecordingTestTags.FINISH).performClick()
        waitForServiceState { it.activeState() == RecordingState.FINALIZING }

        composeRule.onNodeWithTag(RecordingTestTags.FINALIZATION).assertIsDisplayed()
        composeRule.onNodeWithTag("recording_final_type_CYCLING").performScrollTo().performClick()
        composeRule.onNodeWithTag(RecordingTestTags.TITLE).performScrollTo().performTextInput("Morning ride")
        composeRule.onNodeWithTag(RecordingTestTags.NOTES).performScrollTo().performTextInput("Dry roads")
        val activityId = requireNotNull(
            (application.container.recordingStateStore.state.value as RecordingServiceState.Active)
                .snapshot.activityId,
        )
        composeRule.onNodeWithTag(RecordingTestTags.SAVE).performScrollTo().performClick()

        waitForServiceState { it is RecordingServiceState.Idle }
        composeRule.waitUntil(10_000) {
            composeRule.activity.libraryViewModel().state.value.items.any { it.activityId == activityId }
        }
        composeRule.onNodeWithText("Morning ride").assertIsDisplayed()
        val saved = runBlocking { application.container.activityRepository.get(activityId) }
        assertEquals(ActivityType.CYCLING, saved?.type)
        assertEquals("Morning ride", saved?.title)
        assertEquals("Dry roads", saved?.notes)
        assertNotNull(runBlocking { application.container.activityRepository.getSummary(activityId) })
        assertNull(runBlocking { application.container.recordingRepository.loadActiveSession() })
    }

    @Test
    fun uxAt007_discardRequiresConfirmationAndLeavesNoRecoverySession() {
        composeRule.onNodeWithTag(ActivityLibraryTestTags.START).performClick()
        composeRule.onNodeWithTag(RecordingTestTags.START).performClick()
        waitForServiceState { it.activeState() == RecordingState.RECORDING }
        composeRule.onNodeWithTag(RecordingTestTags.FINISH).performClick()
        waitForServiceState { it.activeState() == RecordingState.FINALIZING }
        val activityId = requireNotNull(
            (application.container.recordingStateStore.state.value as RecordingServiceState.Active)
                .snapshot.activityId,
        )

        composeRule.onNodeWithTag(RecordingTestTags.DISCARD).performScrollTo().performClick()
        composeRule.onNodeWithTag(RecordingTestTags.DISCARD_CONFIRM).assertIsDisplayed()
        assertNotNull(runBlocking { application.container.recordingRepository.loadActiveSession() })
        assertNotNull(runBlocking { application.container.activityRepository.get(activityId) })

        composeRule.onNodeWithTag(RecordingTestTags.DISCARD_CONFIRM).performClick()
        waitForServiceState { it is RecordingServiceState.Idle }

        assertNull(runBlocking { application.container.recordingRepository.loadActiveSession() })
        assertNull(runBlocking { application.container.activityRepository.get(activityId) })
    }

    @Test
    fun vvmLib004_savedDeleteRequiresConfirmationAndPreservesOtherActivity() = runBlocking {
        val deleted = M2TestFixtures.activity(951)
        val retained = M2TestFixtures.activity(952)
        application.container.activityRepository.insert(deleted)
        application.container.activityRepository.insert(retained)
        application.container.activityRepository.putSummary(M2TestFixtures.summary(deleted))
        application.container.activityRepository.putSummary(M2TestFixtures.summary(retained))
        composeRule.activity.libraryViewModel().refresh()
        composeRule.waitUntil(10_000) {
            composeRule.activity.libraryViewModel().state.value.items.size == 2
        }

        composeRule.onNodeWithTag("activity_${deleted.id.value}").performClick()
        composeRule.waitUntil(10_000) {
            composeRule.activity.libraryViewModel().state.value.selected?.activity?.id == deleted.id
        }
        composeRule.onNodeWithTag(ActivityLibraryTestTags.DETAIL)
            .performScrollToNode(hasTestTag(ActivityLibraryTestTags.DELETE))
        composeRule.onNodeWithTag(ActivityLibraryTestTags.DELETE).performClick()
        composeRule.onNodeWithTag(ActivityLibraryTestTags.DELETE_CONFIRM).assertIsDisplayed()
        assertNotNull(application.container.activityRepository.get(deleted.id))

        composeRule.onNodeWithTag(ActivityLibraryTestTags.DELETE_CONFIRM).performClick()
        composeRule.waitUntil(10_000) {
            composeRule.activity.libraryViewModel().state.value.selected == null
        }

        assertNull(application.container.activityRepository.get(deleted.id))
        assertNotNull(application.container.activityRepository.get(retained.id))
        assertTrue(application.container.activityRepository.listLibraryItems().any {
            it.activityId == retained.id
        })
    }

    @Test
    fun vvmLib003_metadataEditSurvivesActivityRecreationAndPreservesSource() = runBlocking {
        val activity = M2TestFixtures.activity(953)
        application.container.activityRepository.insert(activity)
        application.container.recordingRepository.appendPositions(M2TestFixtures.positions(activity))
        application.container.recordingRepository.appendSteps(M2TestFixtures.steps(activity))
        application.container.recalculateActivity(activity.id, M2TestFixtures.baseTime)
        val originalPositions = application.container.activityRepository.getPositions(activity.id)
        val originalSteps = application.container.activityRepository.getSteps(activity.id)
        composeRule.activity.libraryViewModel().refresh()
        composeRule.waitUntil(10_000) {
            composeRule.activity.libraryViewModel().state.value.items.any { it.activityId == activity.id }
        }

        composeRule.onNodeWithTag("activity_${activity.id.value}").performClick()
        composeRule.waitUntil(10_000) {
            composeRule.activity.libraryViewModel().state.value.selected?.activity?.id == activity.id
        }
        composeRule.onNodeWithTag(ActivityLibraryTestTags.DETAIL)
            .performScrollToNode(hasTestTag(ActivityLibraryTestTags.EDIT))
        composeRule.onNodeWithTag(ActivityLibraryTestTags.EDIT).performClick()
        composeRule.onNodeWithTag("activity_edit_type_CYCLING").performScrollTo().performClick()
        composeRule.onNodeWithTag(ActivityLibraryTestTags.EDIT_TITLE)
            .performScrollTo()
            .performTextReplacement("Edited ride")
        composeRule.onNodeWithTag(ActivityLibraryTestTags.EDIT_NOTES)
            .performScrollTo()
            .performTextReplacement("Persistent notes")
        composeRule.onNodeWithTag(ActivityLibraryTestTags.EDIT_SAVE).performScrollTo().performClick()
        composeRule.waitUntil(10_000) {
            composeRule.activity.libraryViewModel().state.value.selected?.activity?.title == "Edited ride"
        }

        composeRule.activityRule.scenario.recreate()
        composeRule.waitUntil(10_000) {
            composeRule.activity.libraryViewModel().state.value.items.any { it.title == "Edited ride" }
        }
        composeRule.onNodeWithTag(ActivityLibraryTestTags.DETAIL).performScrollToIndex(0)
        composeRule.onNodeWithText("Edited ride").assertIsDisplayed()
        val reloaded = application.container.activityRepository.get(activity.id)
        assertEquals(ActivityType.CYCLING, reloaded?.type)
        assertEquals("Persistent notes", reloaded?.notes)
        assertEquals(originalPositions, application.container.activityRepository.getPositions(activity.id))
        assertEquals(originalSteps, application.container.activityRepository.getSteps(activity.id))
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
}
