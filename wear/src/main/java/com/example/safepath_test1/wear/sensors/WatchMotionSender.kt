package com.example.safepath_test1.wear.sensors

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

private const val TAG = "WatchMotionSender"

/** Phone-side WearDataRelayService (app module) listens on this same path - keep both in sync. */
const val PATH_MOTION = "/safepath/motion"

/**
 * Synchronizes the latest WatchSensorManager reading to the phone over the Wearable Data Layer's
 * DataClient, mirroring the phone->watch pattern already used by WearMessenger (app module,
 * com.example.safepath_test1.wear.WearMessenger): DataClient + matching applicationId is the
 * combination confirmed working real-device + real-device for this project, so MessageClient is not
 * used. StateFlow and a single DataItem may coalesce updates; this is not a lossless sensor stream.
 *
 * Does not log on success - at 20Hz that would double the log volume WatchSensorManager already
 * produces. Failures are still logged.
 */
object WatchMotionSender {

    fun send(context: Context, motion: MotionData) {
        val putDataMapRequest =
            PutDataMapRequest.create(PATH_MOTION).apply {
                dataMap.putFloat("accelX", motion.accelX)
                dataMap.putFloat("accelY", motion.accelY)
                dataMap.putFloat("accelZ", motion.accelZ)
                dataMap.putFloat("gyroX", motion.gyroX)
                dataMap.putFloat("gyroY", motion.gyroY)
                dataMap.putFloat("gyroZ", motion.gyroZ)
                dataMap.putLong("timestamp", motion.timestamp)
            }
        val request = putDataMapRequest.asPutDataRequest().setUrgent()

        Wearable.getDataClient(context).putDataItem(request).addOnFailureListener { e ->
            Log.e(TAG, "Failed to relay motion data to phone", e)
        }
    }
}
