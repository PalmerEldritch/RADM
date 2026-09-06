package com.jeppe.radm.platform.recording

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecordingServiceManifestTest {
    @Test
    fun vvmCompat004_manifestDeclaresModernLocationForegroundServiceContract() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        @Suppress("DEPRECATION")
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS,
        )
        val requested = packageInfo.requestedPermissions.orEmpty().toSet()
        assertTrue(Manifest.permission.FOREGROUND_SERVICE in requested)
        assertTrue(Manifest.permission.FOREGROUND_SERVICE_LOCATION in requested)
        assertTrue(Manifest.permission.ACCESS_COARSE_LOCATION in requested)
        assertTrue(Manifest.permission.ACCESS_FINE_LOCATION in requested)
        assertTrue(Manifest.permission.POST_NOTIFICATIONS in requested)
        assertTrue(Manifest.permission.ACTIVITY_RECOGNITION in requested)
        assertFalse(Manifest.permission.ACCESS_BACKGROUND_LOCATION in requested)

        @Suppress("DEPRECATION")
        val serviceInfo = context.packageManager.getServiceInfo(
            ComponentName(context, RecordingForegroundService::class.java),
            0,
        )
        assertEquals(
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
            serviceInfo.foregroundServiceType and ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
        )
        assertFalse(serviceInfo.exported)
    }
}
