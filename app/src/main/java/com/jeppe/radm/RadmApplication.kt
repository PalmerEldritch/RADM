package com.jeppe.radm

import android.app.Application
import com.jeppe.radm.data.db.RadmDatabaseFactory
import com.jeppe.radm.data.repository.RoomActivityRepository
import com.jeppe.radm.data.repository.RoomRecordingRepository
import com.jeppe.radm.application.library.DeleteSavedActivity
import com.jeppe.radm.application.library.EditSavedActivity
import com.jeppe.radm.application.library.LoadActivityLibrary
import com.jeppe.radm.application.library.LoadSavedActivity
import com.jeppe.radm.application.processing.RecalculateActivity
import com.jeppe.radm.platform.permissions.RecordingCapabilityChecker
import com.jeppe.radm.platform.recording.RecordingServiceClient
import com.jeppe.radm.platform.recording.RecordingServiceStateStore

class RadmApplication : Application() {
    val container: RadmContainer by lazy { RadmContainer(this) }
}

class RadmContainer(application: Application) {
    val database by lazy { RadmDatabaseFactory.create(application) }
    val activityRepository by lazy { RoomActivityRepository(database) }
    val recordingRepository by lazy { RoomRecordingRepository(database) }
    val recalculateActivity by lazy { RecalculateActivity(activityRepository) }
    val loadActivityLibrary by lazy { LoadActivityLibrary(activityRepository) }
    val loadSavedActivity by lazy { LoadSavedActivity(activityRepository) }
    val editSavedActivity by lazy { EditSavedActivity(activityRepository, recalculateActivity) }
    val deleteSavedActivity by lazy { DeleteSavedActivity(activityRepository) }
    val recordingStateStore = RecordingServiceStateStore()
    val recordingCapabilityChecker = RecordingCapabilityChecker(application)
    val recordingServiceClient = RecordingServiceClient(
        application,
        recordingStateStore,
        recordingCapabilityChecker,
    )
}
