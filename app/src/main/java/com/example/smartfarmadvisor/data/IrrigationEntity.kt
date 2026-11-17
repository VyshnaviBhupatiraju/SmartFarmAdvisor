package com.example.smartfarmadvisor.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "irrigation_history")
data class IrrigationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val farmId: Int,
    val farmName: String,
    val date: String,
    val durationMinutes: Int,
    val timestamp: Long = System.currentTimeMillis()
)
