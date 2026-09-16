package com.example.safepath_test1.wear.presentation

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.delay

private const val TAG = "WearApp"
private const val DATABASE_URL =
    "https://safepath-test-82f0e-default-rtdb.asia-southeast1.firebasedatabase.app/"

/** If the phone hasn't written a fresher status in this long, treat it as stale/offline. */
private const val STALE_AFTER_MS = 10 * 60 * 1000L

private sealed class RouteStatus {
    data object Connecting : RouteStatus()
    data object Idle : RouteStatus()
    data object Searching : RouteStatus()
    data class Found(val routeType: String, val distanceKm: String, val destinationName: String?) : RouteStatus()
    data object Stale : RouteStatus()
    data object Error : RouteStatus()
}

/**
 * Wear OS demo screen. Two states:
 *  - No pairing code saved yet -> [PairingEntryScreen], a small on-screen keypad
 *    for the 4-digit code shown in the phone app's Settings screen.
 *  - Code saved -> [StatusScreen], listening on Firebase for that pairing's
 *    route status (see PairingCode.kt on the phone / WearMessenger.kt).
 *
 * Scoping updates by pairing code keeps two different installs of the app
 * from displaying each other's route status.
 */
@Composable
fun WearApp() {
    val context = LocalContext.current
    var pairingCode by remember { mutableStateOf(WearPairing.getCode(context)) }

    val code = pairingCode
    if (code == null) {
        PairingEntryScreen(
            onCodeEntered = {
                WearPairing.setCode(context, it)
                pairingCode = it
            },
        )
    } else {
        StatusScreen(
            pairingCode = code,
            onChangeCode = {
                WearPairing.clear(context)
                pairingCode = null
            },
        )
    }
}

@Composable
private fun StatusScreen(pairingCode: String, onChangeCode: () -> Unit) {
    var status by remember { mutableStateOf<RouteStatus>(RouteStatus.Connecting) }
    var lastUpdatedAt by remember { mutableStateOf(0L) }

    DisposableEffect(pairingCode) {
        Log.i(TAG, "Registering Firebase listener for pairing code=$pairingCode")
        val database = FirebaseDatabase.getInstance(DATABASE_URL)
        val ref = database.getReference("pairs").child(pairingCode).child("status")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    status = RouteStatus.Idle
                    return
                }
                val state = snapshot.child("state").getValue(String::class.java)
                val updatedAt = (snapshot.child("updatedAt").value as? Number)?.toLong() ?: 0L
                lastUpdatedAt = updatedAt

                if (updatedAt > 0 && System.currentTimeMillis() - updatedAt > STALE_AFTER_MS) {
                    status = RouteStatus.Stale
                    return
                }

                status = when (state) {
                    "SEARCHING" -> RouteStatus.Searching
                    "FOUND" -> {
                        val distanceMeters = (snapshot.child("distanceMeters").value as? Number)?.toDouble() ?: 0.0
                        RouteStatus.Found(
                            routeType = snapshot.child("routeType").getValue(String::class.java) ?: "경로",
                            distanceKm = "%.1f".format(distanceMeters / 1000.0),
                            destinationName = snapshot.child("destinationName").getValue(String::class.java),
                        )
                    }
                    else -> RouteStatus.Idle
                }
                Log.i(TAG, "New status=$status")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Firebase listener cancelled", error.toException())
                status = RouteStatus.Error
            }
        }
        ref.addValueEventListener(listener)
        onDispose {
            Log.i(TAG, "Removing Firebase listener")
            ref.removeEventListener(listener)
        }
    }

    // Also re-check staleness on a timer, in case the phone just stops sending
    // updates entirely (no new Firebase event to trigger the check above).
    LaunchedEffect(pairingCode) {
        while (true) {
            delay(30_000)
            if (lastUpdatedAt > 0 &&
                System.currentTimeMillis() - lastUpdatedAt > STALE_AFTER_MS &&
                status !is RouteStatus.Stale
            ) {
                status = RouteStatus.Stale
            }
        }
    }

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onChangeCode),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                when (val current = status) {
                    RouteStatus.Connecting -> {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text(text = "연결 중...", textAlign = TextAlign.Center)
                    }
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
                        Text(text = "폰 앱을 확인하세요", textAlign = TextAlign.Center)
                    }
                    RouteStatus.Error -> {
                        Text(text = "⚠️ 오류", textAlign = TextAlign.Center)
                        Text(text = "잠시 후 다시 시도", textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun PairingEntryScreen(onCodeEntered: (String) -> Unit) {
    var digits by remember { mutableStateOf("") }

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = "폰 앱 설정에 표시된", textAlign = TextAlign.Center, style = MaterialTheme.typography.caption2)
            Text(text = "코드 입력", textAlign = TextAlign.Center, style = MaterialTheme.typography.caption2)
            Text(
                text = digits.padEnd(4, '_').chunked(1).joinToString(" "),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.title2,
            )
            Keypad(
                onDigit = { d -> if (digits.length < 4) digits += d },
                onBackspace = { if (digits.isNotEmpty()) digits = digits.dropLast(1) },
                onConfirm = { if (digits.length == 4) onCodeEntered(digits) },
                confirmEnabled = digits.length == 4,
            )
        }
    }
}

@Composable
private fun Keypad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean,
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                row.forEach { d -> KeypadButton(label = d, onClick = { onDigit(d) }) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            KeypadButton(label = "⌫", onClick = onBackspace)
            KeypadButton(label = "0", onClick = { onDigit("0") })
            KeypadButton(label = "✓", onClick = onConfirm, enabled = confirmEnabled)
        }
    }
}

@Composable
private fun KeypadButton(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.size(36.dp)) {
        Text(text = label)
    }
}
