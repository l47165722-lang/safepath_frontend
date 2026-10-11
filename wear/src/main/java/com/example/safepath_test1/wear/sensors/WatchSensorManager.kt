package com.example.safepath_test1.wear.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * One combined accelerometer + gyroscope reading.
 * Mirrors the MotionData class in the team class diagram - this is the
 * value WatchSensorManager will eventually hand to WearDataRelayService
 * on the phone side via sendMotionData().
 */
data class MotionData(
    val accelX: Float,
    val accelY: Float,
    val accelZ: Float,
    val gyroX: Float,
    val gyroY: Float,
    val gyroZ: Float,
    val timestamp: Long,
)

private const val TAG = "WatchSensorManager"

/**
 * Reads the watch's built-in accelerometer and gyroscope.
 *
 * Usage (typically from a DisposableEffect so collection stops when the
 * screen leaves composition):
 *   val sensorManager = remember { WatchSensorManager(context) }
 *   DisposableEffect(Unit) {
 *       sensorManager.startAccelCollection()
 *       sensorManager.startGyroCollection()
 *       onDispose { sensorManager.stopCollection() }
 *   }
 *   val motion by sensorManager.motionData.collectAsState()
 *
 * No runtime permission is needed for TYPE_ACCELEROMETER / TYPE_GYROSCOPE at
 * SENSOR_DELAY_NORMAL. HIGH_SAMPLING_RATE_SENSORS would only be required if
 * we asked for a faster-than-normal sampling rate on Android 12+.
 */
class WatchSensorManager(context: Context) {

    private val sensorManager =
        context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private var lastAccel = Triple(0f, 0f, 0f)
    private var lastGyro = Triple(0f, 0f, 0f)

    private val _motionData = MutableStateFlow<MotionData?>(null)
    /** Latest combined reading. null until both sensors have reported at least once. */
    val motionData: StateFlow<MotionData?> = _motionData.asStateFlow()

    val hasAccelerometer: Boolean get() = accelerometer != null
    val hasGyroscope: Boolean get() = gyroscope != null

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER ->
                    lastAccel = Triple(event.values[0], event.values[1], event.values[2])
                Sensor.TYPE_GYROSCOPE ->
                    lastGyro = Triple(event.values[0], event.values[1], event.values[2])
                else -> return
            }
            val updated = MotionData(
                accelX = lastAccel.first,
                accelY = lastAccel.second,
                accelZ = lastAccel.third,
                gyroX = lastGyro.first,
                gyroY = lastGyro.second,
                gyroZ = lastGyro.third,
                timestamp = System.currentTimeMillis(),
            )
            _motionData.value = updated
            Log.d(
                TAG,
                "accel=(%.2f, %.2f, %.2f) gyro=(%.2f, %.2f, %.2f)".format(
                    updated.accelX, updated.accelY, updated.accelZ,
                    updated.gyroX, updated.gyroY, updated.gyroZ,
                ),
            )
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    /** Starts listening to the accelerometer. Safe to call even if startGyroCollection() is also called. */
    fun startAccelCollection() {
        accelerometer?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    /** Starts listening to the gyroscope. Safe to call even if startAccelCollection() is also called. */
    fun startGyroCollection() {
        gyroscope?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    /** Stops both. Call this from onDispose so the sensors don't keep draining the battery. */
    fun stopCollection() {
        sensorManager.unregisterListener(listener)
    }
}
