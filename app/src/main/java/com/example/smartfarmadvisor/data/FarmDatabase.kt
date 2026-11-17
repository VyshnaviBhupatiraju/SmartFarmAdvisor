package com.example.smartfarmadvisor.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FarmEntity::class, IrrigationEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FarmDatabase : RoomDatabase() {

    abstract fun farmDao(): FarmDao
    abstract fun irrigationDao(): IrrigationDao

    companion object {
        @Volatile
        private var INSTANCE: FarmDatabase? = null

        fun getDatabase(context: Context): FarmDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FarmDatabase::class.java,
                    "smart_farm_database"
                )
                .fallbackToDestructiveMigration(true)
                .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
