package com.jeppe.radm.domain.location

import com.jeppe.radm.domain.model.AbsoluteTimestampUtcMillis
import com.jeppe.radm.domain.model.AccuracyMetres
import com.jeppe.radm.domain.model.DistanceMetres
import com.jeppe.radm.domain.model.ElevationMetres
import com.jeppe.radm.domain.model.LatitudeDegrees
import com.jeppe.radm.domain.model.LongitudeDegrees
import com.jeppe.radm.domain.model.MonotonicTimeMillis
import com.jeppe.radm.domain.model.RouteSegmentIndex
import com.jeppe.radm.domain.recording.LocationCandidate
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object R00LocationDefaults {
    const val DESIRED_UPDATE_INTERVAL_MS = 1_000L
    const val MINIMUM_UPDATE_INTERVAL_MS = 1_000L
    const val MAXIMUM_LOCATION_AGE_MS = 10_000L
    const val MAXIMUM_HORIZONTAL_ACCURACY_METRES = 30.0
    const val GROSS_JUMP_SPEED_METRES_PER_SECOND = 60.0
    const val ROUTE_GAP_TIMEOUT_MS = 15_000L
}

data class LocationAcceptanceConfig(
    val maximumLocationAgeMillis: Long = R00LocationDefaults.MAXIMUM_LOCATION_AGE_MS,
    val maximumHorizontalAccuracyMetres: Double =
        R00LocationDefaults.MAXIMUM_HORIZONTAL_ACCURACY_METRES,
    val grossJumpSpeedMetresPerSecond: Double =
        R00LocationDefaults.GROSS_JUMP_SPEED_METRES_PER_SECOND,
    val routeGapTimeoutMillis: Long = R00LocationDefaults.ROUTE_GAP_TIMEOUT_MS,
    val acceptMissingHorizontalAccuracy: Boolean = false,
) {
    init {
        require(maximumLocationAgeMillis >= 0L)
        require(maximumHorizontalAccuracyMetres >= 0.0)
        require(grossJumpSpeedMetresPerSecond > 0.0)
        require(routeGapTimeoutMillis > 0L)
    }
}

enum class LocationAvailability {
    ACQUIRING,
    AVAILABLE,
    DEGRADED,
    UNAVAILABLE,
}

enum class LocationRejectionReason {
    INVALID_STRUCTURE,
    STALE,
    MISSING_HORIZONTAL_ACCURACY,
    POOR_HORIZONTAL_ACCURACY,
    DUPLICATE,
    OUT_OF_ORDER,
    GROSS_JUMP,
}

data class LiveLocationSnapshot(
    val availability: LocationAvailability,
    val distance: DistanceMetres?,
)

data class AcceptedLocationMeasurement(
    val timestamp: AbsoluteTimestampUtcMillis,
    val monotonicTimestamp: MonotonicTimeMillis,
    val latitude: LatitudeDegrees,
    val longitude: LongitudeDegrees,
    val elevation: ElevationMetres?,
    val horizontalAccuracy: AccuracyMetres?,
    val verticalAccuracy: AccuracyMetres?,
    val routeSegmentIndex: RouteSegmentIndex,
    val distanceFromPrevious: DistanceMetres,
    val cumulativeDistance: DistanceMetres,
)

sealed interface LocationAcceptanceResult {
    data class Accepted(val measurement: AcceptedLocationMeasurement) : LocationAcceptanceResult
    data class Rejected(val reason: LocationRejectionReason) : LocationAcceptanceResult
}

