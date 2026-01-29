package com.mrksvt.nyongngene.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mrksvt.nyongngene.data.repository.MapManager
import com.mrksvt.nyongngene.data.repository.MapManifest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrailListScreen(
    mountainName: String,
    onTrailClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val mapManager = remember { MapManager(context) }
    val availableMaps by mapManager.availableMaps.collectAsState()
    
    // Filter trails for this mountain
    val trails = availableMaps.filter { it.mountain == mountainName }
    
    LaunchedEffect(Unit) {
        mapManager.scanLocalMaps()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(mountainName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Text(
                "Pilih Jalur Pendakian", 
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp)
            )

            if (trails.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tidak ada jalur untuk gunung ini.")
                }
            } else {
                LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                    items(trails) { map ->
                        TrailItem(
                            map = map,
                            onClick = { onTrailClick(map.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrailItem(
    map: MapManifest,
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
            Text("Jalur ${map.trail}", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Version ${map.version}", style = MaterialTheme.typography.labelSmall)
        }
    }
}
