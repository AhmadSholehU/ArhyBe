package com.overdevx.arhybe.ui.components

// BottomNavigationBar.kt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.overdevx.arhybe.BottomNavItem
import com.overdevx.arhybe.R
import com.overdevx.arhybe.ui.theme.background
import com.overdevx.arhybe.ui.theme.textColorWhite
import com.overdevx.arhybe.ui.theme.textColorWhite2

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    // Bungkus NavigationBar dengan Box yang diberikan modifier.drawBehind()
    Box(
        modifier = Modifier
            .fillMaxWidth()

    ) {
        NavigationBar(
            containerColor = background,
            tonalElevation = 10.dp

        ) {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            // Ambil route saat ini
            val currentRoute = navBackStackEntry?.destination?.route
            BottomNavItem.items.forEach { item ->
                val icon =
                    painterResource(id = if (currentRoute == item.route) item.selectedIconRes else item.iconRes)
                val tint = if (currentRoute == item.route) textColorWhite else textColorWhite2
                val fontWeight =
                    if (currentRoute == item.route) FontWeight.Bold else FontWeight.Normal
                NavigationBarItem(
                    // Jika selected, gunakan item.selectedIconRes, jika tidak gunakan item.iconRes
                    icon = {
                        Icon(
                            painter = icon,
                            contentDescription = item.title,
                            tint = tint
                        )
                    },
                    label = {
                        Text(
                            text = item.title,
                            fontSize = 12.sp,
                            fontFamily = FontFamily(listOf(Font(R.font.sofia_medium))),
                            fontWeight = fontWeight,
                            color = tint
                        )
                    },
                    selected = currentRoute == item.route,
                    onClick = {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent,
                    )
                )
            }
        }
    }
}