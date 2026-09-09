package com.jeppe.radm.performance

import androidx.room3.withWriteTransaction
import com.jeppe.radm.data.db.RadmDatabase
import com.jeppe.radm.data.db.entity.ActivityEntity
import com.jeppe.radm.data.db.entity.ActivityProcessorStateEntity
import com.jeppe.radm.data.db.entity.ActivitySummaryEntity
import com.jeppe.radm.data.db.entity.DerivedCadenceEntity
import com.jeppe.radm.data.db.entity.DerivedTrackMetricEntity
import com.jeppe.radm.data.db.entity.PositionSampleEntity

internal object M13PerformanceFixtures {
    const val REPRESENTATIVE_POSITION_COUNT = 10_000
    const val LARGE_POSITION_COUNT = 100_000
    const val LARGE_LIBRARY_COUNT = 10_000
    const val REPRESENTATIVE_ACTIVITY_ID = "13000000-0000-4000-8000-000000000001"
    const val LARGE_ACTIVITY_ID = "13000000-0000-4000-8000-000000000002"
    private const val BASE_TIME = 1_788_379_200_000L
    private const val BATCH_SIZE = 1_000

    suspend fun clear(database: RadmDatabase) {
        database.activityDao().deleteAllActivities()
    }

    suspend fun seedAnalysisActivity(
        database: RadmDatabase,
        activityId: String,
        positionCount: Int,
    ) {
        require(positionCount > 1)
        val activityDao = database.activityDao()
        val recordingDao = database.recordingDao()
        val derivedDao = database.derivedDao()
        activityDao.insertActivity(activity(activityId, positionCount, "M13 $positionCount-point activity"))

        (0 until positionCount).chunked(BATCH_SIZE).forEach { indexes ->
            recordingDao.insertPositions(indexes.map { position(activityId, it) })
            derivedDao.insertTrackMetrics(indexes.map { trackMetric(activityId, it) })
        }
        (0 until positionCount step 5).chunked(BATCH_SIZE).forEach { indexes ->
            derivedDao.insertCadence(indexes.map { cadence(activityId, it) })
        }
        activityDao.upsertSummary(summary(activityId, positionCount))
        derivedDao.upsertProcessorStates(processorStates(activityId))
    }

    suspend fun seedLargeLibrary(database: RadmDatabase, count: Int = LARGE_LIBRARY_COUNT) {
        require(count > 0)
        val activityDao = database.activityDao()
        val derivedDao = database.derivedDao()
        (0 until count).chunked(BATCH_SIZE).forEach { indexes ->
            database.withWriteTransaction {
                val activities = indexes.map { index ->
                    activity(libraryActivityId(index), 3_600, "Library activity $index", index)
                }
                activityDao.insertActivities(activities)
                activityDao.upsertSummaries(indexes.map { index -> summary(libraryActivityId(index), 3_600) })
                derivedDao.upsertProcessorStates(
                    indexes.map { index -> processorState(libraryActivityId(index), "summary") },
                )
            }
        }
    }

    fun libraryActivityId(index: Int): String =
        "13000000-0000-4000-9000-${index.toString().padStart(12, '0')}"

    private fun activity(
        activityId: String,
        positionCount: Int,
        title: String,
        offset: Int = 0,
    ): ActivityEntity {
        val duration = (positionCount - 1).coerceAtLeast(0) * 1_000L
        val started = BASE_TIME + offset * 60_000L
        return ActivityEntity(
            activityId = activityId,
            provenanceType = "RADM_NATIVE",
            provenanceExternalId = null,
            activityType = "RUNNING",
            title = title,
            notes = "Deterministic M13 performance fixture",
            startedAtUtcMs = started,
            endedAtUtcMs = started + duration,
            savedAtUtcMs = started + duration + 1_000L,
            activeDurationMs = duration,
            createdAtUtcMs = BASE_TIME,
            updatedAtUtcMs = BASE_TIME,
        )
    }

    private fun position(activityId: String, index: Int): PositionSampleEntity =
        PositionSampleEntity(
            activityId = activityId,
            sampleIndex = index.toLong(),
            routeSegmentIndex = 0L,
            timestampUtcMs = BASE_TIME + index * 1_000L,
            elapsedMs = index * 1_000L,
            latitudeDeg = 59.30 + (index % 2_000) * 0.000002,
            longitudeDeg = 18.00 + index * 0.000002,
            elevationM = 20.0 + (index % 200) * 0.05,
            horizontalAccuracyM = 4.0,
            verticalAccuracyM = 6.0,
        )

    private fun trackMetric(activityId: String, index: Int): DerivedTrackMetricEntity =
        DerivedTrackMetricEntity(
            activityId = activityId,
            sampleIndex = index.toLong(),
            cumulativeDistanceM = index * 2.8,
            paceSPerKm = 345.0 + (index % 120) * 0.25,
            speedMps = null,
        )

    private fun cadence(activityId: String, index: Int): DerivedCadenceEntity =
        DerivedCadenceEntity(
            activityId = activityId,
            sampleIndex = index.toLong(),
            elapsedMs = index * 1_000L,
            cadenceSpm = 164.0 + (index % 12),
        )

    private fun summary(activityId: String, positionCount: Int): ActivitySummaryEntity =
        ActivitySummaryEntity(
            activityId = activityId,
            distanceM = (positionCount - 1).coerceAtLeast(0) * 2.8,
            averagePaceSPerKm = 360.0,
            averageSpeedMps = null,
            minElevationM = 20.0,
            maxElevationM = 29.95,
            totalAscentM = null,
        )

    private fun processorStates(activityId: String): List<ActivityProcessorStateEntity> =
        listOf("distance", "pace", "cadence", "summary").map { processorState(activityId, it) }

    private fun processorState(activityId: String, processor: String) = ActivityProcessorStateEntity(
        activityId = activityId,
        processorName = processor,
        processorVersion = 1,
        status = "CURRENT",
        processedAtUtcMs = BASE_TIME,
    )
}
