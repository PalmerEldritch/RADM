package com.jeppe.radm.performance

import android.app.ActivityManager
import android.Manifest
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.swipe
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jeppe.radm.MainActivity
import com.jeppe.radm.RadmApplication
import com.jeppe.radm.domain.model.ActivityId
import com.jeppe.radm.domain.model.ActivityType
import com.jeppe.radm.domain.recording.RecordingState
import com.jeppe.radm.platform.recording.RecordingForegroundService
import com.jeppe.radm.platform.recording.RecordingServiceAction
import com.jeppe.radm.platform.recording.RecordingServiceState
import com.jeppe.radm.ui.analysis.ActivityAnalysisTestTags
import com.jeppe.radm.ui.library.ActivityLibraryTestTags
import com.jeppe.radm.ui.library.ActivityLibraryViewModel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import java.util.Collections

/** Opt-in M13 performance/capacity measurements for the primary Galaxy S24 reference device. */
@RunWith(AndroidJUnit4::class)
class M13ReferenceDevicePerformanceTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private lateinit var application: RadmApplication

    @Before
    fun requireReferenceDeviceAndCleanData() = runBlocking {
        assumeTrue(arguments().getString(PERFORMANCE_ARGUMENT) == "true")
        assumeTrue(Build.MODEL.equals("SM-S921B", ignoreCase = true))
        application = composeRule.activity.application as RadmApplication
        cleanData()
    }

    @After
    fun cleanUp() = runBlocking {
        if (::application.isInitialized) cleanData()
    }

    @Test
    fun vvmPerf003_representativeActivityLoadIsUsableWithinTwoSeconds() = runBlocking {
        M13PerformanceFixtures.seedAnalysisActivity(
            application.container.database,
            M13PerformanceFixtures.REPRESENTATIVE_ACTIVITY_ID,
            M13PerformanceFixtures.REPRESENTATIVE_POSITION_COUNT,
        )
        val activityId = ActivityId.parse(M13PerformanceFixtures.REPRESENTATIVE_ACTIVITY_ID)

        // Warm the process, JIT, Room statements, and Compose path without retaining the analysis afterward.
        openAndAwaitUsable(activityId)
        composeRule.activity.libraryViewModel().closeActivity()
        composeRule.waitForIdle()

        val runs = buildList {
            repeat(10) {
                Runtime.getRuntime().gc()
                SystemClock.sleep(50L)
                add(openAndAwaitUsable(activityId))
                composeRule.activity.libraryViewModel().closeActivity()
                composeRule.waitForIdle()
            }
        }
        val stats = Statistics(runs)
        report("VVM-PERF-003", stats, "sampleCount=10000 unit=ms")
        assertTrue("Representative analysis p95 ${stats.p95} ms exceeds 2000 ms", stats.p95 <= 2_000L)
    }

    @Test
    fun vvmPerf001And002_selectionVisibleUpdateLatencyAndRateMeetTargets() = runBlocking {
        M13PerformanceFixtures.seedAnalysisActivity(
            application.container.database,
            M13PerformanceFixtures.REPRESENTATIVE_ACTIVITY_ID,
            M13PerformanceFixtures.REPRESENTATIVE_POSITION_COUNT,
        )
        val viewModel = composeRule.activity.libraryViewModel()
        openAndAwaitUsable(ActivityId.parse(M13PerformanceFixtures.REPRESENTATIVE_ACTIVITY_ID))
        composeRule.onNodeWithTag(ActivityAnalysisTestTags.SCREEN)
            .performScrollToNode(hasTestTag(ActivityAnalysisTestTags.PACE_CHART))
        composeRule.waitForIdle()

        val latencies = buildList {
            repeat(180) { index ->
                val fraction = ((index % 90) + 1) / 91.0
                val started = SystemClock.elapsedRealtimeNanos()
                composeRule.runOnIdle { viewModel.selectAnalysisFraction(fraction) }
                composeRule.waitForIdle()
                add((SystemClock.elapsedRealtimeNanos() - started) / 1_000_000L)
            }
        }
        val stats = Statistics(latencies.drop(20))
        val updateTimes = Collections.synchronizedList(mutableListOf<Long>())
        val observation = CoroutineScope(Dispatchers.Default).launch {
            viewModel.state
                .map { it.analysisInteraction?.state?.selectedPosition?.activeElapsedTime?.value }
                .distinctUntilChanged()
                .drop(1)
                .collect { updateTimes += SystemClock.elapsedRealtimeNanos() }
        }
        delay(100L)
        composeRule.onNodeWithTag(ActivityAnalysisTestTags.interaction(ActivityAnalysisTestTags.PACE_CHART))
            .performTouchInput {
                swipe(
                    start = androidx.compose.ui.geometry.Offset(width * 0.05f, height * 0.5f),
                    end = androidx.compose.ui.geometry.Offset(width * 0.95f, height * 0.5f),
                    durationMillis = 2_000L,
                )
            }
        delay(100L)
        observation.cancelAndJoin()
        val observedProcessingSpanSeconds = updateTimes
            .takeIf { it.size >= 2 }
            ?.let { (it.last() - it.first()) / 1_000_000_000.0 }
            ?: Double.POSITIVE_INFINITY
        // Compose's injector may process scheduled touch times faster than wall time.
        // Divide by the requested gesture duration to avoid overstating perceived update rate.
        val effectiveRate = (updateTimes.size - 1).coerceAtLeast(0) / CONTINUOUS_SWIPE_SECONDS
        report(
            "VVM-PERF-001",
            stats,
            "events=${stats.count} unit=ms inputToComposeIdle=true",
        )
        Log.i(
            PERFORMANCE_LOG_TAG,
            "VVM-PERF-002 updates=${updateTimes.size} scheduledSeconds=${format(CONTINUOUS_SWIPE_SECONDS)} " +
                "processingSpanSeconds=${format(observedProcessingSpanSeconds)} " +
                "effectiveUpdateRate=${format(effectiveRate)} updatesPerSecond continuousSwipe=true",
        )
        assertTrue("Selection p95 ${stats.p95} ms exceeds 100 ms", stats.p95 <= 100L)
        assertTrue("Effective selection rate $effectiveRate is below 30/s", effectiveRate >= 30.0)
    }

    @Test
    fun vvmPerf004And008_largeActivityLoadsAndRemainsFunctionalWithoutOom() = runBlocking {
        M13PerformanceFixtures.seedAnalysisActivity(
            application.container.database,
            M13PerformanceFixtures.LARGE_ACTIVITY_ID,
            M13PerformanceFixtures.LARGE_POSITION_COUNT,
        )
        val before = usedPrivateDirtyKb()
        val loadMillis = openAndAwaitUsable(ActivityId.parse(M13PerformanceFixtures.LARGE_ACTIVITY_ID))
        val viewModel = composeRule.activity.libraryViewModel()
        repeat(1_000) { index -> viewModel.selectAnalysisFraction((index % 1_000) / 999.0) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ActivityAnalysisTestTags.SCREEN)
            .performScrollToNode(hasTestTag(ActivityAnalysisTestTags.PACE_CHART))
        composeRule.waitForIdle()
        val after = usedPrivateDirtyKb()
        val state = viewModel.state.value
        assertEquals(M13PerformanceFixtures.LARGE_POSITION_COUNT, state.selected?.routeSegments?.sumOf { it.samples.size })
        assertEquals(M13PerformanceFixtures.LARGE_POSITION_COUNT, state.selected?.distance?.points?.size)
        reportMemory("VVM-PERF-004/008", before, after, "loadMs=$loadMillis sampleCount=100000")
        assertTrue("Large activity did not become usable", state.analysisInteraction != null)
    }

    @Test
    fun vvmPerf005And006And008_largeSummaryLibraryBrowsesWithoutSampleLoadsOrOom() = runBlocking {
        M13PerformanceFixtures.seedLargeLibrary(application.container.database)
        val before = usedPrivateDirtyKb()
        val started = SystemClock.elapsedRealtime()
        composeRule.activity.libraryViewModel().refresh()
        composeRule.waitUntil(30_000L) {
            val state = composeRule.activity.libraryViewModel().state.value
            !state.loading && state.items.size == M13PerformanceFixtures.LARGE_LIBRARY_COUNT
        }
        composeRule.waitForIdle()
        val loadMillis = SystemClock.elapsedRealtime() - started
        composeRule.onNodeWithTag(ActivityLibraryTestTags.LIST)
            .performScrollToIndex(M13PerformanceFixtures.LARGE_LIBRARY_COUNT - 1)
        composeRule.waitForIdle()
        val after = usedPrivateDirtyKb()
        val lastId = M13PerformanceFixtures.libraryActivityId(0)
        composeRule.activity.libraryViewModel().open(ActivityId.parse(lastId))
        composeRule.waitUntil(10_000L) {
            composeRule.activity.libraryViewModel().state.value.selected?.activity?.id?.value == lastId
        }
        reportMemory(
            "VVM-PERF-005/006/008",
            before,
            after,
            "libraryLoadMs=$loadMillis activityCount=10000 summaryOnly=true",
        )
        assertEquals(M13PerformanceFixtures.LARGE_LIBRARY_COUNT, composeRule.activity.libraryViewModel().state.value.items.size)
    }

    @Test
    fun vvmPerf007_normalProductionRecordingDoesNotSustainUiUnresponsiveness() = runBlocking {
        grantRuntimePermissions()
        val client = application.container.recordingServiceClient
        client.start(ActivityType.RUNNING).getOrThrow()
        waitForRecordingState { it.activeState() == RecordingState.RECORDING }
        val activityId = requireNotNull(
            (application.container.recordingStateStore.state.value as RecordingServiceState.Active)
                .snapshot.activityId,
        )
        val mainThreadDispatchMillis = mutableListOf<Long>()
        try {
            repeat(80) {
                val started = SystemClock.elapsedRealtimeNanos()
                composeRule.runOnIdle {
                    mainThreadDispatchMillis += (SystemClock.elapsedRealtimeNanos() - started) / 1_000_000L
                }
                SystemClock.sleep(250L)
            }
            val stats = Statistics(mainThreadDispatchMillis)
            val sourcePositions = application.container.activityRepository.getPositions(activityId).size
            val sourceSteps = application.container.activityRepository.getSteps(activityId).size
            report(
                "VVM-PERF-007",
                stats,
                "observationSeconds=20 sourcePositions=$sourcePositions " +
                    "sourceSteps=$sourceSteps unit=ms mainThreadDispatch=true",
            )
            assertTrue(
                "Normal recording produced sustained UI dispatch stalls: $mainThreadDispatchMillis",
                mainThreadDispatchMillis.windowed(3).none { window -> window.all { it >= 1_000L } },
            )
        } finally {
            client.finish()
            waitForRecordingState { it.activeState() == RecordingState.FINALIZING }
            client.discard()
            waitForRecordingState { it is RecordingServiceState.Idle }
            application.container.activityRepository.delete(activityId)
        }
    }

    private fun openAndAwaitUsable(activityId: ActivityId): Long {
        val started = SystemClock.elapsedRealtime()
        composeRule.runOnIdle { composeRule.activity.libraryViewModel().open(activityId) }
        composeRule.waitUntil(10_000L) {
            val state = composeRule.activity.libraryViewModel().state.value
            !state.loading && state.selected?.activity?.id == activityId && state.analysisInteraction != null
        }
        composeRule.waitForIdle()
        return SystemClock.elapsedRealtime() - started
    }

    private fun usedPrivateDirtyKb(): Int {
        Runtime.getRuntime().gc()
        SystemClock.sleep(100L)
        val manager = application.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getProcessMemoryInfo(intArrayOf(android.os.Process.myPid()))
            .single()
            .totalPrivateDirty
    }

    private suspend fun cleanData() {
        composeRule.activity.applicationContext.stopService(
            RecordingForegroundService.intent(
                composeRule.activity.applicationContext,
                RecordingServiceAction.DISCARD,
            ),
        )
        application.container.recordingStateStore.publish(RecordingServiceState.Idle)
        composeRule.activity.libraryViewModel().closeActivity()
        M13PerformanceFixtures.clear(application.container.database)
        composeRule.activity.libraryViewModel().refresh()
        composeRule.waitUntil(30_000L) { !composeRule.activity.libraryViewModel().state.value.loading }
    }

    private fun arguments() = InstrumentationRegistry.getArguments()

    private fun MainActivity.libraryViewModel(): ActivityLibraryViewModel =
        ViewModelProvider(this)[ActivityLibraryViewModel::class.java]

    private fun waitForRecordingState(predicate: (RecordingServiceState) -> Boolean): RecordingServiceState {
        val deadline = SystemClock.elapsedRealtime() + 10_000L
        do {
            val state = application.container.recordingStateStore.state.value
            if (predicate(state)) return state
            SystemClock.sleep(25L)
        } while (SystemClock.elapsedRealtime() < deadline)
        error("Timed out waiting for recording state; current=${application.container.recordingStateStore.state.value}")
    }

    private fun RecordingServiceState.activeState(): RecordingState? =
        (this as? RecordingServiceState.Active)?.snapshot?.state

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

    private fun report(id: String, stats: Statistics, details: String) {
        Log.i(
            PERFORMANCE_LOG_TAG,
            "$id $details runs=${stats.count} median=${stats.median} p95=${stats.p95} max=${stats.maximum}",
        )
    }

    private fun reportMemory(id: String, beforeKb: Int, afterKb: Int, details: String) {
        val heapMb = Runtime.getRuntime().maxMemory() / (1024L * 1024L)
        Log.i(
            PERFORMANCE_LOG_TAG,
            "$id $details privateDirtyBeforeKb=$beforeKb privateDirtyAfterKb=$afterKb maxHeapMb=$heapMb",
        )
    }

    private fun format(value: Double): String = String.format(Locale.US, "%.2f", value)

    private data class Statistics(private val values: List<Long>) {
        init {
            require(values.isNotEmpty())
        }

        private val sorted = values.sorted()
        val count = values.size
        val median = sorted[sorted.size / 2]
        val p95 = sorted[((sorted.size - 1) * 0.95).toInt()]
        val maximum = sorted.last()
    }

    private companion object {
        const val PERFORMANCE_ARGUMENT = "radmPerformance"
        const val PERFORMANCE_LOG_TAG = "RADM_PERF"
        const val CONTINUOUS_SWIPE_SECONDS = 2.0
    }
}
