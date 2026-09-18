package com.example.safepath_test1.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class NavigationRepositoryTest {
    @Test
    fun threeCandidates_selectSafeBalancedAndShortestRoutes() {
        val shortest = RouteResult("shortest", distanceMeters = 100.0, safetyFacilityScore = 0.0)
        val balanced = RouteResult("balanced", distanceMeters = 110.0, safetyFacilityScore = 50.0)
        val safest = RouteResult("safest", distanceMeters = 130.0, safetyFacilityScore = 100.0)

        val result = NavigationRepository.selectRoutes(listOf(shortest, balanced, safest))

        assertEquals(safest, result.safeRoute)
        assertEquals(shortest, result.shortestRoute)
        assertEquals(balanced, result.recommendedRoute)
        assertEquals(3, result.candidateRouteCount)
        assertEquals(null, result.noticeMessage)
    }

    @Test
    fun oneCandidate_reportsWhyAllOptionsAreIdentical() {
        val onlyRoute = RouteResult("only", distanceMeters = 100.0, safetyFacilityScore = 10.0)

        val result = NavigationRepository.selectRoutes(listOf(onlyRoute))

        assertEquals(onlyRoute, result.safeRoute)
        assertEquals(onlyRoute, result.shortestRoute)
        assertEquals(onlyRoute, result.recommendedRoute)
        assertEquals(1, result.candidateRouteCount)
        assertNotNull(result.noticeMessage)
    }

    @Test
    fun missingSafetyCoverage_usesShortestAndExplainsFallback() {
        val shortest = RouteResult("shortest", distanceMeters = 100.0, safetyFacilityScore = 0.0)
        val alternative = RouteResult("alternative", distanceMeters = 120.0, safetyFacilityScore = 0.0)

        val result = NavigationRepository.selectRoutes(listOf(alternative, shortest))

        assertEquals(shortest, result.safeRoute)
        assertEquals(shortest, result.shortestRoute)
        assertEquals(shortest, result.recommendedRoute)
        assertNotNull(result.noticeMessage)
    }
}
