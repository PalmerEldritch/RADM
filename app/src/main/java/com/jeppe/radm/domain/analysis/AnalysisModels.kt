package com.jeppe.radm.domain.analysis

import com.jeppe.radm.domain.model.ActiveElapsedTimeMillis
import com.jeppe.radm.domain.model.Activity
import com.jeppe.radm.domain.model.ActivitySummary
import com.jeppe.radm.domain.model.CadenceStepsPerMinute
import com.jeppe.radm.domain.model.DistanceMetres
import com.jeppe.radm.domain.model.ElevationMetres
import com.jeppe.radm.domain.model.PaceSecondsPerKilometre
import com.jeppe.radm.domain.model.ProcessorName
import com.jeppe.radm.domain.model.RouteSegment
import com.jeppe.radm.domain.model.SpeedMetresPerSecond

/** Active elapsed time is always present and remains the authoritative coordinate. */
data class AnalysisPosition(
    val activeElapsedTime: ActiveElapsedTimeMillis,
    val cumulativeDistance: DistanceMetres? = null,
)

data class AnalysisRange(
    val start: AnalysisPosition,
    val endInclusive: AnalysisPosition,
) {
    init {
        require(start.activeElapsedTime <= endInclusive.activeElapsedTime) {
            "Analysis range start must not follow its end"
        }
        if (start.cumulativeDistance != null && endInclusive.cumulativeDistance != null) {
            require(start.cumulativeDistance <= endInclusive.cumulativeDistance) {
                "Analysis range distance must be non-decreasing"
            }
        }
    }
}

enum class AnalysisCoordinateMode {
    DISTANCE,
    ACTIVE_ELAPSED_TIME,
}

data class ActivityAnalysisState(
    val selectedPosition: AnalysisPosition,
    val range: AnalysisRange,
    val coordinateMode: AnalysisCoordinateMode,
)

enum class AnalysisProcessorStatus {
    CURRENT,
    STALE,
    UNPROCESSED,
    FAILED,
    NOT_APPLICABLE,
}

data class AnalysisProcessorValidity(
    val processorName: ProcessorName,
    val status: AnalysisProcessorStatus,
    val storedVersion: Int?,
    val currentVersion: Int?,
)

enum class AnalysisSeriesAvailability {
    AVAILABLE,
    UNAVAILABLE_NO_SOURCE,
    UNAVAILABLE_NOT_CURRENT,
    UNAVAILABLE_NOT_APPLICABLE,
}

/**
 * One in-memory analysis point. [continuityGroup] prevents presentation adapters
 * from joining route or counter-epoch discontinuities.
 */
data class AnalysisPoint<T>(
    val position: AnalysisPosition,
    val value: T?,
    val continuityGroup: Long,
)

data class AnalysisSeries<T>(
    val availability: AnalysisSeriesAvailability,
    val points: List<AnalysisPoint<T>>,
) {
    init {
        require(
            availability != AnalysisSeriesAvailability.AVAILABLE || points.any { it.value != null },
        ) { "An available analysis series requires at least one value" }
        require(
            availability == AnalysisSeriesAvailability.AVAILABLE || points.isEmpty(),
        ) { "An unavailable analysis series must not expose values" }
    }
}

data class AnalysisInspectorValues(
    val position: AnalysisPosition,
    val pace: PaceSecondsPerKilometre?,
    val speed: SpeedMetresPerSecond?,
    val elevation: ElevationMetres?,
    val cadence: CadenceStepsPerMinute?,
)

/** Complete persistence-neutral dataset loaded once for analysis interaction. */
data class ActivityAnalysisData(
    val activity: Activity,
    val summary: ActivitySummary?,
    val routeSegments: List<RouteSegment>,
    val distance: AnalysisSeries<DistanceMetres>,
    val pace: AnalysisSeries<PaceSecondsPerKilometre>,
    val speed: AnalysisSeries<SpeedMetresPerSecond>,
    val elevation: AnalysisSeries<ElevationMetres>,
    val cadence: AnalysisSeries<CadenceStepsPerMinute>,
    val processorValidity: Map<ProcessorName, AnalysisProcessorValidity>,
    val initialState: ActivityAnalysisState,
    val initialInspector: AnalysisInspectorValues,
)
