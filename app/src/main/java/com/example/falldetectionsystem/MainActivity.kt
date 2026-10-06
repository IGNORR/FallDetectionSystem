package com.example.falldetectionsystem

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FallDetectionScreen(context = this)
        }
    }
}

@Composable
fun FallDetectionScreen(context: Context) {
    // Стейт для координат та статусу
    var x by remember { mutableFloatStateOf(0f) }
    var y by remember { mutableFloatStateOf(0f) }
    var z by remember { mutableFloatStateOf(0f) }
    var isFalling by remember { mutableStateOf(false) }

    // Життєвий цикл: підписка та відписка від сенсора
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                x = event.values[0]
                y = event.values[1]
                z = event.values[2]

                // Обчислення загального вектора прискорення
                val acceleration = sqrt((x * x + y * y + z * z).toDouble())

                // Threshold: Стан невагомості або різкий удар
                isFalling = acceleration !in 3.0..20.0
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)

        // Викликається при onPause/onStop (захист від втрати ресурсів)
        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // Інтерфейс
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

        Spacer(modifier = Modifier.height(48.dp))

        // Графічний індикатор
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