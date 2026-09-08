package com.jeppe.radm.data.repository

import com.jeppe.radm.domain.model.AbsoluteTimestampUtcMillis
import com.jeppe.radm.domain.model.ActiveElapsedTimeMillis
import com.jeppe.radm.domain.model.Activity
import com.jeppe.radm.domain.model.ActivityId
import com.jeppe.radm.domain.model.ActivityType
import com.jeppe.radm.domain.model.PositionSample
import com.jeppe.radm.domain.model.RecordingEvent
import com.jeppe.radm.domain.model.StepSample
import com.jeppe.radm.domain.recording.RecordingSession

data class UnresolvedRecording(
    val activity: Activity,
    val session: RecordingSession,
    val events: List<RecordingEvent>,
    val positions: List<PositionSample>,
    val steps: List<StepSample>,
) {
    val nextEventIndex: Long
        get() = (events.lastOrNull()?.eventIndex?.value ?: -1L) + 1L
}

data class RecordingFinalization(
    val activityId: ActivityId,
    val activityType: ActivityType,
    val title: String?,
    val notes: String?,
    val endedAt: AbsoluteTimestampUtcMillis,
    val savedAt: AbsoluteTimestampUtcMillis,
    val activeDuration: ActiveElapsedTimeMillis,
    val updatedAt: AbsoluteTimestampUtcMillis,
)

interface RecordingRepository {
    suspend fun createSession(
        activity: Activity,
        session: RecordingSession,
        startEvent: RecordingEvent,
    )

    suspend fun loadActiveSession(): RecordingSession?
    suspend fun loadUnresolvedRecording(): UnresolvedRecording?
    suspend fun appendEvents(events: List<RecordingEvent>)
    suspend fun appendPositions(samples: List<PositionSample>)
    suspend fun appendSteps(samples: List<StepSample>)
    suspend fun persistCheckpoint(
        session: RecordingSession,
        positions: List<PositionSample>,
        steps: List<StepSample>,
    )

    suspend fun persistTransition(
        session: RecordingSession,
        event: RecordingEvent,
        positions: List<PositionSample> = emptyList(),
        steps: List<StepSample> = emptyList(),
    )

    suspend fun finalizeSession(finalization: RecordingFinalization): Activity
    suspend fun discardSession(): Boolean
}
