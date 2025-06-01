package com.overdevx.arhybe.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.overdevx.arhybe.Destinations
import com.overdevx.arhybe.DiagnosisDetailDestination
import com.overdevx.arhybe.DiagnosisType
import com.overdevx.arhybe.DiagnosisTypeNavType
import com.overdevx.arhybe.ui.screens.DiagnosisDetailScreen
import com.overdevx.arhybe.ui.screens.HomeScreen
import com.overdevx.arhybe.ui.screens.SettingsScreen

@Composable
fun MainNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Destinations.HOME
    ) {
        composable(route = Destinations.HOME) {
            HomeScreen(navController = navController)
        }
        composable(route = Destinations.SETTINGS) {
            SettingsScreen()
        }

        composable<DiagnosisDetailDestination> { // Tidak perlu typeMap lagi
            val navArgs: DiagnosisDetailDestination = it.toRoute()
            DiagnosisDetailScreen(
                navArgs = navArgs,
                onNavigateUp = { navController.navigateUp() }
            )
        }
    }
}