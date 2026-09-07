package com.jeppe.radm.data.db.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "activities",
    indices = [
        Index(
            value = ["saved_at_utc_ms", "started_at_utc_ms"],
            name = "idx_activities_saved_start",
        ),
        Index(
            value = ["activity_type", "started_at_utc_ms"],
            name = "idx_activities_type_start",
        ),
    ],
)
data class ActivityEntity(
    @PrimaryKey
    @ColumnInfo(name = "activity_id")
    val activityId: String,
    @ColumnInfo(name = "provenance_type")
    val provenanceType: String,
    @ColumnInfo(name = "provenance_external_id")
    val provenanceExternalId: String?,
    @ColumnInfo(name = "activity_type")
    val activityType: String,
    val title: String?,
    val notes: String?,
    @ColumnInfo(name = "started_at_utc_ms")
    val startedAtUtcMs: Long,
    @ColumnInfo(name = "ended_at_utc_ms")
    val endedAtUtcMs: Long?,
    @ColumnInfo(name = "saved_at_utc_ms")
    val savedAtUtcMs: Long?,
    @ColumnInfo(name = "active_duration_ms", defaultValue = "0")
    val activeDurationMs: Long = 0L,
    @ColumnInfo(name = "created_at_utc_ms")
    val createdAtUtcMs: Long,
    @ColumnInfo(name = "updated_at_utc_ms")
    val updatedAtUtcMs: Long,
)

@Entity(
    tableName = "activity_summaries",
    foreignKeys = [
        ForeignKey(
            entity = ActivityEntity::class,
            parentColumns = ["activity_id"],
            childColumns = ["activity_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ActivitySummaryEntity(
    @PrimaryKey
    @ColumnInfo(name = "activity_id")
    val activityId: String,
    @ColumnInfo(name = "distance_m")
    val distanceM: Double?,
    @ColumnInfo(name = "average_pace_s_per_km")
    val averagePaceSPerKm: Double?,
    @ColumnInfo(name = "average_speed_mps")
    val averageSpeedMps: Double?,
    @ColumnInfo(name = "min_elevation_m")
    val minElevationM: Double?,
    @ColumnInfo(name = "max_elevation_m")
    val maxElevationM: Double?,
    @ColumnInfo(name = "total_ascent_m")
    val totalAscentM: Double?,
)

data class ActivityLibraryItemEntity(
    @ColumnInfo(name = "activity_id")
    val activityId: String,
    @ColumnInfo(name = "activity_type")
    val activityType: String,
    val title: String?,
    @ColumnInfo(name = "started_at_utc_ms")
    val startedAtUtcMs: Long,
    @ColumnInfo(name = "active_duration_ms")
    val activeDurationMs: Long,
    @ColumnInfo(name = "distance_m")
    val distanceM: Double?,
    @ColumnInfo(name = "average_pace_s_per_km")
    val averagePaceSPerKm: Double?,
    @ColumnInfo(name = "average_speed_mps")
    val averageSpeedMps: Double?,
)
