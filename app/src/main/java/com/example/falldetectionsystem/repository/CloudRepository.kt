package com.example.falldetectionsystem.repository

import com.example.falldetectionsystem.model.SensorPayload
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.tasks.await

class CloudRepository {
    private val rootRef = FirebaseDatabase.getInstance().reference
    private val sensorRef = rootRef.child("sensor_data")
    private val controlRef = rootRef.child("control").child("led_status")

    suspend fun sendData(payload: SensorPayload): Boolean {
        return try {
            val newRef = sensorRef.push()
            newRef.setValue(payload).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Слухач команд із хмари (Л.Р. №4)
    fun observeRemoteCommand(onCommandReceived: (Boolean) -> Unit): () -> Unit {
        // Створимо початкове значення false, якщо гілки ще немає в консолі
        controlRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                controlRef.setValue(false)
            }
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.getValue(Boolean::class.java) ?: false
                onCommandReceived(status)
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        controlRef.addValueEventListener(listener)
        return { controlRef.removeEventListener(listener) }
    }

    fun setRemoteCommand(status: Boolean) {
        controlRef.setValue(status)
    }
}
