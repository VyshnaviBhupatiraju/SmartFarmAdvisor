package com.example.smartfarmadvisor.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FarmDao {

    @Insert
    suspend fun insertFarm(farm: FarmEntity): Long

    @Query("SELECT * FROM farms ORDER BY id DESC")
    fun getAllFarms(): Flow<List<FarmEntity>>

    @Query("SELECT * FROM farms WHERE id = :farmId")
    fun getFarmById(farmId: Int): Flow<FarmEntity?>

    @Query("DELETE FROM farms WHERE id = :farmId")
    suspend fun deleteFarm(farmId: Int)
}
