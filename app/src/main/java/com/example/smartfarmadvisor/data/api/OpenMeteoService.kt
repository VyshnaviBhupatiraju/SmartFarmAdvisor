package com.example.smartfarmadvisor.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoService {

    @GET("v1/forecast")
    suspend fun getWeatherForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("daily") daily: String = "et0_fao_evapotranspiration,precipitation_sum,temperature_2m_max",
        @Query("hourly") hourly: String = "soil_moisture_0_to_1cm",
        @Query("timezone") timezone: String = "auto"
    ): OpenMeteoResponse
}
