package com.example.safepath_test1.location

import android.content.Context
import android.util.Log
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SafetyFacility(
    val latitude: Double,
    val longitude: Double,
    val address: String = "",
)

enum class SafetyLevel(
    val title: String,
    val hexColor: Long,
    val icon: String,
    val description: String,
) {
    BAD("나쁨", 0xFFEF4444, "🔴", "주변에 안전시설이 부족하여 주의가 필요합니다."),
    MODERATE("보통", 0xFFEAB308, "🟡", "기본적인 안전시설이 배치되어 있는 구역입니다."),
    GOOD("좋음", 0xFF84CC16, "🟢", "CCTV와 가로등이 잘 갖춰진 안전한 구역입니다."),
    VERY_GOOD("매우 좋음", 0xFF22C55E, "🛡️", "주변 밀집도가 높아 매우 안전한 안심 구역입니다.");
}

data class RadiusAnalysisResult(
    val cctvCount: Int,
    val streetlightCount: Int,
    val level: SafetyLevel,
    val totalScore: Int,
)

object SafetyRepository {
    private const val tag = "SafetyRepository"
    private var cctvFeatureCollection: FeatureCollection? = null
    private var streetlightFeatureCollection: FeatureCollection? = null
    private var cctvList: List<SafetyFacility>? = null
    private var streetlightList: List<SafetyFacility>? = null

    suspend fun getCctvGeoJson(context: Context): FeatureCollection = withContext(Dispatchers.IO) {
        cctvFeatureCollection?.let { return@withContext it }
        val facilities = loadCctv(context)
        cctvList = facilities
        val features = facilities.map { facility ->
            Feature.fromGeometry(Point.fromLngLat(facility.longitude, facility.latitude)).apply {
                addStringProperty("type", "CCTV")
                addStringProperty("address", facility.address)
            }
        }
        val collection = FeatureCollection.fromFeatures(features)
        cctvFeatureCollection = collection
        collection
    }

    suspend fun getStreetlightGeoJson(context: Context): FeatureCollection = withContext(Dispatchers.IO) {
        streetlightFeatureCollection?.let { return@withContext it }
        val facilities = loadStreetlights(context)
        streetlightList = facilities
        val features = facilities.map { facility ->
            Feature.fromGeometry(Point.fromLngLat(facility.longitude, facility.latitude)).apply {
                addStringProperty("type", "STREETLIGHT")
            }
        }
        val collection = FeatureCollection.fromFeatures(features)
        streetlightFeatureCollection = collection
        collection
    }

    suspend fun analyzeRadius(
        context: Context,
        centerLat: Double,
        centerLng: Double,
        radiusMeters: Double,
    ): RadiusAnalysisResult = withContext(Dispatchers.IO) {
        val cctvs = cctvList ?: loadCctv(context).also { cctvList = it }
        val lights = streetlightList ?: loadStreetlights(context).also { streetlightList = it }

        var cctvInRadius = 0
        for (f in cctvs) {
            if (distanceInMeters(centerLat, centerLng, f.latitude, f.longitude) <= radiusMeters) {
                cctvInRadius++
            }
        }

        var lightsInRadius = 0
        for (f in lights) {
            if (distanceInMeters(centerLat, centerLng, f.latitude, f.longitude) <= radiusMeters) {
                lightsInRadius++
            }
        }

        val areaFactor = (radiusMeters / 300.0).let { it * it }.coerceAtLeast(0.1)
        val weightedScore = ((cctvInRadius * 4 + lightsInRadius * 0.5) / areaFactor).toInt()

        val level = when {
            weightedScore >= 35 -> SafetyLevel.VERY_GOOD
            weightedScore >= 18 -> SafetyLevel.GOOD
            weightedScore >= 6 -> SafetyLevel.MODERATE
            else -> SafetyLevel.BAD
        }

        RadiusAnalysisResult(
            cctvCount = cctvInRadius,
            streetlightCount = lightsInRadius,
            level = level,
            totalScore = weightedScore.coerceIn(0, 100),
        )
    }

