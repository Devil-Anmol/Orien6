package com.example.orien6

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SensorDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSensorData(sensorData: SensorData)

    @Query("DELETE FROM sensor_data")
    suspend fun deleteAllSensorData()

    @Query("SELECT * FROM sensor_data")
    suspend fun getAllSensorData(): List<SensorData>
}
