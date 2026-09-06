package com.jeppe.radm.platform.location

import android.location.Location
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidLocationSourceTest {
    @Test
    fun androidLocationMapsToSourceNeutralCandidateWithAvailableMetadata() {
        val location = Location("gps").apply {
            time = 1_788_379_200_123L
            elapsedRealtimeNanos = 12_345_000_000L
            latitude = 59.3293
            longitude = 18.0686
            altitude = 14.5
            accuracy = 4.25f
            verticalAccuracyMeters = 7.5f
        }

        val candidate = location.toLocationCandidate()

        assertEquals(1_788_379_200_123L, candidate.timestampUtcMillis)
        assertEquals(12_345L, candidate.monotonicTimestampMillis)
        assertEquals(59.3293, candidate.latitudeDegrees, 0.0)
        assertEquals(18.0686, candidate.longitudeDegrees, 0.0)
        assertEquals(14.5, candidate.elevationMetres!!, 0.0)
        assertEquals(4.25, candidate.horizontalAccuracyMetres!!, 0.0)
        assertEquals(7.5, candidate.verticalAccuracyMetres!!, 0.0)
    }

    @Test
    fun unavailableOptionalAndroidMeasurementsRemainMissing() {
        val location = Location("gps").apply {
            time = 1_788_379_200_123L
            elapsedRealtimeNanos = 12_345_000_000L
            latitude = 59.3293
            longitude = 18.0686
        }

        val candidate = location.toLocationCandidate()

        assertNull(candidate.elevationMetres)
        assertNull(candidate.horizontalAccuracyMetres)
        assertNull(candidate.verticalAccuracyMetres)
    }
}
