package com.mrksvt.nyongngene.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.mrksvt.nyongngene.ui.screens.*

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    deepLinkChannelId: String? = null
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        // Bottom Bar
        composable(Screen.Home.route) {
            com.mrksvt.nyongngene.ui.screens.HomeScreen()
        }
        
        composable(Screen.Chat.route) {
            com.mrksvt.nyongngene.ui.screens.ChatScreen(deepLinkChannelId = deepLinkChannelId)
        }
        
        composable(Screen.Emergency.route) {
            EmergencyScreen()
        }

        // Drawer
        composable(Screen.Mapping.route) { MappingScreen() }
        composable(Screen.LoRaSettings.route) { LoRaSettingsScreen() }
        composable(Screen.Debug.route) { DebugScreen() }
        composable(Screen.Account.route) { AccountScreen() }
    }
}
