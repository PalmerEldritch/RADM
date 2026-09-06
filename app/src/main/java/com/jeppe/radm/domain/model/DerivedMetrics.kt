package com.jeppe.radm.domain.model

data class DistanceSample(
    val activityId: ActivityId,
    val positionSampleIndex: SampleIndex,
    val activeElapsedTime: ActiveElapsedTimeMillis,
    val cumulativeDistance: DistanceMetres,
)

data class PaceSample(
    val activityId: ActivityId,
    val positionSampleIndex: SampleIndex,
    val activeElapsedTime: ActiveElapsedTimeMillis,
    val pace: PaceSecondsPerKilometre?,
)

data class SpeedSample(
    val activityId: ActivityId,
    val positionSampleIndex: SampleIndex,
    val activeElapsedTime: ActiveElapsedTimeMillis,
    val speed: SpeedMetresPerSecond?,
)

data class CadenceSample(
    val activityId: ActivityId,
    val sampleIndex: SampleIndex,
    val activeElapsedTime: ActiveElapsedTimeMillis,
    val cadence: CadenceStepsPerMinute?,
)

/** Persistence-neutral aggregate matching one position-aligned derived row. */
data class DerivedTrackMetric(
    val activityId: ActivityId,
    val positionSampleIndex: SampleIndex,
    val cumulativeDistance: DistanceMetres,
    val pace: PaceSecondsPerKilometre?,
    val speed: SpeedMetresPerSecond?,
)

data class ActivitySummary(
    val activityId: ActivityId,
    val distance: DistanceMetres?,
    val averagePace: PaceSecondsPerKilometre?,
    val averageSpeed: SpeedMetresPerSecond?,
    val minimumElevation: ElevationMetres?,
    val maximumElevation: ElevationMetres?,
    val totalAscent: DistanceMetres?,
) {
    init {
        if (minimumElevation != null && maximumElevation != null) {
            require(minimumElevation.value <= maximumElevation.value) {
                "Minimum elevation must not exceed maximum elevation"
            }
        }
    }
}

@JvmInline
value class ProcessorName(val value: String) {
    init {
        require(value.isNotBlank()) { "Processor name must not be blank" }
    }

    companion object {
        val DISTANCE = ProcessorName("distance")
        val PACE = ProcessorName("pace")
        val SPEED = ProcessorName("speed")
        val CADENCE = ProcessorName("cadence")
        val SUMMARY = ProcessorName("summary")
    }
}

data class ProcessorVersion(
    val processor: ProcessorName,
    val version: Int,
) {
    init {
        require(version >= 1) { "Processor versions begin at 1" }
    }
}

data class ProcessorDefinition(
    val name: ProcessorName,
    val currentVersion: Int,
) {
    init {
        require(currentVersion >= 1) { "Current processor version must begin at 1" }
    }
}

enum class ProcessorStatus {
    CURRENT,
    UNPROCESSED,
    FAILED,
}

data class ActivityProcessorState(
    val activityId: ActivityId,
    val processorName: ProcessorName,
    val processorVersion: Int?,
    val status: ProcessorStatus,
    val processedAt: AbsoluteTimestampUtcMillis?,
) {
    init {
        require(processorVersion == null || processorVersion >= 1) {
            "A stored processor version must begin at 1"
        }
        require(status != ProcessorStatus.CURRENT || processorVersion != null) {
            "Current processor state requires a version"
        }
    }

    fun isCurrentAgainst(definition: ProcessorDefinition): Boolean =
        processorName == definition.name &&
            status == ProcessorStatus.CURRENT &&
            processorVersion == definition.currentVersion
}
