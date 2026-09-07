package com.jeppe.radm.platform.steps

import android.Manifest
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidStepSourceTest {
    @Test
    fun androidStepCounterValuesRequireFiniteNonNegativeWholeSteps() {
        assertTrue(28_451f.toCumulativeStepsOrNull() == 28_451L)
        assertTrue(0f.toCumulativeStepsOrNull() == 0L)
        assertTrue((-1f).toCumulativeStepsOrNull() == null)
        assertTrue(1.5f.toCumulativeStepsOrNull() == null)
        assertTrue(Float.NaN.toCumulativeStepsOrNull() == null)
        assertTrue(Float.POSITIVE_INFINITY.toCumulativeStepsOrNull() == null)
    }

    @Test
    fun primaryReferenceDeviceProvidesAndRegistersStepCounter() = runBlocking {
        assumeTrue(Build.MODEL.equals("SM-S921B", ignoreCase = true))
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            instrumentation.uiAutomation.grantRuntimePermission(
                context.packageName,
                Manifest.permission.ACTIVITY_RECOGNITION,
            )
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val source = AndroidStepSource(context, scope)
        try {
            assertTrue(source.isSensorAvailable)
            source.start { }
            assertTrue(source.isRegistered)
            source.stop()
            assertFalse(source.isRegistered)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun deniedActivityRecognitionLeavesOptionalSourceUnregistered() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val source = AndroidStepSource(
                context = context,
                callbackScope = scope,
                activityRecognitionPermissionGranted = { false },
            )
            source.start { error("Denied source must emit no measurements") }
            assertFalse(source.isRegistered)
        } finally {
            scope.cancel()
        }
    }
}
