
package com.example.smartfarmadvisor.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "farms")
data class FarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val farmName: String,
    val cropName: String,
    val latitude: Double,
    val longitude: Double,
    val createdAt: Long = System.currentTimeMillis()
)
