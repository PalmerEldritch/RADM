package com.jeppe.radm.platform.recording

import android.content.Context
import android.content.Intent
import com.jeppe.radm.R
import com.jeppe.radm.application.recording.SaveRecordingMetadata
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

    fun resumeRecovery(): Result<Unit> {
        val recoverable = stateStore.state.value as? RecordingServiceState.Recoverable
            ?: return Result.failure(IllegalStateException("No interrupted recording is available"))
        if (recoverable.recording.durableState == com.jeppe.radm.domain.recording.RecordingState.FINALIZING) {
            return Result.failure(IllegalStateException("This activity is already awaiting final save"))
        }
        if (!capabilityChecker.current().canStartLocationForegroundService) {
            return Result.failure(
                IllegalStateException(
                    applicationContext.getString(R.string.recording_service_location_capability_required),
                ),
            )
        }
        stateStore.publish(RecordingServiceState.Starting(recoverable.recording.activityType))
        return runCatching {
            applicationContext.startForegroundService(
                RecordingForegroundService.intent(applicationContext, RecordingServiceAction.RECOVER)
                    .putExtra(
                        RecordingForegroundService.EXTRA_ACTIVITY_TYPE,
                        recoverable.recording.activityType.name,
                    ),
            )
        }.map { Unit }.onFailure {
            stateStore.publish(recoverable)
        }
    }

    fun pause(): Result<Unit> = send(RecordingServiceAction.PAUSE)

    fun resume(): Result<Unit> = send(RecordingServiceAction.RESUME)

    fun finish(): Result<Unit> = send(RecordingServiceAction.FINISH)

    fun save(metadata: SaveRecordingMetadata = SaveRecordingMetadata()): Result<Unit> = runCatching {
        check(stateStore.state.value is RecordingServiceState.Active) {
            "No authoritative recording service session is available"
        }
        val intent = RecordingForegroundService.intent(applicationContext, RecordingServiceAction.SAVE)
        metadata.activityType?.let { intent.putExtra(RecordingForegroundService.EXTRA_FINAL_ACTIVITY_TYPE, it.name) }
        metadata.title?.let { intent.putExtra(RecordingForegroundService.EXTRA_TITLE, it) }
        metadata.notes?.let { intent.putExtra(RecordingForegroundService.EXTRA_NOTES, it) }
        applicationContext.startService(intent)
        Unit
    }

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
    RECOVER("com.jeppe.radm.recording.RECOVER"),
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
