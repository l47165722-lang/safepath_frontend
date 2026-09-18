package com.example.safepath_test1.ui.map

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.safepath_test1.location.SafetyRepository
import com.example.safepath_test1.model.GeoPoint
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val SeoulFallback = Point.fromLngLat(126.9780, 37.5665)
private const val mapTag = "SafePathMapboxView"

@Composable
fun SafePathMapboxView(
    currentLocation: GeoPoint?,
    hasLocationPermission: Boolean,
    recenterToken: Int,
    showSafetyFacilities: Boolean = true,
    destinationPoint: Point? = null,
    routeLineGeoJson: String? = null,
    routeLineColor: String = "#2563EB",
    onMapClick: ((Point) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val latestOnMapClick by rememberUpdatedState(onMapClick)
    val centerPoint = currentLocation?.let {
        Point.fromLngLat(it.longitude, it.latitude)
    } ?: SeoulFallback

    var hasCentered by remember { mutableStateOf(false) }
    var lastRecenterToken by remember { mutableStateOf(recenterToken) }

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(centerPoint)
            zoom(15.5)
            pitch(0.0)
        }
    }

    LaunchedEffect(currentLocation, recenterToken) {
        val location = currentLocation ?: return@LaunchedEffect
        val point = Point.fromLngLat(location.longitude, location.latitude)
        val shouldRecenter = !hasCentered || recenterToken != lastRecenterToken
        if (!shouldRecenter) return@LaunchedEffect

        mapViewportState.setCameraOptions {
            center(point)
            zoom(15.5)
            pitch(0.0)
            bearing(0.0)
        }
        hasCentered = true
        lastRecenterToken = recenterToken
    }

    MapboxMap(
        modifier = modifier.fillMaxSize(),
        mapViewportState = mapViewportState,
        compass = {},
        scaleBar = {},
        logo = {},
        attribution = {},
    ) {
        if (hasLocationPermission) {
            MapEffect(Unit) { mapView ->
                mapView.location.updateSettings {
                    locationPuck = createDefault2DPuck(withBearing = true)
                    puckBearingEnabled = true
                    puckBearing = PuckBearing.HEADING
                    enabled = true
                }
            }
        }

        // Register exactly once for this MapView. The latest callback is read
        // through rememberUpdatedState, so recomposition does not accumulate
        // gesture listeners while still observing the current UI state.
        MapEffect(Unit) { mapView ->
            mapView.gestures.addOnMapClickListener { point ->
                latestOnMapClick?.invoke(point) != null
            }
        }

        // 1. Safety Facilities Layer (Bottom)
        MapEffect(showSafetyFacilities) { mapView ->
            val context = mapView.context

            mapView.mapboxMap.getStyle { style ->
                CoroutineScope(Dispatchers.Main).launch {
                    if (showSafetyFacilities) {
                        val cctvGeoJson = SafetyRepository.getCctvGeoJson(context)
                        val lightGeoJson = SafetyRepository.getStreetlightGeoJson(context)

                        // CCTV Source & Layer
                        if (style.getSource("cctv-source") == null) {
                            style.addSource(geoJsonSource("cctv-source") {
                                featureCollection(cctvGeoJson)
                                cluster(true)
                                clusterRadius(55)
                                clusterMaxZoom(13)
                            })
                        }
                        if (style.getLayer("cctv-layer") == null) {
                            style.addLayer(circleLayer("cctv-layer", "cctv-source") {
                                filter(Expression.not(Expression.has("point_count")))
                                minZoom(14.0)
                                circleColor("#2563EB")
                                circleRadius(4.0)
                                circleStrokeWidth(1.0)
                                circleStrokeColor("#FFFFFF")
                            })
                        }
                        if (style.getLayer("cctv-cluster-layer") == null) {
                            style.addLayer(circleLayer("cctv-cluster-layer", "cctv-source") {
                                filter(Expression.has("point_count"))
                                maxZoom(14.0)
                                circleColor("#2563EB")
                                circleRadius(16.0)
                                circleOpacity(0.88)
                                circleStrokeWidth(2.0)
                                circleStrokeColor("#FFFFFF")
                            })
                        }
                        if (style.getLayer("cctv-cluster-count-layer") == null) {
                            style.addLayer(symbolLayer("cctv-cluster-count-layer", "cctv-source") {
                                filter(Expression.has("point_count"))
                                maxZoom(14.0)
                                textField(Expression.get("point_count_abbreviated"))
                                textSize(11.0)
                                textColor("#FFFFFF")
                                textAllowOverlap(true)
                            })
                        }

                        // Streetlight Source & Layer
                        if (style.getSource("light-source") == null) {
                            style.addSource(geoJsonSource("light-source") {
                                featureCollection(lightGeoJson)
                                cluster(true)
                                clusterRadius(60)
                                clusterMaxZoom(13)
                            })
                        }
                        if (style.getLayer("light-layer") == null) {
                            style.addLayer(circleLayer("light-layer", "light-source") {
                                filter(Expression.not(Expression.has("point_count")))
                                minZoom(14.0)
                                circleColor("#F59E0B")
                                circleRadius(2.5)
                                circleOpacity(0.8)
                            })
                        }
                        if (style.getLayer("light-cluster-layer") == null) {
                            style.addLayer(circleLayer("light-cluster-layer", "light-source") {
                                filter(Expression.has("point_count"))
                                maxZoom(14.0)
                                circleColor("#F59E0B")
                                circleRadius(14.0)
                                circleOpacity(0.82)
                                circleStrokeWidth(2.0)
                                circleStrokeColor("#FFFFFF")
                            })
                        }
                        if (style.getLayer("light-cluster-count-layer") == null) {
                            style.addLayer(symbolLayer("light-cluster-count-layer", "light-source") {
                                filter(Expression.has("point_count"))
                                maxZoom(14.0)
                                textField(Expression.get("point_count_abbreviated"))
                                textSize(10.0)
                                textColor("#FFFFFF")
                                textAllowOverlap(true)
                            })
                        }
                    } else {
                        listOf(
                            "cctv-cluster-count-layer",
                            "cctv-cluster-layer",
                            "cctv-layer",
                            "light-cluster-count-layer",
                            "light-cluster-layer",
                            "light-layer",
                        ).forEach { layerId ->
                            if (style.getLayer(layerId) != null) style.removeStyleLayer(layerId)
                        }
                        if (style.getSource("cctv-source") != null) style.removeStyleSource("cctv-source")
                        if (style.getSource("light-source") != null) style.removeStyleSource("light-source")
                    }
                }
            }
        }

        // 2. Route Line Layer (Middle - Drawn ABOVE Safety Facilities)
        MapEffect(routeLineGeoJson, routeLineColor) { mapView ->
            mapView.mapboxMap.getStyle { style ->
                val geoJson = routeLineGeoJson ?: run {
                    if (style.getLayer("route-layer") != null) style.removeStyleLayer("route-layer")
                    if (style.getLayer("route-casing-layer") != null) style.removeStyleLayer("route-casing-layer")
                    if (style.getSource("route-source") != null) style.removeStyleSource("route-source")
                    return@getStyle
                }

                try {
                    val lineGeometry = LineString.fromJson(geoJson)
                    val routeFeature = FeatureCollection.fromFeature(
                        Feature.fromGeometry(lineGeometry)
                    )

                    if (style.getSource("route-source") == null) {
                        style.addSource(geoJsonSource("route-source") {
                            featureCollection(routeFeature)
                        })
                    } else {
                        style.getSourceAs<GeoJsonSource>("route-source")?.featureCollection(routeFeature)
                    }

                    if (style.getLayer("route-layer") != null) style.removeStyleLayer("route-layer")
                    if (style.getLayer("route-casing-layer") != null) style.removeStyleLayer("route-casing-layer")
                    style.addLayer(lineLayer("route-casing-layer", "route-source") {
                        lineColor("#FFFFFF")
                        lineWidth(11.5)
                        lineOpacity(0.92)
                        lineCap(LineCap.ROUND)
                        lineJoin(LineJoin.ROUND)
                    })
                    style.addLayer(lineLayer("route-layer", "route-source") {
                        lineColor(routeLineColor)
                        lineWidth(7.0)
                        lineCap(LineCap.ROUND)
                        lineJoin(LineJoin.ROUND)
                    })
                } catch (exception: Exception) {
                    Log.e(mapTag, "Failed to render route GeoJSON", exception)
                }
            }
        }

        // 3. Destination Pin Layer (Top)
        MapEffect(destinationPoint) { mapView ->
            mapView.mapboxMap.getStyle { style ->
                val point = destinationPoint ?: run {
                    if (style.getLayer("dest-layer") != null) style.removeStyleLayer("dest-layer")
                    if (style.getSource("dest-source") != null) style.removeStyleSource("dest-source")
                    return@getStyle
                }

                val destFeature = FeatureCollection.fromFeature(
                    Feature.fromGeometry(Point.fromLngLat(point.longitude(), point.latitude()))
                )

                if (style.getSource("dest-source") == null) {
                    style.addSource(geoJsonSource("dest-source") {
                        featureCollection(destFeature)
                    })
                } else {
                    style.getSourceAs<GeoJsonSource>("dest-source")?.featureCollection(destFeature)
                }

                if (style.getLayer("dest-layer") != null) {
                    style.removeStyleLayer("dest-layer")
                }
                style.addLayer(circleLayer("dest-layer", "dest-source") {
                    circleColor("#EF4444")
                    circleRadius(10.0)
                    circleStrokeWidth(3.0)
                    circleStrokeColor("#FFFFFF")
                })
            }
        }
    }
}
