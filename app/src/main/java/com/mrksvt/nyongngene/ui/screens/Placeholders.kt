package com.mrksvt.nyongngene.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mrksvt.nyongngene.ui.viewmodel.AppViewModelFactory
import com.mrksvt.nyongngene.ui.viewmodel.ChatViewModel
import com.mrksvt.nyongngene.ui.viewmodel.MainViewModel

// HomeScreen moved to HomeScreen.kt

@Composable
fun EmergencyScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("SOS Screen (Emergency)")
        // SOS Button Logic here
    }
}

// MappingScreen moved to MappingScreen.kt



@Composable
fun DebugScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Debug Screen (RSSI/SNR)")
    }
}

@Composable
fun AccountScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Account Screen")
    }
}
