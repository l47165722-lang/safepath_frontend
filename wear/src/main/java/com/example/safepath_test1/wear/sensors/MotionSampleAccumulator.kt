package com.example.safepath_test1.wear.sensors

/** Latest readings from both sensors. timestamp is wall-clock milliseconds for the phone. */
data class MotionData(
    val accelX: Float,
    val accelY: Float,
    val accelZ: Float,
    val gyroX: Float,
    val gyroY: Float,
    val gyroZ: Float,
    val timestamp: Long,
)

/** Limits combined updates to 20 Hz using the sensor event's monotonic timestamp. */
internal class MotionSampleAccumulator {
    private var acceleration: Triple<Float, Float, Float>? = null
    private var rotation: Triple<Float, Float, Float>? = null
    private var lastEmissionNanos: Long? = null

    fun updateAcceleration(x: Float, y: Float, z: Float, nanos: Long, millis: Long): MotionData? {
        if (!x.isFinite() || !y.isFinite() || !z.isFinite()) return null
        acceleration = Triple(x, y, z)
        return sample(nanos, millis)
    }

    fun updateRotation(x: Float, y: Float, z: Float, nanos: Long, millis: Long): MotionData? {
        if (!x.isFinite() || !y.isFinite() || !z.isFinite()) return null
        rotation = Triple(x, y, z)
        return sample(nanos, millis)
    }

    fun reset() {
        acceleration = null
        rotation = null
        lastEmissionNanos = null
    }

    private fun sample(nanos: Long, millis: Long): MotionData? {
        val accel = acceleration ?: return null
        val gyro = rotation ?: return null
        val previous = lastEmissionNanos
        if (previous != null && nanos - previous < 50_000_000L) return null
        lastEmissionNanos = nanos
        return MotionData(
            accel.first,
            accel.second,
            accel.third,
            gyro.first,
            gyro.second,
            gyro.third,
            millis,
        )
    }
}
