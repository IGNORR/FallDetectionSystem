package com.example.falldetectionsystem

import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class CloudRepository {
    private val database = FirebaseDatabase.getInstance().reference.child("sensor_data")

    suspend fun sendData(payload: SensorPayload): Boolean {
        return try {
            val newRef = database.push() // Генерує унікальний ID для запису
            newRef.setValue(payload).await() // Асинхронна відправка даних
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}