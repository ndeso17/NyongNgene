package com.mrksvt.nyongngene.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mrksvt.nyongngene.data.repository.MapManager
import com.mrksvt.nyongngene.ui.viewmodel.AppViewModelFactory
import com.mrksvt.nyongngene.ui.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel = viewModel(factory = AppViewModelFactory.Factory),
    onMountainClick: (String) -> Unit
) {
    val context = LocalContext.current
    val mapManager = remember { MapManager(context) }
    val availableMaps by mapManager.availableMaps.collectAsState()
    val isConnected by viewModel.isLoRaConnected.collectAsState()
    val snr by viewModel.snr.collectAsState()
    
    // Group maps by mountain name
    val mountains = availableMaps.map { it.mountain }.distinct()
    
    LaunchedEffect(Unit) {
        mapManager.scanLocalMaps()
    }

    Scaffold { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Status Bar
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("LoRa Status", style = MaterialTheme.typography.labelSmall)
                        Text(
                            if (isConnected) "Connected" else "Disconnected", 
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("SNR: $snr dB", style = MaterialTheme.typography.bodyLarge)
                }
            }

            Text(
                "Pilih Gunung", 
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (mountains.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Belum ada peta.")
                        Text("Buka menu Offline Maps untuk download.")
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                    items(mountains) { mountainName ->
                        val trailCount = availableMaps.count { it.mountain == mountainName }
                        MountainItem(
                            mountainName = mountainName,
                            trailCount = trailCount,
                            onClick = { onMountainClick(mountainName) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MountainItem(
    mountainName: String,
    trailCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(mountainName, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text("$trailCount jalur tersedia", style = MaterialTheme.typography.bodySmall)
        }
    }
}
