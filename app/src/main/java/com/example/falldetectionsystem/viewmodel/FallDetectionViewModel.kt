package com.example.falldetectionsystem.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.falldetectionsystem.model.SensorPayload
import com.example.falldetectionsystem.repository.CloudRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class FallDetectionViewModel(
    private val repository: CloudRepository = CloudRepository()
) : ViewModel() {

    private val _x = MutableStateFlow(0f)
    val x: StateFlow<Float> = _x

    private val _y = MutableStateFlow(0f)
    val y: StateFlow<Float> = _y

    private val _z = MutableStateFlow(0f)
    val z: StateFlow<Float> = _z

    private val _isFalling = MutableStateFlow(false)
    val isFalling: StateFlow<Boolean> = _isFalling

    private val _syncStatus = MutableStateFlow("Очікування...")
    val syncStatus: StateFlow<String> = _syncStatus

    private val _isSyncSuccess = MutableStateFlow(false)
    val isSyncSuccess: StateFlow<Boolean> = _isSyncSuccess

    init {
        startCloudSync()
    }

    fun updateSensorData(newX: Float, newY: Float, newZ: Float) {
        _x.value = newX
        _y.value = newY
        _z.value = newZ

        val acceleration = sqrt((newX * newX + newY * newY + newZ * newZ).toDouble())
        _isFalling.value = acceleration !in 3.0..20.0
    }

    private fun startCloudSync() {
        viewModelScope.launch {
            while (true) {
                delay(5000)
                _syncStatus.value = "Відправка..."
                val payload = SensorPayload(
                    sensorType = "Accelerometer",
                    x = _x.value,
                    y = _y.value,
                    z = _z.value
                )
                val success = repository.sendData(payload)
                _isSyncSuccess.value = success
                _syncStatus.value = if (success) "Успішно" else "Помилка мережі"
            }
        }
    }
}