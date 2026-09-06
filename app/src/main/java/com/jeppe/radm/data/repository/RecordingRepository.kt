package com.jeppe.radm.data.repository

import com.jeppe.radm.domain.model.Activity
import com.jeppe.radm.domain.model.PositionSample
import com.jeppe.radm.domain.model.RecordingEvent
import com.jeppe.radm.domain.model.StepSample
import com.jeppe.radm.domain.recording.RecordingSession

interface RecordingRepository {
    suspend fun createSession(
        activity: Activity,
        session: RecordingSession,
        startEvent: RecordingEvent,
    )

    suspend fun loadActiveSession(): RecordingSession?
    suspend fun appendEvents(events: List<RecordingEvent>)
    suspend fun appendPositions(samples: List<PositionSample>)
    suspend fun appendSteps(samples: List<StepSample>)
    suspend fun persistTransition(session: RecordingSession, event: RecordingEvent)
    suspend fun discardSession(): Boolean
}
