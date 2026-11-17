package com.example.smartfarmadvisor.data.api

import com.google.gson.annotations.SerializedName

data class OpenMeteoResponse(
    val latitude: Double?,
    val longitude: Double?,
    val timezone: String?,
    val daily: DailyUnits?,
    val hourly: HourlyUnits?
)

data class DailyUnits(
    val time: List<String>?,
    @SerializedName("et0_fao_evapotranspiration")
    val et0FaoEvapotranspiration: List<Double?>?,
    @SerializedName("precipitation_sum")
    val precipitationSum: List<Double?>?,
    @SerializedName("temperature_2m_max")
    val temperature2mMax: List<Double?>?
)

data class HourlyUnits(
    val time: List<String>?,
    @SerializedName("soil_moisture_0_to_1cm")
    val soilMoisture0To1cm: List<Double?>?
)
