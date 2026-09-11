package com.example.safepath_test1

import org.junit.Assert.assertEquals
import org.junit.Test
import com.example.safepath_test1.location.LocationShareFormatter
import com.example.safepath_test1.model.GeoPoint

class ExampleUnitTest {
    @Test
    fun locationShareText_containsCoordinatesAndMapLink() {
        val location = GeoPoint(latitude = 37.5665, longitude = 126.9780)

        assertEquals(
            "현재 위치입니다. https://www.google.com/maps/search/?api=1&query=37.5665,126.978",
            LocationShareFormatter.format(location, isEmergency = false),
        )
    }

    @Test
    fun emergencyShareText_marksTheMessageAsSos() {
        val location = GeoPoint(latitude = 37.5665, longitude = 126.9780)

        assertEquals(
            "긴급: 도움이 필요합니다. https://www.google.com/maps/search/?api=1&query=37.5665,126.978",
            LocationShareFormatter.format(location, isEmergency = true),
        )
    }
}
