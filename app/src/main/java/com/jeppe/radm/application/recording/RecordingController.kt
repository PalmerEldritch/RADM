package com.jeppe.radm.application.recording

import com.jeppe.radm.data.repository.RecordingFinalization
import com.jeppe.radm.data.repository.RecordingRepository
import com.jeppe.radm.domain.model.AbsoluteTimestampUtcMillis
import com.jeppe.radm.domain.model.ActiveElapsedTimeMillis
import com.jeppe.radm.domain.model.Activity
import com.jeppe.radm.domain.model.ActivityId
import com.jeppe.radm.domain.model.ActivityType
import com.jeppe.radm.domain.model.DistanceMetres
import com.jeppe.radm.domain.model.EventIndex
import com.jeppe.radm.domain.model.MonotonicTimeMillis
import com.jeppe.radm.domain.model.PositionSample
import com.jeppe.radm.domain.model.ProvenanceType
import com.jeppe.radm.domain.model.RecordingEvent
import com.jeppe.radm.domain.model.RecordingEventType
import com.jeppe.radm.domain.model.RouteSegmentIndex
import com.jeppe.radm.domain.model.SampleIndex
import com.jeppe.radm.domain.model.StepSample
import com.jeppe.radm.domain.location.LiveLocationSnapshot
import com.jeppe.radm.domain.location.LocationAcceptanceProcessor
import com.jeppe.radm.domain.location.LocationAcceptanceResult
import com.jeppe.radm.domain.location.LocationAvailability
import com.jeppe.radm.domain.recording.ActiveTimeTracker
import com.jeppe.radm.domain.recording.ClockSource
import com.jeppe.radm.domain.recording.LocationSource
import com.jeppe.radm.domain.recording.LocationSourceEvent
import com.jeppe.radm.domain.recording.RecordingCommand
import com.jeppe.radm.domain.recording.RecordingSession
import com.jeppe.radm.domain.recording.RecordingState
import com.jeppe.radm.domain.recording.RecordingStateMachine
import com.jeppe.radm.domain.recording.RecordingTransition
import com.jeppe.radm.domain.recording.StepMeasurement
import com.jeppe.radm.domain.recording.StepSource
import com.jeppe.radm.domain.processing.R00DistanceProcessor
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

fun interface ActivityIdSource {
    fun newId(): ActivityId
}

object RandomActivityIdSource : ActivityIdSource {
    override fun newId(): ActivityId = ActivityId.parse(UUID.randomUUID().toString())
}

data class RecordingSnapshot(
    val activityId: ActivityId?,
    val activityType: ActivityType?,
    val state: RecordingState,
    val activeElapsedTime: ActiveElapsedTimeMillis,
    val routeSegmentIndex: RouteSegmentIndex?,
    val locationAvailability: LocationAvailability,
    val liveDistance: DistanceMetres?,
)

data class SaveRecordingMetadata(
    val activityType: ActivityType? = null,
    val title: String? = null,
    val notes: String? = null,
)

class InvalidRecordingCommandException(
    command: RecordingCommand,
    state: RecordingState,
) : IllegalStateException("$command is invalid while recording state is $state")

/**
 * Android-independent recording controller. The foreground service owns its runtime instance;
 * the controller deliberately has no Activity, ViewModel, or Service dependency.
 */
