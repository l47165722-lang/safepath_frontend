package com.example.safepath_test1.model

data class SafetyFacility (
    val id: Int,
    val type: String,
    val name: String?,
    val latitude: Double,
    val longitude: Double,
    val address: String?
)