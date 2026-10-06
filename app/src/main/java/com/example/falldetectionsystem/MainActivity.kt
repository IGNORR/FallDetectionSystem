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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var currentScreen by remember { mutableStateOf("LAB1") }

            Column(modifier = Modifier.fillMaxSize()) {
                // Панель навігації
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { currentScreen = "LAB1" },
                        enabled = currentScreen != "LAB1"
                    ) {
                        Text("Сенсор")
                    }
                    Button(
                        onClick = { currentScreen = "LAB2" },
                        enabled = currentScreen != "LAB2"
                    ) {
                        Text("(BLE)")
                    }
                }

                HorizontalDivider()

                // Відображення екранів
                Box(modifier = Modifier.weight(1f)) {
                    when (currentScreen) {
                        "LAB1" -> FallDetectionScreen(context = this@MainActivity)
                        "LAB2" -> BleEmulatorScreen()
                    }
                }
            }
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
@Composable
fun BleEmulatorScreen() {
    val bleService = remember { MockBleService() }
    val connectionState by bleService.connectionState.collectAsState()
    val foundDevices by bleService.foundDevices.collectAsState()
    val logs by bleService.terminalLogs.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Статус: ${connectionState.name}", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { coroutineScope.launch { bleService.startScan() } },
                enabled = connectionState == BleState.DISCONNECTED
            ) {
                Text("Пошук пристроїв")
            }
            if (connectionState == BleState.SCANNING) {
                Spacer(modifier = Modifier.width(16.dp))
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Список пристроїв
        Text("Знайдені пристрої:", style = MaterialTheme.typography.labelLarge)
        foundDevices.forEach { device ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(device.name, style = MaterialTheme.typography.bodyLarge)
                        Text(device.address, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = {
                            if (connectionState == BleState.CONNECTED) bleService.disconnect()
                            else bleService.connect(device)
                        }
                    ) {
                        Text(if (connectionState == BleState.CONNECTED) "Disconnect" else "Connect")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { bleService.sendData("FALL_DETECTED_CMD") },
            enabled = connectionState == BleState.CONNECTED
        ) {
            Text("Симлювати відправку даних (Tx)")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Термінал
        Text("Термінал:", style = MaterialTheme.typography.labelLarge)
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            reverseLayout = true
        ) {
            items(logs.reversed()) { logMsg ->
                Text(logMsg, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}