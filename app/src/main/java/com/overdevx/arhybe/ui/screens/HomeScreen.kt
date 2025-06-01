package com.overdevx.arhybe.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.dotlottie.dlplayer.Mode
import com.lottiefiles.dotlottie.core.compose.ui.DotLottieAnimation
import com.lottiefiles.dotlottie.core.util.DotLottieSource
import com.overdevx.arhybe.DiagnosisDetailDestination
import com.overdevx.arhybe.DiagnosisType
import com.overdevx.arhybe.R
import com.overdevx.arhybe.ui.components.DiagnosaComponent
import com.overdevx.arhybe.ui.components.StartComponent
import com.overdevx.arhybe.ui.theme.textColorBlack
import com.overdevx.arhybe.ui.theme.textColorGreen
import com.overdevx.arhybe.ui.theme.textColorRed
import com.overdevx.arhybe.ui.theme.textColorWhite
import com.overdevx.arhybe.ui.theme.textColorYellow
import com.overdevx.arhybe.viewmodel.HomeViewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.core.cartesian.AutoScrollCondition
import com.patrykandpatrick.vico.core.cartesian.Scroll
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TYPE_ARRHYTHMIA = "arrhythmia"
private const val TYPE_STRESS = "stress"
@Composable
fun HomeScreen( navController: NavController) {

    val viewModel: HomeViewModel = hiltViewModel()
    val prediction by viewModel.predictionResult.collectAsState()
    Box(
        modifier = Modifier.fillMaxSize(),

        ) {
        Column {
            StartComponent()
            Text(

                text = "Visualisai Sinyal ECG",
                fontSize = 20.sp,
                fontFamily = FontFamily(listOf(Font(R.font.sofia_semibold))),
                color = textColorWhite,
                modifier = Modifier
                    .padding(start = 16.dp)
            )
            JetpackComposeBasicLineChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top=16.dp,start = 16.dp, end = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Hasil Diagnosa",
                fontSize = 20.sp,
                fontFamily = FontFamily(listOf(Font(R.font.sofia_semibold))),
                color = textColorWhite,
                modifier = Modifier.padding(start = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (prediction == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(textColorWhite)
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                    ) {
                        DotLottieAnimation(
                            source = DotLottieSource.Asset("waitingprocess.lottie"),
                            autoplay = true,
                            loop = true,
                            speed = 1f,
                            useFrameInterpolation = false,
                            playMode = Mode.FORWARD,
                            modifier = Modifier.size(130.dp)
                        )

                        Text(
                            text = "Menunggu Data...",
                            fontSize = 20.sp,
                            fontFamily = FontFamily(listOf(Font(R.font.sofia_semibold))),
                            color = textColorBlack,
                            modifier = Modifier
                        )
                    }
                }

            } else {
                // Tampilkan arrhythmia
                prediction?.arrhythmia?.let { arr ->
                    val currentBeatData = prediction?.beat
                    val arrhythmiaProbJson = Json.encodeToString(arr.probabilities)
                    val beatDistJson = currentBeatData?.let { Json.encodeToString(it.distribution) }
                    DiagnosaComponent(
                        title = "Jenis Aritmia",
                        subtitle = arr.prediction,
                        imageResId = R.drawable.ic_aritmia,
                        progress = arr.probabilities[arr.prediction] ?: 0f,
                        subtitleColor = if (arr.prediction == "Normal") textColorGreen else textColorRed,
                        onClick = {
                            // Navigasi ke Detail Screen untuk Aritmia
                            navController.navigate(
                                DiagnosisDetailDestination(
                                    diagnosisTypeString = TYPE_ARRHYTHMIA,
                                    arrhythmiaMainPrediction = arr.prediction,
                                    arrhythmiaProbabilitiesJson = arrhythmiaProbJson,
                                    beatDistributionJson = beatDistJson,
                                    // Set field stress ke null
                                    stressNumericPrediction = 0,
                                    stressLevelPrediction = null,
                                    stressProbabilitiesJson = null,
                                    overallMainPrediction = arr.prediction,
                                    overallMainProbability = arr.probabilities[arr.prediction] ?: 0f)
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tampilkan stress
                prediction?.stress?.let { st ->
                    val stressProbJson = Json.encodeToString(st.probabilities)
                    DiagnosaComponent(
                        title = "Level Kecemasan",
                        subtitle = st.level,
                        progress = st.probabilities[st.level] ?: 0f,
                        imageResId = R.drawable.ic_cemas,
                        subtitleColor = when (st.level) {
                            "low" -> textColorGreen
                            "medium" -> textColorYellow
                            else -> textColorRed
                        },
                        onClick = {
                            navController.navigate(
                                DiagnosisDetailDestination(
                                    diagnosisTypeString = TYPE_STRESS,
                                    // Set field arrhythmia & beat ke null
                                    arrhythmiaMainPrediction = null,
                                    arrhythmiaProbabilitiesJson = null,
                                    beatDistributionJson = null,
                                    stressNumericPrediction = st.prediction,
                                    stressLevelPrediction = st.level,
                                    stressProbabilitiesJson = stressProbJson,
                                    overallMainPrediction = st.level,
                                    overallMainProbability = st.probabilities[st.level] ?: 0f )
                            )
                        }
                    )
                }
            }
        }
    }
}

/**
 * 1) Versi private: menerima modelProducer, tidak diubah dari semula
 */
@Composable
private fun JetpackComposeBasicLineChart(
    modelProducer: CartesianChartModelProducer,
    modifier: Modifier = Modifier,
) {
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(),
        ),
        modelProducer = modelProducer,
        modifier = modifier
    )
}

/**
 * 2) Versi publik: observe ecgBuffer di ViewModel, lalu setiap kali berubah,
 *    jalankan runTransaction { lineSeries { series(...) } } agar data chart ter‐update.
 */

@Composable
fun JetpackComposeBasicLineChart(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    // Sampling rate yang sama seperti di ESP32 (700 Hz)
    val samplingRate = 700f
    // Jumlah sampel yang ingin ditampilkan (~5 detik × 700 Hz = 3500)
    val maxDisplaySamples = 3500
    val ecgBuffer by viewModel.ecgBuffer.collectAsState()

    val modelProducer = remember { CartesianChartModelProducer() }

    // Gunakan efek untuk mengupdate chart setiap kali ecgBuffer berubah
    LaunchedEffect(ecgBuffer) {
        if (ecgBuffer.size > 100) {
            modelProducer.runTransaction {
                // Ambil maksimum 500 data terakhir (misalnya)
                val visiblePoints = ecgBuffer.takeLast(maxDisplaySamples)

                // Reset data dan isi dengan data baru
                lineSeries {
                    series(visiblePoints)
                }
            }
        }
    }
    if (ecgBuffer.size < 100) {
        Box(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .height(200.dp)
                .background(textColorBlack)
        ) {
            Text(
                text = "Tekan tombol play \n" +
                        "untuk mulai melihat grafik",
                fontSize = 20.sp,
                fontFamily = FontFamily(listOf(Font(R.font.sofia_semibold))),
                color = textColorWhite,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
            )
        }
    } else {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(),
                startAxis = VerticalAxis.rememberStart(),
                bottomAxis = HorizontalAxis.rememberBottom(),
            ),
            scrollState = rememberVicoScrollState(
                initialScroll = Scroll.Absolute.End,
                autoScrollCondition = AutoScrollCondition.OnModelGrowth,
                scrollEnabled = true
            ),
            modelProducer = modelProducer,
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
        )
    }
}



