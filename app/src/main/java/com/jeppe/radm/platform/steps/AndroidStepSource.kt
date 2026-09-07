package com.jeppe.radm.platform.steps

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.SystemClock
import com.jeppe.radm.domain.recording.StepCounterCandidate
import com.jeppe.radm.domain.recording.StepCounterProcessor
import com.jeppe.radm.domain.recording.StepCounterResult
import com.jeppe.radm.domain.recording.StepMeasurement
import com.jeppe.radm.domain.recording.StepSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Phone-native cumulative step-counter adapter. No Android sensor type crosses its boundary. */
class AndroidStepSource(
    context: Context,
    private val callbackScope: CoroutineScope,
    private val activityRecognitionPermissionGranted: () -> Boolean = {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            context.applicationContext.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED
    },
) : StepSource {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val processor = StepCounterProcessor()

    @Volatile
    private var consumer: (suspend (StepMeasurement) -> Unit)? = null
    private var registered = false
    private var hasStartedAcquisition = false
    private var acquisitionStartedAtMillis = 0L

    internal val isSensorAvailable: Boolean
        get() = stepCounter != null

    internal val isRegistered: Boolean
        get() = registered

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_STEP_COUNTER) return
            val candidate = event.toStepCounterCandidate() ?: return
            // Do not allow an already queued pre-resume event to establish the resumed baseline.
            if (candidate.monotonicTimestampMillis < acquisitionStartedAtMillis) return
            val accepted = processor.accept(candidate) as? StepCounterResult.Accepted ?: return
            publish(accepted.measurement)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    @SuppressLint("MissingPermission")
    override suspend fun start(consumer: suspend (StepMeasurement) -> Unit) {
        this.consumer = consumer
        val sensor = stepCounter
        if (sensor == null || !activityRecognitionPermissionGranted()) {
            this.consumer = null
            return
        }

        acquisitionStartedAtMillis = SystemClock.elapsedRealtime()
        if (hasStartedAcquisition) processor.beginNewBaseline()
        try {
            val registeredNow = withContext(Dispatchers.Main.immediate) {
                if (registered) true else sensorManager.registerListener(
                    listener,
                    sensor,
                    SensorManager.SENSOR_DELAY_NORMAL,
                )
            }
            registered = registeredNow
            if (registeredNow) hasStartedAcquisition = true else this.consumer = null
        } catch (failure: RuntimeException) {
            this.consumer = null
            throw failure
        }
    }

    override suspend fun stop() {
        try {
            withContext(Dispatchers.Main.immediate) {
                if (registered) sensorManager.unregisterListener(listener, stepCounter)
                registered = false
            }
        } finally {
            consumer = null
        }
    }

    private fun publish(measurement: StepMeasurement) {
        val currentConsumer = consumer ?: return
        callbackScope.launch { currentConsumer(measurement) }
    }
}

internal fun SensorEvent.toStepCounterCandidate(): StepCounterCandidate? {
    val cumulativeSteps = values.firstOrNull()?.toCumulativeStepsOrNull() ?: return null
    val monotonicMillis = timestamp / NANOS_PER_MILLISECOND
    val callbackMonotonicMillis = SystemClock.elapsedRealtime()
    if (monotonicMillis < 0L || monotonicMillis > callbackMonotonicMillis) return null
    val eventAgeMillis = callbackMonotonicMillis - monotonicMillis
    val timestampUtcMillis = System.currentTimeMillis() - eventAgeMillis
    if (timestampUtcMillis < 0L) return null
    return StepCounterCandidate(
        timestampUtcMillis = timestampUtcMillis,
        monotonicTimestampMillis = monotonicMillis,
        cumulativeSteps = cumulativeSteps,
    )
}

internal fun Float.toCumulativeStepsOrNull(): Long? {
    if (!isFinite() || this < 0f || this % 1f != 0f || this > Long.MAX_VALUE.toFloat()) return null
    return toLong()
}

private const val NANOS_PER_MILLISECOND = 1_000_000L
