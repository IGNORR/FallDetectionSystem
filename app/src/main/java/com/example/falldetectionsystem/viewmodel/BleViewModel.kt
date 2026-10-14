package com.example.falldetectionsystem.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.falldetectionsystem.model.MockDevice
import com.example.falldetectionsystem.repository.MockBleService
import kotlinx.coroutines.launch

class BleViewModel(
    private val bleService: MockBleService = MockBleService()
) : ViewModel() {

    val connectionState = bleService.connectionState
    val foundDevices = bleService.foundDevices
    val logs = bleService.terminalLogs

    fun startScan() {
        viewModelScope.launch {
            bleService.startScan()
        }
    }

    fun toggleConnection(device: MockDevice) {
        if (connectionState.value == com.example.falldetectionsystem.model.BleState.CONNECTED) {
            bleService.disconnect()
        } else {
            bleService.connect(device)
        }
    }

    fun sendFallCommand() {
        bleService.sendData("FALL_DETECTED_CMD")
    }
}