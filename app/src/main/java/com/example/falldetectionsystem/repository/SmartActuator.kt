package com.example.falldetectionsystem.repository

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

class SmartActuator(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var isTorchOn = false

    private val cameraIdWithFlash: String? by lazy {
        try {
            cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            null
        }
    }

    // Слухач системного стану ліхтарика (реагує на шторку Android)
    fun registerTorchCallback(onStatusChanged: (Boolean) -> Unit): CameraManager.TorchCallback {
        val callback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                if (cameraId == cameraIdWithFlash) {
                    isTorchOn = enabled
                    onStatusChanged(enabled)
                }
            }
        }
        cameraManager.registerTorchCallback(callback, null)
        return callback
    }

    fun unregisterTorchCallback(callback: CameraManager.TorchCallback) {
        cameraManager.unregisterTorchCallback(callback)
    }

    fun triggerAlarm() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(500)
            }
        }
        Log.d("SmartActuator", "Vibrator triggered (500ms)")
    }

    fun toggleFlashlight(status: Boolean): Boolean {
        val hasFlash = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
        if (!hasFlash) return false

        return try {
            val id = cameraIdWithFlash
            if (id != null && isTorchOn != status) {
                cameraManager.setTorchMode(id, status)
                isTorchOn = status
            }
            isTorchOn
        } catch (e: Exception) {
            Log.e("SmartActuator", "Failed to toggle flashlight", e)
            false
        }
    }
}