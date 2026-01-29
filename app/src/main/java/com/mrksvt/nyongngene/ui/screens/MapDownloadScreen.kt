package com.mrksvt.nyongngene.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mrksvt.nyongngene.data.repository.MapManager
import com.mrksvt.nyongngene.data.repository.MapManifest
import com.mrksvt.nyongngene.service.MapShareService
import kotlinx.coroutines.launch

@Composable
fun MapDownloadScreen() {
    val context = LocalContext.current
    val mapManager = remember { MapManager(context) }
    val scope = rememberCoroutineScope()
    val availableMaps by mapManager.availableMaps.collectAsState()
    
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        mapManager.scanLocalMaps()
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Add Map") },
            text = { Text("Choose a method to add an offline map.") },
            confirmButton = {
                TextButton(
                    onClick = { 
                        scope.launch {
                            createSampleMap(context)
                            mapManager.scanLocalMaps()
                            showDialog = false
                        }
                    }
                ) {
                    Text("Download Sample (Gunung Slamet)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, "Download Map")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Text(
                "Offline Maps", 
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp)
            )
            
            if (availableMaps.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No maps found. Place map folders in\nAndroid/data/com.mrksvt.nyongngene/files/maps/")
                }
            } else {
                LazyColumn {
                    items(availableMaps) { map ->
                        MapItem(
                            map = map,
                            onShare = { 
                                val intent = Intent(context, MapShareService::class.java).apply {
                                    action = MapShareService.ACTION_START_SERVER
                                    putExtra(MapShareService.EXTRA_MAP_ID, map.id)
                                }
                                context.startService(intent)
                            },
                            onDelete = {
                                // TODO: Delete folder
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MapItem(
    map: MapManifest,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(map.mountain, style = MaterialTheme.typography.titleMedium)
                    Text("Trail: ${map.trail}", style = MaterialTheme.typography.bodyMedium)
                    Text("Ver: ${map.version}", style = MaterialTheme.typography.bodySmall)
                }
                Row {
                    IconButton(onClick = onShare) {
                        Icon(Icons.Default.Share, "Share via Wi-Fi")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, "Delete")
                    }
                }
            }
        }
    }
}

private fun createSampleMap(context: android.content.Context) {
    val mapsDir = java.io.File(context.getExternalFilesDir(null), "maps")
    if (!mapsDir.exists()) mapsDir.mkdirs()
    
    val sampleMapDir = java.io.File(mapsDir, "gunung_slamet_v1")
    if (!sampleMapDir.exists()) sampleMapDir.mkdirs()
    
    val manifest = """
        {
          "map_id": "gunung_slamet_v1",
          "mountain": "Gunung Slamet",
          "trail": "Bambangan",
          "version": "1.0.0",
          "min_zoom": 12,
          "max_zoom": 15
        }
    """.trimIndent()
    
    java.io.File(sampleMapDir, "manifest.json").writeText(manifest)
    // Create dummy tile directory so it looks valid
    java.io.File(sampleMapDir, "tiles").mkdirs()
}
