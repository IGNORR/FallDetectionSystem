package com.example.falldetectionsystem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.falldetectionsystem.ui.BleEmulatorScreen
import com.example.falldetectionsystem.ui.FallDetectionScreen
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        setContent {
            var currentScreen by remember { mutableStateOf("LAB1") }

            Column(modifier = Modifier.fillMaxSize()) {
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

                Box(modifier = Modifier.weight(1f)) {
                    when (currentScreen) {
                        "LAB1" -> FallDetectionScreen()
                        "LAB2" -> BleEmulatorScreen()
                    }
                }
            }
        }
    }
}