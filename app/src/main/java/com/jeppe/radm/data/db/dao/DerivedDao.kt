package com.jeppe.radm.data.db.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Upsert
import com.jeppe.radm.data.db.entity.ActivityProcessorStateEntity
import com.jeppe.radm.data.db.entity.DerivedCadenceEntity
import com.jeppe.radm.data.db.entity.DerivedTrackMetricEntity
import com.jeppe.radm.data.db.entity.ProcessorDefinitionEntity
import com.jeppe.radm.data.db.entity.ProcessorStateWithDefinitionEntity

@Dao
interface DerivedDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTrackMetrics(metrics: List<DerivedTrackMetricEntity>)

    @Query("DELETE FROM derived_track_metrics WHERE activity_id = :activityId")
    suspend fun deleteTrackMetrics(activityId: String)

    @Query(
        """
        SELECT * FROM derived_track_metrics
        WHERE activity_id = :activityId
        ORDER BY sample_index ASC
        """,
    )
    suspend fun getTrackMetrics(activityId: String): List<DerivedTrackMetricEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCadence(samples: List<DerivedCadenceEntity>)

    @Query("DELETE FROM derived_cadence WHERE activity_id = :activityId")
    suspend fun deleteCadence(activityId: String)

    @Query(
        """
        SELECT * FROM derived_cadence
        WHERE activity_id = :activityId
        ORDER BY sample_index ASC
        """,
    )
    suspend fun getCadence(activityId: String): List<DerivedCadenceEntity>

    @Upsert
    suspend fun upsertProcessorDefinition(definition: ProcessorDefinitionEntity)

    @Query("SELECT * FROM processor_definitions WHERE processor_name = :processorName")
    suspend fun getProcessorDefinition(processorName: String): ProcessorDefinitionEntity?

    @Query(
        """
        UPDATE processor_definitions
        SET current_version = :currentVersion
        WHERE processor_name = :processorName
        """,
    )
    suspend fun updateProcessorVersion(processorName: String, currentVersion: Int): Int

    @Upsert
    suspend fun upsertProcessorState(state: ActivityProcessorStateEntity)

    @Upsert
    suspend fun upsertProcessorStates(states: List<ActivityProcessorStateEntity>)

    @Query(
        """
        SELECT * FROM activity_processor_state
        WHERE activity_id = :activityId
        ORDER BY processor_name ASC
        """,
    )
    suspend fun getProcessorStates(activityId: String): List<ActivityProcessorStateEntity>

    @Query("DELETE FROM activity_processor_state WHERE activity_id = :activityId")
    suspend fun deleteProcessorStates(activityId: String)

    @Query(
        """
        UPDATE activity_processor_state
        SET processor_version = NULL,
            status = 'UNPROCESSED',
            processed_at_utc_ms = NULL
        WHERE activity_id = :activityId
        """,
    )
    suspend fun invalidateProcessorStates(activityId: String)

    @Query(
        """
        SELECT activity_processor_state.*,
               processor_definitions.current_version AS current_processor_version
        FROM activity_processor_state
        INNER JOIN processor_definitions USING (processor_name)
        WHERE activity_processor_state.activity_id = :activityId
          AND activity_processor_state.processor_name = :processorName
        """,
    )
    suspend fun getProcessorStateWithDefinition(
        activityId: String,
        processorName: String,
    ): ProcessorStateWithDefinitionEntity?
}
