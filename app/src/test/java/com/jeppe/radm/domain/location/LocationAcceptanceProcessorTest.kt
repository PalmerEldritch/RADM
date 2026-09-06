package com.jeppe.radm.domain.location

import com.jeppe.radm.domain.model.LatitudeDegrees
import com.jeppe.radm.domain.model.LongitudeDegrees
import com.jeppe.radm.domain.model.MonotonicTimeMillis
import com.jeppe.radm.domain.model.RouteSegmentIndex
import com.jeppe.radm.domain.recording.LocationCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationAcceptanceProcessorTest {
    @Test
    fun `VVM LOC 001 rejects invalid structure`() {
        val candidates = listOf(
            candidate(latitude = 90.1),
            candidate(longitude = 180.1),
            candidate(latitude = Double.NaN),
            candidate(longitude = Double.POSITIVE_INFINITY),
            candidate(horizontalAccuracy = Double.NaN),
            candidate(horizontalAccuracy = -1.0),
        )

        candidates.forEach { invalid ->
            val processor = processor()
            assertRejected(LocationRejectionReason.INVALID_STRUCTURE, processor.accept(invalid, at(0L)))
            assertNull(processor.snapshot(at(0L)).distance)
        }
    }

    @Test
    fun `VVM LOC 002 applies the ten second freshness threshold`() {
        val processor = processor()

        assertRejected(
            LocationRejectionReason.STALE,
            processor.accept(candidate(), at(R00LocationDefaults.MAXIMUM_LOCATION_AGE_MS + 1L)),
        )
        assertTrue(
            processor().accept(
                candidate(),
                at(R00LocationDefaults.MAXIMUM_LOCATION_AGE_MS),
            ) is LocationAcceptanceResult.Accepted,
        )
    }

    @Test
    fun `VVM LOC 003 accepts 29 metre accuracy and rejects 31 metre accuracy`() {
        val poorProcessor = processor()
        assertRejected(
            LocationRejectionReason.POOR_HORIZONTAL_ACCURACY,
            poorProcessor.accept(candidate(horizontalAccuracy = 31.0), at(0L)),
        )

        val accepted = processor().accept(candidate(horizontalAccuracy = 29.0), at(0L))
        assertTrue(accepted is LocationAcceptanceResult.Accepted)

        assertRejected(
            LocationRejectionReason.MISSING_HORIZONTAL_ACCURACY,
            processor().accept(candidate(horizontalAccuracy = null), at(0L)),
        )
    }

    @Test
    fun `VVM LOC 004 rejects a repeated source measurement`() {
        val processor = processor()
        val source = candidate()

        assertTrue(processor.accept(source, at(0L)) is LocationAcceptanceResult.Accepted)
        assertRejected(LocationRejectionReason.DUPLICATE, processor.accept(source, at(0L)))
    }

    @Test
    fun `source time ordering rejects a candidate older than the retained source`() {
        val processor = processor()
        assertTrue(processor.accept(candidate(offsetMillis = 2_000L), at(2_000L)) is LocationAcceptanceResult.Accepted)

        assertRejected(
            LocationRejectionReason.OUT_OF_ORDER,
            processor.accept(candidate(offsetMillis = 1_000L), at(3_000L)),
        )
    }

    @Test
    fun `VVM LOC 005 rejects a gross jump and adds no distance`() {
        val processor = processor()
        assertTrue(processor.accept(candidate(), at(0L)) is LocationAcceptanceResult.Accepted)

        assertRejected(
            LocationRejectionReason.GROSS_JUMP,
            processor.accept(candidate(offsetMillis = 1_000L, latitude = 1.0), at(1_000L)),
        )
        assertEquals(0.0, requireNotNull(processor.snapshot(at(1_000L)).distance).value, 0.0)
    }

    @Test
    fun `VVM LOC 006 starts a new segment after fifteen seconds without accepted location`() {
        val processor = processor()
        val first = accepted(processor.accept(candidate(), at(0L)))
        val afterGap = accepted(
            processor.accept(
                candidate(
                    offsetMillis = R00LocationDefaults.ROUTE_GAP_TIMEOUT_MS,
                    latitude = 1.0,
                ),
                at(R00LocationDefaults.ROUTE_GAP_TIMEOUT_MS),
            ),
        )

        assertEquals(0L, first.routeSegmentIndex.value)
        assertEquals(1L, afterGap.routeSegmentIndex.value)
        assertEquals(0.0, afterGap.distanceFromPrevious.value, 0.0)
        assertEquals(0.0, afterGap.cumulativeDistance.value, 0.0)
    }

    @Test
    fun `VVM LOC 007 keeps a short interruption in the current segment`() {
        val processor = processor()
        accepted(processor.accept(candidate(), at(0L)))
        val next = accepted(
            processor.accept(
                candidate(offsetMillis = 14_999L, longitude = 0.001),
                at(14_999L),
            ),
        )

        assertEquals(0L, next.routeSegmentIndex.value)
        assertTrue(next.cumulativeDistance.value > 100.0)
    }

    @Test
    fun `VVM LOC 010 retains a route with explicitly unavailable elevation`() {
        val processor = processor()

        val first = accepted(processor.accept(candidate(elevation = null), at(0L)))
        val second = accepted(
            processor.accept(
                candidate(offsetMillis = 1_000L, longitude = 0.0001, elevation = null),
                at(1_000L),
            ),
        )

        assertNull(first.elevation)
        assertNull(second.elevation)
        assertTrue(second.cumulativeDistance.value > 0.0)
        assertEquals(LocationAvailability.AVAILABLE, processor.snapshot(at(1_000L)).availability)
    }

    @Test
    fun `location availability progresses without resetting accumulated distance`() {
        val processor = processor()
        assertEquals(LocationAvailability.ACQUIRING, processor.snapshot(at(0L)).availability)
        assertEquals(LocationAvailability.UNAVAILABLE, processor.snapshot(at(15_000L)).availability)

        accepted(processor.accept(candidate(offsetMillis = 15_000L), at(15_000L)))
        assertEquals(LocationAvailability.AVAILABLE, processor.snapshot(at(15_000L)).availability)
        assertEquals(0.0, requireNotNull(processor.snapshot(at(15_000L)).distance).value, 0.0)

        processor.providerUnavailable()
        val unavailable = processor.snapshot(at(16_000L))
        assertEquals(LocationAvailability.UNAVAILABLE, unavailable.availability)
        assertEquals(0.0, requireNotNull(unavailable.distance).value, 0.0)

        processor.providerAvailable(at(16_000L))
        val reacquiring = processor.snapshot(at(16_000L))
        assertEquals(LocationAvailability.ACQUIRING, reacquiring.availability)
        assertEquals(0.0, requireNotNull(reacquiring.distance).value, 0.0)
    }

    @Test
    fun `haversine distance uses stable WGS84 short-route semantics`() {
        val distance = GeoDistance.haversine(
            LatitudeDegrees(0.0),
            LongitudeDegrees(0.0),
            LatitudeDegrees(0.0),
            LongitudeDegrees(0.001),
        )

        assertEquals(111.195, distance.value, 0.01)
    }

    private fun processor() = LocationAcceptanceProcessor(
        acquisitionStartedAt = at(0L),
        initialRouteSegmentIndex = RouteSegmentIndex(0L),
    )

    private fun candidate(
        offsetMillis: Long = 0L,
        latitude: Double = 0.0,
        longitude: Double = 0.0,
        elevation: Double? = null,
        horizontalAccuracy: Double? = 4.0,
    ) = LocationCandidate(
        timestampUtcMillis = BASE_UTC + offsetMillis,
        monotonicTimestampMillis = BASE_MONOTONIC + offsetMillis,
        latitudeDegrees = latitude,
        longitudeDegrees = longitude,
        elevationMetres = elevation,
        horizontalAccuracyMetres = horizontalAccuracy,
    )

    private fun at(offsetMillis: Long) = MonotonicTimeMillis(BASE_MONOTONIC + offsetMillis)

    private fun accepted(result: LocationAcceptanceResult) =
        (result as LocationAcceptanceResult.Accepted).measurement

    private fun assertRejected(
        expected: LocationRejectionReason,
        result: LocationAcceptanceResult,
    ) {
        assertEquals(expected, (result as LocationAcceptanceResult.Rejected).reason)
    }

    private companion object {
        const val BASE_UTC = 1_788_379_200_000L
        const val BASE_MONOTONIC = 10_000L
    }
}