    suspend fun scoreRouteFacilities(
        context: Context,
        routeGeoJson: String,
        routeDistanceMeters: Double,
    ): Double = withContext(Dispatchers.IO) {
        val routePoints = try {
            val coordinates = org.json.JSONObject(routeGeoJson).optJSONArray("coordinates") ?: return@withContext 0.0
            buildList<SafetyFacility> {
                for (index in 0 until coordinates.length()) {
                    val coordinate = coordinates.optJSONArray(index) ?: continue
                    val longitude = coordinate.optDouble(0, Double.NaN)
                    val latitude = coordinate.optDouble(1, Double.NaN)
                    if (latitude.isFinite() && longitude.isFinite()) add(SafetyFacility(latitude, longitude))
                }
            }
        } catch (exception: Exception) {
            Log.e(tag, "Failed to parse route geometry for facility scoring", exception)
            return@withContext 0.0
        }
        if (routePoints.size < 2) return@withContext 0.0
        val cctvs = cctvList ?: loadCctv(context).also { cctvList = it }
        val lights = streetlightList ?: loadStreetlights(context).also { streetlightList = it }
        if (cctvs.isEmpty() && lights.isEmpty()) return@withContext 0.0
        val weightedFacilityCount = cctvs.count { isNearRoute(it, routePoints, 50.0) } * 4.0 +
            lights.count { isNearRoute(it, routePoints, 40.0) }
        weightedFacilityCount * 1_000.0 / routeDistanceMeters.coerceAtLeast(100.0)
    }

    private fun isNearRoute(facility: SafetyFacility, route: List<SafetyFacility>, thresholdMeters: Double): Boolean =
        route.zipWithNext().any { (start, end) -> distanceToSegmentMeters(facility, start, end) <= thresholdMeters }

    private fun distanceToSegmentMeters(point: SafetyFacility, start: SafetyFacility, end: SafetyFacility): Double {
        val latitudeScale = 111_320.0
        val longitudeScale = latitudeScale * Math.cos(Math.toRadians(point.latitude))
        val px = point.longitude * longitudeScale
        val py = point.latitude * latitudeScale
        val sx = start.longitude * longitudeScale
        val sy = start.latitude * latitudeScale
        val ex = end.longitude * longitudeScale
        val ey = end.latitude * latitudeScale
        val dx = ex - sx
        val dy = ey - sy
        val lengthSquared = dx * dx + dy * dy
        val ratio = if (lengthSquared == 0.0) 0.0 else (((px - sx) * dx + (py - sy) * dy) / lengthSquared).coerceIn(0.0, 1.0)
        return Math.hypot(px - (sx + ratio * dx), py - (sy + ratio * dy))
    }

    private fun distanceInMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    private fun loadCctv(context: Context): List<SafetyFacility> {
        val list = mutableListOf<SafetyFacility>()
        try {
            context.assets.open("cctv.csv").bufferedReader().useLines { lines ->
                lines.drop(1).forEach { line ->
                    val cols = line.split(",")
                    if (cols.size >= 2) {
                        val lat = cols[0].trim().toDoubleOrNull()
                        val lng = cols[1].trim().toDoubleOrNull()
                        val addr = if (cols.size > 3) cols[3].trim() else ""
                        if (lat != null && lng != null && lat in 30.0..40.0 && lng in 120.0..135.0) {
                            list.add(
                                SafetyFacility(
                                    latitude = lat,
                                    longitude = lng,
                                    address = addr,
                                )
                            )
                        }
                    }
                }
            }
        } catch (exception: Exception) {
            Log.e(tag, "Failed to load or parse cctv.csv", exception)
        }
        return list
    }

    private fun loadStreetlights(context: Context): List<SafetyFacility> {
        val list = mutableListOf<SafetyFacility>()
        try {
            context.assets.open("streetlight.csv").bufferedReader().useLines { lines ->
                lines.drop(1).forEachIndexed { index, line ->
                    val cols = line.split(",")
                    if (cols.size >= 3) {
                        val lat = cols[1].trim().toDoubleOrNull()
                        val lng = cols[2].trim().toDoubleOrNull()
                        if (lat != null && lng != null && lat in 30.0..40.0 && lng in 120.0..135.0) {
                            list.add(
                                SafetyFacility(
                                    latitude = lat,
                                    longitude = lng,
                                )
                            )
                        }
                    }
                }
            }
        } catch (exception: Exception) {
            Log.e(tag, "Failed to load or parse streetlight.csv", exception)
        }
        return list
    }
}
