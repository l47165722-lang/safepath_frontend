package com.example.safepath_test1.ui.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safepath_test1.model.GeoPoint
import com.example.safepath_test1.model.PlaceSelection
import com.example.safepath_test1.ui.map.SafePathMapboxView
import com.example.safepath_test1.ui.theme.DestRed
import com.example.safepath_test1.ui.theme.FieldBg
import com.example.safepath_test1.ui.theme.SafeBlue
import com.example.safepath_test1.ui.theme.TextMain
import com.example.safepath_test1.ui.theme.TextMuted

private enum class RouteType(val title: String, val icon: ImageVector) {
    Safe("안전", Icons.Default.Lock),
    Shortest("최단", Icons.Default.Info),
    Recommended("추천", Icons.Default.Star),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    currentLocation: GeoPoint?,
    hasLocationPermission: Boolean,
    origin: PlaceSelection,
    destination: PlaceSelection,
    onOriginChanged: (PlaceSelection) -> Unit,
    onDestinationChanged: (PlaceSelection) -> Unit,
    onSwap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var routeType by rememberSaveable { mutableStateOf(RouteType.Safe.name) }
    var recenterToken by remember { mutableIntStateOf(0) }
    var showSafetyFacilities by rememberSaveable { mutableStateOf(true) }
    var activeTab by rememberSaveable { mutableStateOf("destination") }
    var multiRouteResult by remember { mutableStateOf<com.example.safepath_test1.location.MultiRouteResult?>(null) }
    val destinationPoint = if (destination.hasCoordinates()) com.mapbox.geojson.Point.fromLngLat(destination.longitude!!, destination.latitude!!) else null

    LaunchedEffect(origin, destination) {
        if (!origin.hasCoordinates() || !destination.hasCoordinates()) {
            multiRouteResult = null
            return@LaunchedEffect
        }
        val token = context.getString(com.example.safepath_test1.R.string.mapbox_access_token)
        val result = com.example.safepath_test1.location.NavigationRepository.fetchMultiRoutes(
            context = context,
            accessToken = token,
            originLat = origin.latitude!!,
            originLng = origin.longitude!!,
            destLat = destination.latitude!!,
            destLng = destination.longitude!!,
        )
        multiRouteResult = result
    }

    val activeRoute = when (routeType) {
        RouteType.Safe.name -> multiRouteResult?.safeRoute
        RouteType.Shortest.name -> multiRouteResult?.shortestRoute
        else -> multiRouteResult?.recommendedRoute
    }

    val activeRouteColor = when (routeType) {
        RouteType.Safe.name -> "#22C55E" // Safe Green
        RouteType.Shortest.name -> "#F59E0B" // Shortest Amber
        else -> "#2563EB" // Recommended Blue
    }

