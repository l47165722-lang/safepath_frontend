package com.example.safepath_test1.wear

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

/**
 * Experimental replacement for WearMessenger.kt: syncs the same status
 * updates over the Play Services Wearable Data Layer's DataClient instead
 * of Firebase, now that the wear module's applicationId matches the
 * phone module's (see wear/build.gradle.kts).
 *
 * Uses DataClient (PutDataMapRequest/putDataItem) rather than MessageClient:
 * MessageClient's sendMessage() kept failing on real-device testing with
 * "Failed to deliver message to AppKey" from Google Play Services'
 * WearableService, while DataClient + matching applicationId is the
 * combination that was confirmed working real-device + real-device.
 *
 * DataClient dedupes identical payloads (no event fires if nothing in the
 * DataMap changed), so `updatedAt` is always included to force a fresh
 * item on every call even if state/route are unchanged.
 */
object WearMessengerNative {
    private const val TAG = "WearMessengerNative"
    const val PATH_STATUS = "/safepath/status"

    private fun send(
        context: Context,
        state: String,
        routeType: String? = null,
        distanceMeters: Double? = null,
        destinationName: String? = null,
    ) {
        val putDataMapRequest = PutDataMapRequest.create(PATH_STATUS).apply {
            dataMap.putString("state", state)
            if (routeType != null) dataMap.putString("routeType", routeType)
            if (distanceMeters != null) dataMap.putDouble("distanceMeters", distanceMeters)
            if (destinationName != null) dataMap.putString("destinationName", destinationName)
            dataMap.putLong("updatedAt", System.currentTimeMillis())
        }
        val request = putDataMapRequest.asPutDataRequest().setUrgent()

        Wearable.getDataClient(context).putDataItem(request)
            .addOnSuccessListener {
                Log.i(TAG, "DataClient synced state=$state routeType=$routeType destination=$destinationName")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "DataClient sync failed for state=$state", e)
            }
    }

    fun sendIdle(context: Context) { send(context, "IDLE") }
    fun sendRouteSearching(context: Context) { send(context, "SEARCHING") }
    fun sendRouteFound(
        context: Context,
        routeType: String,
        destinationName: String?,
        distanceMeters: Double,
    ) {
        send(context, "FOUND", routeType = routeType, distanceMeters = distanceMeters, destinationName = destinationName)
    }
}
