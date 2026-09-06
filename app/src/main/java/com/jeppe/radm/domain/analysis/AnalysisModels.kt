package com.jeppe.radm.domain.analysis

import com.jeppe.radm.domain.model.ActiveElapsedTimeMillis
import com.jeppe.radm.domain.model.DistanceMetres

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
