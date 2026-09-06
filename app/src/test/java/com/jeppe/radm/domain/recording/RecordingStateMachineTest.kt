package com.jeppe.radm.domain.recording

import com.jeppe.radm.domain.model.RecordingEventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** VVM-REC-001: deterministic valid and invalid recording-state transitions. */
class RecordingStateMachineTest {
    @Test
    fun `VVM REC 001 accepts every specified lifecycle transition`() {
        assertAccepted(RecordingState.IDLE, RecordingCommand.START, RecordingState.RECORDING, RecordingEventType.START)
        assertAccepted(RecordingState.RECORDING, RecordingCommand.PAUSE, RecordingState.PAUSED, RecordingEventType.PAUSE)
        assertAccepted(RecordingState.PAUSED, RecordingCommand.RESUME, RecordingState.RECORDING, RecordingEventType.RESUME)
        assertAccepted(RecordingState.RECORDING, RecordingCommand.FINISH, RecordingState.FINALIZING, RecordingEventType.FINISH)
        assertAccepted(RecordingState.PAUSED, RecordingCommand.FINISH, RecordingState.FINALIZING, RecordingEventType.FINISH)

        val saved = RecordingStateMachine.transition(RecordingState.FINALIZING, RecordingCommand.SAVE)
        assertTrue(saved is RecordingTransition.Accepted)
        saved as RecordingTransition.Accepted
        assertEquals(RecordingState.IDLE, saved.state)
        assertEquals(FinalizationOutcome.SAVED, saved.finalizationOutcome)

        val discarded = RecordingStateMachine.transition(RecordingState.FINALIZING, RecordingCommand.DISCARD)
        assertTrue(discarded is RecordingTransition.Accepted)
        discarded as RecordingTransition.Accepted
        assertEquals(RecordingState.IDLE, discarded.state)
        assertEquals(FinalizationOutcome.DISCARDED, discarded.finalizationOutcome)
    }

    @Test
    fun `VVM REC 001 rejects all unspecified transitions`() {
        val acceptedPairs = setOf(
            RecordingState.IDLE to RecordingCommand.START,
            RecordingState.RECORDING to RecordingCommand.PAUSE,
            RecordingState.RECORDING to RecordingCommand.FINISH,
            RecordingState.PAUSED to RecordingCommand.RESUME,
            RecordingState.PAUSED to RecordingCommand.FINISH,
            RecordingState.FINALIZING to RecordingCommand.SAVE,
            RecordingState.FINALIZING to RecordingCommand.DISCARD,
        )

        RecordingState.entries.forEach { state ->
            RecordingCommand.entries.forEach { command ->
                if (state to command !in acceptedPairs) {
                    assertTrue(
                        "$state + $command should be rejected",
                        RecordingStateMachine.transition(state, command) is RecordingTransition.Rejected,
                    )
                }
            }
        }
    }

    private fun assertAccepted(
        from: RecordingState,
        command: RecordingCommand,
        expected: RecordingState,
        eventType: RecordingEventType,
    ) {
        val result = RecordingStateMachine.transition(from, command)
        assertTrue(result is RecordingTransition.Accepted)
        result as RecordingTransition.Accepted
        assertEquals(expected, result.state)
        assertEquals(eventType, result.eventType)
    }
}
