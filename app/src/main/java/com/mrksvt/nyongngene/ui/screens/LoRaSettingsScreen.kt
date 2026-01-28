package com.mrksvt.nyongngene.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mrksvt.nyongngene.ui.viewmodel.AppViewModelFactory
import com.mrksvt.nyongngene.ui.viewmodel.MainViewModel

@Composable
fun LoRaSettingsScreen(
    viewModel: MainViewModel = viewModel(factory = AppViewModelFactory.Factory)
) {
    val isConnected by viewModel.isLoRaConnected.collectAsState()
    val snr by viewModel.snr.collectAsState()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "LoRa Settings",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (isConnected) "Status: Connected (USB)" else "Status: Disconnected",
                color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            
            if (isConnected) {
                Text(text = "Signal (SNR): $snr dB")
            } else {
                Text(text = "Connect a supported LoRa module via USB OTG")
            }
        }
    }
}
