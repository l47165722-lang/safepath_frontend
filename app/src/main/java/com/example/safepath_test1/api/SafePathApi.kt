package com.example.safepath_test1.api

import com.example.safepath_test1.model.SafetyFacility
import retrofit2.http.GET

interface SafePathApi {
    @GET("facilities")
    suspend fun getFacilities(): List<SafetyFacility>
}