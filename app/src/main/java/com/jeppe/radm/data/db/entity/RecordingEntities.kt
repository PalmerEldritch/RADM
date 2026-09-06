package com.jeppe.radm.data.db.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "recording_sessions",
    foreignKeys = [
        ForeignKey(
            entity = ActivityEntity::class,
            parentColumns = ["activity_id"],
            childColumns = ["activity_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["activity_id"], unique = true)],
)
data class RecordingSessionEntity(
    @PrimaryKey
    @ColumnInfo(name = "singleton_id")
    val singletonId: Int = SINGLETON_ID,
    @ColumnInfo(name = "activity_id")
    val activityId: String,
    val state: String,
    @ColumnInfo(name = "active_elapsed_ms")
    val activeElapsedMs: Long,
    @ColumnInfo(name = "state_entered_at_utc_ms")
    val stateEnteredAtUtcMs: Long,
    @ColumnInfo(name = "last_checkpoint_at_utc_ms")
    val lastCheckpointAtUtcMs: Long,
    @ColumnInfo(name = "route_segment_index", defaultValue = "0")
    val routeSegmentIndex: Long = 0L,
    @ColumnInfo(name = "position_sample_count", defaultValue = "0")
    val positionSampleCount: Long = 0L,
    @ColumnInfo(name = "step_sample_count", defaultValue = "0")
    val stepSampleCount: Long = 0L,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}

@Entity(
    tableName = "recording_events",
    primaryKeys = ["activity_id", "event_index"],
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
            value = ["activity_id", "active_elapsed_ms"],
            name = "idx_recording_event_activity_elapsed",
        ),
    ],
)
data class RecordingEventEntity(
    @ColumnInfo(name = "activity_id")
    val activityId: String,
    @ColumnInfo(name = "event_index")
    val eventIndex: Long,
    @ColumnInfo(name = "event_type")
    val eventType: String,
    @ColumnInfo(name = "occurred_at_utc_ms")
    val occurredAtUtcMs: Long,
    @ColumnInfo(name = "active_elapsed_ms")
    val activeElapsedMs: Long,
)
