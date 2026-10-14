package com.example.falldetectionsystem.repository

import com.example.falldetectionsystem.model.SensorPayload
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class CloudRepository {
    private val database = FirebaseDatabase.getInstance().reference.child("sensor_data")

    suspend fun sendData(payload: SensorPayload): Boolean {
        return try {
            val newRef = database.push()
            newRef.setValue(payload).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}