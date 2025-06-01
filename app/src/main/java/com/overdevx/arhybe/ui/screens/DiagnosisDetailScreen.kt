package com.overdevx.arhybe.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.overdevx.arhybe.DIAGNOSIS_TYPE_ARRHYTHMIA
import com.overdevx.arhybe.DIAGNOSIS_TYPE_STRESS
import com.overdevx.arhybe.DiagnosisDetailDestination
import com.overdevx.arhybe.DiagnosisType
import kotlinx.serialization.json.Json
import kotlin.text.replaceFirstChar
import kotlin.text.toList
import kotlin.text.uppercase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosisDetailScreen(
    navArgs: DiagnosisDetailDestination,
    onNavigateUp: () -> Unit
) {
    // Helper untuk deserialize, bisa diletakkan di luar Composable atau di ViewModel jika lebih kompleks
    // 'remember' digunakan agar Json parser tidak dibuat ulang pada setiap recomposition
    val jsonParser = remember { Json { ignoreUnknownKeys = true } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (navArgs.diagnosisTypeString) {
                            DIAGNOSIS_TYPE_ARRHYTHMIA -> "Detail Aritmia & Beat"
                            DIAGNOSIS_TYPE_STRESS -> "Detail Level Kecemasan"
                            else -> "Detail Diagnosa"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Prediksi Utama: ${navArgs.overallMainPrediction ?: "N/A"}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        navArgs.overallMainProbability?.let {
                            Text(
                                text = "Probabilitas Utama: %.2f%%".format(it * 100),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }

            when (navArgs.diagnosisTypeString) {
                DIAGNOSIS_TYPE_ARRHYTHMIA -> {
                    // Deserialize dan tampilkan probabilitas aritmia
                    navArgs.arrhythmiaProbabilitiesJson?.let { jsonString ->
                        try {
                            val probabilities = jsonParser.decodeFromString<Map<String, Float>>(jsonString)
                            item {
                                Text(
                                    "Semua Probabilitas Aritmia:",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }
                            items(probabilities.toList()) { (type, probability) ->
                                ProbabilityRow(label = type, probability = probability)
                            }
                            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
                        } catch (e: Exception) {
                            item { Text("Gagal memuat probabilitas aritmia: ${e.localizedMessage}") }
                        }
                    }

                    // Deserialize dan tampilkan distribusi beat
                    navArgs.beatDistributionJson?.let { jsonString ->
                        try {
                            val distribution = jsonParser.decodeFromString<Map<String, Int>>(jsonString)
                            item {
                                Text(
                                    "Distribusi Detak Jantung (Beat):",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }
                            items(distribution.toList()) { (type, count) ->
                                BeatRow(label = type, count = count)
                            }
                        } catch (e: Exception) {
                            item { Text("Gagal memuat distribusi beat: ${e.localizedMessage}") }
                        }
                    }
                }

                DIAGNOSIS_TYPE_STRESS -> {
                    // Deserialize dan tampilkan probabilitas stres
                    navArgs.stressProbabilitiesJson?.let { jsonString ->
                        try {
                            val probabilities = jsonParser.decodeFromString<Map<String, Float>>(jsonString)
                            item {
                                Text(
                                    "Semua Probabilitas Level Kecemasan:",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }
                            items(probabilities.toList()) { (level, probability) ->
                                ProbabilityRow(label = level.replaceFirstChar { it.uppercase() }, probability = probability)
                            }
                        } catch (e: Exception) {
                            item { Text("Gagal memuat probabilitas stres: ${e.localizedMessage}") }
                        }
                    }
                    // Anda juga bisa menampilkan navArgs.stressNumericPrediction dan navArgs.stressLevelPrediction jika perlu
                    navArgs.stressLevelPrediction?.let {
                        item {
                            Text(
                                "Level Prediksi: ${it.replaceFirstChar { char -> char.uppercase() }}",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp)) // Sedikit ruang di bagian bawah
            }
        }
    }
}

// Composable ProbabilityRow dan BeatRow tetap sama seperti sebelumnya
@Composable
fun ProbabilityRow(label: String, probability: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = "%.2f%%".format(probability * 100),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun BeatRow(label: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = "Tipe Beat: $label", style = MaterialTheme.typography.bodyLarge)
        Text(
            text = "Jumlah: $count",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}