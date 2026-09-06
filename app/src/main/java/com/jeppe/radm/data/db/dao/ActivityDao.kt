package com.jeppe.radm.data.db.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Upsert
import com.jeppe.radm.data.db.entity.ActivityEntity
import com.jeppe.radm.data.db.entity.ActivitySummaryEntity

@Dao
interface ActivityDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertActivity(activity: ActivityEntity)

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

    @Query("DELETE FROM activities WHERE activity_id = :activityId")
    suspend fun deleteActivity(activityId: String): Int

    @Upsert
    suspend fun upsertSummary(summary: ActivitySummaryEntity)

    @Query("SELECT * FROM activity_summaries WHERE activity_id = :activityId")
    suspend fun getSummary(activityId: String): ActivitySummaryEntity?

    @Query("DELETE FROM activity_summaries WHERE activity_id = :activityId")
    suspend fun deleteSummary(activityId: String)
}
