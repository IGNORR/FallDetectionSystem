package com.example.falldetectionsystem.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class SensorPayload(
    val sensorType: String,
    val x: Float,
    val y: Float,
    val z: Float,
    val timestamp: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("Europe/Kiev")
    }.format(Date())
)