package com.example.safepath_test1.wear

import android.content.Context
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import java.util.UUID

/**
 * Sends route-status updates from the phone app to the paired watch, using
 * Firebase Realtime Database as a relay.
 *
 * This replaces the Google Play Services Wearable Data Layer (MessageClient),
 * which needs a working Bluetooth/ADB pairing between the phone and watch.
 * Firebase only needs both devices to have internet access.
 *
 * Every write is scoped under /pairs/{PairingCode}/status (see [PairingCode])
 * so that two different installs of the app don't see each other's status,
 * and carries an `updatedAt` timestamp + `requestId` so the watch can detect
 * and discard stale or out-of-order writes (see WearApp.kt on the wear side).
 */
object WearMessenger {
    private const val TAG = "WearMessenger"
    private const val DATABASE_URL =
        "https://safepath-test-82f0e-default-rtdb.asia-southeast1.firebasedatabase.app/"

    private fun statusRef(context: Context) =
        FirebaseDatabase.getInstance(DATABASE_URL)
            .getReference("pairs")
            .child(PairingCode.getOrCreate(context))
            .child("status")

    private fun send(
        context: Context,
        state: String,
        routeType: String? = null,
        distanceMeters: Double? = null,
        destinationName: String? = null,
    ) {
        val payload = mapOf(
            "state" to state,
            "routeType" to routeType,
            "distanceMeters" to distanceMeters,
            "destinationName" to destinationName,
            "updatedAt" to System.currentTimeMillis(),
            "requestId" to UUID.randomUUID().toString(),
        )
        Log.i(TAG, "send() state=$state routeType=$routeType destination=$destinationName")
        statusRef(context).setValue(payload)
            .addOnSuccessListener { Log.i(TAG, "Wrote state=$state to Firebase") }
            .addOnFailureListener { e -> Log.e(TAG, "Failed to write state=$state", e) }
    }

    fun sendIdle(context: Context) {
        send(context, "IDLE")
    }

    fun sendRouteSearching(context: Context) {
        send(context, "SEARCHING")
    }

    /**
     * @param routeType display label for the route the user currently has selected
     *   (e.g. "안전", "최단", "추천" — see RouteType.title in HomeScreen.kt), not
     *   hardcoded to the safe route.
     */
    fun sendRouteFound(
        context: Context,
        routeType: String,
        destinationName: String?,
        distanceMeters: Double,
    ) {
        send(context, "FOUND", routeType = routeType, distanceMeters = distanceMeters, destinationName = destinationName)
    }
}
