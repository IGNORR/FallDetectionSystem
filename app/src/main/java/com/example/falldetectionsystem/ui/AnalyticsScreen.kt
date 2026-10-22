package com.example.falldetectionsystem.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.example.falldetectionsystem.model.SensorRecord
import com.example.falldetectionsystem.viewmodel.FallDetectionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun AnalyticsScreen(viewModel: FallDetectionViewModel) {
    val records by viewModel.historyRecords.collectAsState()
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                viewModel.updateSensorData(event.values[0], event.values[1], event.values[2], event.timestamp)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        var registered = false
        fun startSensor() {
            if (!registered && accelerometer != null) {
                registered = sensorManager.registerListener(listener, accelerometer, 10_000)
            }
        }
        fun stopSensor() {
            if (registered) sensorManager.unregisterListener(listener)
            registered = false
            viewModel.stopSensorStream()
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> startSensor()
                Lifecycle.Event.ON_STOP -> stopSensor()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) startSensor()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            stopSensor()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Аналітика сенсора (Room DB)",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Відображено останніх точок: ${records.size} / 50",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                if (records.isEmpty()) {
                    Text(
                        text = "Очікування даних...",
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    SensorLineChart(records = records, timeFormatter = timeFormatter)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Журнал записів у БД (останні зверху):",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            items(records.reversed()) { record ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ID: #${record.id} | Час: ${timeFormatter.format(Date(record.timestamp))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Text(
                                text = "Прискорення: ${"%.2f".format(record.sensorValue)} м/с²",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Text(
                            text = if (record.actionTriggered) "ТРИВОГА (ON)" else "Норма",
                            color = if (record.actionTriggered) Color.Red else Color(0xFF2E7D32),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SensorLineChart(records: List<SensorRecord>, timeFormatter: SimpleDateFormat) {
    val textPaint = remember {
        Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = 28f
            isAntiAlias = true
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val leftPadding = 70f
        val bottomPadding = 45f
        val topPadding = 20f
        val rightPadding = 20f

        val chartWidth = size.width - leftPadding - rightPadding
        val chartHeight = size.height - bottomPadding - topPadding

        val maxVal = maxOf(25f, (records.maxOfOrNull { it.sensorValue } ?: 25f) + 2f)
        val minVal = 0f

        fun valueToY(value: Float): Float {
            val ratio = ((value - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
            return topPadding + chartHeight * (1f - ratio)
        }

        val ySteps = listOf(0f, 5f, 10f, 15f, 20f, 25f)
        ySteps.forEach { step ->
            if (step <= maxVal) {
                val y = valueToY(step)
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.6f),
                    start = Offset(leftPadding, y),
                    end = Offset(size.width - rightPadding, y),
                    strokeWidth = 1.5f
                )
                drawContext.canvas.nativeCanvas.drawText(
                    "${step.toInt()}",
                    10f,
                    y + 10f,
                    textPaint
                )
            }
        }

        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
        listOf(3f, 20f).forEach { threshold ->
            val y = valueToY(threshold)
            drawLine(
                color = Color.Red.copy(alpha = 0.7f),
                start = Offset(leftPadding, y),
                end = Offset(size.width - rightPadding, y),
                strokeWidth = 2.5f,
                pathEffect = dashEffect
            )
        }

        val path = Path()
        val points = records.mapIndexed { index, record ->
            val x = if (records.size > 1) {
                leftPadding + (index.toFloat() / (records.size - 1)) * chartWidth
            } else {
                leftPadding + chartWidth / 2f
            }
            val y = valueToY(record.sensorValue)
            Offset(x, y)
        }

        points.forEachIndexed { index, offset ->
            if (index == 0) path.moveTo(offset.x, offset.y)
            else path.lineTo(offset.x, offset.y)
        }

        if (points.size > 1) {
            drawPath(
                path = path,
                color = Color(0xFF3F51B5),
                style = Stroke(width = 5f)
            )
        }

        points.forEachIndexed { index, offset ->
            val isTriggered = records[index].actionTriggered
            drawCircle(
                color = if (isTriggered) Color.Red else Color(0xFF3F51B5),
                radius = if (isTriggered) 9f else 6f,
                center = offset
            )
        }

        if (records.isNotEmpty()) {
            val firstTime = timeFormatter.format(Date(records.first().timestamp))
            val lastTime = timeFormatter.format(Date(records.last().timestamp))
            drawContext.canvas.nativeCanvas.drawText(
                firstTime,
                leftPadding,
                size.height - 5f,
                textPaint
            )
            if (records.size > 1) {
                drawContext.canvas.nativeCanvas.drawText(
                    lastTime,
                    size.width - rightPadding - 110f,
                    size.height - 5f,
                    textPaint
                )
            }
        }
    }
}
