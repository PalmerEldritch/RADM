package com.jeppe.radm.platform.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import com.jeppe.radm.domain.location.R00LocationDefaults
import com.jeppe.radm.domain.recording.LocationCandidate
import com.jeppe.radm.domain.recording.LocationSource
import com.jeppe.radm.domain.recording.LocationSourceEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Phone-native, high-accuracy location adapter that emits no Android types across its boundary. */
class AndroidLocationSource(
    context: Context,
    private val callbackScope: CoroutineScope,
) : LocationSource {
    private val locationManager = context.getSystemService(LocationManager::class.java)

    @Volatile
    private var consumer: (suspend (LocationSourceEvent) -> Unit)? = null
    private var registered = false

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            publish(LocationSourceEvent.Candidate(location.toLocationCandidate()))
        }

        override fun onProviderEnabled(provider: String) {
            if (provider == LocationManager.GPS_PROVIDER) {
                publish(LocationSourceEvent.ProviderAvailable)
            }
        }

        override fun onProviderDisabled(provider: String) {
            if (provider == LocationManager.GPS_PROVIDER) {
                publish(LocationSourceEvent.ProviderUnavailable)
            }
        }

        @Deprecated("Required by LocationListener on the minimum Android API")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
    }

    @SuppressLint("MissingPermission")
    override suspend fun start(consumer: suspend (LocationSourceEvent) -> Unit) {
        this.consumer = consumer
        if (LocationManager.GPS_PROVIDER !in locationManager.allProviders) {
            publish(LocationSourceEvent.ProviderUnavailable)
            return
        }
        try {
            withContext(Dispatchers.Main.immediate) {
                if (!registered) {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        R00LocationDefaults.MINIMUM_UPDATE_INTERVAL_MS,
                        0f,
                        listener,
                        Looper.getMainLooper(),
                    )
                    registered = true
                }
            }
            publish(
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    LocationSourceEvent.ProviderAvailable
                } else {
                    LocationSourceEvent.ProviderUnavailable
                },
            )
        } catch (failure: RuntimeException) {
            this.consumer = null
            throw failure
        }
    }

    override suspend fun stop() {
        try {
            withContext(Dispatchers.Main.immediate) {
                if (registered) locationManager.removeUpdates(listener)
                registered = false
            }
        } finally {
            consumer = null
        }
    }

    private fun publish(event: LocationSourceEvent) {
        val currentConsumer = consumer ?: return
        callbackScope.launch { currentConsumer(event) }
    }
}

internal fun Location.toLocationCandidate() = LocationCandidate(
    timestampUtcMillis = time,
    monotonicTimestampMillis = elapsedRealtimeNanos / NANOS_PER_MILLISECOND,
    latitudeDegrees = latitude,
    longitudeDegrees = longitude,
    elevationMetres = altitude.takeIf { hasAltitude() },
    horizontalAccuracyMetres = accuracy.toDouble().takeIf { hasAccuracy() },
    verticalAccuracyMetres = verticalAccuracyMeters.toDouble().takeIf { hasVerticalAccuracy() },
)

private const val NANOS_PER_MILLISECOND = 1_000_000L