/** Stateful, Android-independent implementation of the REC R00 live acceptance pipeline. */
class LocationAcceptanceProcessor(
    acquisitionStartedAt: MonotonicTimeMillis,
    initialRouteSegmentIndex: RouteSegmentIndex,
    private val config: LocationAcceptanceConfig = LocationAcceptanceConfig(),
) {
    private var segmentStartedAt = acquisitionStartedAt
    private var currentRouteSegmentIndex = initialRouteSegmentIndex
    private var lastAccepted: AcceptedLocationMeasurement? = null
    private var cumulativeDistance = DistanceMetres.ZERO
    private var hasAcceptedLocation = false
    private var providerAvailable = true
    private var waitingForFix = true
    private var degraded = false

    fun accept(
        candidate: LocationCandidate,
        evaluatedAt: MonotonicTimeMillis,
    ): LocationAcceptanceResult {
        val normalized = normalize(candidate, evaluatedAt)
            ?: return reject(LocationRejectionReason.INVALID_STRUCTURE)

        val ageMillis = evaluatedAt.value - normalized.monotonicTimestamp.value
        if (ageMillis > config.maximumLocationAgeMillis) {
            return reject(LocationRejectionReason.STALE)
        }
        val horizontalAccuracy = normalized.horizontalAccuracy
        if (horizontalAccuracy == null && !config.acceptMissingHorizontalAccuracy) {
            return reject(LocationRejectionReason.MISSING_HORIZONTAL_ACCURACY)
        }
        if (
            horizontalAccuracy != null &&
            horizontalAccuracy.value > config.maximumHorizontalAccuracyMetres
        ) {
            return reject(LocationRejectionReason.POOR_HORIZONTAL_ACCURACY)
        }

        val previous = lastAccepted
        if (previous != null) {
            if (sameSourceMeasurement(previous, normalized)) {
                return LocationAcceptanceResult.Rejected(LocationRejectionReason.DUPLICATE)
            }
            if (normalized.monotonicTimestamp <= previous.monotonicTimestamp) {
                return LocationAcceptanceResult.Rejected(LocationRejectionReason.OUT_OF_ORDER)
            }
        }

        val sourceIntervalMillis = previous?.let {
            normalized.monotonicTimestamp.value - it.monotonicTimestamp.value
        }
        val beginsGapSegment = sourceIntervalMillis != null &&
            sourceIntervalMillis >= config.routeGapTimeoutMillis
        val routeSegmentIndex = if (beginsGapSegment) {
            RouteSegmentIndex(Math.addExact(currentRouteSegmentIndex.value, 1L))
        } else {
            currentRouteSegmentIndex
        }
        val geographicalDistance = previous?.let {
            GeoDistance.haversine(
                it.latitude,
                it.longitude,
                normalized.latitude,
                normalized.longitude,
            )
        } ?: DistanceMetres.ZERO

        if (previous != null && !beginsGapSegment) {
            val impliedSpeed = geographicalDistance.value / (sourceIntervalMillis!! / 1_000.0)
            if (impliedSpeed > config.grossJumpSpeedMetresPerSecond) {
                return reject(LocationRejectionReason.GROSS_JUMP)
            }
        }

        val distanceIncrement = if (previous == null || beginsGapSegment) {
            DistanceMetres.ZERO
        } else {
            geographicalDistance
        }
        cumulativeDistance = DistanceMetres(cumulativeDistance.value + distanceIncrement.value)
        currentRouteSegmentIndex = routeSegmentIndex
        val accepted = normalized.copy(
            routeSegmentIndex = routeSegmentIndex,
            distanceFromPrevious = distanceIncrement,
            cumulativeDistance = cumulativeDistance,
        )
        lastAccepted = accepted
        hasAcceptedLocation = true
        providerAvailable = true
        waitingForFix = false
        degraded = false
        return LocationAcceptanceResult.Accepted(accepted)
    }

    fun providerUnavailable() {
        providerAvailable = false
    }

    fun providerAvailable(at: MonotonicTimeMillis) {
        if (!providerAvailable) {
            segmentStartedAt = at
            waitingForFix = true
        }
        providerAvailable = true
        degraded = false
    }

    fun beginNewSegment(
        routeSegmentIndex: RouteSegmentIndex,
        acquisitionStartedAt: MonotonicTimeMillis,
    ) {
        currentRouteSegmentIndex = routeSegmentIndex
        segmentStartedAt = acquisitionStartedAt
        lastAccepted = null
        providerAvailable = true
        waitingForFix = true
        degraded = false
    }

    fun snapshot(evaluatedAt: MonotonicTimeMillis): LiveLocationSnapshot {
        val referenceTime = if (waitingForFix) {
            segmentStartedAt
        } else {
            lastAccepted?.monotonicTimestamp ?: segmentStartedAt
        }
        val timedOut = evaluatedAt.value - referenceTime.value >= config.routeGapTimeoutMillis
        val availability = when {
            !providerAvailable || timedOut -> LocationAvailability.UNAVAILABLE
            waitingForFix && degraded -> LocationAvailability.DEGRADED
            waitingForFix -> LocationAvailability.ACQUIRING
            degraded -> LocationAvailability.DEGRADED
            else -> LocationAvailability.AVAILABLE
        }
        return LiveLocationSnapshot(
            availability = availability,
            distance = cumulativeDistance.takeIf { hasAcceptedLocation },
        )
    }

    private fun normalize(
        candidate: LocationCandidate,
        evaluatedAt: MonotonicTimeMillis,
    ): AcceptedLocationMeasurement? {
        if (candidate.timestampUtcMillis < 0L || candidate.monotonicTimestampMillis < 0L) return null
        if (candidate.monotonicTimestampMillis < segmentStartedAt.value) return null
        if (candidate.monotonicTimestampMillis > evaluatedAt.value) return null
        if (!candidate.latitudeDegrees.isFinite() || candidate.latitudeDegrees !in -90.0..90.0) return null
        if (!candidate.longitudeDegrees.isFinite() || candidate.longitudeDegrees !in -180.0..180.0) return null
        if (!validAccuracy(candidate.horizontalAccuracyMetres)) return null

        return AcceptedLocationMeasurement(
            timestamp = AbsoluteTimestampUtcMillis(candidate.timestampUtcMillis),
            monotonicTimestamp = MonotonicTimeMillis(candidate.monotonicTimestampMillis),
            latitude = LatitudeDegrees(candidate.latitudeDegrees),
            longitude = LongitudeDegrees(candidate.longitudeDegrees),
            elevation = candidate.elevationMetres
                ?.takeIf(Double::isFinite)
                ?.let(::ElevationMetres),
            horizontalAccuracy = candidate.horizontalAccuracyMetres?.let(::AccuracyMetres),
            verticalAccuracy = candidate.verticalAccuracyMetres
                ?.takeIf(::validAccuracyValue)
                ?.let(::AccuracyMetres),
            routeSegmentIndex = currentRouteSegmentIndex,
            distanceFromPrevious = DistanceMetres.ZERO,
            cumulativeDistance = cumulativeDistance,
        )
    }

    private fun reject(reason: LocationRejectionReason): LocationAcceptanceResult.Rejected {
        degraded = true
        return LocationAcceptanceResult.Rejected(reason)
    }

    private fun validAccuracy(value: Double?): Boolean = value == null || validAccuracyValue(value)

    private fun validAccuracyValue(value: Double): Boolean = value.isFinite() && value >= 0.0

    private fun sameSourceMeasurement(
        previous: AcceptedLocationMeasurement,
        candidate: AcceptedLocationMeasurement,
    ): Boolean = previous.monotonicTimestamp == candidate.monotonicTimestamp &&
        previous.timestamp == candidate.timestamp &&
        previous.latitude == candidate.latitude &&
        previous.longitude == candidate.longitude
}

object GeoDistance {
    private const val MEAN_EARTH_RADIUS_METRES = 6_371_008.8

    fun haversine(
        startLatitude: LatitudeDegrees,
        startLongitude: LongitudeDegrees,
        endLatitude: LatitudeDegrees,
        endLongitude: LongitudeDegrees,
    ): DistanceMetres {
        val startLatitudeRadians = Math.toRadians(startLatitude.value)
        val endLatitudeRadians = Math.toRadians(endLatitude.value)
        val latitudeDelta = endLatitudeRadians - startLatitudeRadians
        val longitudeDelta = Math.toRadians(endLongitude.value - startLongitude.value)
        val haversine = sin(latitudeDelta / 2.0).let { it * it } +
            cos(startLatitudeRadians) * cos(endLatitudeRadians) *
            sin(longitudeDelta / 2.0).let { it * it }
        val centralAngle = 2.0 * asin(sqrt(haversine.coerceIn(0.0, 1.0)))
        return DistanceMetres(MEAN_EARTH_RADIUS_METRES * centralAngle)
    }
}
