package com.example.falldetectionsystem.repository

import com.example.falldetectionsystem.model.BleState
import com.example.falldetectionsystem.model.MockDevice
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MockBleService {
    private val _connectionState = MutableStateFlow(BleState.DISCONNECTED)
    val connectionState: StateFlow<BleState> = _connectionState

    private val _foundDevices = MutableStateFlow<List<MockDevice>>(emptyList())
    val foundDevices: StateFlow<List<MockDevice>> = _foundDevices

    private val _terminalLogs = MutableStateFlow<List<String>>(emptyList())
    val terminalLogs: StateFlow<List<String>> = _terminalLogs

    suspend fun startScan() {
        _connectionState.value = BleState.SCANNING
        log("Початок сканування BLE...")
        delay(2000)
        _foundDevices.value = listOf(
            MockDevice("FallSensor_Pro", "00:11:22:33:FF:EE"),
            MockDevice("Medical_Band_v2", "AA:BB:CC:DD:EE:FF")
        )
        _connectionState.value = BleState.DISCONNECTED
        log("Сканування завершено. Знайдено пристроїв: 2")
    }

    fun connect(device: MockDevice) {
        _connectionState.value = BleState.CONNECTED
        log("Підключено до ${device.name} (${device.address})")
    }

    fun disconnect() {
        _connectionState.value = BleState.DISCONNECTED
        log("Пристрій відключено")
    }

    fun sendData(data: String) {
        if (_connectionState.value == BleState.CONNECTED) {
            log("Tx -> $data")
            log("Rx <- ACK_OK")
        }
    }

    private fun log(message: String) {
        _terminalLogs.value = _terminalLogs.value + message
    }
}