package com.example.falldetectionsystem.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.falldetectionsystem.model.SensorRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: SensorRecord)

    @Query("SELECT * FROM sensor_records ORDER BY timestamp DESC LIMIT 50")
    fun getRecentRecords(): Flow<List<SensorRecord>>
}