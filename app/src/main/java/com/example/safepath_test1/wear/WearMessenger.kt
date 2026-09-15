package com.example.safepath_test1.wear

import android.content.Context
import android.util.Log
import com.google.firebase.database.FirebaseDatabase

/**
 * Sends simple status updates from the phone app to any watch running the
 * SafePath wear app, using Firebase Realtime Database as a relay.
 *
 * This replaces the Google Play Services Wearable Data Layer (MessageClient),
 * which requires a real Bluetooth/ADB pairing between the phone and watch.
 * Firebase only needs both devices to have internet access, so it works
 * regardless of pairing/transport issues between the phone and watch.
 *
 * Protocol (kept intentionally simple for the demo):
 *  - "IDLE"                     -> no active route search
 *  - "SEARCHING"                -> a route search just started
 *  - "FOUND|<label>|<km>"       -> a route was found (label e.g. "안전경로", km e.g. "2.1")
 */
object WearMessenger {
    private const val TAG = "WearMessenger"
    private const val STATUS_PATH = "safepath_status"
    private const val DATABASE_URL =
        "https://safepath-test-82f0e-default-rtdb.asia-southeast1.firebasedatabase.app/"

    private fun sendStatus(message: String) {
        Log.i(TAG, "sendStatus() called with message=\"$message\" — writing to Firebase")
        val ref = FirebaseDatabase.getInstance(DATABASE_URL).getReference(STATUS_PATH)
        ref.setValue(message)
            .addOnSuccessListener {
                Log.i(TAG, "Wrote \"$message\" to Firebase ($STATUS_PATH)")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to write status \"$message\" to Firebase", exception)
            }
    }

    // context is kept as a parameter (unused) so call sites in HomeScreen.kt
    // don't need to change.
    fun sendIdle(context: Context) {
        sendStatus("IDLE")
    }

    fun sendRouteSearching(context: Context) {
        sendStatus("SEARCHING")
    }

    fun sendRouteFound(context: Context, routeLabel: String, distanceMeters: Double) {
        val km = distanceMeters / 1000.0
        sendStatus("FOUND|$routeLabel|${"%.1f".format(km)}")
    }
}
