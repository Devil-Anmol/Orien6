package com.example.orien6

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sensor_data")
data class SensorData(
    @PrimaryKey val timestamp: Long,
    val x: Float,
    val y: Float,
    val z: Float
)
