package com.jeppe.radm.data.repository

import com.jeppe.radm.domain.model.AbsoluteTimestampUtcMillis
import com.jeppe.radm.domain.model.Activity
import com.jeppe.radm.domain.model.ActivityId
import com.jeppe.radm.domain.model.ActivityLibraryItem
import com.jeppe.radm.domain.model.ActivityProcessorState
import com.jeppe.radm.domain.model.ActivitySummary
import com.jeppe.radm.domain.model.ActivityType
import com.jeppe.radm.domain.model.CadenceSample
import com.jeppe.radm.domain.model.DerivedTrackMetric
import com.jeppe.radm.domain.model.PositionSample
import com.jeppe.radm.domain.model.ProcessorDefinition
import com.jeppe.radm.domain.model.ProcessorName
import com.jeppe.radm.domain.model.RecordingEvent
import com.jeppe.radm.domain.model.StepSample

data class ActivityMetadataUpdate(
    val type: ActivityType,
    val title: String?,
    val notes: String?,
    val updatedAt: AbsoluteTimestampUtcMillis,
)

interface ActivityRepository {
    suspend fun insert(activity: Activity)
    suspend fun get(activityId: ActivityId): Activity?
    suspend fun listSaved(): List<Activity>
    suspend fun listLibraryItems(): List<ActivityLibraryItem>
    suspend fun updateMetadata(activityId: ActivityId, update: ActivityMetadataUpdate): Boolean
    suspend fun delete(activityId: ActivityId): Boolean

    suspend fun putSummary(summary: ActivitySummary)
    suspend fun getSummary(activityId: ActivityId): ActivitySummary?

    suspend fun getPositions(activityId: ActivityId): List<PositionSample>
    suspend fun getSteps(activityId: ActivityId): List<StepSample>
    suspend fun getRecordingEvents(activityId: ActivityId): List<RecordingEvent>
    suspend fun getTrackMetrics(activityId: ActivityId): List<DerivedTrackMetric>
    suspend fun getCadence(activityId: ActivityId): List<CadenceSample>

    suspend fun putProcessorDefinition(definition: ProcessorDefinition)
    suspend fun getProcessorDefinition(name: ProcessorName): ProcessorDefinition?
    suspend fun updateProcessorVersion(name: ProcessorName, version: Int): Boolean
    suspend fun getProcessorStates(activityId: ActivityId): List<ActivityProcessorState>
    suspend fun isProcessorCurrent(activityId: ActivityId, name: ProcessorName): Boolean
    suspend fun putProcessorStates(states: List<ActivityProcessorState>)

    suspend fun replaceTrackMetrics(
        activityId: ActivityId,
        metrics: List<DerivedTrackMetric>,
        processorStates: List<ActivityProcessorState>,
    )

    suspend fun replaceCadence(
        activityId: ActivityId,
        samples: List<CadenceSample>,
        processorState: ActivityProcessorState,
    )

    suspend fun replaceSummary(
        activityId: ActivityId,
        summary: ActivitySummary,
        processorState: ActivityProcessorState,
    )

    suspend fun deleteDerivedOutputs(activityId: ActivityId)
}
