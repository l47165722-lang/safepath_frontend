package com.example.safepath_test1

import android.Manifest
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.safepath_test1.model.GeoPoint
import com.example.safepath_test1.model.PlaceSelection
import com.example.safepath_test1.ui.SafePathTab
import com.example.safepath_test1.ui.components.SafePathBottomBar
import com.example.safepath_test1.ui.guardian.GuardianScreen
import com.example.safepath_test1.ui.home.HomeScreen
import com.example.safepath_test1.ui.profile.ProfileScreen
import com.example.safepath_test1.ui.safetymap.SafetyMapScreen
import com.example.safepath_test1.ui.theme.AppBackground
import com.example.safepath_test1.ui.theme.SafePathTheme

@Composable
fun SafePathApp() {
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableStateOf(SafePathTab.Home.name) }
    var currentLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var origin by rememberSaveable(stateSaver = placeSelectionSaver) { mutableStateOf(PlaceSelection()) }
    var destination by rememberSaveable(stateSaver = placeSelectionSaver) { mutableStateOf(PlaceSelection()) }
    var hasLocationPermission by remember {
        mutableStateOf(hasLocationPermission(context))
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        hasLocationPermission = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        // Pre-load safety facilities in background thread (IO) so map displays them instantly on open
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            com.example.safepath_test1.location.SafetyRepository.getCctvGeoJson(context)
            com.example.safepath_test1.location.SafetyRepository.getStreetlightGeoJson(context)
        }

        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    DisposableEffect(hasLocationPermission) {
        if (!hasLocationPermission) {
            onDispose { }
        } else {
            val locationManager = context.getSystemService(LocationManager::class.java)
            val listener = LocationListener { location ->
                currentLocation = GeoPoint(location.latitude, location.longitude)
            }
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)

            try {
                providers
                    .mapNotNull { provider -> locationManager.getLastKnownLocation(provider) }
                    .maxByOrNull { location -> location.time }
                    ?.let { location ->
                        currentLocation = GeoPoint(location.latitude, location.longitude)
                    }
                providers.forEach { provider ->
                    if (locationManager.isProviderEnabled(provider)) {
                        locationManager.requestLocationUpdates(provider, 1_000L, 1f, listener)
                    }
                }
            } catch (exception: SecurityException) {
                Log.e("SafePathApp", "Location permission was revoked while requesting updates", exception)
                currentLocation = null
            }

            onDispose { locationManager.removeUpdates(listener) }
        }
    }

    val tab = SafePathTab.entries.find { it.name == selectedTab } ?: SafePathTab.Home

    SafePathTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground),
        ) {
            when (tab) {
                SafePathTab.Home -> HomeScreen(
                    currentLocation = currentLocation,
                    hasLocationPermission = hasLocationPermission,
                    origin = origin,
                    destination = destination,
                    onOriginChanged = { origin = it },
                    onDestinationChanged = { destination = it },
                    onSwap = {
                        val previousOrigin = origin
                        origin = destination
                        destination = previousOrigin
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                SafePathTab.SafetyMap -> SafetyMapScreen(
                    currentLocation = currentLocation,
                    modifier = Modifier.padding(bottom = 100.dp),
                )
                SafePathTab.Guardian -> GuardianScreen(
                    currentLocation = currentLocation,
                    modifier = Modifier.padding(bottom = 100.dp),
                )
                SafePathTab.Profile -> ProfileScreen(
                    hasLocationPermission = hasLocationPermission,
                    modifier = Modifier.padding(bottom = 100.dp),
                )
            }

            SafePathBottomBar(
                selectedTab = tab,
                onTabSelected = { selectedTab = it.name },
                onSosClick = {
                    android.widget.Toast.makeText(context, "🚨 SOS 호출! (준비 중)", android.widget.Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

private val placeSelectionSaver = mapSaver(
    save = { place -> mapOf("name" to place.name, "latitude" to place.latitude, "longitude" to place.longitude) },
    restore = { values -> PlaceSelection(values["name"] as String, values["latitude"] as Double?, values["longitude"] as Double?) },
)

private fun hasLocationPermission(context: android.content.Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
}
