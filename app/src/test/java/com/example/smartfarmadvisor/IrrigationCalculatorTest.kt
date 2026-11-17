package com.example.smartfarmadvisor

import com.example.smartfarmadvisor.domain.IrrigationCalculator
import com.example.smartfarmadvisor.domain.RecommendationStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class IrrigationCalculatorTest {

    @Test
    fun test1_irrigate_when_deficit_above_3_and_soil_below_0_2() {
        // ET0 = 5.0, Rainfall = 1.0 -> Deficit = 4.0 (> 3.0), Soil moisture = 0.15 (< 0.2)
        val recommendation = IrrigationCalculator.calculateRecommendation(
            et0 = 5.0,
            rainfall = 1.0,
            soilMoisture = 0.15
        )
        assertEquals(RecommendationStatus.IRRIGATE, recommendation.status)
    }

    @Test
    fun test2_not_needed_when_soil_moisture_above_0_2() {
        // ET0 = 5.0, Rainfall = 1.0 -> Deficit = 4.0 (> 3.0), Soil moisture = 0.25 (>= 0.2)
        val recommendation = IrrigationCalculator.calculateRecommendation(
            et0 = 5.0,
            rainfall = 1.0,
            soilMoisture = 0.25
        )
        assertEquals(RecommendationStatus.NOT_NEEDED, recommendation.status)
    }

    @Test
    fun test3_not_needed_when_deficit_equals_3() {
        // ET0 = 5.0, Rainfall = 2.0 -> Deficit = 3.0 (NOT > 3.0), Soil moisture = 0.15
        val recommendation = IrrigationCalculator.calculateRecommendation(
            et0 = 5.0,
            rainfall = 2.0,
            soilMoisture = 0.15
        )
        assertEquals(RecommendationStatus.NOT_NEEDED, recommendation.status)
    }

    @Test
    fun test4_not_needed_when_deficit_equals_3_case2() {
        // ET0 = 4.0, Rainfall = 1.0 -> Deficit = 3.0 (NOT > 3.0), Soil moisture = 0.15
        val recommendation = IrrigationCalculator.calculateRecommendation(
            et0 = 4.0,
            rainfall = 1.0,
            soilMoisture = 0.15
        )
        assertEquals(RecommendationStatus.NOT_NEEDED, recommendation.status)
    }

    @Test
    fun test5_not_needed_when_soil_moisture_equals_0_2() {
        // ET0 = 5.0, Rainfall = 0.0 -> Deficit = 5.0 (> 3.0), Soil moisture = 0.20 (NOT < 0.2)
        val recommendation = IrrigationCalculator.calculateRecommendation(
            et0 = 5.0,
            rainfall = 0.0,
            soilMoisture = 0.20
        )
        assertEquals(RecommendationStatus.NOT_NEEDED, recommendation.status)
    }

    @Test
    fun test6_insufficient_data_when_soil_moisture_missing() {
        // Missing soil moisture
        val recommendation = IrrigationCalculator.calculateRecommendation(
            et0 = 5.0,
            rainfall = 1.0,
            soilMoisture = null
        )
        assertEquals(RecommendationStatus.INSUFFICIENT_DATA, recommendation.status)
    }

    @Test
    fun test7_insufficient_data_when_et0_or_rainfall_missing() {
        // Missing ET0 or rainfall
        val recommendation1 = IrrigationCalculator.calculateRecommendation(
            et0 = null,
            rainfall = 1.0,
            soilMoisture = 0.15
        )
        assertEquals(RecommendationStatus.INSUFFICIENT_DATA, recommendation1.status)

        val recommendation2 = IrrigationCalculator.calculateRecommendation(
            et0 = 5.0,
            rainfall = null,
            soilMoisture = 0.15
        )
        assertEquals(RecommendationStatus.INSUFFICIENT_DATA, recommendation2.status)
    }
}
