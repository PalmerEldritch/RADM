package com.jeppe.radm.ui

import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.doubleClick
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import com.jeppe.radm.MainActivity
import com.jeppe.radm.RadmApplication
import com.jeppe.radm.data.M2TestFixtures
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
import com.jeppe.radm.ui.map.ActivityRouteMapLayers
import com.jeppe.radm.ui.map.ActivityRouteMapTestTags
import com.jeppe.radm.ui.map.OpenFreeMapConfiguration
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.net.URI
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class M12MapSynchronizationTest {
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
        composeRule.activity.libraryViewModel().closeActivity()
        composeRule.activity.libraryViewModel().refresh()
        composeRule.waitUntil(10_000) { !composeRule.activity.libraryViewModel().state.value.loading }
    }

    @After
    fun cleanUp() = runBlocking {
        composeRule.activity.libraryViewModel().closeActivity()
        restoreNetwork()
        cleanData()
    }

    @Test
    fun vvmAn003_graphSelectionMovesRenderedMapMarkerAndInspector() = runBlocking {
        val activity = M2TestFixtures.activity(1_301)
        seedAnalysis(activity)
        open(activity)
        dragPaceTo(0.75f)
        composeRule.waitUntil(10_000) { selectedElapsed().value == 45_000L }
        val map = showMapAndAwaitStyle()

        composeRule.onNodeWithTag(ActivityRouteMapTestTags.SELECTION)
            .assertTextEquals("Map selection: 0:00:45")
        composeRule.waitUntil(10_000) { renderedSelectionCount(map) > 0 }
        scrollTo(ActivityAnalysisTestTags.SELECTED_TIME)
        composeRule.onNodeWithTag(ActivityAnalysisTestTags.SELECTED_TIME)
            .assertTextEquals("Active elapsed time: 0:00:45")
        Unit
    }

    @Test
    fun vvmAn004_touchNearRouteUpdatesGraphsInspectorAndCanonicalSelection() = runBlocking {
        val activity = M2TestFixtures.activity(1_302)
        seedAnalysis(activity)
        open(activity)
        showMapAndAwaitStyle()

        composeRule.onNodeWithTag(ActivityRouteMapTestTags.MAP).performTouchInput {
            click(center)
        }
        composeRule.waitUntil(10_000) { selectedElapsed().value in 20_000L..40_000L }
        val selected = selectedElapsed()

        scrollTo(ActivityAnalysisTestTags.selection(ActivityAnalysisTestTags.PACE_CHART))
        composeRule.onNodeWithTag(ActivityAnalysisTestTags.selection(ActivityAnalysisTestTags.PACE_CHART))
            .assertTextEquals("Cursor: ${formatDuration(selected.value)}")
        scrollTo(ActivityAnalysisTestTags.SELECTED_TIME)
        composeRule.onNodeWithTag(ActivityAnalysisTestTags.SELECTED_TIME)
            .assertTextEquals("Active elapsed time: ${formatDuration(selected.value)}")
        Unit
    }

    @Test
    fun vvmAn008And009_panDoesNotSelectAndGraphSelectionDoesNotRecenter() = runBlocking {
        val activity = M2TestFixtures.activity(1_303)
        seedAnalysis(activity)
        open(activity)
        val map = showMapAndAwaitStyle()
        composeRule.activity.libraryViewModel().selectAnalysisFraction(0.5)
        composeRule.waitUntil(10_000) { selectedElapsed().value == 30_000L }
        val selectedBeforePan = selectedElapsed()

        val cameraBeforePan = cameraTarget(map)
        assertTrue(map.uiSettings.isScrollGesturesEnabled)
        assertTrue(map.uiSettings.isZoomGesturesEnabled)
        onView(isAssignableFrom(MapView::class.java)).perform(swipeRight())
        SystemClock.sleep(1_000L)
        val cameraAfterPan = cameraTarget(map)

        assertEquals(selectedBeforePan, selectedElapsed())
        assertTrue(
            "Expected pan to move map camera from $cameraBeforePan, but it remained $cameraAfterPan",
            abs(cameraBeforePan.longitude - cameraAfterPan.longitude) > 0.00001,
        )

        val zoomBefore = cameraZoom(map)
        onView(isAssignableFrom(MapView::class.java)).perform(doubleClick())
        SystemClock.sleep(750L)
        assertTrue(cameraZoom(map) > zoomBefore)
        assertEquals(selectedBeforePan, selectedElapsed())
        val cameraAfterViewportGestures = cameraTarget(map)

        repeat(20) { index ->
            composeRule.activity.libraryViewModel().selectAnalysisFraction(0.5 + index * 0.3 / 19.0)
        }
        composeRule.waitUntil(10_000) { selectedElapsed().value == 48_000L }
        SystemClock.sleep(500L)
        val cameraAfterGraphSelection = cameraTarget(map)
        assertEquals(cameraAfterViewportGestures.latitude, cameraAfterGraphSelection.latitude, 0.000001)
        assertEquals(cameraAfterViewportGestures.longitude, cameraAfterGraphSelection.longitude, 0.000001)
        Unit
    }

    @Test
    fun vvmPriv001_loadedProviderRequestContainsNoActivityMetadata() = runBlocking {
        val activity = M2TestFixtures.activity(1_306).copy(
            title = "Private training title",
            notes = "Private notes must not enter map requests",
        )
        seedAnalysis(activity)
        open(activity)
        val map = showMapAndAwaitStyle()

        var loadedStyleUri: String? = null
        composeRule.runOnIdle { loadedStyleUri = map.style?.uri }
        assertEquals(OpenFreeMapConfiguration.STYLE_URI, loadedStyleUri)
        val uri = URI(checkNotNull(loadedStyleUri))
        assertEquals("tiles.openfreemap.org", uri.host)
        assertEquals("/styles/liberty", uri.path)
        assertEquals(null, uri.query)
        assertTrue(!loadedStyleUri.orEmpty().contains(activity.id.value))
        assertTrue(!loadedStyleUri.orEmpty().contains("Private"))
        Unit
    }

    @Test
    fun vvmAn007_routeRangeRemainsSegmentedAndUsesCanonicalBounds() = runBlocking {
        val activity = M2TestFixtures.activity(1_304)
        seedAnalysis(activity, gapAfterIndex = 2)
        open(activity)
        showMapAndAwaitStyle()

        composeRule.activity.libraryViewModel().setAnalysisRange(0.25, 0.75)
        composeRule.waitUntil(10_000) {
            interaction().rangeStartFraction == 0.25 && interaction().rangeEndFraction == 0.75
        }

        scrollTo(ActivityRouteMapTestTags.SEGMENTS)
        composeRule.onNodeWithTag(ActivityRouteMapTestTags.SEGMENTS)
            .assertTextEquals("Map route: 2 separate segments")
        composeRule.onNodeWithTag(ActivityRouteMapTestTags.RANGE)
            .assertTextEquals("Highlighted range: 3 route points")
        assertEquals(15_000L, interaction().state.range.start.activeElapsedTime.value)
        assertEquals(45_000L, interaction().state.range.endInclusive.activeElapsedTime.value)
        Unit
    }

    @Test
    fun vvmOff002AndUx006_offlineFallbackKeepsLocalRouteAndAnalysisUsable() = runBlocking {
        val activity = M2TestFixtures.activity(1_305)
        seedAnalysis(activity)
        disableNetwork()
        open(activity)
        showMapAndAwaitStyle(expectOffline = true)

        composeRule.onNodeWithTag(ActivityRouteMapTestTags.STATUS)
            .assertTextEquals("Basemap unavailable — local recorded route remains available")
        composeRule.onNodeWithTag(ActivityRouteMapTestTags.SEGMENTS)
            .assertTextEquals("Map route: 1 separate segment")
        composeRule.activity.libraryViewModel().selectAnalysisFraction(0.6)
        composeRule.waitUntil(10_000) { selectedElapsed().value == 36_000L }
        scrollTo(ActivityAnalysisTestTags.SELECTED_TIME)
        composeRule.onNodeWithTag(ActivityAnalysisTestTags.SELECTED_TIME)
            .assertTextEquals("Active elapsed time: 0:00:36")
        Unit
    }

    private suspend fun seedAnalysis(activity: Activity, gapAfterIndex: Int? = null) {
        val times = listOf(0L, 15_000L, 30_000L, 45_000L, 60_000L)
        val positions = times.mapIndexed { index, elapsed ->
            PositionSample(
                activityId = activity.id,
                sampleIndex = SampleIndex(index.toLong()),
                routeSegmentIndex = RouteSegmentIndex(if (gapAfterIndex != null && index > gapAfterIndex) 1 else 0),
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

    private fun showMapAndAwaitStyle(expectOffline: Boolean = false): MapLibreMap {
        scrollTo(ActivityRouteMapTestTags.MAP)
        composeRule.onNodeWithTag(ActivityRouteMapTestTags.MAP).assertIsDisplayed()
        composeRule.waitUntil(30_000) {
            val status = composeRule.onNodeWithTag(ActivityRouteMapTestTags.STATUS)
                .fetchSemanticsNode().config[SemanticsProperties.Text]
                .joinToString { it.text }
            !status.contains("loading") && (!expectOffline || status.contains("unavailable"))
        }
        val mapView = composeRule.activity.window.decorView.findDescendant(MapView::class.java)
        checkNotNull(mapView) { "MapLibre MapView was not attached" }
        val latch = CountDownLatch(1)
        var readyMap: MapLibreMap? = null
        composeRule.runOnIdle {
            mapView.getMapAsync {
                readyMap = it
                latch.countDown()
            }
        }
        check(latch.await(10, TimeUnit.SECONDS)) { "MapLibre map was not ready" }
        return checkNotNull(readyMap)
    }

    private fun dragPaceTo(fraction: Float) {
        val tag = ActivityAnalysisTestTags.interaction(ActivityAnalysisTestTags.PACE_CHART)
        scrollTo(tag)
        composeRule.onNodeWithTag(tag).performTouchInput {
            swipe(
                start = Offset(width * 0.1f, height * 0.5f),
                end = Offset(width * fraction, height * 0.5f),
                durationMillis = 400L,
            )
        }
        composeRule.waitForIdle()
    }

    private fun renderedSelectionCount(map: MapLibreMap): Int {
        val coordinate = interaction().route.selectedCoordinate ?: return 0
        var count = 0
        composeRule.runOnIdle {
            val screen = map.projection.toScreenLocation(LatLng(coordinate.latitude, coordinate.longitude))
            count = map.queryRenderedFeatures(screen, ActivityRouteMapLayers.SELECTION).size
        }
        return count
    }

    private fun cameraTarget(map: MapLibreMap): LatLng {
        var target: LatLng? = null
        composeRule.runOnIdle { target = map.cameraPosition.target }
        return checkNotNull(target)
    }

    private fun cameraZoom(map: MapLibreMap): Double {
        var zoom = Double.NaN
        composeRule.runOnIdle { zoom = map.zoom }
        return zoom
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

    private fun disableNetwork() {
        shell("svc wifi disable")
        shell("svc data disable")
        SystemClock.sleep(1_000L)
    }

    private fun restoreNetwork() {
        shell("svc wifi ${if (wifiWasEnabled) "enable" else "disable"}")
        shell("svc data ${if (mobileDataWasEnabled) "enable" else "disable"}")
    }

    private fun shell(command: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).use {
            it.readBytes().decodeToString()
        }
    }

    private fun MainActivity.libraryViewModel(): ActivityLibraryViewModel =
        ViewModelProvider(this)[ActivityLibraryViewModel::class.java]

    private fun <T : View> View.findDescendant(type: Class<T>): T? {
        if (type.isInstance(this)) return type.cast(this)
        if (this !is ViewGroup) return null
        repeat(childCount) { index ->
            getChildAt(index).findDescendant(type)?.let { return it }
        }
        return null
    }

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
