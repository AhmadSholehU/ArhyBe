package com.overdevx.arhybe.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.overdevx.arhybe.R
import com.overdevx.arhybe.ui.theme.ArhyBeTheme
import com.overdevx.arhybe.ui.theme.background
import com.overdevx.arhybe.ui.theme.primary
import com.overdevx.arhybe.ui.theme.textColorWhite
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.overdevx.arhybe.UserGuideDestination

// Data class untuk merepresentasikan setiap item menu
data class InfoMenuItem(
    val title: String,
    val description: String,
    @DrawableRes val iconResId: Int,
    val destinationRoute: String
)

@Composable
fun InfoScreen(navController: NavController) {
    // Daftar menu informasi
    val menuItems = listOf(
        InfoMenuItem(
            title = "Info",
            description = "Aritmia",
            iconResId = R.drawable.ic_info,
            destinationRoute = "info/arrhythmia" // Contoh route
        ),
        InfoMenuItem(
            title = "Panduan",
            description = "Pengguna",
            iconResId = R.drawable.ic_guide,
            destinationRoute = "user_guide_route"
        ),
        InfoMenuItem(
            title = "Info",
            description = "Lainnya",
            iconResId = R.drawable.ic_other,
            destinationRoute = "info/other"
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(16.dp)
    ) {
        // Judul Halaman
        Text(
            text = "Pusat Informasi",
            color = textColorWhite,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            textAlign = TextAlign.Center
        )

        // Grid untuk menu
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp)
        ) {
            items(menuItems) { item ->
                InfoCard(
                    title = item.title,
                    description = item.description,
                    iconResId = item.iconResId,
                    onClick = {
                        if (item.destinationRoute == "user_guide_route") {
                            navController.navigate(UserGuideDestination)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun InfoCard(
    title: String,
    description: String,
    @DrawableRes iconResId: Int,
    onClick: () -> Unit
) {
    val cardColor = primary
    // --- FIX: Gunakan shape kustom di sini ---
    val ticketShape = remember { PointingCardShape(cornerRadius = 24.dp, tabWidth = 18.dp) }

    Box(
        modifier = Modifier
            .height(150.dp)
            .clickable(onClick = onClick)
    ) {
        Image(
            painter = painterResource(R.drawable.ic_card),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
                .align(Alignment.Center)
        )

        // Konten utama kartu
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Latar belakang ikon
            Box(
                modifier = Modifier
                    .size(55.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(textColorWhite.copy(alpha = 0.2f))
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = iconResId),
                    contentDescription = title,
                    modifier = Modifier.size(35.dp)
                )
            }

            // Teks Judul dan Deskripsi
            Column(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(
                    text = title,
                    color = textColorWhite,
                    fontSize = 22.sp,
                    fontFamily = FontFamily(listOf(Font(R.font.sofia_semibold))),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Text(
                    text = description,
                    color = textColorWhite.copy(alpha = 0.8f),
                    fontSize = 18.sp,
                    fontFamily = FontFamily(listOf(Font(R.font.sofia_regular))),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

        }
    }
}

// --- Shape Kustom untuk membuat bentuk kartu seperti pada desain ---
class PointingCardShape(private val cornerRadius: Dp, private val tabWidth: Dp) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val cornerRadiusPx = with(density) { cornerRadius.toPx() }
        val tabWidthPx = with(density) { tabWidth.toPx() }
        val tabHeight = size.height * 0.4f // Tinggi dari tab panah

        val path = Path().apply {
            reset()
            // Mulai dari sudut kiri atas
            moveTo(cornerRadiusPx, 0f)
            // Garis atas
            lineTo(size.width - cornerRadiusPx, 0f)
            // Sudut kanan atas
            arcTo(
                Rect(size.width - 2 * cornerRadiusPx, 0f, size.width, 2 * cornerRadiusPx),
                -90f,
                90f,
                false
            )

            // --- Logika Tab Panah Kanan ---
            val midY = size.height / 2
            lineTo(size.width, midY - tabHeight / 2) // Garis ke awal tab
            lineTo(size.width + tabWidthPx, midY) // Puncak tab
            lineTo(size.width, midY + tabHeight / 2) // Garis kembali dari tab
            // --- Akhir Logika Tab Panah ---

            lineTo(size.width, size.height - cornerRadiusPx)
            // Sudut kanan bawah
            arcTo(
                Rect(
                    size.width - 2 * cornerRadiusPx,
                    size.height - 2 * cornerRadiusPx,
                    size.width,
                    size.height
                ), 0f, 90f, false
            )
            // Garis bawah
            lineTo(cornerRadiusPx, size.height)
            // Sudut kiri bawah
            arcTo(
                Rect(0f, size.height - 2 * cornerRadiusPx, 2 * cornerRadiusPx, size.height),
                90f,
                90f,
                false
            )
            // Garis kiri
            lineTo(0f, cornerRadiusPx)
            // Kembali ke sudut kiri atas
            arcTo(Rect(0f, 0f, 2 * cornerRadiusPx, 2 * cornerRadiusPx), 180f, 90f, false)
            close()
        }
        return Outline.Generic(path)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun InfoScreenPreview() {
    ArhyBeTheme {
        // Kita butuh NavController palsu untuk preview
        InfoScreen(navController = NavController(LocalContext.current))
    }
}
