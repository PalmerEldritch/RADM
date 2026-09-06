package com.jeppe.radm.domain.recording

import com.jeppe.radm.domain.model.RecordingEventType

enum class RecordingState {
    IDLE,
    RECORDING,
    PAUSED,
    FINALIZING,
}

enum class RecordingCommand {
    START,
    PAUSE,
    RESUME,
    FINISH,
    SAVE,
    DISCARD,
}

enum class FinalizationOutcome {
    SAVED,
    DISCARDED,
}

sealed interface RecordingTransition {
    val previousState: RecordingState
    val command: RecordingCommand

    data class Accepted(
        override val previousState: RecordingState,
        override val command: RecordingCommand,
        val state: RecordingState,
        val eventType: RecordingEventType? = null,
        val finalizationOutcome: FinalizationOutcome? = null,
    ) : RecordingTransition

    data class Rejected(
        override val previousState: RecordingState,
        override val command: RecordingCommand,
    ) : RecordingTransition
}

/** Pure transition rules. Persistence and Android service orchestration are later boundaries. */
object RecordingStateMachine {
    fun transition(state: RecordingState, command: RecordingCommand): RecordingTransition =
        when (state to command) {
            RecordingState.IDLE to RecordingCommand.START -> accepted(
                state,
                command,
                RecordingState.RECORDING,
                RecordingEventType.START,
            )

            RecordingState.RECORDING to RecordingCommand.PAUSE -> accepted(
                state,
                command,
                RecordingState.PAUSED,
                RecordingEventType.PAUSE,
            )

            RecordingState.PAUSED to RecordingCommand.RESUME -> accepted(
                state,
                command,
                RecordingState.RECORDING,
                RecordingEventType.RESUME,
            )

            RecordingState.RECORDING to RecordingCommand.FINISH,
            RecordingState.PAUSED to RecordingCommand.FINISH,
            -> accepted(
                state,
                command,
                RecordingState.FINALIZING,
                RecordingEventType.FINISH,
            )

            RecordingState.FINALIZING to RecordingCommand.SAVE -> accepted(
                state,
                command,
                RecordingState.IDLE,
                outcome = FinalizationOutcome.SAVED,
            )

            RecordingState.FINALIZING to RecordingCommand.DISCARD -> accepted(
                state,
                command,
                RecordingState.IDLE,
                outcome = FinalizationOutcome.DISCARDED,
            )

            else -> RecordingTransition.Rejected(state, command)
        }

    private fun accepted(
        previousState: RecordingState,
        command: RecordingCommand,
        state: RecordingState,
        eventType: RecordingEventType? = null,
        outcome: FinalizationOutcome? = null,
    ) = RecordingTransition.Accepted(
        previousState = previousState,
        command = command,
        state = state,
        eventType = eventType,
        finalizationOutcome = outcome,
    )
}
