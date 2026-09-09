package com.jeppe.radm.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jeppe.radm.MainActivity
import com.jeppe.radm.RadmApplication
import com.jeppe.radm.data.M2TestFixtures
import com.jeppe.radm.domain.analysis.AnalysisCoordinateMode
import com.jeppe.radm.domain.model.AbsoluteTimestampUtcMillis
import com.jeppe.radm.domain.model.ActiveElapsedTimeMillis
import com.jeppe.radm.domain.model.Activity
import com.jeppe.radm.domain.model.ActivitySummary
import com.jeppe.radm.domain.model.CadenceSample
import com.jeppe.radm.domain.model.CadenceStepsPerMinute
import com.jeppe.radm.domain.model.DerivedTrackMetric
import com.jeppe.radm.domain.model.DistanceMetres
import com.jeppe.radm.domain.model.ElevationMetres
import com.jeppe.radm.domain.model.LatitudeDegrees
import com.jeppe.radm.domain.model.LongitudeDegrees
import com.jeppe.radm.domain.model.PaceSecondsPerKilometre
import com.jeppe.radm.domain.model.PositionSample
import com.jeppe.radm.domain.model.ProcessorName
import com.jeppe.radm.domain.model.RouteSegmentIndex
import com.jeppe.radm.domain.model.SampleIndex
import com.jeppe.radm.domain.model.StepCounterEpoch
import com.jeppe.radm.domain.model.StepSample
import com.jeppe.radm.platform.recording.RecordingForegroundService
import com.jeppe.radm.platform.recording.RecordingServiceAction
import com.jeppe.radm.platform.recording.RecordingServiceState
import com.jeppe.radm.ui.analysis.ActivityAnalysisTestTags
import com.jeppe.radm.ui.library.ActivityLibraryViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class M11AnalysisUserFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private lateinit var application: RadmApplication

    @Before
    fun prepareCleanApp() = runBlocking {
        application = composeRule.activity.application as RadmApplication
        cleanData()
        composeRule.activity.libraryViewModel().closeActivity()
        composeRule.activity.libraryViewModel().refresh()
        composeRule.waitUntil(10_000) { !composeRule.activity.libraryViewModel().state.value.loading }
    }

    @After
    fun cleanUp() = runBlocking {
        cleanData()
    }

    @Test
    fun vvmAn002And003GraphSideAnd005_dragUpdatesSharedPersistentCursorAndInspector() = runBlocking {
        val activity = M2TestFixtures.activity(1_201)
        seedContinuousAnalysis(activity)
        open(activity)

        scrollTo(ActivityAnalysisTestTags.interaction(ActivityAnalysisTestTags.PACE_CHART))
        composeRule.onNodeWithTag(ActivityAnalysisTestTags.interaction(ActivityAnalysisTestTags.PACE_CHART))
            .performTouchInput {
                swipe(
                    start = Offset(width * 0.2f, height * 0.5f),
                    end = Offset(width * 0.8f, height * 0.5f),
                    durationMillis = 500L,
                )
            }
        composeRule.waitUntil(10_000) {
            selectedElapsed().value >= 45_000L
        }
        val selected = selectedElapsed()
        val cursorText = "Cursor: ${formatDuration(selected.value)}"

        scrollTo(ActivityAnalysisTestTags.selection(ActivityAnalysisTestTags.PACE_CHART))
        composeRule.onNodeWithTag(ActivityAnalysisTestTags.selection(ActivityAnalysisTestTags.PACE_CHART))
            .assertTextEquals(cursorText)
        scrollTo(ActivityAnalysisTestTags.selection(ActivityAnalysisTestTags.ELEVATION_CHART))
        composeRule.onNodeWithTag(ActivityAnalysisTestTags.selection(ActivityAnalysisTestTags.ELEVATION_CHART))
            .assertTextEquals(cursorText)
        scrollTo(ActivityAnalysisTestTags.selection(ActivityAnalysisTestTags.CADENCE_CHART))
        composeRule.onNodeWithTag(ActivityAnalysisTestTags.selection(ActivityAnalysisTestTags.CADENCE_CHART))
            .assertTextEquals(cursorText)
        scrollTo(ActivityAnalysisTestTags.SELECTED_TIME)
        composeRule.onNodeWithTag(ActivityAnalysisTestTags.SELECTED_TIME)
            .assertTextEquals("Active elapsed time: ${formatDuration(selected.value)}")

        composeRule.waitForIdle()
        assertEquals(selected, selectedElapsed())
        composeRule.activityRule.scenario.recreate()
        composeRule.waitUntil(10_000) { selectedElapsed() == selected }
        assertEquals(selected, selectedElapsed())
        Unit
    }

    @Test
    fun vvmAn006_coordinateSwitchUpdatesAllGraphsAndPreservesSelectedElapsedTime() = runBlocking {
        val activity = M2TestFixtures.activity(1_202)
        seedContinuousAnalysis(activity)
        open(activity)
        dragPaceTo(0.6f)
        val selected = selectedElapsed()

        scrollTo(ActivityAnalysisTestTags.COORDINATE_ELAPSED)
        composeRule.onNodeWithTag(ActivityAnalysisTestTags.COORDINATE_ELAPSED).performClick()
        composeRule.waitUntil(10_000) {
            interaction().state.coordinateMode == AnalysisCoordinateMode.ACTIVE_ELAPSED_TIME
        }

        assertEquals(selected, selectedElapsed())
        scrollTo(ActivityAnalysisTestTags.axis(ActivityAnalysisTestTags.PACE_CHART))
        composeRule.onNodeWithTag(ActivityAnalysisTestTags.axis(ActivityAnalysisTestTags.PACE_CHART))
            .assertTextEquals("Horizontal axis: Active Elapsed Time")
        scrollTo(ActivityAnalysisTestTags.axis(ActivityAnalysisTestTags.ELEVATION_CHART))
        composeRule.onNodeWithTag(ActivityAnalysisTestTags.axis(ActivityAnalysisTestTags.ELEVATION_CHART))
            .assertTextEquals("Horizontal axis: Active Elapsed Time")
        scrollTo(ActivityAnalysisTestTags.axis(ActivityAnalysisTestTags.CADENCE_CHART))
        composeRule.onNodeWithTag(ActivityAnalysisTestTags.axis(ActivityAnalysisTestTags.CADENCE_CHART))
            .assertTextEquals("Horizontal axis: Active Elapsed Time")
        Unit
    }

    @Test
    fun vvmAn007_rangeControlsShareCanonicalBoundsClampSelectionAndRestoreFullRange() = runBlocking {
        val activity = M2TestFixtures.activity(1_203)
        seedContinuousAnalysis(activity)
        open(activity)
        dragPaceTo(0.9f)

        scrollTo(ActivityAnalysisTestTags.RANGE_START)
        composeRule.onNodeWithTag(ActivityAnalysisTestTags.RANGE_START)
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress ->
                setProgress(0.25f)
            }
        scrollTo(ActivityAnalysisTestTags.RANGE_END)
        composeRule.onNodeWithTag(ActivityAnalysisTestTags.RANGE_END)
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress ->
                setProgress(0.65f)
            }
        composeRule.waitUntil(10_000) {
            interaction().rangeEndFraction in 0.649..0.651
        }

        val restricted = interaction()
        assertEquals(ActiveElapsedTimeMillis(15_000L), restricted.state.range.start.activeElapsedTime)
        assertEquals(ActiveElapsedTimeMillis(39_000L), restricted.state.range.endInclusive.activeElapsedTime)
        assertEquals(restricted.state.range.endInclusive.activeElapsedTime, selectedElapsed())
        val expectedRange = "Visible: 0.25 km–0.65 km"
        scrollTo(ActivityAnalysisTestTags.visibleRange(ActivityAnalysisTestTags.PACE_CHART))
        composeRule.onNodeWithTag(ActivityAnalysisTestTags.visibleRange(ActivityAnalysisTestTags.PACE_CHART))
            .assertTextEquals(expectedRange)
        scrollTo(ActivityAnalysisTestTags.visibleRange(ActivityAnalysisTestTags.ELEVATION_CHART))
        composeRule.onNodeWithTag(ActivityAnalysisTestTags.visibleRange(ActivityAnalysisTestTags.ELEVATION_CHART))
            .assertTextEquals(expectedRange)
        scrollTo(ActivityAnalysisTestTags.ROUTE_RANGE)
        composeRule.onNodeWithTag(ActivityAnalysisTestTags.ROUTE_RANGE).assertIsDisplayed()

        scrollTo(ActivityAnalysisTestTags.RANGE_RESET)
        composeRule.onNodeWithTag(ActivityAnalysisTestTags.RANGE_RESET).performClick()
        composeRule.waitUntil(10_000) {
            interaction().rangeStartFraction == 0.0 && interaction().rangeEndFraction == 1.0
        }
        assertEquals(ActiveElapsedTimeMillis.ZERO, interaction().state.range.start.activeElapsedTime)
        assertEquals(activity.activeDuration, interaction().state.range.endInclusive.activeElapsedTime)
        Unit
    }

    private suspend fun seedContinuousAnalysis(activity: Activity) {
        val times = listOf(0L, 15_000L, 30_000L, 45_000L, 60_000L)
        val positions = times.mapIndexed { index, elapsed ->
            PositionSample(
                activityId = activity.id,
                sampleIndex = SampleIndex(index.toLong()),
                routeSegmentIndex = RouteSegmentIndex(0),
                timestamp = AbsoluteTimestampUtcMillis(activity.startedAt.value + elapsed),
                activeElapsedTime = ActiveElapsedTimeMillis(elapsed),
                latitude = LatitudeDegrees(59.32 + index * 0.001),
                longitude = LongitudeDegrees(18.06 + index * 0.001),
                elevation = ElevationMetres(20.0 + index),
            )
        }
        val trackMetrics = times.mapIndexed { index, _ ->
            DerivedTrackMetric(
                activityId = activity.id,
                positionSampleIndex = SampleIndex(index.toLong()),
                cumulativeDistance = DistanceMetres(index * 250.0),
                pace = PaceSecondsPerKilometre(360.0 - index * 10.0),
                speed = null,
            )
        }
        val steps = times.mapIndexed { index, elapsed ->
            StepSample(
                activityId = activity.id,
                sampleIndex = SampleIndex(index.toLong()),
                counterEpoch = StepCounterEpoch(0),
                timestamp = AbsoluteTimestampUtcMillis(activity.startedAt.value + elapsed),
                activeElapsedTime = ActiveElapsedTimeMillis(elapsed),
                cumulativeSteps = 1_000 + index * 40L,
            )
        }
        val cadence = times.mapIndexed { index, elapsed ->
            CadenceSample(
                activityId = activity.id,
                sampleIndex = SampleIndex(index.toLong()),
                activeElapsedTime = ActiveElapsedTimeMillis(elapsed),
                cadence = CadenceStepsPerMinute(160.0 + index),
            )
        }
        val activities = application.container.activityRepository
        activities.insert(activity)
        application.container.recordingRepository.appendPositions(positions)
        application.container.recordingRepository.appendSteps(steps)
        activities.replaceTrackMetrics(
            activity.id,
            trackMetrics,
            listOf(
                M2TestFixtures.processorState(activity, ProcessorName.DISTANCE),
                M2TestFixtures.processorState(activity, ProcessorName.PACE),
            ),
        )
        activities.replaceCadence(
            activity.id,
            cadence,
            M2TestFixtures.processorState(activity, ProcessorName.CADENCE),
        )
        activities.putSummary(
            ActivitySummary(
                activityId = activity.id,
                distance = DistanceMetres(1_000.0),
                averagePace = PaceSecondsPerKilometre(340.0),
                averageSpeed = null,
                minimumElevation = ElevationMetres(20.0),
                maximumElevation = ElevationMetres(24.0),
                totalAscent = null,
            ),
        )
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
            composeRule.activity.libraryViewModel().state.value.analysisInteraction != null
        }
    }

    private fun dragPaceTo(fraction: Float) {
        val tag = ActivityAnalysisTestTags.interaction(ActivityAnalysisTestTags.PACE_CHART)
        scrollTo(tag)
        composeRule.onNodeWithTag(tag).performTouchInput {
            swipe(
                start = Offset(width * 0.1f, height * 0.5f),
                end = Offset(width * fraction, height * 0.5f),
                durationMillis = 300L,
            )
        }
        composeRule.waitForIdle()
    }

    private fun scrollTo(tag: String) {
        composeRule.onNodeWithTag(ActivityAnalysisTestTags.SCREEN)
            .performScrollToNode(hasTestTag(tag))
    }

    private fun interaction() = requireNotNull(
        composeRule.activity.libraryViewModel().state.value.analysisInteraction,
    )

    private fun selectedElapsed(): ActiveElapsedTimeMillis =
        interaction().state.selectedPosition.activeElapsedTime

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

    private fun MainActivity.libraryViewModel(): ActivityLibraryViewModel =
        ViewModelProvider(this)[ActivityLibraryViewModel::class.java]

    private fun formatDuration(milliseconds: Long): String {
        val seconds = milliseconds / 1_000L
        return String.format(
            Locale.US,
            "%d:%02d:%02d",
            seconds / 3_600L,
            (seconds % 3_600L) / 60L,
            seconds % 60L,
        )
    }
}
