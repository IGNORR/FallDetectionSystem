package com.example.falldetectionsystem.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sensor_records")
data class SensorRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val sensorValue: Float,
    val actionTriggered: Boolean
)