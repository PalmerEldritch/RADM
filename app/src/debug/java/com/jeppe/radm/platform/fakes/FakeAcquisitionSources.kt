package com.jeppe.radm.platform.fakes

import com.jeppe.radm.domain.model.AbsoluteTimestampUtcMillis
import com.jeppe.radm.domain.model.MonotonicTimeMillis
import com.jeppe.radm.domain.recording.ClockSource
import com.jeppe.radm.domain.recording.LocationMeasurement
import com.jeppe.radm.domain.recording.LocationSource
import com.jeppe.radm.domain.recording.StepMeasurement
import com.jeppe.radm.domain.recording.StepSource

class FakeClockSource(
    absoluteTime: AbsoluteTimestampUtcMillis,
    monotonicTime: MonotonicTimeMillis,
) : ClockSource {
    var absoluteTime: AbsoluteTimestampUtcMillis = absoluteTime
        private set
    var monotonicTime: MonotonicTimeMillis = monotonicTime
        private set

    override fun absoluteNow(): AbsoluteTimestampUtcMillis = absoluteTime

    override fun monotonicNow(): MonotonicTimeMillis = monotonicTime

    fun advance(absoluteMillis: Long, monotonicMillis: Long = absoluteMillis) {
        require(absoluteMillis >= 0L && monotonicMillis >= 0L) { "Clock advances must be non-negative" }
        absoluteTime = AbsoluteTimestampUtcMillis(Math.addExact(absoluteTime.value, absoluteMillis))
        monotonicTime = MonotonicTimeMillis(Math.addExact(monotonicTime.value, monotonicMillis))
    }

    fun adjustCivilTimeTo(value: AbsoluteTimestampUtcMillis) {
        absoluteTime = value
    }
}

class FakeLocationSource : LocationSource {
    var isStarted: Boolean = false
        private set
    var startCount: Int = 0
        private set
    var stopCount: Int = 0
        private set

    private var consumer: (suspend (LocationMeasurement) -> Unit)? = null

    override suspend fun start(consumer: suspend (LocationMeasurement) -> Unit) {
        this.consumer = consumer
        isStarted = true
        startCount += 1
    }

    override suspend fun stop() {
        isStarted = false
        stopCount += 1
    }

    suspend fun emit(measurement: LocationMeasurement): Boolean {
        if (!isStarted) return false
        checkNotNull(consumer)(measurement)
        return true
    }

    /** Simulates a delayed callback already queued when acquisition was stopped. */
    suspend fun emitDelayed(measurement: LocationMeasurement) {
        checkNotNull(consumer)(measurement)
    }
}

class FakeStepSource : StepSource {
    var isStarted: Boolean = false
        private set
    var startCount: Int = 0
        private set
    var stopCount: Int = 0
        private set

    private var consumer: (suspend (StepMeasurement) -> Unit)? = null

    override suspend fun start(consumer: suspend (StepMeasurement) -> Unit) {
        this.consumer = consumer
        isStarted = true
        startCount += 1
    }

    override suspend fun stop() {
        isStarted = false
        stopCount += 1
    }

    suspend fun emit(measurement: StepMeasurement): Boolean {
        if (!isStarted) return false
        checkNotNull(consumer)(measurement)
        return true
    }

    /** Simulates a delayed callback already queued when acquisition was stopped. */
    suspend fun emitDelayed(measurement: StepMeasurement) {
        checkNotNull(consumer)(measurement)
    }
}
