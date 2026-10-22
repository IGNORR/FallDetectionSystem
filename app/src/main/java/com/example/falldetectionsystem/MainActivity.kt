package com.example.falldetectionsystem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.falldetectionsystem.model.ActivityClassifier
import com.example.falldetectionsystem.repository.AppDatabase
import com.example.falldetectionsystem.ui.AnalyticsScreen
import com.example.falldetectionsystem.ui.BleEmulatorScreen
import com.example.falldetectionsystem.ui.FallDetectionScreen
import com.example.falldetectionsystem.ui.theme.FallDetectionSystemTheme
import com.example.falldetectionsystem.viewmodel.FallDetectionViewModel
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        val database = AppDatabase.getDatabase(this)
        val fallViewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass == FallDetectionViewModel::class.java)
                @Suppress("UNCHECKED_CAST")
                return FallDetectionViewModel(
                    sensorDao = database.sensorDao(),
                    classifier = ActivityClassifier(applicationContext)
                ) as T
            }
        })[FallDetectionViewModel::class.java]

        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true

        setContent {
            FallDetectionSystemTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf("LAB1") }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
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
                            Button(
                                onClick = { currentScreen = "LAB5" },
                                enabled = currentScreen != "LAB5"
                            ) {
                                Text("Аналітика")
                            }
                        }

                        HorizontalDivider()

                        Box(modifier = Modifier.weight(1f)) {
                            when (currentScreen) {
                                "LAB1" -> FallDetectionScreen(viewModel = fallViewModel)
                                "LAB2" -> BleEmulatorScreen()
                                "LAB5" -> AnalyticsScreen(viewModel = fallViewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
