package com.example.falldetectionsystem

import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Модель пристрою
data class BleDevice(val name: String, val address: String)

// Інтерфейс з'єднання
interface IBleConnector {
    fun connect(device: BleDevice)
    fun disconnect()
    fun sendData(data: String)
}

// Стани UI
enum class BleState { DISCONNECTED, SCANNING, CONNECTING, CONNECTED }

// Mock-сервіс (Імітація)
class MockBleService : IBleConnector {
    private val _connectionState = MutableStateFlow(BleState.DISCONNECTED)
    val connectionState: StateFlow<BleState> = _connectionState

    private val _foundDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    val foundDevices: StateFlow<List<BleDevice>> = _foundDevices

    private val _terminalLogs = MutableStateFlow<List<String>>(emptyList())
    val terminalLogs: StateFlow<List<String>> = _terminalLogs

    private fun log(message: String) {
        Log.d("MockBLE", message)
        _terminalLogs.value += message
    }

    suspend fun startScan() {
        _connectionState.value = BleState.SCANNING
        log("Початок сканування...")
        _foundDevices.value = emptyList()
        delay(2000) // Імітація затримки сканування
        _foundDevices.value = listOf(BleDevice("SmartHub-FallDet", "00:11:22:33:44:55"))
        log("Пристрій знайдено.")
        _connectionState.value = BleState.DISCONNECTED
    }

    override fun connect(device: BleDevice) {
        _connectionState.value = BleState.CONNECTING
        log("Підключення до ${device.name}...")
        // Імітація підключення без корутин у UI-потоці для спрощення
        _connectionState.value = BleState.CONNECTED
        log("Підключено до ${device.name}.")
    }

    override fun disconnect() {
        _connectionState.value = BleState.DISCONNECTED
        log("Відключено.")
    }

    override fun sendData(data: String) {
        if (_connectionState.value == BleState.CONNECTED) {
            log("TX -> $data")
        } else {
            log("Помилка: Немає з'єднання для відправки $data")
        }
    }
}