package com.example.safepath_test1.wear.presentation

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.delay

private const val TAG = "WearApp"
private const val PATH_STATUS = "/safepath/status"

/** If no update has arrived from the phone in this long, treat it as stale/disconnected. */
private const val STALE_AFTER_MS = 10 * 60 * 1000L

private sealed class RouteStatus {
    data object Idle : RouteStatus()
    data object Searching : RouteStatus()
    data class Found(val routeType: String, val distanceKm: String, val destinationName: String?) : RouteStatus()
    data object Stale : RouteStatus()
}

/**
 * Experimental counterpart to WearApp.kt: syncs status over the Wearable
 * Data Layer's DataClient instead of Firebase. Uses DataClient (not
 * MessageClient) — see WearMessenger.kt on the phone side for why.
 * No pairing code screen here - Bluetooth pairing between this watch and
 * a phone already scopes who can push data to it.
 */
@Composable
fun WearApp() {
    val context = LocalContext.current
    var status by remember { mutableStateOf<RouteStatus>(RouteStatus.Idle) }
    var lastReceivedAt by remember { mutableStateOf(0L) }

    fun applyDataMap(dataMap: com.google.android.gms.wearable.DataMap) {
        lastReceivedAt = System.currentTimeMillis()
        status = when (dataMap.getString("state")) {
            "SEARCHING" -> RouteStatus.Searching
            "FOUND" -> RouteStatus.Found(
                routeType = dataMap.getString("routeType") ?: "경로",
                distanceKm = "%.1f".format(dataMap.getDouble("distanceMeters") / 1000.0),
                destinationName = dataMap.getString("destinationName"),
            )
            else -> RouteStatus.Idle
        }
        Log.i(TAG, "New status=$status")
    }

    // Pick up whatever state is already there (registering the listener below
    // only delivers *future* changes, not the item as it stood before we
    // started listening).
    DisposableEffect(Unit) {
        Wearable.getDataClient(context).dataItems
            .addOnSuccessListener { dataItems ->
                for (i in 0 until dataItems.count) {
                    val item = dataItems[i]
                    if (item.uri.path == PATH_STATUS) {
                        applyDataMap(DataMapItem.fromDataItem(item).dataMap)
                    }
                }
                dataItems.release()
            }
            .addOnFailureListener { e -> Log.e(TAG, "Failed to read existing DataItems", e) }
        onDispose { }
    }

    DisposableEffect(Unit) {
        val dataClient = Wearable.getDataClient(context)
        val listener = DataClient.OnDataChangedListener { dataEvents ->
            for (event in dataEvents) {
                Log.i(TAG, "DataEvent type=${event.type} path=${event.dataItem.uri.path}")
                if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == PATH_STATUS) {
                    applyDataMap(DataMapItem.fromDataItem(event.dataItem).dataMap)
                }
            }
            dataEvents.release()
        }
        dataClient.addListener(listener)
        Log.i(TAG, "Registered DataClient listener")
        onDispose {
            dataClient.removeListener(listener)
            Log.i(TAG, "Removed DataClient listener")
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            if (lastReceivedAt > 0 &&
                System.currentTimeMillis() - lastReceivedAt > STALE_AFTER_MS &&
                status !is RouteStatus.Stale
            ) {
                status = RouteStatus.Stale
            }
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
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text(text = "탐색 중...", textAlign = TextAlign.Center)
                    }
                    is RouteStatus.Found -> {
                        Text(text = "✅ ${current.routeType}", textAlign = TextAlign.Center)
                        Text(text = "${current.distanceKm} km", textAlign = TextAlign.Center)
                        current.destinationName?.let {
                            Text(text = it, textAlign = TextAlign.Center, style = MaterialTheme.typography.caption2)
                        }
                    }
                    RouteStatus.Stale -> {
                        Text(text = "⏱️ 연결 끊김", textAlign = TextAlign.Center)
                        Text(text = "폰과 블루투스 연결을 확인하세요", textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
