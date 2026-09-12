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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable

private const val PATH_STATUS = "/safepath/status"

private sealed class RouteStatus {
    data object Idle : RouteStatus()
    data object Searching : RouteStatus()
    data class Found(val routeLabel: String, val distanceKm: String) : RouteStatus()
}

/**
 * Minimal Wear OS demo screen: listens for status messages sent from the
 * phone app's HomeScreen (see WearMessenger on the phone side) and shows
 * whether SafePath is currently searching for a route on the watch face.
 */
@Composable
fun WearApp() {
    val context = LocalContext.current
    var status by remember { mutableStateOf<RouteStatus>(RouteStatus.Idle) }

    DisposableEffect(Unit) {
        val messageClient = Wearable.getMessageClient(context)
        val listener = MessageClient.OnMessageReceivedListener { event: MessageEvent ->
            if (event.path == PATH_STATUS) {
                val payload = String(event.data, Charsets.UTF_8)
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
            }
        }
        messageClient.addListener(listener)
        onDispose { messageClient.removeListener(listener) }
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
