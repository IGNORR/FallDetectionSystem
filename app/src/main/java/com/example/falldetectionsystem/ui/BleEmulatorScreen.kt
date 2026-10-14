package com.example.falldetectionsystem.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.falldetectionsystem.model.BleState
import com.example.falldetectionsystem.viewmodel.BleViewModel

@Composable
fun BleEmulatorScreen(viewModel: BleViewModel = viewModel()) {
    val connectionState by viewModel.connectionState.collectAsState()
    val foundDevices by viewModel.foundDevices.collectAsState()
    val logs by viewModel.logs.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Статус: ${connectionState.name}", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { viewModel.startScan() },
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
                    Button(onClick = { viewModel.toggleConnection(device) }) {
                        Text(if (connectionState == BleState.CONNECTED) "Disconnect" else "Connect")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.sendFallCommand() },
            enabled = connectionState == BleState.CONNECTED
        ) {
            Text("Симулювати відправку даних (Tx)")
        }

        Spacer(modifier = Modifier.height(16.dp))

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