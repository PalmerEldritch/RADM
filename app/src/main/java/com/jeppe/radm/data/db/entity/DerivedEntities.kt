package com.jeppe.radm.data.db.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "derived_track_metrics",
    primaryKeys = ["activity_id", "sample_index"],
    foreignKeys = [
        ForeignKey(
            entity = PositionSampleEntity::class,
            parentColumns = ["activity_id", "sample_index"],
            childColumns = ["activity_id", "sample_index"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class DerivedTrackMetricEntity(
    @ColumnInfo(name = "activity_id")
    val activityId: String,
    @ColumnInfo(name = "sample_index")
    val sampleIndex: Long,
    @ColumnInfo(name = "cumulative_distance_m")
    val cumulativeDistanceM: Double,
    @ColumnInfo(name = "pace_s_per_km")
    val paceSPerKm: Double?,
    @ColumnInfo(name = "speed_mps")
    val speedMps: Double?,
)

@Entity(
    tableName = "derived_cadence",
    primaryKeys = ["activity_id", "sample_index"],
    foreignKeys = [
        ForeignKey(
            entity = ActivityEntity::class,
            parentColumns = ["activity_id"],
            childColumns = ["activity_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(
            value = ["activity_id", "elapsed_ms"],
            name = "idx_cadence_activity_elapsed",
        ),
    ],
)
data class DerivedCadenceEntity(
    @ColumnInfo(name = "activity_id")
    val activityId: String,
    @ColumnInfo(name = "sample_index")
    val sampleIndex: Long,
    @ColumnInfo(name = "elapsed_ms")
    val elapsedMs: Long,
    @ColumnInfo(name = "cadence_spm")
    val cadenceSpm: Double?,
)

@Entity(tableName = "processor_definitions")
data class ProcessorDefinitionEntity(
    @PrimaryKey
    @ColumnInfo(name = "processor_name")
    val processorName: String,
    @ColumnInfo(name = "current_version")
    val currentVersion: Int,
)

@Entity(
    tableName = "activity_processor_state",
    primaryKeys = ["activity_id", "processor_name"],
    foreignKeys = [
        ForeignKey(
            entity = ActivityEntity::class,
            parentColumns = ["activity_id"],
            childColumns = ["activity_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ProcessorDefinitionEntity::class,
            parentColumns = ["processor_name"],
            childColumns = ["processor_name"],
        ),
    ],
    indices = [
        Index(value = ["processor_name"], name = "idx_activity_processor_name"),
    ],
)
data class ActivityProcessorStateEntity(
    @ColumnInfo(name = "activity_id")
    val activityId: String,
    @ColumnInfo(name = "processor_name")
    val processorName: String,
    @ColumnInfo(name = "processor_version")
    val processorVersion: Int?,
    val status: String,
    @ColumnInfo(name = "processed_at_utc_ms")
    val processedAtUtcMs: Long?,
)

data class ProcessorStateWithDefinitionEntity(
    @ColumnInfo(name = "activity_id")
    val activityId: String,
    @ColumnInfo(name = "processor_name")
    val processorName: String,
    @ColumnInfo(name = "processor_version")
    val processorVersion: Int?,
    val status: String,
    @ColumnInfo(name = "processed_at_utc_ms")
    val processedAtUtcMs: Long?,
    @ColumnInfo(name = "current_processor_version")
    val currentProcessorVersion: Int,
)