    Box(modifier = modifier.fillMaxSize()) {
        SafePathMapboxView(
            currentLocation = currentLocation,
            hasLocationPermission = hasLocationPermission,
            recenterToken = recenterToken,
            showSafetyFacilities = showSafetyFacilities,
            destinationPoint = destinationPoint,
            routeLineGeoJson = activeRoute?.geoJsonLineString,
            routeLineColor = activeRouteColor,
            onMapClick = { point ->
                val latStr = String.format(java.util.Locale.US, "%.4f", point.latitude())
                val lngStr = String.format(java.util.Locale.US, "%.4f", point.longitude())
                val selectedPlace = PlaceSelection("선택한 장소 ($latStr, $lngStr)", point.latitude(), point.longitude())

                if (activeTab == "origin") {
                    onOriginChanged(selectedPlace)
                    activeTab = "destination"
                    Toast.makeText(context, "출발지가 설정되었습니다. 이제 도착지를 지정해 주세요.", Toast.LENGTH_SHORT).show()
                } else {
                    onDestinationChanged(selectedPlace)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        RouteSearchCard(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 2.dp),
            origin = origin,
            destination = destination,
            activeTab = activeTab,
            onActiveTabChanged = { activeTab = it },
            onOriginChanged = {
                onOriginChanged(it)
                if (it.hasCoordinates() && !destination.hasCoordinates()) {
                    activeTab = "destination"
                }
            },
            onDestinationChanged = onDestinationChanged,
            onSwap = onSwap,
            onUseCurrentLocation = {
                currentLocation?.let {
                    onOriginChanged(PlaceSelection("내 위치", it.latitude, it.longitude))
                    activeTab = "destination"
                    Toast.makeText(context, "출발지가 설정되었습니다. 이제 도착지를 지정해 주세요.", Toast.LENGTH_SHORT).show()
                }
            },
            selectedRouteType = RouteType.valueOf(routeType),
            onRouteTypeSelected = { routeType = it.name },
        )

        MapSideControls(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            locationEnabled = hasLocationPermission,
            safetyFacilitiesEnabled = showSafetyFacilities,
            onRecenter = { recenterToken++ },
            onToggleSafetyFacilities = {
                showSafetyFacilities = !showSafetyFacilities
            },
        )
    }
}

@Composable
private fun RouteSearchCard(
    modifier: Modifier = Modifier,
    origin: PlaceSelection,
    destination: PlaceSelection,
    activeTab: String,
    onActiveTabChanged: (String) -> Unit,
    onOriginChanged: (PlaceSelection) -> Unit,
    onDestinationChanged: (PlaceSelection) -> Unit,
    onSwap: () -> Unit,
    onUseCurrentLocation: () -> Unit,
    selectedRouteType: RouteType,
    onRouteTypeSelected: (RouteType) -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.98f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SafeBlue),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("S", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "SafePath",
                    color = TextMain,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RouteFieldRow(
                    leading = {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(SafeBlue),
                        )
                    },
                    value = origin.name,
                    placeholder = "내 위치",
                    isSelected = activeTab == "origin",
                    onSelect = { onActiveTabChanged("origin") },
                    onValueChange = { onOriginChanged(PlaceSelection(name = it)) },
                    trailing = {
                        Surface(
                            modifier = Modifier
                                .size(26.dp)
                                .clickable {
                                    onActiveTabChanged("origin")
                                    onUseCurrentLocation()
                                },
                            shape = CircleShape,
                            color = SafeBlue.copy(alpha = 0.12f),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = "내 위치 설정",
                                    tint = SafeBlue,
                                    modifier = Modifier.size(15.dp),
                                )
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                )

                Surface(
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .size(36.dp)
                        .clickable(onClick = onSwap),
                    shape = CircleShape,
                    color = FieldBg,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "스왑",
                            tint = SafeBlue,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                RouteFieldRow(
                    leading = {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "도착지",
                            tint = DestRed,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    value = destination.name,
                    placeholder = "도착지",
                    isSelected = activeTab == "destination",
                    onSelect = { onActiveTabChanged("destination") },
                    onValueChange = { onDestinationChanged(PlaceSelection(name = it)) },
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(FieldBg)
                    .padding(3.dp),
            ) {
                RouteType.entries.forEach { type ->
                    RouteTypeChip(
                        type = type,
                        selected = type == selectedRouteType,
                        onClick = { onRouteTypeSelected(type) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun RouteFieldRow(
    leading: @Composable () -> Unit,
    value: String,
    placeholder: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onValueChange: (String) -> Unit,
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (isSelected) Color.White else FieldBg
    val borderColor = if (isSelected) SafeBlue else Color.Transparent

    Surface(
        modifier = modifier
            .shadow(if (isSelected) 2.dp else 0.dp, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSelect,
            ),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSelect,
                ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    leading()
                }
                Spacer(Modifier.width(6.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isBlank()) {
                        Text(
                            text = placeholder,
                            color = if (isSelected) TextMain else TextMuted,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = {
                            onSelect()
                            onValueChange(it)
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = TextMain,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        cursorBrush = SolidColor(Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    onSelect()
                                }
                            },
                    )
                }
                if (trailing != null) {
                    Spacer(Modifier.width(6.dp))
                    trailing()
                }
            }
        }
    }
}

@Composable
private fun RouteTypeChip(
    type: RouteType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = if (selected) SafeBlue else Color.Transparent
    val content = if (selected) Color.White else TextMuted
    Row(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = type.icon,
            contentDescription = type.title,
            tint = content,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = type.title,
            color = content,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun MapSideControls(
    modifier: Modifier,
    locationEnabled: Boolean,
    safetyFacilitiesEnabled: Boolean,
    onRecenter: () -> Unit,
    onToggleSafetyFacilities: () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        RoundMapButton(
            icon = Icons.Default.LocationOn,
            enabled = locationEnabled,
            selected = false,
            onClick = onRecenter,
        )
        RoundMapButton(
            icon = Icons.Default.Lock,
            enabled = true,
            selected = safetyFacilitiesEnabled,
            onClick = onToggleSafetyFacilities,
        )
    }
}

@Composable
private fun RoundMapButton(
    icon: ImageVector,
    enabled: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(42.dp)
            .shadow(4.dp, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        shape = CircleShape,
        color = if (selected) SafeBlue else Color.White,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = when {
                    selected -> Color.White
                    enabled -> SafeBlue
                    else -> Color.Gray
                },
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
