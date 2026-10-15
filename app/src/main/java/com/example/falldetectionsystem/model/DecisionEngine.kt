package com.example.falldetectionsystem.model

import kotlin.math.sqrt

class DecisionEngine(
    private val minThreshold: Double = 3.0,
    private val maxThreshold: Double = 20.0
) {
    fun calculateAcceleration(x: Float, y: Float, z: Float): Double {
        return sqrt((x * x + y * y + z * z).toDouble())
    }

    // Повертає true, якщо зафіксовано падіння (критичне значення)
    fun shouldActivateActuator(x: Float, y: Float, z: Float): Boolean {
        val acceleration = calculateAcceleration(x, y, z)
        return acceleration !in minThreshold..maxThreshold
    }
}