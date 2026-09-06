package com.jeppe.radm.platform.recording

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import com.jeppe.radm.MainActivity
import com.jeppe.radm.R
import com.jeppe.radm.RadmApplication
import com.jeppe.radm.application.recording.RecordingController
import com.jeppe.radm.application.recording.RecordingSnapshot
import com.jeppe.radm.domain.model.ActivityType
import com.jeppe.radm.domain.recording.RecordingState
import com.jeppe.radm.platform.location.AndroidLocationSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RecordingForegroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val commands = Channel<ServiceCommand>(Channel.UNLIMITED)
    private lateinit var stateStore: RecordingServiceStateStore
    private lateinit var controller: RecordingController
    private lateinit var notificationManager: NotificationManager
    private var acceptedStartCommand = false
    private var resolvedNormally = false
    private var lastSnapshot: RecordingSnapshot? = null

    override fun onCreate() {
        super.onCreate()
        val container = (application as RadmApplication).container
        stateStore = container.recordingStateStore
        controller = RecordingController(
            recordingRepository = container.recordingRepository,
            locationSource = AndroidLocationSource(this, serviceScope),
            stepSource = PendingStepSource(),
            clockSource = AndroidClockSource,
        )
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()
        serviceScope.launch {
            for (command in commands) execute(command)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (val action = RecordingServiceAction.from(intent)) {
            RecordingServiceAction.START -> acceptStart(checkNotNull(intent))
            RecordingServiceAction.PAUSE,
            RecordingServiceAction.RESUME,
            RecordingServiceAction.FINISH,
            RecordingServiceAction.SAVE,
            RecordingServiceAction.DISCARD,
            -> commands.trySend(ServiceCommand.Action(checkNotNull(action)))

            null -> {
                publishCriticalError(getString(R.string.recording_service_restart_requires_recovery))
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        commands.close()
        serviceScope.cancel()
        if (!resolvedNormally && acceptedStartCommand) {
            publishCriticalError(getString(R.string.recording_service_stopped_unexpectedly))
        }
        super.onDestroy()
    }

    private fun acceptStart(intent: Intent) {
        if (acceptedStartCommand) return
        acceptedStartCommand = true
        val activityType = intent.getStringExtra(EXTRA_ACTIVITY_TYPE)
            ?.let { runCatching { ActivityType.valueOf(it) }.getOrNull() }
        if (activityType == null) {
            failServiceStart(getString(R.string.recording_service_invalid_start))
            return
        }
        val capabilities = (application as RadmApplication).container.recordingCapabilityChecker.current()
        if (!capabilities.canStartLocationForegroundService) {
            failServiceStart(getString(R.string.recording_service_location_capability_required))
            return
        }
        if (!promoteToForeground(activityType)) return
        commands.trySend(ServiceCommand.Start(activityType))
    }

    private fun promoteToForeground(activityType: ActivityType): Boolean = try {
        val notification = buildNotification(
            title = getString(R.string.recording_notification_title),
            text = getString(R.string.recording_notification_starting, activityType.displayName()),
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        true
    } catch (failure: RuntimeException) {
        failServiceStart(
            failure.message ?: getString(R.string.recording_service_start_blocked),
        )
        false
    }

    private suspend fun execute(command: ServiceCommand) {
        try {
            when (command) {
                is ServiceCommand.Start -> {
                    publish(controller.start(command.activityType))
                    launchElapsedTicker()
                }

                is ServiceCommand.Action -> when (command.action) {
                    RecordingServiceAction.PAUSE -> publish(controller.pause())
                    RecordingServiceAction.RESUME -> publish(controller.resume())
                    RecordingServiceAction.FINISH -> publish(controller.finish())
                    RecordingServiceAction.SAVE -> {
                        controller.save()
                        resolveAndStop()
                    }

                    RecordingServiceAction.DISCARD -> {
                        controller.discard()
                        resolveAndStop()
                    }

                    RecordingServiceAction.START -> Unit
                }
            }
        } catch (failure: Throwable) {
            publishCriticalError(
                failure.message ?: getString(R.string.recording_command_failed),
            )
            if (command is ServiceCommand.Start) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun launchElapsedTicker() {
        serviceScope.launch {
            while (true) {
                delay(ELAPSED_UPDATE_INTERVAL_MS)
                val snapshot = runCatching { controller.snapshot() }.getOrNull() ?: return@launch
                if (snapshot.state == RecordingState.IDLE) return@launch
                publish(snapshot, updateNotification = false)
            }
        }
    }

    private fun publish(snapshot: RecordingSnapshot, updateNotification: Boolean = true) {
        lastSnapshot = snapshot
        stateStore.publish(RecordingServiceState.Active(snapshot))
        if (updateNotification) {
            notificationManager.notify(NOTIFICATION_ID, notificationFor(snapshot))
        }
    }

    private fun notificationFor(snapshot: RecordingSnapshot): Notification {
        val activity = snapshot.activityType?.displayName().orEmpty()
        val stateText = when (snapshot.state) {
            RecordingState.RECORDING -> getString(R.string.recording_notification_recording, activity)
            RecordingState.PAUSED -> getString(R.string.recording_notification_paused, activity)
            RecordingState.FINALIZING -> getString(R.string.recording_notification_finalizing, activity)
            RecordingState.IDLE -> getString(R.string.recording_notification_starting, activity)
        }
        return buildNotification(getString(R.string.recording_notification_title), stateText)
    }

    private fun buildNotification(title: String, text: String): Notification {
        val openApplication = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openApplication)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }
        return builder.build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.recording_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.recording_notification_channel_description)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun failServiceStart(message: String) {
        publishCriticalError(message)
        stopSelf()
    }

    private fun publishCriticalError(message: String) {
        stateStore.publish(RecordingServiceState.CriticalError(message, lastSnapshot))
    }

    private fun resolveAndStop() {
        resolvedNormally = true
        stateStore.publish(RecordingServiceState.Idle)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun ActivityType.displayName(): String = when (this) {
        ActivityType.RUNNING -> getString(R.string.activity_type_running)
        ActivityType.CYCLING -> getString(R.string.activity_type_cycling)
        ActivityType.CROSS_COUNTRY_SKIING -> getString(R.string.activity_type_skiing)
    }

    private sealed interface ServiceCommand {
        data class Start(val activityType: ActivityType) : ServiceCommand
        data class Action(val action: RecordingServiceAction) : ServiceCommand
    }

    companion object {
        const val EXTRA_ACTIVITY_TYPE = "activity_type"
        const val NOTIFICATION_CHANNEL_ID = "recording"
        const val NOTIFICATION_ID = 1001
        private const val ELAPSED_UPDATE_INTERVAL_MS = 250L

        fun intent(context: Context, action: RecordingServiceAction): Intent =
            Intent(context, RecordingForegroundService::class.java).setAction(action.intentAction)
    }
}
