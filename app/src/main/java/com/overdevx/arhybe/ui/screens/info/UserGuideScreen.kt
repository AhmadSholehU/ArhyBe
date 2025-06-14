package com.overdevx.arhybe.ui.screens.info

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.overdevx.arhybe.R
import com.overdevx.arhybe.ui.screens.CustomTopAppBar
import com.overdevx.arhybe.ui.theme.primary
import com.overdevx.arhybe.ui.theme.textColorBlack2
import com.overdevx.arhybe.ui.theme.textColorWhite


// --- Data Structures for Guide Pages ---
data class GuidePage(
    val pageTitle: String,
    val pageDescription: String,
    val sections: List<GuideSection>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserGuideScreen(navController: NavController) {
    // --- State Management ---
    // Daftar semua halaman panduan
    val allGuidePages = remember {
        listOf(
            // Halaman 1: Pengenalan
            GuidePage(
                pageTitle = "Bagaimana Menggunakan Aplikasi ARhyBe",
                pageDescription = "Lorem ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s.",
                sections = listOf(
                    GuideSection(
                        title = "Bagian 1: Pengenalan",
                        content = "Selamat datang di ARhyBe. Aplikasi ini dirancang untuk membantu Anda memantau kesehatan jantung dengan mudah dan intuitif.",
                        iconResId = R.drawable.ic_headercard
                    )
                )
            ),
            // Halaman 2: Fitur Utama
            GuidePage(
                pageTitle = "Mengenal Fitur Utama",
                pageDescription = "Pelajari fungsi-fungsi utama yang akan sering Anda gunakan untuk mendapatkan hasil maksimal dari aplikasi ini.",
                sections = listOf(
                    GuideSection(
                        title = "Bagian 2: Fitur Utama",
                        content = "Fitur utama kami meliputi:\n1. Koneksi Perangkat Cerdas\n2. Visualisasi Sinyal Real-time\n3. Analisis dan Prediksi Cerdas",
                        iconResId = R.drawable.ic_headercard
                    )
                )
            ),
            // Halaman 3 (dan seterusnya...)
            GuidePage(
                pageTitle = "Memahami Hasil Anda",
                pageDescription = "Setelah data terkumpul, aplikasi akan memberikan hasil analisis. Berikut cara membacanya.",
                sections = listOf(
                    GuideSection(
                        title = "Bagian 3: Hasil Diagnosis",
                        content = "Hasil akan dibagi menjadi dua kategori utama: analisis Aritmia dan tingkat Kecemasan. Anda dapat melihat detail probabilitas untuk setiap kategori.",
                        iconResId = R.drawable.ic_beat
                    )
                )
            )
        )
    }
    val totalSteps = allGuidePages.size
    var currentStepIndex by remember { mutableStateOf(0) } // Gunakan index (0, 1, 2, ...)
    val progress by animateFloatAsState(targetValue = (currentStepIndex + 1).toFloat() / totalSteps, label = "progressAnimation")
    // State baru untuk mengontrol arah animasi
    var isNavigatingForward by remember { mutableStateOf(true) }
    Column (modifier = Modifier.fillMaxSize()
        ) {
        CustomTopAppBar(
            title = "Panduan Pengguna",
            onNavigateUp = { navController.popBackStack() }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            ProgressIndicatorSection(currentStep = currentStepIndex + 1, totalSteps = totalSteps, progress = progress)
            Spacer(modifier = Modifier.height(24.dp))

            // Animated Content Section
            AnimatedContent(
                targetState = currentStepIndex,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                },
                label = "GuidePageAnimation",
                modifier = Modifier.weight(1f)
            ) { targetIndex ->
                val page = allGuidePages[targetIndex]
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = page.pageTitle,
                        color = textColorWhite,
                        fontSize = 36.sp,
                        fontFamily = FontFamily(Font(R.font.sofia_semibold)),
                        lineHeight = 38.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = page.pageDescription,
                        color = textColorWhite.copy(alpha = 0.7f),
                        fontFamily = FontFamily(Font(R.font.sofia_regular)),
                        fontSize = 16.sp,
                        lineHeight = 24.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    page.sections.forEach { section ->
                        GuideSectionCard(section = section)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
            // --- FIX: Navigation Buttons Section ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tombol Kembali
                AnimatedVisibility(
                    visible = currentStepIndex > 0,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    OutlinedButton(
                        onClick = {
                            isNavigatingForward = false // Set arah animasi
                            currentStepIndex--
                        },
                        modifier = Modifier.height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF00695C))
                    ) {
                        Text("Kembali", color = Color(0xFF00695C), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Memberi ruang agar tombol Lanjut/Selesai tetap di kanan
                if (currentStepIndex == 0) {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Tombol Lanjut/Selesai
                Button(
                    onClick = {
                        if (currentStepIndex < totalSteps - 1) {
                            isNavigatingForward = true // Set arah animasi
                            currentStepIndex++
                        } else {
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C))
                ) {
                    Text(
                        text = if (currentStepIndex < totalSteps - 1) "Lanjut" else "Selesai",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(painter = painterResource(id = R.drawable.ic_next), contentDescription = "Lanjut")
                }
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
                color = textColorWhite,
                fontFamily = FontFamily(Font(R.font.sofia_semibold)),
                fontSize = 14.sp
            )
//            Text(
//                text = "${(progress * 100).toInt()}%",
//                color = textColorWhite,
//                fontSize = 14.sp,
//                fontFamily = FontFamily(Font(R.font.sofia_semibold)),
//            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = primary,
            trackColor = textColorWhite,
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
fun GuideSectionCard(section: GuideSection) {
    Card(
        modifier = Modifier.fillMaxWidth()
            .shadow(8.dp, shape = RoundedCornerShape(16.dp), clip = true, spotColor = primary, ambientColor = primary.copy(0.7f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, primary),
        colors = CardDefaults.cardColors(containerColor = textColorBlack2),

    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.Top
            ) {
                Image(
                    painter = painterResource(id = section.iconResId),
                    contentDescription = null,
                    modifier = Modifier
                        .size(30.dp)
                        .align(Alignment.CenterVertically)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = section.title,
                    color = textColorWhite,
                    fontSize = 18.sp,
                    fontFamily = FontFamily(Font(R.font.sofia_semibold)),
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = section.content,
                color = textColorWhite.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontFamily = FontFamily(Font(R.font.sofia_light)),
            )

        }
    }
}




// Data class untuk setiap bagian panduan
data class GuideSection(
    val title: String,
    val content: String,
    @DrawableRes val iconResId: Int
)



