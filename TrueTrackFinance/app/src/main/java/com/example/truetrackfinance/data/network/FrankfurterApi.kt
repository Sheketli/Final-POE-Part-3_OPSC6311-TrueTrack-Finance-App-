package com.example.truetrackfinance.data.network

import com.squareup.moshi.Json
import retrofit2.http.GET
import retrofit2.http.Query

data class FrankfurterResponse(
    @Json(name = "amount") val amount: Double,
    @Json(name = "base") val base: String,
    @Json(name = "date") val date: String,
    @Json(name = "rates") val rates: Map<String, Double>
)

interface FrankfurterApi {
    @GET("latest")
    suspend fun getLatestRates(
        @Query("base") base: String = "EUR"
    ): FrankfurterResponse
}
