package com.jeppe.radm.data.repository

import androidx.room3.withReadTransaction
import androidx.room3.withWriteTransaction
import com.jeppe.radm.data.db.RadmDatabase
import com.jeppe.radm.data.db.toDomain
import com.jeppe.radm.data.db.toEntity
import com.jeppe.radm.domain.model.Activity
import com.jeppe.radm.domain.model.PositionSample
import com.jeppe.radm.domain.model.RecordingEvent
import com.jeppe.radm.domain.model.RecordingEventType
import com.jeppe.radm.domain.model.StepSample
import com.jeppe.radm.domain.recording.RecordingCommand
import com.jeppe.radm.domain.recording.RecordingSession
import com.jeppe.radm.domain.recording.RecordingState
import com.jeppe.radm.domain.recording.RecordingStateMachine
import com.jeppe.radm.domain.recording.RecordingTransition

class RoomRecordingRepository(
    private val database: RadmDatabase,
) : RecordingRepository {
    private val activityDao = database.activityDao()
    private val recordingDao = database.recordingDao()

    override suspend fun createSession(
        activity: Activity,
        session: RecordingSession,
        startEvent: RecordingEvent,
    ) {
        require(session.activityId == activity.id && startEvent.activityId == activity.id) {
            "Activity, session, and START event identities must match"
        }
        require(session.state == RecordingState.RECORDING) {
            "A new session must begin in RECORDING"
        }
        require(session.activeElapsedTime.value == 0L) {
            "A new session must begin at zero active elapsed time"
        }
        require(startEvent.type == RecordingEventType.START && startEvent.eventIndex.value == 0L) {
            "A new session requires event zero with type START"
        }
        database.withWriteTransaction {
            activityDao.insertActivity(activity.toEntity())
            recordingDao.insertSession(session.toEntity())
            recordingDao.insertEvents(listOf(startEvent.toEntity()))
        }
    }

    override suspend fun loadActiveSession() = recordingDao.getSession()?.toDomain()

    override suspend fun loadUnresolvedRecording(): UnresolvedRecording? = database.withReadTransaction {
        val session = recordingDao.getSession()?.toDomain() ?: return@withReadTransaction null
        val activity = checkNotNull(activityDao.getActivity(session.activityId.value)) {
            "Unresolved recording activity is missing"
        }.toDomain()
        UnresolvedRecording(
            activity = activity,
            session = session,
            events = recordingDao.getEvents(session.activityId.value).map { it.toDomain() },
            positions = recordingDao.getPositions(session.activityId.value).map { it.toDomain() },
            steps = recordingDao.getSteps(session.activityId.value).map { it.toDomain() },
        )
    }

    override suspend fun appendEvents(events: List<RecordingEvent>) {
        appendOrdered(
            values = events,
            activityId = { it.activityId.value },
            index = { it.eventIndex.value },
            lastPersistedIndex = { recordingDao.getLastEventIndex(it) },
        ) { recordingDao.insertEvents(events.map { it.toEntity() }) }
    }

    override suspend fun appendPositions(samples: List<PositionSample>) {
        appendOrdered(
            values = samples,
            activityId = { it.activityId.value },
            index = { it.sampleIndex.value },
            lastPersistedIndex = { recordingDao.getLastPositionIndex(it) },
        ) { recordingDao.insertPositions(samples.map { it.toEntity() }) }
    }

    override suspend fun appendSteps(samples: List<StepSample>) {
        appendOrdered(
            values = samples,
            activityId = { it.activityId.value },
            index = { it.sampleIndex.value },
            lastPersistedIndex = { recordingDao.getLastStepIndex(it) },
        ) { recordingDao.insertSteps(samples.map { it.toEntity() }) }
    }

    override suspend fun persistCheckpoint(
        session: RecordingSession,
        positions: List<PositionSample>,
        steps: List<StepSample>,
    ) {
        requireBatchOwnership(session, positions, steps)
        database.withWriteTransaction {
            val current = requireCurrentSession(session)
            require(current.state == session.state) { "A checkpoint cannot change recording state" }
            requireUpdatedCounts(current, session, positions, steps)
            insertSourceBatches(current, positions, steps)
            check(recordingDao.updateSession(session.toEntity()) == 1) { "No unresolved session exists" }
        }
    }

    override suspend fun persistTransition(
        session: RecordingSession,
        event: RecordingEvent,
        positions: List<PositionSample>,
        steps: List<StepSample>,
    ) {
        require(session.activityId == event.activityId) { "Session and event identities must match" }
        requireBatchOwnership(session, positions, steps)
        database.withWriteTransaction {
            val current = requireCurrentSession(session)
            requireValidTransition(current, session, event)
            requireUpdatedCounts(current, session, positions, steps)
            insertSourceBatches(current, positions, steps)
            check(recordingDao.updateSession(session.toEntity()) == 1) { "No unresolved session exists" }
            insertOrderedEvent(event)
        }
    }

    override suspend fun finalizeSession(finalization: RecordingFinalization): Activity =
        database.withWriteTransaction {
            val current = recordingDao.getSession()?.toDomain()
                ?: error("No unresolved session exists")
            require(current.activityId == finalization.activityId) {
                "Finalization must target the unresolved activity"
            }
            require(current.state == RecordingState.FINALIZING) {
                "Only a FINALIZING session can be saved"
            }
            require(current.activeElapsedTime == finalization.activeDuration) {
                "Final active duration must match the durable recording checkpoint"
            }
            check(
                activityDao.finalizeActivity(
                    activityId = finalization.activityId.value,
                    activityType = finalization.activityType.name,
                    title = finalization.title,
                    notes = finalization.notes,
                    endedAtUtcMs = finalization.endedAt.value,
                    savedAtUtcMs = finalization.savedAt.value,
                    activeDurationMs = finalization.activeDuration.value,
                    updatedAtUtcMs = finalization.updatedAt.value,
                ) == 1,
            ) { "Recording activity does not exist" }
            check(recordingDao.deleteSession() == 1) { "Recording session disappeared during save" }
            checkNotNull(activityDao.getActivity(finalization.activityId.value)).toDomain()
        }

    override suspend fun discardSession(): Boolean = database.withWriteTransaction {
        val session = recordingDao.getSession() ?: return@withWriteTransaction false
        activityDao.deleteActivity(session.activityId) == 1
    }

    private suspend fun requireCurrentSession(session: RecordingSession): RecordingSession {
        val current = recordingDao.getSession()?.toDomain()
            ?: error("No unresolved session exists")
        require(current.activityId == session.activityId) { "Session identity cannot change" }
        return current
    }

    private suspend fun insertSourceBatches(
        current: RecordingSession,
        positions: List<PositionSample>,
        steps: List<StepSample>,
    ) {
        if (positions.isNotEmpty()) {
            require(positions.first().sampleIndex.value == current.positionSampleCount) {
                "First position index must match the durable position count"
            }
            require(positions.zipWithNext().all { (left, right) ->
                right.sampleIndex.value == left.sampleIndex.value + 1L
            }) { "Position batch indexes must increase by one" }
            recordingDao.insertPositions(positions.map { it.toEntity() })
        }
        if (steps.isNotEmpty()) {
            require(steps.first().sampleIndex.value == current.stepSampleCount) {
                "First step index must match the durable step count"
            }
            require(steps.zipWithNext().all { (left, right) ->
                right.sampleIndex.value == left.sampleIndex.value + 1L
            }) { "Step batch indexes must increase by one" }
            recordingDao.insertSteps(steps.map { it.toEntity() })
        }
    }

    private fun requireUpdatedCounts(
        current: RecordingSession,
        updated: RecordingSession,
        positions: List<PositionSample>,
        steps: List<StepSample>,
    ) {
        require(updated.positionSampleCount == current.positionSampleCount + positions.size.toLong()) {
            "Session position count must equal the durable count plus the appended batch"
        }
        require(updated.stepSampleCount == current.stepSampleCount + steps.size.toLong()) {
            "Session step count must equal the durable count plus the appended batch"
        }
    }

    private fun requireBatchOwnership(
        session: RecordingSession,
        positions: List<PositionSample>,
        steps: List<StepSample>,
    ) {
        require(positions.all { it.activityId == session.activityId }) {
            "Position batch must belong to the session activity"
        }
        require(steps.all { it.activityId == session.activityId }) {
            "Step batch must belong to the session activity"
        }
        require(session.positionSampleCount >= positions.size.toLong()) {
            "Session position count cannot be smaller than its batch"
        }
        require(session.stepSampleCount >= steps.size.toLong()) {
            "Session step count cannot be smaller than its batch"
        }
    }

    private suspend fun insertOrderedEvent(event: RecordingEvent) {
        val expectedIndex = (recordingDao.getLastEventIndex(event.activityId.value) ?: -1L) + 1L
        require(event.eventIndex.value == expectedIndex) { "Event index must be $expectedIndex" }
        recordingDao.insertEvents(listOf(event.toEntity()))
    }

    private fun requireValidTransition(
        current: RecordingSession,
        updated: RecordingSession,
        event: RecordingEvent,
    ) {
        val command = when (event.type) {
            RecordingEventType.PAUSE -> RecordingCommand.PAUSE
            RecordingEventType.RESUME -> RecordingCommand.RESUME
            RecordingEventType.FINISH -> RecordingCommand.FINISH
            RecordingEventType.START -> error("START is only valid in createSession")
            RecordingEventType.RECOVERY_RESUME -> null
        }
        if (event.type == RecordingEventType.RECOVERY_RESUME) {
            require(current.state != RecordingState.FINALIZING && updated.state == RecordingState.RECORDING) {
                "Recovery resume requires an interrupted RECORDING or PAUSED session"
            }
        } else {
            val transition = RecordingStateMachine.transition(current.state, checkNotNull(command))
            require(transition is RecordingTransition.Accepted && transition.state == updated.state) {
                "Invalid durable transition ${current.state} -> ${updated.state} for ${event.type}"
            }
        }
        require(event.activeElapsedTime == updated.activeElapsedTime) {
            "Transition event and session must share an active-time boundary"
        }
        val expectedSegment = when (event.type) {
            RecordingEventType.RESUME,
            RecordingEventType.RECOVERY_RESUME,
            -> current.routeSegmentIndex.value + 1L
            else -> current.routeSegmentIndex.value
        }
        require(updated.routeSegmentIndex.value == expectedSegment) {
            "${event.type} requires route segment $expectedSegment"
        }
    }

    private suspend fun <T> appendOrdered(
        values: List<T>,
        activityId: (T) -> String,
        index: (T) -> Long,
        lastPersistedIndex: suspend (String) -> Long?,
        insert: suspend () -> Unit,
    ) {
        if (values.isEmpty()) return
        val owner = activityId(values.first())
        require(values.all { activityId(it) == owner }) { "An append batch must have one activity owner" }
        require(values.zipWithNext().all { (left, right) -> index(right) == index(left) + 1L }) {
            "Append batch indexes must increase by one"
        }
        database.withWriteTransaction {
            val expectedFirst = (lastPersistedIndex(owner) ?: -1L) + 1L
            require(index(values.first()) == expectedFirst) {
                "First appended index must be $expectedFirst"
            }
            insert()
        }
    }
}
