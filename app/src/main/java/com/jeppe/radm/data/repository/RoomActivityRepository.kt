package com.jeppe.radm.data.repository

import androidx.room3.withWriteTransaction
import com.jeppe.radm.data.db.RadmDatabase
import com.jeppe.radm.data.db.toDomain
import com.jeppe.radm.data.db.toEntity
import com.jeppe.radm.domain.model.Activity
import com.jeppe.radm.domain.model.ActivityId
import com.jeppe.radm.domain.model.ActivityProcessorState
import com.jeppe.radm.domain.model.ActivitySummary
import com.jeppe.radm.domain.model.CadenceSample
import com.jeppe.radm.domain.model.DerivedTrackMetric
import com.jeppe.radm.domain.model.ProcessorDefinition
import com.jeppe.radm.domain.model.ProcessorName

class RoomActivityRepository(
    private val database: RadmDatabase,
) : ActivityRepository {
    private val activityDao = database.activityDao()
    private val recordingDao = database.recordingDao()
    private val derivedDao = database.derivedDao()

    override suspend fun insert(activity: Activity) = activityDao.insertActivity(activity.toEntity())

    override suspend fun get(activityId: ActivityId) =
        activityDao.getActivity(activityId.value)?.toDomain()

    override suspend fun listSaved() = activityDao.listSavedActivities().map { it.toDomain() }

    override suspend fun updateMetadata(
        activityId: ActivityId,
        update: ActivityMetadataUpdate,
    ): Boolean = activityDao.updateMetadata(
        activityId = activityId.value,
        activityType = update.type.name,
        title = update.title,
        notes = update.notes,
        updatedAtUtcMs = update.updatedAt.value,
    ) == 1

    override suspend fun delete(activityId: ActivityId): Boolean =
        activityDao.deleteActivity(activityId.value) == 1

    override suspend fun putSummary(summary: ActivitySummary) =
        activityDao.upsertSummary(summary.toEntity())

    override suspend fun getSummary(activityId: ActivityId) =
        activityDao.getSummary(activityId.value)?.toDomain()

    override suspend fun getPositions(activityId: ActivityId) =
        recordingDao.getPositions(activityId.value).map { it.toDomain() }

    override suspend fun getSteps(activityId: ActivityId) =
        recordingDao.getSteps(activityId.value).map { it.toDomain() }

    override suspend fun getRecordingEvents(activityId: ActivityId) =
        recordingDao.getEvents(activityId.value).map { it.toDomain() }

    override suspend fun getTrackMetrics(activityId: ActivityId) =
        derivedDao.getTrackMetrics(activityId.value).map { it.toDomain() }

    override suspend fun getCadence(activityId: ActivityId) =
        derivedDao.getCadence(activityId.value).map { it.toDomain() }

    override suspend fun putProcessorDefinition(definition: ProcessorDefinition) =
        derivedDao.upsertProcessorDefinition(definition.toEntity())

    override suspend fun getProcessorDefinition(name: ProcessorName) =
        derivedDao.getProcessorDefinition(name.value)?.toDomain()

    override suspend fun updateProcessorVersion(name: ProcessorName, version: Int): Boolean {
        require(version >= 1) { "Processor versions begin at 1" }
        return derivedDao.updateProcessorVersion(name.value, version) == 1
    }

    override suspend fun getProcessorStates(activityId: ActivityId) =
        derivedDao.getProcessorStates(activityId.value).map { it.toDomain() }

    override suspend fun isProcessorCurrent(activityId: ActivityId, name: ProcessorName): Boolean {
        val joined = derivedDao.getProcessorStateWithDefinition(activityId.value, name.value)
            ?: return false
        return joined.status == "CURRENT" &&
            joined.processorVersion == joined.currentProcessorVersion
    }

    override suspend fun replaceTrackMetrics(
        activityId: ActivityId,
        metrics: List<DerivedTrackMetric>,
        processorStates: List<ActivityProcessorState>,
    ) {
        require(metrics.all { it.activityId == activityId }) { "Track metrics must belong to the activity" }
        require(processorStates.all { it.activityId == activityId }) {
            "Processor states must belong to the activity"
        }
        database.withWriteTransaction {
            derivedDao.deleteTrackMetrics(activityId.value)
            if (metrics.isNotEmpty()) {
                derivedDao.insertTrackMetrics(metrics.map { it.toEntity() })
            }
            processorStates.forEach { derivedDao.upsertProcessorState(it.toEntity()) }
        }
    }

    override suspend fun replaceCadence(
        activityId: ActivityId,
        samples: List<CadenceSample>,
        processorState: ActivityProcessorState,
    ) {
        require(samples.all { it.activityId == activityId }) { "Cadence samples must belong to the activity" }
        require(processorState.activityId == activityId) { "Processor state must belong to the activity" }
        require(processorState.processorName == ProcessorName.CADENCE) {
            "Cadence replacement requires cadence processor state"
        }
        database.withWriteTransaction {
            derivedDao.deleteCadence(activityId.value)
            if (samples.isNotEmpty()) {
                derivedDao.insertCadence(samples.map { it.toEntity() })
            }
            derivedDao.upsertProcessorState(processorState.toEntity())
        }
    }

    override suspend fun deleteDerivedOutputs(activityId: ActivityId) {
        database.withWriteTransaction {
            derivedDao.deleteTrackMetrics(activityId.value)
            derivedDao.deleteCadence(activityId.value)
            activityDao.deleteSummary(activityId.value)
            derivedDao.deleteProcessorStates(activityId.value)
        }
    }
}
