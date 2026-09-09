package com.jeppe.radm

import android.app.Application
import com.jeppe.radm.application.analysis.LoadActivityAnalysis
import com.jeppe.radm.data.db.RadmDatabaseFactory
import com.jeppe.radm.data.repository.RoomActivityRepository
import com.jeppe.radm.data.repository.RoomRecordingRepository
import com.jeppe.radm.application.library.DeleteSavedActivity
import com.jeppe.radm.application.library.EditSavedActivity
import com.jeppe.radm.application.library.LoadActivityLibrary
import com.jeppe.radm.application.library.LoadSavedActivity
import com.jeppe.radm.application.processing.RecalculateActivity
import com.jeppe.radm.application.recording.RecordingRecovery
import com.jeppe.radm.platform.permissions.RecordingCapabilityChecker
import com.jeppe.radm.platform.recording.AndroidClockSource
import com.jeppe.radm.platform.recording.RecordingServiceClient
import com.jeppe.radm.platform.recording.RecordingServiceState
import com.jeppe.radm.platform.recording.RecordingServiceStateStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RadmApplication : Application() {
    val container: RadmContainer by lazy { RadmContainer(this) }
}

class RadmContainer(application: Application) {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val database by lazy { RadmDatabaseFactory.create(application) }
    val activityRepository by lazy { RoomActivityRepository(database) }
    val recordingRepository by lazy { RoomRecordingRepository(database) }
    val recalculateActivity by lazy { RecalculateActivity(activityRepository) }
    val loadActivityLibrary by lazy { LoadActivityLibrary(activityRepository) }
    val loadSavedActivity by lazy { LoadSavedActivity(activityRepository) }
    val loadActivityAnalysis by lazy { LoadActivityAnalysis(activityRepository) }
    val editSavedActivity by lazy { EditSavedActivity(activityRepository, recalculateActivity) }
    val deleteSavedActivity by lazy { DeleteSavedActivity(activityRepository) }
    val recordingStateStore = RecordingServiceStateStore()
    val recordingRecovery by lazy {
        RecordingRecovery(
            recordingRepository = recordingRepository,
            clockSource = AndroidClockSource,
            recalculateActivity = { activityId, processedAt ->
                recalculateActivity(activityId, processedAt)
            },
        )
    }
    val recordingCapabilityChecker = RecordingCapabilityChecker(application)
    val recordingServiceClient = RecordingServiceClient(
        application,
        recordingStateStore,
        recordingCapabilityChecker,
    )

    init {
        refreshRecoveryState()
    }

    fun refreshRecoveryState(criticalMessage: String? = null) {
        applicationScope.launch {
            val state = runCatching { recordingRecovery.detect() }
                .fold(
                    onSuccess = { recovery ->
                        recovery?.let { RecordingServiceState.Recoverable(it, criticalMessage) }
                            ?: RecordingServiceState.Idle
                    },
                    onFailure = { failure ->
                        RecordingServiceState.CriticalError(
                            failure.message ?: "Unable to inspect durable recording state",
                        )
                    },
                )
            recordingStateStore.publish(state)
        }
    }
}
