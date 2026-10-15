package com.example.falldetectionsystem.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.falldetectionsystem.repository.SmartActuator
import com.example.falldetectionsystem.viewmodel.FallDetectionViewModel

@Composable
fun FallDetectionScreen(viewModel: FallDetectionViewModel) {
    val context = LocalContext.current
    val smartActuator = remember { SmartActuator(context) }

    val x by viewModel.x.collectAsState()
    val y by viewModel.y.collectAsState()
    val z by viewModel.z.collectAsState()
    val isFalling by viewModel.isFalling.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val isSyncSuccess by viewModel.isSyncSuccess.collectAsState()

    val remoteLedStatus by viewModel.remoteLedStatus.collectAsState()
    val manualFlashlight by viewModel.manualFlashlight.collectAsState()
    val isFlashlightActive by viewModel.isFlashlightActive.collectAsState()

    // Активація фізичного ліхтарика при зміні стану
    LaunchedEffect(isFlashlightActive) {
        smartActuator.toggleFlashlight(isFlashlightActive)
    }

    // Автоматична вібрація при фіксації падіння
    LaunchedEffect(isFalling) {
        if (isFalling) {
            smartActuator.triggerAlarm()
        }
    }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                viewModel.updateSensorData(event.values[0], event.values[1], event.values[2])
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        // Реєструємо зворотний зв'язок від системного ліхтарика (шторки)
        val torchCallback = smartActuator.registerTorchCallback { systemEnabled ->
            if (manualFlashlight != systemEnabled && !isFalling && !remoteLedStatus) {
                viewModel.toggleManualFlashlight(systemEnabled)
            }
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        onDispose {
            sensorManager.unregisterListener(listener)
            smartActuator.unregisterTorchCallback(torchCallback)
            smartActuator.toggleFlashlight(false)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Fall Detection System", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Осі акселерометра:")
        Text(text = "X: ${"%.2f".format(x)} | Y: ${"%.2f".format(y)} | Z: ${"%.2f".format(z)}")

        Spacer(modifier = Modifier.height(16.dp))

        // Індикатор хмарної синхронізації (Л.Р. №3)
        Card(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Хмарна синхронізація: $syncStatus", modifier = Modifier.weight(1f))
                Canvas(modifier = Modifier.size(16.dp)) {
                    drawCircle(
                        color = if (isSyncSuccess) Color.Green
                        else if (syncStatus == "Помилка мережі") Color.Red
                        else Color.Gray
                    )
                }
            }
        }

        // Індикатор дистанційної команди Firebase (Л.Р. №4)
        Card(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Дистанційна тривога (control/led_status): ${if (remoteLedStatus) "ON" else "OFF"}",
                    modifier = Modifier.weight(1f)
                )
                Canvas(modifier = Modifier.size(16.dp)) {
                    drawCircle(color = if (remoteLedStatus) Color.Yellow else Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val indicatorColor = if (isFalling) Color.Red else Color.Green
        Canvas(modifier = Modifier.size(90.dp)) {
            drawCircle(color = indicatorColor)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isFalling) "УВАГА: ПАДІННЯ! (Актуатори ON)" else "Стан стабільний",
            color = indicatorColor,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Панель ручного керування Manual Control (Л.Р. №4)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Manual Control (Керування актуаторами)", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Статус ліхтарика (Torch): ${if (isFlashlightActive) "УВІМКНЕНО" else "ВИМКНЕНО"}",
                    color = if (isFlashlightActive) Color(0xFF2E7D32) else Color.Gray
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Примусовий ліхтарик:")
                    Switch(
                        checked = manualFlashlight,
                        onCheckedChange = { viewModel.toggleManualFlashlight(it) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { smartActuator.triggerAlarm() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Тест вібровідгуку (Trigger Alarm)")
                }
            }
        }
    }
}