package com.example.safepath_test1.location

import org.junit.Assert.assertEquals
import org.junit.Test

class SafetyRepositoryTest {
    @Test
    fun routeBounds_keepNearbyFacilitiesAndRejectDistantOnes() {
        val route = listOf(
            SafetyFacility(latitude = 37.5000, longitude = 127.0000),
            SafetyFacility(latitude = 37.5010, longitude = 127.0010),
        )
        val nearby = SafetyFacility(latitude = 37.5005, longitude = 127.0005)
        val distant = SafetyFacility(latitude = 35.8572, longitude = 128.5712)

        val candidates = SafetyRepository.facilitiesInsideRouteBounds(
            facilities = listOf(nearby, distant),
            route = route,
            paddingMeters = 50.0,
        )

        assertEquals(listOf(nearby), candidates)
    }

    @Test
    fun emptyRoute_hasNoCandidates() {
        val candidates = SafetyRepository.facilitiesInsideRouteBounds(
            facilities = listOf(SafetyFacility(37.5, 127.0)),
            route = emptyList(),
            paddingMeters = 50.0,
        )

        assertEquals(emptyList<SafetyFacility>(), candidates)
    }
}
