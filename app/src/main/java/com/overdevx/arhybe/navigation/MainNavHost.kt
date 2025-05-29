package com.overdevx.arhybe.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.overdevx.arhybe.Destinations
import com.overdevx.arhybe.ui.screens.HomeScreen
import com.overdevx.arhybe.ui.screens.SettingsScreen

@Composable
fun MainNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Destinations.HOME
    ) {
        composable(route = Destinations.HOME) {
            HomeScreen()
        }
        composable(route = Destinations.SETTINGS) {
            SettingsScreen()
        }
    }
}