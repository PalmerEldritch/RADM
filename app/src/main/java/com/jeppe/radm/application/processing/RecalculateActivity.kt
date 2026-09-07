package com.jeppe.radm.application.processing

import com.jeppe.radm.data.repository.ActivityRepository
import com.jeppe.radm.domain.model.AbsoluteTimestampUtcMillis
import com.jeppe.radm.domain.model.ActivityId
import com.jeppe.radm.domain.model.ActivityProcessorState
import com.jeppe.radm.domain.model.ActivityType
import com.jeppe.radm.domain.model.DerivedTrackMetric
import com.jeppe.radm.domain.model.ProcessorDefinition
import com.jeppe.radm.domain.model.ProcessorName
import com.jeppe.radm.domain.model.ProcessorStatus
import com.jeppe.radm.domain.processing.CadenceProcessor
import com.jeppe.radm.domain.processing.DistanceProcessor
import com.jeppe.radm.domain.processing.MovementProcessor
import com.jeppe.radm.domain.processing.R00CadenceProcessor
import com.jeppe.radm.domain.processing.R00DistanceProcessor
import com.jeppe.radm.domain.processing.R00MovementProcessor
import com.jeppe.radm.domain.processing.R00SummaryProcessor
import com.jeppe.radm.domain.processing.SummaryProcessor

data class ActivityRecalculationResult(
    val activityId: ActivityId,
    val processorStates: Map<ProcessorName, ProcessorStatus>,
    val failures: Map<ProcessorName, String>,
) {
    val successful: Boolean get() = failures.isEmpty()
}

