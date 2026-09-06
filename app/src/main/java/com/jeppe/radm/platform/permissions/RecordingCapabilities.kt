package com.jeppe.radm.platform.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build

enum class LocationPermissionCapability {
    NONE,
    APPROXIMATE,
    PRECISE,
}

enum class RuntimePermissionCapability {
    NOT_REQUIRED,
    DENIED,
    GRANTED,
}

data class RecordingCapabilities(
    val locationPermission: LocationPermissionCapability,
    val locationServicesEnabled: Boolean,
    val notificationPermission: RuntimePermissionCapability,
    val activityRecognitionPermission: RuntimePermissionCapability,
) {
    val canStartLocationForegroundService: Boolean
        get() = locationPermission != LocationPermissionCapability.NONE && locationServicesEnabled
}

/** Central Android permission/capability boundary for recording. */
class RecordingCapabilityChecker(
    private val context: Context,
) {
    fun current(): RecordingCapabilities {
        val fine = isGranted(Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = isGranted(Manifest.permission.ACCESS_COARSE_LOCATION)
        val locationPermission = when {
            fine -> LocationPermissionCapability.PRECISE
            coarse -> LocationPermissionCapability.APPROXIMATE
            else -> LocationPermissionCapability.NONE
        }
        return RecordingCapabilities(
            locationPermission = locationPermission,
            locationServicesEnabled = locationServicesEnabled(),
            notificationPermission = permissionCapability(
                permission = Manifest.permission.POST_NOTIFICATIONS,
                requiredFromApi = Build.VERSION_CODES.TIRAMISU,
            ),
            activityRecognitionPermission = permissionCapability(
                permission = Manifest.permission.ACTIVITY_RECOGNITION,
                requiredFromApi = Build.VERSION_CODES.Q,
            ),
        )
    }

    fun permissionsForUserVisibleStart(includeRunningSteps: Boolean): Array<String> {
        val capabilities = current()
        return buildList {
            if (capabilities.locationPermission != LocationPermissionCapability.PRECISE) {
                // Android 12+ requires coarse and fine to be requested together, including
                // when upgrading an existing approximate grant to precise location.
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            if (capabilities.notificationPermission == RuntimePermissionCapability.DENIED) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (
                includeRunningSteps &&
                capabilities.activityRecognitionPermission == RuntimePermissionCapability.DENIED
            ) {
                add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }.distinct().toTypedArray()
    }

    private fun permissionCapability(permission: String, requiredFromApi: Int): RuntimePermissionCapability =
        if (Build.VERSION.SDK_INT < requiredFromApi) {
            RuntimePermissionCapability.NOT_REQUIRED
        } else if (isGranted(permission)) {
            RuntimePermissionCapability.GRANTED
        } else {
            RuntimePermissionCapability.DENIED
        }

    private fun isGranted(permission: String): Boolean =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    private fun locationServicesEnabled(): Boolean {
        val manager = context.getSystemService(LocationManager::class.java)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            manager.isLocationEnabled
        } else {
            @Suppress("DEPRECATION")
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    }
}
