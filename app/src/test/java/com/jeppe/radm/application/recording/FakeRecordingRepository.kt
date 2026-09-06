package com.jeppe.radm.application.recording

import com.jeppe.radm.data.repository.RecordingFinalization
import com.jeppe.radm.data.repository.RecordingRepository
import com.jeppe.radm.domain.model.Activity
import com.jeppe.radm.domain.model.PositionSample
import com.jeppe.radm.domain.model.RecordingEvent
import com.jeppe.radm.domain.model.StepSample
import com.jeppe.radm.domain.recording.RecordingSession

class FakeRecordingRepository : RecordingRepository {
    var activity: Activity? = null
    var session: RecordingSession? = null
    val events = mutableListOf<RecordingEvent>()
    val positions = mutableListOf<PositionSample>()
    val steps = mutableListOf<StepSample>()
    val operations = mutableListOf<String>()
    var failFinalization = false

    override suspend fun createSession(
        activity: Activity,
        session: RecordingSession,
        startEvent: RecordingEvent,
    ) {
        check(this.session == null)
        this.activity = activity
        this.session = session
        events += startEvent
        operations += "start"
    }

    override suspend fun loadActiveSession(): RecordingSession? = session

    override suspend fun appendEvents(events: List<RecordingEvent>) {
        this.events += events
    }

    override suspend fun appendPositions(samples: List<PositionSample>) {
        positions += samples
    }

    override suspend fun appendSteps(samples: List<StepSample>) {
        steps += samples
    }

    override suspend fun persistCheckpoint(
        session: RecordingSession,
        positions: List<PositionSample>,
        steps: List<StepSample>,
    ) {
        this.positions += positions
        this.steps += steps
        this.session = session
        operations += "checkpoint"
    }

    override suspend fun persistTransition(
        session: RecordingSession,
        event: RecordingEvent,
        positions: List<PositionSample>,
        steps: List<StepSample>,
    ) {
        this.positions += positions
        this.steps += steps
        events += event
        this.session = session
        operations += event.type.name.lowercase()
    }

    override suspend fun finalizeSession(finalization: RecordingFinalization): Activity {
        if (failFinalization) error("Injected finalization failure")
        val current = checkNotNull(activity)
        val saved = current.copy(
            type = finalization.activityType,
            title = finalization.title,
            notes = finalization.notes,
            endedAt = finalization.endedAt,
            savedAt = finalization.savedAt,
            activeDuration = finalization.activeDuration,
            updatedAt = finalization.updatedAt,
        )
        activity = saved
        session = null
        operations += "save"
        return saved
    }

    override suspend fun discardSession(): Boolean {
        if (session == null) return false
        activity = null
        session = null
        events.clear()
        positions.clear()
        steps.clear()
        operations += "discard"
        return true
    }
}
