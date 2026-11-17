package com.example.smartfarmadvisor.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IrrigationDao {

    @Insert
    suspend fun insertIrrigation(irrigation: IrrigationEntity): Long

    @Query("SELECT * FROM irrigation_history WHERE farmId = :farmId ORDER BY timestamp DESC")
    fun getIrrigationHistoryForFarm(farmId: Int): Flow<List<IrrigationEntity>>

    @Query("SELECT * FROM irrigation_history ORDER BY timestamp DESC")
    fun getAllIrrigationHistory(): Flow<List<IrrigationEntity>>

    @Query("DELETE FROM irrigation_history WHERE id = :irrigationId")
    suspend fun deleteIrrigation(irrigationId: Int)
}
