package com.example.safepath_test1.wear

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable

/**
 * Sends simple status updates from the phone app to any paired Wear OS watch
 * using the Wearable Data Layer API's MessageClient.
 *
 * Protocol (kept intentionally simple for the demo):
 *  - "IDLE"                     -> no active route search
 *  - "SEARCHING"                -> a route search just started
 *  - "FOUND|<label>|<km>"       -> a route was found (label e.g. "안전경로", km e.g. "2.1")
 */
object WearMessenger {
    private const val TAG = "WearMessenger"
    const val PATH_STATUS = "/safepath/status"

    private fun sendStatus(context: Context, message: String) {
        val messageClient = Wearable.getMessageClient(context)
        val nodeClient = Wearable.getNodeClient(context)
        nodeClient.connectedNodes
            .addOnSuccessListener { nodes ->
                if (nodes.isEmpty()) {
                    Log.d(TAG, "No connected Wear OS node found; skipping status send")
                }
                val payload = message.toByteArray(Charsets.UTF_8)
                nodes.forEach { node ->
                    messageClient.sendMessage(node.id, PATH_STATUS, payload)
                        .addOnFailureListener { exception ->
                            Log.e(TAG, "Failed to send status to ${node.displayName}", exception)
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to fetch connected Wear OS nodes", exception)
            }
    }

    fun sendIdle(context: Context) {
        sendStatus(context, "IDLE")
    }

    fun sendRouteSearching(context: Context) {
        sendStatus(context, "SEARCHING")
    }

    fun sendRouteFound(context: Context, routeLabel: String, distanceMeters: Double) {
        val km = distanceMeters / 1000.0
        sendStatus(context, "FOUND|$routeLabel|${"%.1f".format(km)}")
    }
}
