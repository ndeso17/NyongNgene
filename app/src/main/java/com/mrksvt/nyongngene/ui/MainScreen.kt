package com.mrksvt.nyongngene.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mrksvt.nyongngene.ui.navigation.AppNavHost
import com.mrksvt.nyongngene.ui.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(deepLinkChannelId: String? = null) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    // Disable drawer swipe on mapping screen
    val isOnMappingScreen = currentDestination?.route?.startsWith("mapping") == true ||
                            currentDestination?.route?.startsWith("trails") == true

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isOnMappingScreen,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "NyongNgene",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge
                )
                HorizontalDivider()
                Screen.getDrawerItems().forEach { screen ->
                    NavigationDrawerItem(
                        label = { Text(screen.title) },
                        icon = { Icon(screen.icon!!, contentDescription = null) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (!isOnMappingScreen) {
                    CenterAlignedTopAppBar(
                        title = { Text("NyongNgene") },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (!isOnMappingScreen) {
                    NavigationBar {
                        Screen.getBottomBarItems().forEach { screen ->
                            NavigationBarItem(
                                icon = { Icon(screen.icon!!, contentDescription = null) },
                                label = { Text(screen.title) },
                                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            AppNavHost(
                navController = navController, 
                modifier = if (isOnMappingScreen) Modifier else Modifier.padding(innerPadding),
                deepLinkChannelId = deepLinkChannelId
            )
        }
    }
}

