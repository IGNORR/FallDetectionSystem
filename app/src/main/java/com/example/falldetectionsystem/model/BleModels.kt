package com.example.falldetectionsystem.model

enum class BleState {
    DISCONNECTED, SCANNING, CONNECTED
}

data class MockDevice(
    val name: String,
    val address: String
)