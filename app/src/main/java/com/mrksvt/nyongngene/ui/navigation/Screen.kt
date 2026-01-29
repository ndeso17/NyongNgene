package com.mrksvt.nyongngene.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    // Primary (Bottom Bar)
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object Chat : Screen("chat", "Chat", Icons.Default.Email)
    data object Emergency : Screen("emergency", "SOS", Icons.Default.Warning)

    // Secondary (Drawer / Other)
    data object Mapping : Screen("mapping", "Mapping", Icons.Default.Place)
    data object OfflineMaps : Screen("offline_maps", "Offline Maps", Icons.Default.Place)
    data object LoRaSettings : Screen("lora_settings", "LoRa Settings", Icons.Default.Settings)
    data object Debug : Screen("debug", "Debug", Icons.Default.Info)
    data object Account : Screen("account", "Account", Icons.Default.AccountCircle)

    companion object {
        fun getBottomBarItems() = listOf(Home, Chat, Emergency)
        fun getDrawerItems() = listOf(Mapping, OfflineMaps, LoRaSettings, Debug, Account)
    }
}
