package com.jeppe.radm.data.db.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index

@Entity(
    tableName = "position_samples",
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
            name = "idx_position_activity_elapsed",
        ),
        Index(
            value = ["activity_id", "route_segment_index", "sample_index"],
            name = "idx_position_activity_segment_sample",
        ),
    ],
)
data class PositionSampleEntity(
    @ColumnInfo(name = "activity_id")
    val activityId: String,
    @ColumnInfo(name = "sample_index")
    val sampleIndex: Long,
    @ColumnInfo(name = "route_segment_index")
    val routeSegmentIndex: Long,
    @ColumnInfo(name = "timestamp_utc_ms")
    val timestampUtcMs: Long,
    @ColumnInfo(name = "elapsed_ms")
    val elapsedMs: Long,
    @ColumnInfo(name = "latitude_deg")
    val latitudeDeg: Double,
    @ColumnInfo(name = "longitude_deg")
    val longitudeDeg: Double,
    @ColumnInfo(name = "elevation_m")
    val elevationM: Double?,
    @ColumnInfo(name = "horizontal_accuracy_m")
    val horizontalAccuracyM: Double?,
    @ColumnInfo(name = "vertical_accuracy_m")
    val verticalAccuracyM: Double?,
)

@Entity(
    tableName = "step_samples",
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
            name = "idx_step_activity_elapsed",
        ),
    ],
)
data class StepSampleEntity(
    @ColumnInfo(name = "activity_id")
    val activityId: String,
    @ColumnInfo(name = "sample_index")
    val sampleIndex: Long,
    @ColumnInfo(name = "counter_epoch")
    val counterEpoch: Long,
    @ColumnInfo(name = "timestamp_utc_ms")
    val timestampUtcMs: Long,
    @ColumnInfo(name = "elapsed_ms")
    val elapsedMs: Long,
    @ColumnInfo(name = "cumulative_steps")
    val cumulativeSteps: Long,
)
