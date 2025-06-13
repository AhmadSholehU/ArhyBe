package com.overdevx.arhybe.ui.screens.info

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.overdevx.arhybe.R
import com.overdevx.arhybe.ui.theme.*

// Data class untuk setiap bagian panduan
data class GuideSection(
    val title: String,
    val content: String,
    @DrawableRes val iconResId: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserGuideScreen(navController: NavController) {
    // Konten panduan (bisa diambil dari ViewModel atau resource)
    val guideSections = listOf(
        GuideSection(
            title = "Bagian 1: Pengenalan",
            content = "Lorem ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s.",
            iconResId = R.drawable.ic_headercard // Ganti dengan ikon yang sesuai
        ),
        GuideSection(
            title = "Bagian 2: Fitur Utama",
            content = "Lorem ipsum is simply dummy text of the printing and typesetting industry.\n1. Connect Device\n2. Watching Data\n3. See the Result",
            iconResId = R.drawable.ic_headercard // Ganti dengan ikon yang sesuai
        )
    )
    val totalSteps = 5
    var currentStep by remember { mutableStateOf(1) }
    val progress by animateFloatAsState(targetValue = currentStep.toFloat() / totalSteps, label = "progressAnimation")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panduan Pengguna", color = textColorWhite) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = textColorWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF00695C)) // Warna hijau tua
            )
        },
        containerColor = background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Progress Section
            Spacer(modifier = Modifier.height(24.dp))
            ProgressIndicatorSection(currentStep = currentStep, totalSteps = totalSteps, progress = progress)
            Spacer(modifier = Modifier.height(24.dp))

            // Main Title and Description
            Text(
                text = "Bagaimana Menggunakan Aplikasi ARhyBe",
                color = textColorWhite,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 32.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Lorem ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s.",
                color = textColorWhite.copy(alpha = 0.7f),
                fontSize = 16.sp,
                lineHeight = 24.sp
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Guide Sections
            guideSections.forEach { section ->
                GuideSectionCard(section = section)
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.weight(1f)) // Mendorong tombol ke bawah

            // Next Button
            Button(
                onClick = { if (currentStep < totalSteps) currentStep++ else navController.popBackStack() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C))
            ) {
                Text(
                    text = if (currentStep < totalSteps) "Lanjut" else "Selesai",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(painter = painterResource(id = R.drawable.ic_next), contentDescription = "Lanjut")
            }
        }
    }
}

@Composable
fun ProgressIndicatorSection(currentStep: Int, totalSteps: Int, progress: Float) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Langkah $currentStep dari $totalSteps",
                color = textColorWhite.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                color = textColorWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = Color.White,
            trackColor = Color.White.copy(alpha = 0.3f),
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
fun GuideSectionCard(section: GuideSection) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = secondary)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                painter = painterResource(id = section.iconResId),
                contentDescription = null,
                tint = textColorWhite.copy(alpha = 0.8f),
                modifier = Modifier
                    .size(24.dp)
                    .padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = section.title,
                    color = textColorWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = section.content,
                    color = textColorWhite.copy(alpha = 0.7f),
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
fun UserGuideScreenPreview() {
    ArhyBeTheme {
        UserGuideScreen(navController = NavController(LocalContext.current))
    }
}
