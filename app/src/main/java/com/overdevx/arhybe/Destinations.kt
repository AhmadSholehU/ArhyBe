package com.overdevx.arhybe

import kotlinx.serialization.Serializable

@Serializable
object HomeDestination

@Serializable
object SettingsDestination

// BottomNavItem.kt
sealed class BottomNavItem(
    val title: String,
    val route: String,
    val iconRes: Int,
    val selectedIconRes: Int
) {
    object Home : BottomNavItem(
        title = "Home",
        route = Destinations.HOME,
        iconRes = R.drawable.ic_home2,               // Vector drawable untuk state normal
        selectedIconRes = R.drawable.ic_home // Vector drawable untuk state selected
    )
    object Settings : BottomNavItem(
        title = "Settings",
        route = Destinations.SETTINGS,
        iconRes = R.drawable.ic_setting2,
        selectedIconRes = R.drawable.ic_setting
    )

    companion object {
        val items = listOf(Home, Settings)
    }
}

object Destinations {
    const val HOME = "home"
    const val SETTINGS = "settings"
}
