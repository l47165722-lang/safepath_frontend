package com.example.safepath_test1.wear

import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

private const val TAG = "WearDataRelayService"

/** Must match WatchMotionSender.PATH_MOTION (wear module) - the two modules can't share code. */
private const val PATH_MOTION = "/safepath/motion"

/**
 * Receives each accelerometer/gyroscope reading relayed from the watch (WatchMotionSender, wear
 * module) over the Wearable Data Layer.
 *
 * This is the watch -> phone half only. For now it just logs what arrives; once the AI server's
 * analyzeMotion() request/response format is agreed with the AI teammate, the HTTP call to the AI
 * server goes here.
 */
class WearDataRelayService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            if (event.dataItem.uri.path != PATH_MOTION) continue

            val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
            val accelX = dataMap.getFloat("accelX")
            val accelY = dataMap.getFloat("accelY")
            val accelZ = dataMap.getFloat("accelZ")
            val gyroX = dataMap.getFloat("gyroX")
            val gyroY = dataMap.getFloat("gyroY")
            val gyroZ = dataMap.getFloat("gyroZ")
            val timestamp = dataMap.getLong("timestamp")

            Log.d(
                TAG,
                "accel=(%.2f, %.2f, %.2f) gyro=(%.2f, %.2f, %.2f) timestamp=$timestamp"
                    .format(
                        accelX,
                        accelY,
                        accelZ,
                        gyroX,
                        gyroY,
                        gyroZ,
                    ),
            )
        }
    }
}
