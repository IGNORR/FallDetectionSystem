package com.example.falldetectionsystem.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.falldetectionsystem.viewmodel.FallDetectionViewModel

@Composable
fun FallDetectionScreen(viewModel: FallDetectionViewModel = viewModel()) {
    val context = LocalContext.current

    val x by viewModel.x.collectAsState()
    val y by viewModel.y.collectAsState()
    val z by viewModel.z.collectAsState()
    val isFalling by viewModel.isFalling.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val isSyncSuccess by viewModel.isSyncSuccess.collectAsState()

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                viewModel.updateSensorData(
                    event.values[0],
                    event.values[1],
                    event.values[2]
                )
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Fall Detection System", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(32.dp))

        Text(text = "Осі акселерометра:")
        Text(text = "X: ${"%.2f".format(x)}")
        Text(text = "Y: ${"%.2f".format(y)}")
        Text(text = "Z: ${"%.2f".format(z)}")

        Spacer(modifier = Modifier.height(32.dp))

        Card(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Хмарна синхронізація: $syncStatus", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.width(8.dp))
                Canvas(modifier = Modifier.size(16.dp)) {
                    drawCircle(
                        color = if (isSyncSuccess) Color.Green
                        else if (syncStatus == "Помилка мережі") Color.Red
                        else Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        val indicatorColor = if (isFalling) Color.Red else Color.Green
        Canvas(modifier = Modifier.size(120.dp)) {
            drawCircle(color = indicatorColor)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isFalling) "УВАГА: ПАДІННЯ!" else "Стан стабільний",
            color = indicatorColor,
            style = MaterialTheme.typography.titleLarge
        )
    }
}