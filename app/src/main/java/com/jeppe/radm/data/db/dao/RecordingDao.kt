package com.jeppe.radm.data.db.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import com.jeppe.radm.data.db.entity.PositionSampleEntity
import com.jeppe.radm.data.db.entity.RecordingEventEntity
import com.jeppe.radm.data.db.entity.RecordingSessionEntity
import com.jeppe.radm.data.db.entity.StepSampleEntity

@Dao
interface RecordingDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: RecordingSessionEntity)

    @Query("SELECT * FROM recording_sessions WHERE singleton_id = 1")
    suspend fun getSession(): RecordingSessionEntity?

    @Update
    suspend fun updateSession(session: RecordingSessionEntity): Int

    @Query("DELETE FROM recording_sessions WHERE singleton_id = 1")
    suspend fun deleteSession(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEvents(events: List<RecordingEventEntity>)

    @Query(
        """
        SELECT * FROM recording_events
        WHERE activity_id = :activityId
        ORDER BY event_index ASC
        """,
    )
    suspend fun getEvents(activityId: String): List<RecordingEventEntity>

    @Query("SELECT MAX(event_index) FROM recording_events WHERE activity_id = :activityId")
    suspend fun getLastEventIndex(activityId: String): Long?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPositions(samples: List<PositionSampleEntity>)

    @Query(
        """
        SELECT * FROM position_samples
        WHERE activity_id = :activityId
        ORDER BY sample_index ASC
        """,
    )
    suspend fun getPositions(activityId: String): List<PositionSampleEntity>

    @Query("SELECT MAX(sample_index) FROM position_samples WHERE activity_id = :activityId")
    suspend fun getLastPositionIndex(activityId: String): Long?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSteps(samples: List<StepSampleEntity>)

    @Query(
        """
        SELECT * FROM step_samples
        WHERE activity_id = :activityId
        ORDER BY sample_index ASC
        """,
    )
    suspend fun getSteps(activityId: String): List<StepSampleEntity>

    @Query("SELECT MAX(sample_index) FROM step_samples WHERE activity_id = :activityId")
    suspend fun getLastStepIndex(activityId: String): Long?
}
