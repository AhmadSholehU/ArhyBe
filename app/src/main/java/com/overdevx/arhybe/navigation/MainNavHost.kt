package com.overdevx.arhybe.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.overdevx.arhybe.Destinations
import com.overdevx.arhybe.DiagnosisDetailDestination
import com.overdevx.arhybe.UserGuideDestination
import com.overdevx.arhybe.ui.screens.DiagnosisDetailScreen
import com.overdevx.arhybe.ui.screens.HomeScreen
import com.overdevx.arhybe.ui.screens.InfoScreen
import com.overdevx.arhybe.ui.screens.info.UserGuideScreen
import com.overdevx.arhybe.viewmodel.BluetoothViewModelAdvance

@Composable
fun MainNavHost(navController: NavHostController,
                bluetoothViewModel: BluetoothViewModelAdvance,
                requestPermissionsLambda: () -> Unit) {
    NavHost(
        navController = navController,
        startDestination = Destinations.HOME
    ) {
        composable(route = Destinations.HOME) {
            HomeScreen(
                navController = navController,
                bluetoothViewModel = bluetoothViewModel,
                onRequestPermissions = requestPermissionsLambda)
        }
        composable(route = Destinations.SETTINGS) {
            InfoScreen(navController = navController)
        }

        composable<DiagnosisDetailDestination> { // Tidak perlu typeMap lagi
            val navArgs: DiagnosisDetailDestination = it.toRoute()
            DiagnosisDetailScreen(
                navArgs = navArgs,
                onNavigateUp = { navController.navigateUp() }
            )
        }
        composable<UserGuideDestination> {
            UserGuideScreen(navController = navController)
        }


    }
}