package com.example.falldetectionsystem.model

import kotlin.math.abs
import kotlin.math.sqrt

/** Rejects a fall prediction when the sensor window contains no substantial motion. */
object FallMotionGate {
    private const val GRAVITY = 9.80665f
    private const val MIN_PEAK_DEVIATION = 4.9f // Approximately 0.5 g from rest.
    private const val MAX_STATIONARY_STD = 0.2f // m/s² over the whole window.

    fun isStationary(window: FloatArray): Boolean {
        require(window.isNotEmpty() && window.size % 3 == 0) { "Expected accelerometer triples" }
        var mean = 0.0
        var sumSquaredDifferences = 0.0
        var count = 0
        for (index in window.indices step 3) {
            val x = window[index]
            val y = window[index + 1]
            val z = window[index + 2]
            val magnitude = sqrt(x * x + y * y + z * z).toDouble()
            count++
            val difference = magnitude - mean
            mean += difference / count
            sumSquaredDifferences += difference * (magnitude - mean)
        }
        return sqrt(sumSquaredDifferences / count) < MAX_STATIONARY_STD
    }

    fun hasImpact(window: FloatArray): Boolean {
        require(window.size % 3 == 0) { "Expected accelerometer triples" }
        for (index in window.indices step 3) {
            val x = window[index]
            val y = window[index + 1]
            val z = window[index + 2]
            val magnitude = sqrt(x * x + y * y + z * z)
            if (abs(magnitude - GRAVITY) >= MIN_PEAK_DEVIATION) return true
        }
        return false
    }
}