class RecordingController(
    private val recordingRepository: RecordingRepository,
    private val locationSource: LocationSource,
    private val stepSource: StepSource,
    private val clockSource: ClockSource,
    private val activityIdSource: ActivityIdSource = RandomActivityIdSource,
    private val persistencePolicy: RecordingPersistencePolicy = RecordingPersistencePolicy(),
    private val onCriticalPersistenceFailure: suspend (RecordingPersistenceException) -> Unit = { throw it },
) {
    private val commandMutex = Mutex()
    private var runtime: RuntimeSession? = null

    suspend fun start(activityType: ActivityType): RecordingSnapshot = commandMutex.withLock {
        requireTransition(RecordingCommand.START)
        check(recordingRepository.loadActiveSession() == null) {
            "A durable unresolved recording already exists"
        }

        val occurredAt = clockSource.absoluteNow()
        val monotonicNow = clockSource.monotonicNow()
        val activityId = activityIdSource.newId()
        val activity = Activity(
            id = activityId,
            provenanceType = ProvenanceType.RADM_NATIVE,
            type = activityType,
            startedAt = occurredAt,
            createdAt = occurredAt,
            updatedAt = occurredAt,
        )
        val tracker = ActiveTimeTracker.start(monotonicNow)
        val session = RecordingSession(
            activityId = activityId,
            state = RecordingState.RECORDING,
            activeElapsedTime = ActiveElapsedTimeMillis.ZERO,
            stateEnteredAt = occurredAt,
            lastCheckpointAt = occurredAt,
            routeSegmentIndex = RouteSegmentIndex(0L),
            positionSampleCount = 0L,
            stepSampleCount = 0L,
        )
        val startEvent = RecordingEvent(
            activityId = activityId,
            eventIndex = EventIndex(0L),
            type = RecordingEventType.START,
            occurredAt = occurredAt,
            activeElapsedTime = ActiveElapsedTimeMillis.ZERO,
        )

        persistencePolicy.execute {
            recordingRepository.createSession(activity, session, startEvent)
        }
        runtime = RuntimeSession(
            activityType = activityType,
            session = session,
            tracker = tracker,
            locationProcessor = LocationAcceptanceProcessor(
                acquisitionStartedAt = monotonicNow,
                initialRouteSegmentIndex = session.routeSegmentIndex,
            ),
            nextEventIndex = 1L,
            lastFlushAtMonotonic = monotonicNow,
        )
        startApplicableSources(activityType)
        snapshotLocked()
    }

    suspend fun recoverAndResume(): RecordingSnapshot = commandMutex.withLock {
        check(runtime == null) { "A runtime recording already exists" }
        val unresolved = checkNotNull(recordingRepository.loadUnresolvedRecording()) {
            "No interrupted recording is available"
        }
        require(unresolved.session.state != RecordingState.FINALIZING) {
            "A recording already awaiting final save cannot be resumed"
        }
        val occurredAt = clockSource.absoluteNow()
        val monotonicNow = clockSource.monotonicNow()
        val recoveredSession = unresolved.session.copy(
            state = RecordingState.RECORDING,
            stateEnteredAt = occurredAt,
            lastCheckpointAt = occurredAt,
            routeSegmentIndex = RouteSegmentIndex(
                Math.addExact(unresolved.session.routeSegmentIndex.value, 1L),
            ),
        )
        val recoveryEvent = RecordingEvent(
            activityId = recoveredSession.activityId,
            eventIndex = EventIndex(unresolved.nextEventIndex),
            type = RecordingEventType.RECOVERY_RESUME,
            occurredAt = occurredAt,
            activeElapsedTime = recoveredSession.activeElapsedTime,
        )
        persistencePolicy.execute {
            recordingRepository.persistTransition(recoveredSession, recoveryEvent)
        }
        val retainedDistance = unresolved.positions.takeIf { it.isNotEmpty() }
            ?.let { R00DistanceProcessor().process(it).last().cumulativeDistance }
            ?: DistanceMetres.ZERO
        val nextStepEpoch = unresolved.steps.maxOfOrNull { it.counterEpoch.value }
            ?.let { Math.addExact(it, 1L) }
            ?: 0L
        runtime = RuntimeSession(
            activityType = unresolved.activity.type,
            session = recoveredSession,
            tracker = ActiveTimeTracker.recoverRecording(
                checkpoint = recoveredSession.activeElapsedTime,
                resumedAt = monotonicNow,
            ),
            locationProcessor = LocationAcceptanceProcessor(
                acquisitionStartedAt = monotonicNow,
                initialRouteSegmentIndex = recoveredSession.routeSegmentIndex,
                initialCumulativeDistance = retainedDistance,
                hasRetainedLocation = unresolved.positions.isNotEmpty(),
            ),
            nextEventIndex = Math.addExact(unresolved.nextEventIndex, 1L),
            lastFlushAtMonotonic = monotonicNow,
            nextPositionIndex = recoveredSession.positionSampleCount,
            nextStepIndex = recoveredSession.stepSampleCount,
            stepEpochOffset = nextStepEpoch,
        )
        startApplicableSources(unresolved.activity.type)
        snapshotLocked()
    }

    suspend fun checkpointIfDue(): RecordingSnapshot = commandMutex.withLock {
        val current = runtime ?: return@withLock snapshotLocked()
        if (current.session.state == RecordingState.RECORDING) {
            flushIfRequired(current, clockSource.monotonicNow())
        }
        snapshotLocked()
    }

    suspend fun stopAfterCriticalPersistenceFailure(): RecordingSnapshot = commandMutex.withLock {
        runtime?.let { stopApplicableSources(it.activityType) }
        snapshotLocked()
    }

    suspend fun pause(): RecordingSnapshot = commandMutex.withLock {
        val current = requireRuntime(RecordingCommand.PAUSE)
        requireTransition(RecordingCommand.PAUSE, current.session.state)
        val occurredAt = clockSource.absoluteNow()
        val monotonicNow = clockSource.monotonicNow()
        val pausedTracker = current.tracker.pause(monotonicNow)

        stopApplicableSources(current.activityType)
        current.frozenLocationSnapshot = current.locationProcessor.snapshot(monotonicNow)
        val pausedSession = current.session.copy(
            state = RecordingState.PAUSED,
            activeElapsedTime = pausedTracker.elapsedAt(monotonicNow),
            stateEnteredAt = occurredAt,
            lastCheckpointAt = occurredAt,
            positionSampleCount = current.nextPositionIndex,
            stepSampleCount = current.nextStepIndex,
        )
        val event = current.event(RecordingEventType.PAUSE, occurredAt, pausedSession.activeElapsedTime)

        try {
            persistencePolicy.execute {
                recordingRepository.persistTransition(
                    session = pausedSession,
                    event = event,
                    positions = current.pendingPositions,
                    steps = current.pendingSteps,
                )
            }
        } catch (failure: Throwable) {
            startApplicableSources(current.activityType)
            throw failure
        }

        current.commitTransition(pausedSession, pausedTracker)
        snapshotLocked()
    }

    suspend fun resume(): RecordingSnapshot = commandMutex.withLock {
        val current = requireRuntime(RecordingCommand.RESUME)
        requireTransition(RecordingCommand.RESUME, current.session.state)
        val occurredAt = clockSource.absoluteNow()
        val monotonicNow = clockSource.monotonicNow()
        val resumedSession = current.session.copy(
            state = RecordingState.RECORDING,
            stateEnteredAt = occurredAt,
            lastCheckpointAt = occurredAt,
            routeSegmentIndex = RouteSegmentIndex(Math.addExact(current.session.routeSegmentIndex.value, 1L)),
        )
        val event = current.event(
            RecordingEventType.RESUME,
            occurredAt,
            resumedSession.activeElapsedTime,
        )

        persistencePolicy.execute { recordingRepository.persistTransition(resumedSession, event) }
        current.commitTransition(resumedSession, current.tracker.resume(monotonicNow))
        current.locationProcessor.beginNewSegment(resumedSession.routeSegmentIndex, monotonicNow)
        current.frozenLocationSnapshot = null
        current.lastFlushAtMonotonic = monotonicNow
        startApplicableSources(current.activityType)
        snapshotLocked()
    }

    suspend fun finish(): RecordingSnapshot = commandMutex.withLock {
        val current = requireRuntime(RecordingCommand.FINISH)
        requireTransition(RecordingCommand.FINISH, current.session.state)
        val occurredAt = clockSource.absoluteNow()
        val monotonicNow = clockSource.monotonicNow()
        val finishedTracker = current.tracker.finish(monotonicNow)

        stopApplicableSources(current.activityType)
        current.frozenLocationSnapshot = current.locationProcessor.snapshot(monotonicNow)
        val finalizingSession = current.session.copy(
            state = RecordingState.FINALIZING,
            activeElapsedTime = finishedTracker.elapsedAt(monotonicNow),
            stateEnteredAt = occurredAt,
            lastCheckpointAt = occurredAt,
            positionSampleCount = current.nextPositionIndex,
            stepSampleCount = current.nextStepIndex,
        )
        val event = current.event(
            RecordingEventType.FINISH,
            occurredAt,
            finalizingSession.activeElapsedTime,
        )

        try {
            persistencePolicy.execute {
                recordingRepository.persistTransition(
                    session = finalizingSession,
                    event = event,
                    positions = current.pendingPositions,
                    steps = current.pendingSteps,
                )
            }
        } catch (failure: Throwable) {
            startApplicableSources(current.activityType)
            throw failure
        }

        current.commitTransition(finalizingSession, finishedTracker)
        current.finishedAt = occurredAt
        snapshotLocked()
    }

    suspend fun save(metadata: SaveRecordingMetadata = SaveRecordingMetadata()): Activity =
        commandMutex.withLock {
            val current = requireRuntime(RecordingCommand.SAVE)
            requireTransition(RecordingCommand.SAVE, current.session.state)
            val savedAt = clockSource.absoluteNow()
            val finalization = RecordingFinalization(
                activityId = current.session.activityId,
                activityType = metadata.activityType ?: current.activityType,
                title = metadata.title,
                notes = metadata.notes,
                endedAt = checkNotNull(current.finishedAt) { "Finalizing recording requires a finish time" },
                savedAt = savedAt,
                activeDuration = current.session.activeElapsedTime,
                updatedAt = savedAt,
            )

            val saved = persistencePolicy.execute { recordingRepository.finalizeSession(finalization) }
            runtime = null
            saved
        }

    suspend fun discard(): Boolean = commandMutex.withLock {
        val current = requireRuntime(RecordingCommand.DISCARD)
        requireTransition(RecordingCommand.DISCARD, current.session.state)
        val discarded = persistencePolicy.execute { recordingRepository.discardSession() }
        check(discarded) { "No durable unresolved recording exists to discard" }
        runtime = null
        true
    }

    suspend fun snapshot(): RecordingSnapshot = commandMutex.withLock { snapshotLocked() }

    private suspend fun acceptLocation(event: LocationSourceEvent) {
        try {
            commandMutex.withLock {
                val current = runtime ?: return@withLock
                if (current.session.state != RecordingState.RECORDING) return@withLock
                when (event) {
                    LocationSourceEvent.ProviderAvailable -> {
                        current.locationProcessor.providerAvailable(clockSource.monotonicNow())
                        return@withLock
                    }

                    LocationSourceEvent.ProviderUnavailable -> {
                        current.locationProcessor.providerUnavailable()
                        return@withLock
                    }

                    is LocationSourceEvent.Candidate -> Unit
                }
                val accepted = when (
                    val result = current.locationProcessor.accept(
                        candidate = event.value,
                        evaluatedAt = clockSource.monotonicNow(),
                    )
                ) {
                    is LocationAcceptanceResult.Accepted -> result.measurement
                    is LocationAcceptanceResult.Rejected -> return@withLock
                }
                val elapsed = current.tracker.elapsedAtOrNull(accepted.monotonicTimestamp) ?: return@withLock
                if (accepted.routeSegmentIndex != current.session.routeSegmentIndex) {
                    current.session = current.session.copy(routeSegmentIndex = accepted.routeSegmentIndex)
                }
                current.pendingPositions += PositionSample(
                    activityId = current.session.activityId,
                    sampleIndex = SampleIndex(current.nextPositionIndex++),
                    routeSegmentIndex = accepted.routeSegmentIndex,
                    timestamp = accepted.timestamp,
                    activeElapsedTime = elapsed,
                    latitude = accepted.latitude,
                    longitude = accepted.longitude,
                    elevation = accepted.elevation,
                    horizontalAccuracy = accepted.horizontalAccuracy,
                    verticalAccuracy = accepted.verticalAccuracy,
                )
                flushIfRequired(current, accepted.monotonicTimestamp)
            }
        } catch (failure: RecordingPersistenceException) {
            onCriticalPersistenceFailure(failure)
        }
    }

    private suspend fun acceptStep(measurement: StepMeasurement) {
        try {
            commandMutex.withLock {
                val current = runtime ?: return@withLock
                if (current.session.state != RecordingState.RECORDING || current.activityType != ActivityType.RUNNING) {
                    return@withLock
                }
                val elapsed = current.tracker.elapsedAtOrNull(measurement.monotonicTimestamp) ?: return@withLock
                current.pendingSteps += StepSample(
                    activityId = current.session.activityId,
                    sampleIndex = SampleIndex(current.nextStepIndex++),
                    counterEpoch = measurement.counterEpoch.offsetBy(current.stepEpochOffset),
                    timestamp = measurement.timestamp,
                    activeElapsedTime = elapsed,
                    cumulativeSteps = measurement.cumulativeSteps,
                )
                flushIfRequired(current, measurement.monotonicTimestamp)
            }
        } catch (failure: RecordingPersistenceException) {
            onCriticalPersistenceFailure(failure)
        }
    }

    private fun com.jeppe.radm.domain.model.StepCounterEpoch.offsetBy(offset: Long) =
        com.jeppe.radm.domain.model.StepCounterEpoch(Math.addExact(value, offset))

    private suspend fun flushIfRequired(current: RuntimeSession, now: MonotonicTimeMillis) {
        val intervalMillis = Math.subtractExact(now.value, current.lastFlushAtMonotonic.value)
        val streamLimitReached = current.pendingPositions.size >= MAX_BUFFERED_SAMPLES_PER_STREAM ||
            current.pendingSteps.size >= MAX_BUFFERED_SAMPLES_PER_STREAM
        if (intervalMillis < MAX_UNCOMMITTED_INTERVAL_MS && !streamLimitReached) return

        val checkpointAt = clockSource.absoluteNow()
        val checkpoint = current.session.copy(
            activeElapsedTime = current.tracker.elapsedAt(now),
            lastCheckpointAt = checkpointAt,
            positionSampleCount = current.nextPositionIndex,
            stepSampleCount = current.nextStepIndex,
        )
        persistencePolicy.execute {
            recordingRepository.persistCheckpoint(
                checkpoint,
                current.pendingPositions,
                current.pendingSteps,
            )
        }
        current.commitCheckpoint(checkpoint, now)
    }

    private suspend fun startApplicableSources(activityType: ActivityType) {
        val current = checkNotNull(runtime)
        runCatching { locationSource.start(::acceptLocation) }
            .onFailure { current.locationProcessor.providerUnavailable() }
        if (activityType == ActivityType.RUNNING) {
            // Step capability is optional; denial, absence, or adapter failure must not abort recording.
            runCatching { stepSource.start(::acceptStep) }
        }
    }

    private suspend fun stopApplicableSources(activityType: ActivityType) {
        runCatching { locationSource.stop() }
        if (activityType == ActivityType.RUNNING) stepSource.stop()
    }

    private fun ActiveTimeTracker.elapsedAtOrNull(at: MonotonicTimeMillis): ActiveElapsedTimeMillis? =
        try {
            elapsedAt(at)
        } catch (_: IllegalArgumentException) {
            // A queued measurement from a paused/pre-resume interval is not an active source sample.
            null
        }

    private fun requireRuntime(command: RecordingCommand): RuntimeSession =
        runtime ?: throw InvalidRecordingCommandException(command, RecordingState.IDLE)

    private fun requireTransition(command: RecordingCommand, state: RecordingState = runtime?.session?.state ?: RecordingState.IDLE) {
        if (RecordingStateMachine.transition(state, command) is RecordingTransition.Rejected) {
            throw InvalidRecordingCommandException(command, state)
        }
    }

    private fun snapshotLocked(): RecordingSnapshot {
        val current = runtime ?: return RecordingSnapshot(
            activityId = null,
            activityType = null,
            state = RecordingState.IDLE,
            activeElapsedTime = ActiveElapsedTimeMillis.ZERO,
            routeSegmentIndex = null,
            locationAvailability = LocationAvailability.UNAVAILABLE,
            liveDistance = null,
        )
        val location = current.frozenLocationSnapshot
            ?: current.locationProcessor.snapshot(clockSource.monotonicNow())
        return RecordingSnapshot(
            activityId = current.session.activityId,
            activityType = current.activityType,
            state = current.session.state,
            activeElapsedTime = current.tracker.elapsedAt(clockSource.monotonicNow()),
            routeSegmentIndex = current.session.routeSegmentIndex,
            locationAvailability = location.availability,
            liveDistance = location.distance,
        )
    }

    private data class RuntimeSession(
        val activityType: ActivityType,
        var session: RecordingSession,
        var tracker: ActiveTimeTracker,
        val locationProcessor: LocationAcceptanceProcessor,
        var nextEventIndex: Long,
        var lastFlushAtMonotonic: MonotonicTimeMillis,
        var nextPositionIndex: Long = 0L,
        var nextStepIndex: Long = 0L,
        val pendingPositions: MutableList<PositionSample> = mutableListOf(),
        val pendingSteps: MutableList<StepSample> = mutableListOf(),
        var finishedAt: AbsoluteTimestampUtcMillis? = null,
        var frozenLocationSnapshot: LiveLocationSnapshot? = null,
        val stepEpochOffset: Long = 0L,
    ) {
        fun event(
            type: RecordingEventType,
            occurredAt: AbsoluteTimestampUtcMillis,
            elapsed: ActiveElapsedTimeMillis,
        ) = RecordingEvent(
            activityId = session.activityId,
            eventIndex = EventIndex(nextEventIndex),
            type = type,
            occurredAt = occurredAt,
            activeElapsedTime = elapsed,
        )

        fun commitTransition(newSession: RecordingSession, newTracker: ActiveTimeTracker) {
            session = newSession
            tracker = newTracker
            nextEventIndex = Math.addExact(nextEventIndex, 1L)
            pendingPositions.clear()
            pendingSteps.clear()
        }

        fun commitCheckpoint(
            checkpoint: RecordingSession,
            monotonicNow: MonotonicTimeMillis,
        ) {
            session = checkpoint
            lastFlushAtMonotonic = monotonicNow
            pendingPositions.clear()
            pendingSteps.clear()
        }
    }

    private companion object {
        const val MAX_UNCOMMITTED_INTERVAL_MS = 5_000L
        const val MAX_BUFFERED_SAMPLES_PER_STREAM = 20
    }
}
