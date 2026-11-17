package com.example.smartfarmadvisor.domain

import java.util.Locale

enum class RecommendationStatus {
    IRRIGATE,
    NOT_NEEDED,
    INSUFFICIENT_DATA
}

data class IrrigationRecommendation(
    val status: RecommendationStatus,
    val et0: Double?,
    val rainfall: Double?,
    val waterDeficit: Double?,
    val soilMoisture: Double?,
    val explanation: String
)

object IrrigationCalculator {

    fun calculateRecommendation(
        et0: Double?,
        rainfall: Double?,
        soilMoisture: Double?
    ): IrrigationRecommendation {
        if (et0 == null || rainfall == null || soilMoisture == null) {
            return IrrigationRecommendation(
                status = RecommendationStatus.INSUFFICIENT_DATA,
                et0 = et0,
                rainfall = rainfall,
                waterDeficit = if (et0 != null && rainfall != null) et0 - rainfall else null,
                soilMoisture = soilMoisture,
                explanation = "INSUFFICIENT WEATHER DATA: Required parameters (ET0, rainfall, or soil moisture) are missing for calculation."
            )
        }

        val waterDeficit = et0 - rainfall
        val shouldIrrigate = waterDeficit > 3.0 && soilMoisture < 0.2

        val et0Str = String.format(Locale.US, "%.2f", et0)
        val rainStr = String.format(Locale.US, "%.2f", rainfall)
        val deficitStr = String.format(Locale.US, "%.2f", waterDeficit)
        val soilStr = String.format(Locale.US, "%.2f", soilMoisture)

        return if (shouldIrrigate) {
            IrrigationRecommendation(
                status = RecommendationStatus.IRRIGATE,
                et0 = et0,
                rainfall = rainfall,
                waterDeficit = waterDeficit,
                soilMoisture = soilMoisture,
                explanation = "IRRIGATE: Water deficit ($deficitStr mm = ET0 $et0Str mm - Rain $rainStr mm) > 3.0 mm AND Soil Moisture ($soilStr m³/m³) < 0.2 m³/m³."
            )
        } else {
            val reason = when {
                waterDeficit <= 3.0 && soilMoisture >= 0.2 ->
                    "Water deficit ($deficitStr mm) <= 3.0 mm AND Soil Moisture ($soilStr m³/m³) >= 0.2 m³/m³."
                waterDeficit <= 3.0 ->
                    "Water deficit ($deficitStr mm) <= 3.0 mm."
                else ->
                    "Soil Moisture ($soilStr m³/m³) >= 0.2 m³/m³."
            }
            IrrigationRecommendation(
                status = RecommendationStatus.NOT_NEEDED,
                et0 = et0,
                rainfall = rainfall,
                waterDeficit = waterDeficit,
                soilMoisture = soilMoisture,
                explanation = "NOT NEEDED: $reason (ET0: $et0Str mm, Rain: $rainStr mm, Deficit: $deficitStr mm, Soil: $soilStr m³/m³)"
            )
        }
    }
}
