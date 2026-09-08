package com.jeppe.radm.application.recording

import com.jeppe.radm.data.repository.RecordingFinalization
import com.jeppe.radm.data.repository.RecordingRepository
import com.jeppe.radm.data.repository.UnresolvedRecording
import com.jeppe.radm.domain.model.ActiveElapsedTimeMillis
import com.jeppe.radm.domain.model.ActivityId
import com.jeppe.radm.domain.model.ActivityType
import com.jeppe.radm.domain.model.DistanceMetres
import com.jeppe.radm.domain.model.EventIndex
import com.jeppe.radm.domain.model.RecordingEvent
import com.jeppe.radm.domain.model.RecordingEventType
import com.jeppe.radm.domain.model.AbsoluteTimestampUtcMillis
import com.jeppe.radm.domain.processing.R00DistanceProcessor
import com.jeppe.radm.domain.recording.ClockSource
import com.jeppe.radm.domain.recording.RecordingState
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class RecoverableRecording(
    val activityId: ActivityId,
    val activityType: ActivityType,
    val startedAtUtcMillis: Long,
    val retainedActiveTime: ActiveElapsedTimeMillis,
    val retainedDistance: DistanceMetres?,
    val durableState: RecordingState,
    val positionSampleCount: Long,
    val stepSampleCount: Long,
)

/** Application workflow for resolving an interrupted durable session without fabricating runtime. */
class RecordingRecovery(
    private val recordingRepository: RecordingRepository,
    private val clockSource: ClockSource,
    private val recalculateActivity: suspend (ActivityId, AbsoluteTimestampUtcMillis) -> Unit,
    private val persistencePolicy: RecordingPersistencePolicy = RecordingPersistencePolicy(),
) {
    private val mutex = Mutex()

    suspend fun detect(): RecoverableRecording? =
        recordingRepository.loadUnresolvedRecording()?.toRecoverableRecording()

    suspend fun finishAndSave(): ActivityId = mutex.withLock {
        val unresolved = checkNotNull(recordingRepository.loadUnresolvedRecording()) {
            "No interrupted recording is available"
        }
        val savedAt = clockSource.absoluteNow()
        val finalizingSession = if (unresolved.session.state == RecordingState.FINALIZING) {
            unresolved.session
        } else {
            val session = unresolved.session.copy(
                state = RecordingState.FINALIZING,
                stateEnteredAt = savedAt,
            )
            val event = RecordingEvent(
                activityId = session.activityId,
                eventIndex = EventIndex(unresolved.nextEventIndex),
                type = RecordingEventType.FINISH,
                occurredAt = savedAt,
                activeElapsedTime = session.activeElapsedTime,
            )
            persistencePolicy.execute {
                recordingRepository.persistTransition(session, event)
            }
            session
        }
        val endedAt = if (unresolved.session.state == RecordingState.FINALIZING) {
            unresolved.session.stateEnteredAt
        } else {
            // This is the last trustworthy wall-clock boundary, not the later recovery action.
            unresolved.session.lastCheckpointAt
        }
        val saved = persistencePolicy.execute {
            recordingRepository.finalizeSession(
                RecordingFinalization(
                    activityId = unresolved.activity.id,
                    activityType = unresolved.activity.type,
                    title = unresolved.activity.title,
                    notes = unresolved.activity.notes,
                    endedAt = endedAt,
                    savedAt = savedAt,
                    activeDuration = finalizingSession.activeElapsedTime,
                    updatedAt = savedAt,
                ),
            )
        }
        runCatching { recalculateActivity(saved.id, savedAt) }
        saved.id
    }

    suspend fun discard(): Boolean = mutex.withLock {
        persistencePolicy.execute { recordingRepository.discardSession() }
    }

    private fun UnresolvedRecording.toRecoverableRecording(): RecoverableRecording {
        val retainedDistance = positions.takeIf { it.isNotEmpty() }
            ?.let { R00DistanceProcessor().process(it).last().cumulativeDistance }
        return RecoverableRecording(
            activityId = activity.id,
            activityType = activity.type,
            startedAtUtcMillis = activity.startedAt.value,
            retainedActiveTime = session.activeElapsedTime,
            retainedDistance = retainedDistance,
            durableState = session.state,
            positionSampleCount = session.positionSampleCount,
            stepSampleCount = session.stepSampleCount,
        )
    }
}
