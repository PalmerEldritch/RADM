package com.jeppe.radm.data.db.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Upsert
import com.jeppe.radm.data.db.entity.ActivityEntity
import com.jeppe.radm.data.db.entity.ActivityLibraryItemEntity
import com.jeppe.radm.data.db.entity.ActivitySummaryEntity

@Dao
interface ActivityDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertActivity(activity: ActivityEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertActivities(activities: List<ActivityEntity>)

    @Query("SELECT * FROM activities WHERE activity_id = :activityId")
    suspend fun getActivity(activityId: String): ActivityEntity?

    @Query(
        """
        SELECT * FROM activities
        WHERE saved_at_utc_ms IS NOT NULL
        ORDER BY started_at_utc_ms DESC, activity_id ASC
        """,
    )
    suspend fun listSavedActivities(): List<ActivityEntity>

    @Query(
        """
        SELECT activities.activity_id,
               activities.activity_type,
               activities.title,
               activities.started_at_utc_ms,
               activities.active_duration_ms,
               CASE WHEN activity_processor_state.status = 'CURRENT'
                          AND activity_processor_state.processor_version = processor_definitions.current_version
                    THEN activity_summaries.distance_m END AS distance_m,
               CASE WHEN activity_processor_state.status = 'CURRENT'
                          AND activity_processor_state.processor_version = processor_definitions.current_version
                    THEN activity_summaries.average_pace_s_per_km END AS average_pace_s_per_km,
               CASE WHEN activity_processor_state.status = 'CURRENT'
                          AND activity_processor_state.processor_version = processor_definitions.current_version
                    THEN activity_summaries.average_speed_mps END AS average_speed_mps
        FROM activities
        LEFT JOIN activity_summaries USING (activity_id)
        LEFT JOIN activity_processor_state
               ON activity_processor_state.activity_id = activities.activity_id
              AND activity_processor_state.processor_name = 'summary'
        LEFT JOIN processor_definitions
               ON processor_definitions.processor_name = 'summary'
        WHERE activities.saved_at_utc_ms IS NOT NULL
        ORDER BY activities.started_at_utc_ms DESC, activities.activity_id ASC
        """,
    )
    suspend fun listLibraryItems(): List<ActivityLibraryItemEntity>

    @Query(
        """
        UPDATE activities
        SET activity_type = :activityType,
            title = :title,
            notes = :notes,
            updated_at_utc_ms = :updatedAtUtcMs
        WHERE activity_id = :activityId
        """,
    )
    suspend fun updateMetadata(
        activityId: String,
        activityType: String,
        title: String?,
        notes: String?,
        updatedAtUtcMs: Long,
    ): Int

    @Query(
        """
        UPDATE activities
        SET activity_type = :activityType,
            title = :title,
            notes = :notes,
            ended_at_utc_ms = :endedAtUtcMs,
            saved_at_utc_ms = :savedAtUtcMs,
            active_duration_ms = :activeDurationMs,
            updated_at_utc_ms = :updatedAtUtcMs
        WHERE activity_id = :activityId
        """,
    )
    suspend fun finalizeActivity(
        activityId: String,
        activityType: String,
        title: String?,
        notes: String?,
        endedAtUtcMs: Long,
        savedAtUtcMs: Long,
        activeDurationMs: Long,
        updatedAtUtcMs: Long,
    ): Int

    @Query("DELETE FROM activities WHERE activity_id = :activityId")
    suspend fun deleteActivity(activityId: String): Int

    @Query("DELETE FROM activities")
    suspend fun deleteAllActivities()

    @Upsert
    suspend fun upsertSummary(summary: ActivitySummaryEntity)

    @Upsert
    suspend fun upsertSummaries(summaries: List<ActivitySummaryEntity>)

    @Query("SELECT * FROM activity_summaries WHERE activity_id = :activityId")
    suspend fun getSummary(activityId: String): ActivitySummaryEntity?

    @Query("DELETE FROM activity_summaries WHERE activity_id = :activityId")
    suspend fun deleteSummary(activityId: String)
}
