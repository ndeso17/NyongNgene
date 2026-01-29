package com.mrksvt.nyongngene.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.mrksvt.nyongngene.ui.screens.*

import androidx.navigation.navArgument

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
            HomeScreen(
                onMountainClick = { mountainName ->
                    navController.navigate("trails/$mountainName")
                }
            )
        }
        
        // Trail List Screen (Step 2)
        composable(
            route = "trails/{mountainName}",
            arguments = listOf(navArgument("mountainName") { defaultValue = "" })
        ) { entry ->
            val mountainName = entry.arguments?.getString("mountainName") ?: ""
            TrailListScreen(
                mountainName = mountainName,
                onTrailClick = { mapId ->
                    navController.navigate("mapping?mapId=$mapId")
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Chat.route) {
            ChatScreen(deepLinkChannelId = deepLinkChannelId)
        }
        
        composable(Screen.Emergency.route) {
            EmergencyScreen()
        }

        // Drawer
        composable(
            route = "mapping?mapId={mapId}",
            arguments = listOf(navArgument("mapId") { defaultValue = "" })
        ) { entry ->
            val mapId = entry.arguments?.getString("mapId") ?: ""
            MappingScreen(
                mapId = mapId,
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.OfflineMaps.route) { MapDownloadScreen() }
        composable(Screen.LoRaSettings.route) { LoRaSettingsScreen() }
        composable(Screen.Debug.route) { DebugScreen() }
        composable(Screen.Account.route) { AccountScreen() }
    }
}
