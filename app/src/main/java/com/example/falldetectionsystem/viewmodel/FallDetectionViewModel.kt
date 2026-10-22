package com.example.falldetectionsystem.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.falldetectionsystem.model.ActivityClassifier
import com.example.falldetectionsystem.model.AiInsight
import com.example.falldetectionsystem.model.DecisionEngine
import com.example.falldetectionsystem.model.FallMotionGate
import com.example.falldetectionsystem.model.SensorPayload
import com.example.falldetectionsystem.model.SensorRecord
import com.example.falldetectionsystem.model.SensorWindow
import com.example.falldetectionsystem.repository.CloudRepository
import com.example.falldetectionsystem.repository.SensorDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class FallDetectionViewModel(
    private val sensorDao: SensorDao,
    private val classifier: ActivityClassifier,
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

    private val _isThresholdAlarm = MutableStateFlow(false)
    val isThresholdAlarm: StateFlow<Boolean> = _isThresholdAlarm

    private val _aiInsight = MutableStateFlow(AiInsight())
    val aiInsight: StateFlow<AiInsight> = _aiInsight
    val aiWindowSize: Int = ActivityClassifier.WINDOW_SIZE

    private val sensorWindow = SensorWindow(ActivityClassifier.WINDOW_SIZE)
    private var lastSensorTimeNs = 0L
    private var samplesSinceInference = 0
    private var inferenceJob: Job? = null
    private var stopCloudCommandListener: (() -> Unit)? = null

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

    fun updateSensorData(newX: Float, newY: Float, newZ: Float, sensorTimeNs: Long) {
        _x.value = newX
        _y.value = newY
        _z.value = newZ

        val currentAcc = decisionEngine.calculateAcceleration(newX, newY, newZ).toFloat()
        _isThresholdAlarm.value = decisionEngine.shouldActivateActuator(newX, newY, newZ)
        val falling = _isThresholdAlarm.value
        _isFalling.value = falling
        updateActuatorState()

        saveToDatabaseIfNeeded(currentAcc, falling)
        acceptAiSample(newX, newY, newZ, sensorTimeNs)
    }

    private fun acceptAiSample(x: Float, y: Float, z: Float, timeNs: Long) {
        if (lastSensorTimeNs != 0L) {
            val elapsed = timeNs - lastSensorTimeNs
            if (elapsed < 18_000_000L) return
            if (elapsed > 250_000_000L) {
                sensorWindow.clear()
                samplesSinceInference = 0
                _aiInsight.value = AiInsight()
            }
        }
        lastSensorTimeNs = timeNs
        sensorWindow.add(x, y, z)
        samplesSinceInference++
        if (sensorWindow.size < ActivityClassifier.WINDOW_SIZE) {
            _aiInsight.value = AiInsight(samplesCollected = sensorWindow.size)
            return
        }
        if (samplesSinceInference < 25 || inferenceJob?.isActive == true) return
        samplesSinceInference = 0
        val window = sensorWindow.snapshot() ?: return
        if (FallMotionGate.isStationary(window)) {
            _aiInsight.value = AiInsight(
                status = "Немає руху",
                samplesCollected = ActivityClassifier.WINDOW_SIZE
            )
            return
        }
        val hasImpact = FallMotionGate.hasImpact(window)
        _aiInsight.value = _aiInsight.value.copy(status = "Аналіз руху")
        inferenceJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                val probabilities = classifier.classify(window)
                withContext(Dispatchers.Main) {
                    val predictedIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: return@withContext
                    val fallProbability = probabilities[classifier.fallClassIndex]
                    val fallCandidate = hasImpact &&
                        predictedIndex == classifier.fallClassIndex &&
                        fallProbability >= classifier.fallThreshold
                    _aiInsight.value = AiInsight(
                        status = when {
                            fallCandidate -> "Можливе падіння"
                            predictedIndex == classifier.fallClassIndex && !hasImpact ->
                                "Схоже на падіння, але удару немає"
                            predictedIndex == classifier.fallClassIndex -> "Інший рух"
                            else -> classifier.classNames[predictedIndex]
                        },
                        probabilities = probabilities.toList(),
                        samplesCollected = ActivityClassifier.WINDOW_SIZE,
                        isFallHint = fallCandidate
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                withContext(Dispatchers.Main) {
                    _aiInsight.value = AiInsight(
                        status = "Помилка моделі: ${error.message}",
                        samplesCollected = ActivityClassifier.WINDOW_SIZE
                    )
                }
            }
        }
    }

    fun stopSensorStream() {
        lastSensorTimeNs = 0L
        sensorWindow.clear()
        samplesSinceInference = 0
        inferenceJob?.cancel()
        _aiInsight.value = AiInsight()
        _isThresholdAlarm.value = false
        _isFalling.value = false
        updateActuatorState()
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
        stopCloudCommandListener = repository.observeRemoteCommand { status ->
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

    override fun onCleared() {
        stopCloudCommandListener?.invoke()
        inferenceJob?.cancel()
        classifier.close()
        super.onCleared()
    }
}
