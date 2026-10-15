package com.example.falldetectionsystem.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.falldetectionsystem.model.DecisionEngine
import com.example.falldetectionsystem.model.SensorPayload
import com.example.falldetectionsystem.model.SensorRecord
import com.example.falldetectionsystem.repository.CloudRepository
import com.example.falldetectionsystem.repository.SensorDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs

class FallDetectionViewModel(
    private val sensorDao: SensorDao,
    private val repository: CloudRepository = CloudRepository(),
    private val decisionEngine: DecisionEngine = DecisionEngine()
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

    private val _remoteLedStatus = MutableStateFlow(false)
    val remoteLedStatus: StateFlow<Boolean> = _remoteLedStatus

    private val _manualFlashlight = MutableStateFlow(false)
    val manualFlashlight: StateFlow<Boolean> = _manualFlashlight

    private val _isFlashlightActive = MutableStateFlow(false)
    val isFlashlightActive: StateFlow<Boolean> = _isFlashlightActive

    val historyRecords: StateFlow<List<SensorRecord>> = sensorDao.getRecentRecords()
        .map { list -> list.reversed() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var hasSavedInitialPoint = false
    private var lastSavedAcceleration = 0f
    private var lastSavedFallingState = false
    private var lastSaveTimeMs = 0L

    init {
        startCloudSync()
        listenToCloudCommands()
    }

    fun updateSensorData(newX: Float, newY: Float, newZ: Float) {
        _x.value = newX
        _y.value = newY
        _z.value = newZ

        val currentAcc = decisionEngine.calculateAcceleration(newX, newY, newZ).toFloat()
        val falling = decisionEngine.shouldActivateActuator(newX, newY, newZ)
        _isFalling.value = falling
        updateActuatorState()

        saveToDatabaseIfNeeded(currentAcc, falling)
    }

    private fun saveToDatabaseIfNeeded(currentAcc: Float, isFallingNow: Boolean) {
        val now = System.currentTimeMillis()
        val deltaAcc = abs(currentAcc - lastSavedAcceleration)
        val stateChanged = isFallingNow != lastSavedFallingState

        val shouldSave = !hasSavedInitialPoint ||
                stateChanged ||
                (deltaAcc >= 0.5f && (now - lastSaveTimeMs) >= 250L)

        if (shouldSave) {
            hasSavedInitialPoint = true
            lastSavedAcceleration = currentAcc
            lastSavedFallingState = isFallingNow
            lastSaveTimeMs = now

            viewModelScope.launch(Dispatchers.IO) {
                sensorDao.insertRecord(
                    SensorRecord(
                        timestamp = now,
                        sensorValue = currentAcc,
                        actionTriggered = isFallingNow
                    )
                )
            }
        }
    }

    private fun listenToCloudCommands() {
        repository.observeRemoteCommand { status ->
            _remoteLedStatus.value = status
            updateActuatorState()
        }
    }

    fun toggleManualFlashlight(enabled: Boolean) {
        _manualFlashlight.value = enabled
        updateActuatorState()
    }

    private fun updateActuatorState() {
        _isFlashlightActive.value = _isFalling.value || _remoteLedStatus.value || _manualFlashlight.value
    }

    private fun startCloudSync() {
        viewModelScope.launch {
            while (true) {
                delay(5000)
                _syncStatus.value = "Відправка..."
                val payload = SensorPayload("Accelerometer", _x.value, _y.value, _z.value)
                val success = repository.sendData(payload)
                _isSyncSuccess.value = success
                _syncStatus.value = if (success) "Успішно" else "Помилка мережі"
            }
        }
    }
}