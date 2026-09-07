package com.jeppe.radm.platform.recording

import android.content.Context
import android.content.Intent
import com.jeppe.radm.R
import com.jeppe.radm.domain.model.ActivityType
import com.jeppe.radm.platform.permissions.RecordingCapabilityChecker

/** User-visible command entry point. It never mutates recording state itself. */
class RecordingServiceClient(
    context: Context,
    private val stateStore: RecordingServiceStateStore,
    private val capabilityChecker: RecordingCapabilityChecker,
) {
    private val applicationContext = context.applicationContext

    fun start(activityType: ActivityType): Result<Unit> {
        val current = stateStore.state.value
        if (current is RecordingServiceState.Starting || current is RecordingServiceState.Active) {
            return Result.failure(IllegalStateException("A recording service session already exists"))
        }
        if (!capabilityChecker.current().canStartLocationForegroundService) {
            val failure = IllegalStateException(
                applicationContext.getString(R.string.recording_service_location_capability_required),
            )
            stateStore.publish(RecordingServiceState.CriticalError(checkNotNull(failure.message)))
            return Result.failure(failure)
        }
        stateStore.publish(RecordingServiceState.Starting(activityType))
        return runCatching {
            applicationContext.startForegroundService(
                RecordingForegroundService.intent(applicationContext, RecordingServiceAction.START)
                    .putExtra(RecordingForegroundService.EXTRA_ACTIVITY_TYPE, activityType.name),
            )
        }.map { Unit }.onFailure { failure ->
            stateStore.publish(
                RecordingServiceState.CriticalError(
                    failure.message ?: "Android prevented the recording service from starting",
                ),
            )
        }
    }

    fun pause(): Result<Unit> = send(RecordingServiceAction.PAUSE)

    fun resume(): Result<Unit> = send(RecordingServiceAction.RESUME)

    fun finish(): Result<Unit> = send(RecordingServiceAction.FINISH)

    fun save(): Result<Unit> = send(RecordingServiceAction.SAVE)

    fun discard(): Result<Unit> = send(RecordingServiceAction.DISCARD)

    private fun send(action: RecordingServiceAction): Result<Unit> = runCatching {
        check(stateStore.state.value is RecordingServiceState.Active) {
            "No authoritative recording service session is available"
        }
        applicationContext.startService(RecordingForegroundService.intent(applicationContext, action))
        Unit
    }
}

enum class RecordingServiceAction(val intentAction: String) {
    START("com.jeppe.radm.recording.START"),
    PAUSE("com.jeppe.radm.recording.PAUSE"),
    RESUME("com.jeppe.radm.recording.RESUME"),
    FINISH("com.jeppe.radm.recording.FINISH"),
    SAVE("com.jeppe.radm.recording.SAVE"),
    DISCARD("com.jeppe.radm.recording.DISCARD"),
    ;

    companion object {
        fun from(intent: Intent?): RecordingServiceAction? = entries.firstOrNull {
            it.intentAction == intent?.action
        }
    }
}
