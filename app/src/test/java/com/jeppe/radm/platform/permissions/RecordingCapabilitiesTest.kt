package com.jeppe.radm.platform.permissions

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingCapabilitiesTest {
    @Test
    fun `SRS LOCSTART 001 and VVM PERM 001 allow precise or approximate location capability`() {
        assertTrue(capabilities(LocationPermissionCapability.PRECISE).canStartLocationForegroundService)
        assertTrue(capabilities(LocationPermissionCapability.APPROXIMATE).canStartLocationForegroundService)
    }

    @Test
    fun `SRS LOCSTART 001 blocks start when location foreground service prerequisites are absent`() {
        assertFalse(capabilities(LocationPermissionCapability.NONE).canStartLocationForegroundService)
        assertFalse(
            capabilities(
                locationPermission = LocationPermissionCapability.PRECISE,
                locationServicesEnabled = false,
            ).canStartLocationForegroundService,
        )
        assertFalse(
            capabilities(
                locationPermission = LocationPermissionCapability.APPROXIMATE,
                locationServicesEnabled = false,
            ).canStartLocationForegroundService,
        )
    }

    @Test
    fun `unrelated runtime permissions do not redefine location foreground service capability`() {
        val capabilities = capabilities(
            locationPermission = LocationPermissionCapability.PRECISE,
            notificationPermission = RuntimePermissionCapability.DENIED,
            activityRecognitionPermission = RuntimePermissionCapability.DENIED,
        )

        assertTrue(capabilities.canStartLocationForegroundService)
    }

    private fun capabilities(
        locationPermission: LocationPermissionCapability,
        locationServicesEnabled: Boolean = true,
        notificationPermission: RuntimePermissionCapability = RuntimePermissionCapability.GRANTED,
        activityRecognitionPermission: RuntimePermissionCapability = RuntimePermissionCapability.GRANTED,
    ) = RecordingCapabilities(
        locationPermission = locationPermission,
        locationServicesEnabled = locationServicesEnabled,
        notificationPermission = notificationPermission,
        activityRecognitionPermission = activityRecognitionPermission,
    )
}
