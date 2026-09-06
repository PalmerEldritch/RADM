package com.jeppe.radm.data.repository

import androidx.room3.withWriteTransaction
import com.jeppe.radm.data.db.RadmDatabase
import com.jeppe.radm.data.db.toDomain
import com.jeppe.radm.data.db.toEntity
import com.jeppe.radm.domain.model.Activity
import com.jeppe.radm.domain.model.PositionSample
import com.jeppe.radm.domain.model.RecordingEvent
import com.jeppe.radm.domain.model.RecordingEventType
import com.jeppe.radm.domain.model.StepSample
import com.jeppe.radm.domain.recording.RecordingSession

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

    override suspend fun persistTransition(session: RecordingSession, event: RecordingEvent) {
        require(session.activityId == event.activityId) { "Session and event identities must match" }
        database.withWriteTransaction {
            check(recordingDao.updateSession(session.toEntity()) == 1) { "No unresolved session exists" }
            appendEvents(listOf(event))
        }
    }

    override suspend fun discardSession(): Boolean = database.withWriteTransaction {
        val session = recordingDao.getSession() ?: return@withWriteTransaction false
        activityDao.deleteActivity(session.activityId) == 1
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
