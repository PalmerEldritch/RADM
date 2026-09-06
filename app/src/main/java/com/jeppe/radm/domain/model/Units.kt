package com.jeppe.radm.domain.model

private fun requireFinite(name: String, value: Double) {
    require(value.isFinite()) { "$name must be finite" }
}

private fun requireNonNegative(name: String, value: Double) {
    requireFinite(name, value)
    require(value >= 0.0) { "$name must be non-negative" }
}

/** Unix epoch UTC milliseconds. Kept distinct from activity-relative time. */
@JvmInline
value class AbsoluteTimestampUtcMillis(val value: Long) {
    init {
        require(value >= 0L) { "Absolute timestamp must be non-negative" }
    }
}

/** Canonical activity-relative active time; paused/recovery downtime is excluded. */
@JvmInline
value class ActiveElapsedTimeMillis(val value: Long) : Comparable<ActiveElapsedTimeMillis> {
    init {
        require(value >= 0L) { "Active elapsed time must be non-negative" }
    }

    override fun compareTo(other: ActiveElapsedTimeMillis): Int = value.compareTo(other.value)

    companion object {
        val ZERO = ActiveElapsedTimeMillis(0L)
    }
}

/** Process-local monotonic clock reading; never a civil/UTC timestamp. */
@JvmInline
value class MonotonicTimeMillis(val value: Long) : Comparable<MonotonicTimeMillis> {
    init {
        require(value >= 0L) { "Monotonic time must be non-negative" }
    }

    override fun compareTo(other: MonotonicTimeMillis): Int = value.compareTo(other.value)
}

/** General elevation in metres; negative values are valid below sea level. */
@JvmInline
value class ElevationMetres(val value: Double) {
    init {
        requireFinite("Elevation", value)
    }
}

/** Non-negative travelled or cumulative distance in metres. */
@JvmInline
value class DistanceMetres(val value: Double) : Comparable<DistanceMetres> {
    init {
        requireNonNegative("Distance", value)
    }

    override fun compareTo(other: DistanceMetres): Int = value.compareTo(other.value)

    companion object {
        val ZERO = DistanceMetres(0.0)
    }
}

/** Non-negative source accuracy radius in metres. */
@JvmInline
value class AccuracyMetres(val value: Double) {
    init {
        requireNonNegative("Accuracy", value)
    }
}

@JvmInline
value class SpeedMetresPerSecond(val value: Double) {
    init {
        requireNonNegative("Speed", value)
    }
}

@JvmInline
value class PaceSecondsPerKilometre(val value: Double) {
    init {
        requireNonNegative("Pace", value)
    }
}

@JvmInline
value class CadenceStepsPerMinute(val value: Double) {
    init {
        requireNonNegative("Cadence", value)
    }
}

@JvmInline
value class LatitudeDegrees(val value: Double) {
    init {
        requireFinite("Latitude", value)
        require(value in -90.0..90.0) { "Latitude must be between -90 and 90 degrees" }
    }
}

@JvmInline
value class LongitudeDegrees(val value: Double) {
    init {
        requireFinite("Longitude", value)
        require(value in -180.0..180.0) { "Longitude must be between -180 and 180 degrees" }
    }
}

@JvmInline
value class SampleIndex(val value: Long) : Comparable<SampleIndex> {
    init {
        require(value >= 0L) { "Sample index must be non-negative" }
    }

    override fun compareTo(other: SampleIndex): Int = value.compareTo(other.value)
}

@JvmInline
value class EventIndex(val value: Long) : Comparable<EventIndex> {
    init {
        require(value >= 0L) { "Event index must be non-negative" }
    }

    override fun compareTo(other: EventIndex): Int = value.compareTo(other.value)
}

@JvmInline
value class RouteSegmentIndex(val value: Long) : Comparable<RouteSegmentIndex> {
    init {
        require(value >= 0L) { "Route segment index must be non-negative" }
    }

    override fun compareTo(other: RouteSegmentIndex): Int = value.compareTo(other.value)
}

@JvmInline
value class StepCounterEpoch(val value: Long) : Comparable<StepCounterEpoch> {
    init {
        require(value >= 0L) { "Step counter epoch must be non-negative" }
    }

    override fun compareTo(other: StepCounterEpoch): Int = value.compareTo(other.value)
}
