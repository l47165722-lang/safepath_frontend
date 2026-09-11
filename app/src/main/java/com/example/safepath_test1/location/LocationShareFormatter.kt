package com.example.safepath_test1.location

import com.example.safepath_test1.model.GeoPoint

object LocationShareFormatter {
    fun format(location: GeoPoint, isEmergency: Boolean): String {
        val prefix = if (isEmergency) {
            "긴급: 도움이 필요합니다. "
        } else {
            "현재 위치입니다. "
        }
        return prefix +
            "https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}"
    }
}
