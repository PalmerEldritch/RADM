package com.jeppe.radm

import android.app.Application
import com.jeppe.radm.data.db.RadmDatabaseFactory
import com.jeppe.radm.data.repository.RoomRecordingRepository
import com.jeppe.radm.platform.permissions.RecordingCapabilityChecker
import com.jeppe.radm.platform.recording.RecordingServiceClient
import com.jeppe.radm.platform.recording.RecordingServiceStateStore

class RadmApplication : Application() {
    val container: RadmContainer by lazy { RadmContainer(this) }
}

class RadmContainer(application: Application) {
    val database by lazy { RadmDatabaseFactory.create(application) }
    val recordingRepository by lazy { RoomRecordingRepository(database) }
    val recordingStateStore = RecordingServiceStateStore()
    val recordingServiceClient = RecordingServiceClient(application, recordingStateStore)
    val recordingCapabilityChecker = RecordingCapabilityChecker(application)
}
