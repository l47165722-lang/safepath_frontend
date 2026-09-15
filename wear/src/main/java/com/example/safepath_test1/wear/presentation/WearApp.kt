package com.example.safepath_test1.wear.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.util.Log
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

private const val TAG = "WearApp"
private const val STATUS_PATH = "safepath_status"
private const val DATABASE_URL =
    "https://safepath-test-82f0e-default-rtdb.asia-southeast1.firebasedatabase.app/"

private sealed class RouteStatus {
    data object Idle : RouteStatus()
    data object Searching : RouteStatus()
    data class Found(val routeLabel: String, val distanceKm: String) : RouteStatus()
}

/**
 * Minimal Wear OS demo screen: listens for status updates written by the
 * phone app's HomeScreen (see WearMessenger on the phone side) to Firebase
 * Realtime Database, and shows whether SafePath is currently searching for
 * a route on the watch face.
 *
 * Uses Firebase instead of the Wearable Data Layer API (MessageClient) so
 * this works over plain internet, without needing a working Bluetooth/ADB
 * pairing between the phone and watch.
 */
@Composable
fun WearApp() {
    var status by remember { mutableStateOf<RouteStatus>(RouteStatus.Idle) }

    DisposableEffect(Unit) {
        Log.i(TAG, "WearApp composed — registering Firebase listener")
        val ref = FirebaseDatabase.getInstance(DATABASE_URL).getReference(STATUS_PATH)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val payload = snapshot.getValue(String::class.java) ?: return
                Log.i(TAG, "Payload=\"$payload\"")
                status = when {
                    payload == "SEARCHING" -> RouteStatus.Searching
                    payload == "IDLE" -> RouteStatus.Idle
                    payload.startsWith("FOUND|") -> {
                        val parts = payload.split("|")
                        RouteStatus.Found(
                            routeLabel = parts.getOrNull(1) ?: "안전경로",
                            distanceKm = parts.getOrNull(2) ?: "-",
                        )
                    }
                    else -> status
                }
                Log.i(TAG, "New status=$status")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Firebase listener cancelled", error.toException())
            }
        }
        ref.addValueEventListener(listener)
        onDispose {
            Log.i(TAG, "WearApp disposed — removing Firebase listener")
            ref.removeEventListener(listener)
        }
    }

    MaterialTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                when (val current = status) {
                    RouteStatus.Idle -> {
                        Text(text = "SafePath", textAlign = TextAlign.Center)
                        Text(text = "대기 중", textAlign = TextAlign.Center)
                    }
                    RouteStatus.Searching -> {
                        Text(text = "🔍 안전경로", textAlign = TextAlign.Center)
                        Text(text = "탐색 중...", textAlign = TextAlign.Center)
                    }
                    is RouteStatus.Found -> {
                        Text(text = "✅ ${current.routeLabel}", textAlign = TextAlign.Center)
                        Text(text = "${current.distanceKm} km", textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
