package com.example.safepath_test1.location

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.cos

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
    private var cctvList: List<SafetyFacility>? = null
    private var streetlightList: List<SafetyFacility>? = null
    private val cctvCacheLock = Mutex()
    private val streetlightCacheLock = Mutex()

    suspend fun getCctvFacilities(context: Context): List<SafetyFacility> =
        cctvCacheLock.withLock { cctvList ?: loadCctv(context).also { cctvList = it } }

    suspend fun getStreetlightFacilities(context: Context): List<SafetyFacility> =
        streetlightCacheLock.withLock { streetlightList ?: loadStreetlights(context).also { streetlightList = it } }

    suspend fun analyzeRadius(
        context: Context,
        centerLat: Double,
        centerLng: Double,
        radiusMeters: Double,
    ): RadiusAnalysisResult = withContext(Dispatchers.IO) {
        val cctvs = getCctvFacilities(context)
        val lights = getStreetlightFacilities(context)

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
        val cctvs = getCctvFacilities(context)
        val lights = getStreetlightFacilities(context)
        if (cctvs.isEmpty() && lights.isEmpty()) return@withContext 0.0
        // Most facilities are nowhere near the requested route. A cheap
        // geographic bounding-box pass avoids comparing every one of the
        // ~74k streetlights with every route segment.
        val cctvCandidates = facilitiesInsideRouteBounds(cctvs, routePoints, 50.0)
        val lightCandidates = facilitiesInsideRouteBounds(lights, routePoints, 40.0)
        val weightedFacilityCount = cctvCandidates.count { isNearRoute(it, routePoints, 50.0) } * 4.0 +
            lightCandidates.count { isNearRoute(it, routePoints, 40.0) }
        weightedFacilityCount * 1_000.0 / routeDistanceMeters.coerceAtLeast(100.0)
    }

    internal fun facilitiesInsideRouteBounds(
        facilities: List<SafetyFacility>,
        route: List<SafetyFacility>,
        paddingMeters: Double,
    ): List<SafetyFacility> {
        if (route.isEmpty()) return emptyList()
        val centerLatitude = route.map { it.latitude }.average()
        val latitudePadding = paddingMeters / 111_320.0
        val longitudeScale = (111_320.0 * cos(Math.toRadians(centerLatitude))).coerceAtLeast(1.0)
        val longitudePadding = paddingMeters / longitudeScale
        val minLatitude = route.minOf { it.latitude } - latitudePadding
        val maxLatitude = route.maxOf { it.latitude } + latitudePadding
        val minLongitude = route.minOf { it.longitude } - longitudePadding
        val maxLongitude = route.maxOf { it.longitude } + longitudePadding
        return facilities.filter {
            it.latitude in minLatitude..maxLatitude && it.longitude in minLongitude..maxLongitude
        }
    }

    /** Returns only facilities near a point, avoiding creation of tens of thousands of map labels. */
    internal fun facilitiesNearPoint(
        facilities: List<SafetyFacility>,
        latitude: Double,
        longitude: Double,
        radiusMeters: Double,
        limit: Int,
    ): List<SafetyFacility> {
        if (limit <= 0 || radiusMeters <= 0.0) return emptyList()
        val latitudePadding = radiusMeters / 111_320.0
        val longitudeScale = (111_320.0 * cos(Math.toRadians(latitude))).coerceAtLeast(1.0)
        val longitudePadding = radiusMeters / longitudeScale
        return facilities.asSequence()
            .filter {
                it.latitude in (latitude - latitudePadding)..(latitude + latitudePadding) &&
                    it.longitude in (longitude - longitudePadding)..(longitude + longitudePadding)
            }
            .take(limit)
            .toList()
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

    // A minimal CSV splitter that respects double-quoted fields (so an
    // address like "서울시, 강남구" doesn't get split into two extra
    // columns and shift every value after it). Plain String.split(",")
    // breaks on rows like that.
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        for (c in line) {
            when {
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(c)
            }
        }
        result.add(current.toString())
        return result
    }

    // cctv.csv is distributed as CP949/EUC-KR (common for Korean public-data
    // exports); reading it as UTF-8 silently garbles the address column.
    private fun loadCctv(context: Context): List<SafetyFacility> {
        val list = mutableListOf<SafetyFacility>()
        try {
            context.assets.open("cctv.csv").bufferedReader(charset("EUC-KR")).useLines { lines ->
                lines.drop(1).forEach { line ->
                    val cols = parseCsvLine(line)
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
                    val cols = parseCsvLine(line)
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
