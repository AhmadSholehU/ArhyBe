package com.overdevx.arhybe.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.overdevx.arhybe.DIAGNOSIS_TYPE_ARRHYTHMIA
import com.overdevx.arhybe.DIAGNOSIS_TYPE_STRESS
import com.overdevx.arhybe.DiagnosisDetailDestination
import com.overdevx.arhybe.DiagnosisType
import com.overdevx.arhybe.R
import com.overdevx.arhybe.ui.theme.background
import com.overdevx.arhybe.ui.theme.primary
import com.overdevx.arhybe.ui.theme.secondary
import com.overdevx.arhybe.ui.theme.textColorBlack
import com.overdevx.arhybe.ui.theme.textColorGreen
import com.overdevx.arhybe.ui.theme.textColorRed
import com.overdevx.arhybe.ui.theme.textColorWhite
import com.overdevx.arhybe.ui.theme.textColorYellow
import kotlinx.serialization.json.Json
import java.util.Locale
import kotlin.text.replaceFirstChar
import kotlin.text.toList
import kotlin.text.uppercase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosisDetailScreen(
    navArgs: DiagnosisDetailDestination,
    onNavigateUp: () -> Unit
) {
    val jsonParser = remember { Json { ignoreUnknownKeys = true } }

    val topBarTitle = when (navArgs.diagnosisTypeString) {
        DIAGNOSIS_TYPE_ARRHYTHMIA -> "Informasi Jenis Aritmia"
        DIAGNOSIS_TYPE_STRESS -> "Informasi Level Kecemasan"
        else -> "Detail Diagnosa"
    }

    // Menggunakan Column sebagai root untuk menempatkan app bar kustom di atas LazyColumn
    Column(modifier = Modifier.fillMaxSize().background(background)) {
        // --- FIX: Mengganti TopAppBar dengan Row Kustom ---
        CustomTopAppBar(
            title = topBarTitle,
            onNavigateUp = onNavigateUp)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal=16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) } // Spacer pengganti padding atas
            when (navArgs.diagnosisTypeString) {
                DIAGNOSIS_TYPE_ARRHYTHMIA -> {

                    // --- Sesi Hasil Prediksi Aritmia ---
                    item {
                        ResultSectionHeader(
                            iconResId = R.drawable.ic_aritmia,
                            title = "Hasil prediksi jenis Aritmia"
                        )
                    }
                    navArgs.arrhythmiaProbabilitiesJson?.let { jsonString ->
                        try {
                            val probabilities = jsonParser.decodeFromString<Map<String, Float>>(jsonString)
                            items(probabilities.toList().sortedByDescending { it.second }) { (type, probability) ->
                                ResultRow(
                                    label = type.formatLabel(),
                                    value = probability,
                                    isPercentage = true
                                )
                            }
                        } catch (e: Exception) {
                            item { Text("Gagal memuat probabilitas aritmia: ${e.localizedMessage}", color = textColorWhite) }
                        }
                    }

                    // Spacer
                    item { Spacer(Modifier.height(16.dp)) }

                    // --- Sesi Distribusi Detak Jantung ---
                    item {
                        ResultSectionHeader(
                            iconResId = R.drawable.ic_beat,
                            title = "Distribusi jenis detak jantung"
                        )
                    }
                    navArgs.beatDistributionJson?.let { jsonString ->
                        try {
                            val distribution = jsonParser.decodeFromString<Map<String, Int>>(jsonString)
                            val totalBeats = distribution.values.sum().toFloat().coerceAtLeast(1f)
                            items(distribution.toList().sortedByDescending { it.second }) { (type, count) ->
                                ResultRow(
                                    label = type.formatLabel(),
                                    value = count / totalBeats, // Convert count to percentage
                                    isPercentage = true,
                                    absoluteValueText = "$count" // Show absolute count as well
                                )
                            }
                        } catch (e: Exception) {
                            item { Text("Gagal memuat distribusi beat: ${e.localizedMessage}", color = textColorWhite) }
                        }
                    }
                }

                DIAGNOSIS_TYPE_STRESS -> {
                    item {
                        ResultSectionHeader(
                            iconResId = R.drawable.ic_cemas,
                            title = "Hasil prediksi level kecemasan"
                        )
                    }
                    navArgs.stressProbabilitiesJson?.let { jsonString ->
                        try {
                            val probabilities = jsonParser.decodeFromString<Map<String, Float>>(jsonString)
                            items(probabilities.toList().sortedByDescending { it.second }) { (level, probability) ->
                                ResultRow(
                                    label = level.formatLabel(),
                                    value = probability,
                                    isPercentage = true
                                )
                            }
                        } catch (e: Exception) {
                            item { Text("Gagal memuat probabilitas stres: ${e.localizedMessage}", color = textColorWhite) }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ResultSectionHeader(iconResId: Int, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(textColorWhite)
        ) {
            Image(
                painter = painterResource(id = iconResId),
                contentDescription = null,
                modifier = Modifier
                    .size(35.dp)
                    .align(Alignment.Center)
            )
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text = title,
            fontFamily = FontFamily(listOf(Font(R.font.sofia_semibold))),
            fontSize = 18.sp,
            color = textColorWhite
        )
    }
}

@Composable
private fun ResultRow(
    label: String,
    value: Float, // Nilai progress (antara 0.0 dan 1.0)
    isPercentage: Boolean,
    absoluteValueText: String? = null // Nilai absolut opsional untuk ditampilkan (misal: jumlah beat)
) {
    val percentage = (value * 100).toInt()
    val displayValue = if (isPercentage) "$percentage%" else absoluteValueText ?: ""

    val progressColor = when {
        label.contains("Normal", ignoreCase = true) -> textColorGreen
        label.contains("Low", ignoreCase = true) -> textColorGreen
        label.contains("Medium", ignoreCase = true) -> textColorYellow
        label.contains("(N", ignoreCase = true) -> textColorGreen
        else -> textColorRed
    }
Box(modifier = Modifier
    .background(color = secondary)
    .padding(16.dp)
    ) {
    Column(
        modifier = Modifier.padding(16.dp)

    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontFamily = FontFamily(listOf(Font(R.font.sofia_medium))),
                fontSize = 16.sp,
                color = textColorWhite,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = displayValue,
                fontFamily = FontFamily(listOf(Font(R.font.sofia_semibold))),
                fontSize = 16.sp,
                color = progressColor
            )
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { value },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = progressColor,
            trackColor = textColorWhite,
            strokeCap = StrokeCap.Round
        )
    }
}
}

// Helper function to format labels
private fun String.formatLabel(): String {
    return this.replace("(", " ").replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
}

// Composable baru untuk Top App Bar kustom
@Composable
fun CustomTopAppBar(title: String, onNavigateUp: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp) // Mengatur tinggi secara eksplisit (lebih kecil dari 64.dp)
            .background(primary) // Latar belakang putih
            .padding(horizontal = 4.dp), // Padding untuk ikon dan judul
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateUp) {
            Icon(
                painter = painterResource(R.drawable.ic_back),
                contentDescription = "Kembali",
                tint = textColorWhite
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = textColorWhite,
            fontFamily = FontFamily(listOf(Font(R.font.sofia_semibold))),
            fontSize = 20.sp // Sedikit memperbesar teks agar pas
        )
    }
}
@Preview(showSystemUi = true)
@Composable
fun ListPreview(modifier: Modifier = Modifier) {
    CustomTopAppBar("Informasi Jenis Aritmia") { }
}