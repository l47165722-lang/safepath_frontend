package com.example.safepath_test1.location

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class RouteResult(val geoJsonLineString: String, val distanceMeters: Double, val safetyFacilityScore: Double = 0.0)
data class MultiRouteResult(val safeRoute: RouteResult?, val shortestRoute: RouteResult?, val recommendedRoute: RouteResult?)

object NavigationRepository {
    private const val tag = "NavigationRepository"

    suspend fun fetchMultiRoutes(context: Context, accessToken: String, originLat: Double, originLng: Double, destLat: Double, destLng: Double): MultiRouteResult = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) {
            Log.e(tag, "Mapbox access token is blank")
            return@withContext MultiRouteResult(null, null, null)
        }
        val candidates = fetchWalkingRoutes(accessToken, originLat, originLng, destLat, destLng)
        val shortestRoute = candidates.minByOrNull { it.distanceMeters } ?: return@withContext MultiRouteResult(null, null, null)
        val scoredCandidates = try {
            candidates.map {
                it.copy(
                    safetyFacilityScore = SafetyRepository.scoreRouteFacilities(
                        context = context,
                        routeGeoJson = it.geoJsonLineString,
                        routeDistanceMeters = it.distanceMeters,
                    ),
                )
            }
        } catch (exception: Exception) {
            Log.e(tag, "Failed to score walking routes with safety facilities; using shortest route", exception)
            return@withContext MultiRouteResult(shortestRoute, shortestRoute, shortestRoute)
        }
        val shortestDistance = shortestRoute.distanceMeters.coerceAtLeast(1.0)
        fun detourCost(route: RouteResult, weight: Double) = ((route.distanceMeters / shortestDistance) - 1.0).coerceAtLeast(0.0) * weight
        val hasSafetyCoverage = scoredCandidates.any { it.safetyFacilityScore > 0.0 }
        val safeRoute = if (hasSafetyCoverage) {
            scoredCandidates.maxByOrNull { it.safetyFacilityScore - detourCost(it, 40.0) } ?: shortestRoute
        } else shortestRoute
        val recommendedRoute = if (hasSafetyCoverage) {
            scoredCandidates.maxByOrNull { it.safetyFacilityScore - detourCost(it, 65.0) } ?: shortestRoute
        } else shortestRoute
        MultiRouteResult(safeRoute, shortestRoute, recommendedRoute)
    }

    private fun fetchWalkingRoutes(accessToken: String, originLat: Double, originLng: Double, destLat: Double, destLng: Double): List<RouteResult> {
        var connection: HttpURLConnection? = null
        return try {
            val url = "https://api.mapbox.com/directions/v5/mapbox/walking/$originLng,$originLat;$destLng,$destLat?alternatives=true&geometries=geojson&overview=full&access_token=$accessToken"
            connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(tag, "Mapbox walking route request failed: HTTP ${connection.responseCode}")
                emptyList()
            } else {
                val routes = JSONObject(connection.inputStream.bufferedReader().use { it.readText() }).optJSONArray("routes")
                buildList {
                    if (routes != null) for (index in 0 until routes.length()) {
                        val route = routes.optJSONObject(index) ?: continue
                        val geometry = route.optJSONObject("geometry")?.toString().orEmpty()
                        val distance = route.optDouble("distance", Double.NaN)
                        if (geometry.isNotBlank() && distance.isFinite() && distance >= 0.0) add(RouteResult(geometry, distance))
                    }
                }
            }
        } catch (exception: Exception) {
            Log.e(tag, "Mapbox walking route network or response parsing failed", exception)
            emptyList()
        } finally {
            connection?.disconnect()
        }
    }
}