/** IMP M7 application workflow: retained source -> deterministic processors -> atomic replacement. */
class RecalculateActivity(
    private val repository: ActivityRepository,
    private val distanceProcessor: DistanceProcessor = R00DistanceProcessor(),
    private val movementProcessor: MovementProcessor = R00MovementProcessor(),
    private val cadenceProcessor: CadenceProcessor = R00CadenceProcessor(),
    private val summaryProcessor: SummaryProcessor = R00SummaryProcessor(),
) {
    suspend operator fun invoke(
        activityId: ActivityId,
        processedAt: AbsoluteTimestampUtcMillis,
    ): ActivityRecalculationResult {
        val activity = requireNotNull(repository.get(activityId)) { "Activity does not exist: $activityId" }
        val definitions = ensureDefinitions()
        ensureInitialStates(activityId)
        val positions = repository.getPositions(activityId)
        val steps = repository.getSteps(activityId)
        val statuses = linkedMapOf<ProcessorName, ProcessorStatus>()
        val failures = linkedMapOf<ProcessorName, String>()

        val distanceResult = runCatching { distanceProcessor.process(positions) }
        val trackMetrics: List<DerivedTrackMetric>? = distanceResult.getOrNull()?.let { distance ->
            runCatching { movementProcessor.process(activity.type, positions, distance) }
                .onFailure { failures[applicableMovementProcessor(activity.type)] = it.failureDescription() }
                .getOrElse {
                    distance.map { sample ->
                        DerivedTrackMetric(
                            activityId = sample.activityId,
                            positionSampleIndex = sample.positionSampleIndex,
                            cumulativeDistance = sample.cumulativeDistance,
                            pace = null,
                            speed = null,
                        )
                    }
                }
        }

        if (trackMetrics == null) {
            failures[ProcessorName.DISTANCE] = distanceResult.exceptionOrNull().failureDescription()
            val failedTrackStates = TRACK_PROCESSORS.map { name ->
                state(activityId, definitions.getValue(name), ProcessorStatus.FAILED, processedAt)
            }
            repository.putProcessorStates(failedTrackStates)
            failedTrackStates.forEach { statuses[it.processorName] = it.status }
        } else {
            val failedMovement = failures.keys.firstOrNull { it == ProcessorName.PACE || it == ProcessorName.SPEED }
            val trackStates = TRACK_PROCESSORS.map { name ->
                val status = if (name == failedMovement) ProcessorStatus.FAILED else ProcessorStatus.CURRENT
                state(activityId, definitions.getValue(name), status, processedAt)
            }
            repository.replaceTrackMetrics(activityId, trackMetrics, trackStates)
            trackStates.forEach { statuses[it.processorName] = it.status }
        }

        val cadenceState = runCatching { cadenceProcessor.process(activity.type, steps) }
            .fold(
                onSuccess = { cadence ->
                    state(
                        activityId,
                        definitions.getValue(ProcessorName.CADENCE),
                        ProcessorStatus.CURRENT,
                        processedAt,
                    ).also { repository.replaceCadence(activityId, cadence, it) }
                },
                onFailure = { failure ->
                    failures[ProcessorName.CADENCE] = failure.failureDescription()
                    state(
                        activityId,
                        definitions.getValue(ProcessorName.CADENCE),
                        ProcessorStatus.FAILED,
                        processedAt,
                    ).also { repository.putProcessorStates(listOf(it)) }
                },
            )
        statuses[ProcessorName.CADENCE] = cadenceState.status

        val summaryState = if (trackMetrics == null) {
            failures[ProcessorName.SUMMARY] = "Distance processing failed"
            state(
                activityId,
                definitions.getValue(ProcessorName.SUMMARY),
                ProcessorStatus.FAILED,
                processedAt,
            ).also { repository.putProcessorStates(listOf(it)) }
        } else {
            runCatching { summaryProcessor.process(activity, positions, trackMetrics) }
                .fold(
                    onSuccess = { summary ->
                        state(
                            activityId,
                            definitions.getValue(ProcessorName.SUMMARY),
                            ProcessorStatus.CURRENT,
                            processedAt,
                        ).also { repository.replaceSummary(activityId, summary, it) }
                    },
                    onFailure = { failure ->
                        failures[ProcessorName.SUMMARY] = failure.failureDescription()
                        state(
                            activityId,
                            definitions.getValue(ProcessorName.SUMMARY),
                            ProcessorStatus.FAILED,
                            processedAt,
                        ).also { repository.putProcessorStates(listOf(it)) }
                    },
                )
        }
        statuses[ProcessorName.SUMMARY] = summaryState.status

        return ActivityRecalculationResult(activityId, statuses, failures)
    }

    private suspend fun ensureDefinitions(): Map<ProcessorName, ProcessorDefinition> =
        ALL_PROCESSORS.associateWith { name ->
            repository.getProcessorDefinition(name) ?: ProcessorDefinition(name, 1).also {
                repository.putProcessorDefinition(it)
            }
        }

    private suspend fun ensureInitialStates(
        activityId: ActivityId,
    ) {
        val existing = repository.getProcessorStates(activityId).map { it.processorName }.toSet()
        repository.putProcessorStates(
            ALL_PROCESSORS.filterNot(existing::contains).map { name ->
                ActivityProcessorState(
                    activityId = activityId,
                    processorName = name,
                    processorVersion = null,
                    status = ProcessorStatus.UNPROCESSED,
                    processedAt = null,
                )
            },
        )
    }

    private fun state(
        activityId: ActivityId,
        definition: ProcessorDefinition,
        status: ProcessorStatus,
        processedAt: AbsoluteTimestampUtcMillis,
    ) = ActivityProcessorState(
        activityId = activityId,
        processorName = definition.name,
        processorVersion = definition.currentVersion,
        status = status,
        processedAt = processedAt,
    )

    private fun applicableMovementProcessor(activityType: ActivityType) =
        if (activityType == ActivityType.CYCLING) {
            ProcessorName.SPEED
        } else {
            ProcessorName.PACE
        }

    private fun Throwable?.failureDescription(): String =
        this?.message?.takeIf { it.isNotBlank() } ?: this?.javaClass?.simpleName ?: "Unknown failure"

    private companion object {
        val TRACK_PROCESSORS = listOf(ProcessorName.DISTANCE, ProcessorName.PACE, ProcessorName.SPEED)
        val ALL_PROCESSORS = TRACK_PROCESSORS + ProcessorName.CADENCE + ProcessorName.SUMMARY
    }
}
