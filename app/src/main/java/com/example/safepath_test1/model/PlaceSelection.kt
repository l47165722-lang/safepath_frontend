package com.example.safepath_test1.model

data class PlaceSelection(
    val name: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
) {
    fun hasCoordinates(): Boolean = latitude != null && longitude != null
}
