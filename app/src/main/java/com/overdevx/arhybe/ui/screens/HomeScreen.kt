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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.dotlottie.dlplayer.Mode
import com.lottiefiles.dotlottie.core.compose.ui.DotLottieAnimation
import com.lottiefiles.dotlottie.core.util.DotLottieSource
import com.overdevx.arhybe.DiagnosisDetailDestination
import com.overdevx.arhybe.DiagnosisType
import com.overdevx.arhybe.R
import com.overdevx.arhybe.ui.components.DiagnosaComponent
import com.overdevx.arhybe.ui.components.StartComponent
import com.overdevx.arhybe.ui.theme.background
import com.overdevx.arhybe.ui.theme.secondary
import com.overdevx.arhybe.ui.theme.textColorBlack
import com.overdevx.arhybe.ui.theme.textColorGreen
import com.overdevx.arhybe.ui.theme.textColorRed
import com.overdevx.arhybe.ui.theme.textColorWhite
import com.overdevx.arhybe.ui.theme.textColorYellow
import com.overdevx.arhybe.viewmodel.BluetoothViewModelAdvance
import com.overdevx.arhybe.viewmodel.HomeViewModel
import com.overdevx.arhybe.viewmodel.TrackingPhase
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
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
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.shader.toShaderProvider

import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.Fill

private const val TYPE_ARRHYTHMIA = "arrhythmia"
private const val TYPE_STRESS = "stress"
@Composable
fun HomeScreen( navController: NavController,
                bluetoothViewModel: BluetoothViewModelAdvance,
                onRequestPermissions: () -> Unit) {

    val viewModel: HomeViewModel = hiltViewModel()
    val prediction by viewModel.predictionResult.collectAsState()
    val isWifiProvisioned by bluetoothViewModel.isWifiProvisioned.collectAsStateWithLifecycle()
    // State untuk mengontrol visibilitas bottom sheet
    var showBluetoothSheet by rememberSaveable { mutableStateOf(false) }
    val trackingPhase by viewModel.trackingPhase.collectAsStateWithLifecycle()


    Box(
        modifier = Modifier.fillMaxSize(),

        ) {
        Column {
            StartComponent(
               navController = navController,
                bluetoothViewModel = bluetoothViewModel,
                trackingPhase = trackingPhase,
                onStartClicked = {
                    if (isWifiProvisioned || trackingPhase != TrackingPhase.IDLE) {
                        // Jika wifi siap, atau jika kita sedang dalam proses (ingin menghentikan),
                        // panggil viewmodel.
                        viewModel.toggleTracking()
                    } else {
                        // Jika wifi belum siap dan kita ingin memulai, tampilkan sheet.
                        showBluetoothSheet = true
                    }
                }
            )
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


            if (prediction == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)

                        .clip(RoundedCornerShape(20.dp))
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
                                .padding(bottom = 16.dp)
                        )
                    }
                }

            } else {
                Spacer(modifier = Modifier.height(16.dp))
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

        if (showBluetoothSheet) {
            BluetoothConnectionBottomSheet(
                onDismiss = { showBluetoothSheet = false },
                viewModel= bluetoothViewModel,
                onRequestPermissions = onRequestPermissions
            )
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



@Composable
fun JetpackComposeBasicLineChart(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {


    val ecgBuffer by viewModel.ecgBuffer.collectAsState()
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(ecgBuffer) {
        if (ecgBuffer.isNotEmpty()) {
            modelProducer.runTransaction {
                lineSeries {
                    series(ecgBuffer.takeLast(3500))
                }
            }
        }
    }

    if (ecgBuffer.size < 100) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(secondary)
        ) {
            Text(
                text = "Tekan tombol play \nuntuk mulai melihat grafik",
                fontSize = 20.sp,
                color = textColorWhite,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    } else {
        CartesianChartHost(
            modelProducer = modelProducer,
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(secondary)
                .padding(16.dp),
            scrollState = rememberVicoScrollState(
                initialScroll = Scroll.Absolute.End,
                autoScrollCondition = AutoScrollCondition.OnModelGrowth
            ),
            chart = rememberCartesianChart(
                // --- Kustomisasi Layer Garis ---
                rememberLineCartesianLayer(
                    // Gunakan `LineProvider` untuk kustomisasi mendetail
                    lineProvider = LineCartesianLayer.LineProvider.series(
                        // `rememberLine` untuk mendefinisikan setiap garis
                        LineCartesianLayer.rememberLine(
                            // 1. Warna Garis
                            fill = LineCartesianLayer.LineFill.single(Fill(color = textColorRed.toArgb())),
                            // 2. Gradien di bawah garis
                            areaFill = LineCartesianLayer.AreaFill.single(
                                fill = Fill(color = textColorRed.copy(alpha = 0.1f).toArgb())
                            ),
                        )
                    )
                ),
                // --- Kustomisasi Sumbu dengan API v2.3.1 ---
                startAxis = VerticalAxis.rememberStart(),
                bottomAxis = HorizontalAxis.rememberBottom()
            )
        )

    }
}


//@Composable
//fun JetpackComposeBasicLineChart2(
//    modifier: Modifier = Modifier,
//    viewModel: HomeViewModel = hiltViewModel() // Diasumsikan
//) {
//
//    val maxDisplaySamples = 3500
//    val ecgBuffer by viewModel.ecgBuffer.collectAsState()
//    val modelProducer = remember { CartesianChartModelProducer() }
//
//    // Efek untuk mengupdate chart setiap kali ecgBuffer berubah
//    LaunchedEffect(ecgBuffer) {
//        if (ecgBuffer.isNotEmpty()) {
//            modelProducer.runTransaction {
//                val visiblePoints = ecgBuffer.takeLast(maxDisplaySamples)
//                lineSeries {
//                    series(visiblePoints)
//                }
//            }
//        }
//    }
//
//    if (ecgBuffer.size < 100) {
//        // Placeholder saat data belum cukup
//        Box(
//            modifier = modifier
//                .padding(top = 16.dp)
//                .fillMaxWidth()
//                .height(200.dp)
//                .clip(RoundedCornerShape(20.dp))
//                .background(secondary) // Ganti dengan `secondary` Anda
//        ) {
//            Text(
//                text = "Tekan tombol play \nuntuk mulai melihat grafik",
//                fontSize = 20.sp,
//                // fontFamily = FontFamily(listOf(Font(R.font.sofia_semibold))), // Diasumsikan
//                color = textColorWhite, // Ganti dengan `textColorWhite` Anda
//                textAlign = TextAlign.Center,
//                modifier = Modifier.align(Alignment.Center)
//            )
//        }
//    } else {
//        // Tampilkan chart jika data sudah ada
//        CartesianChartHost(
//            chart = rememberCartesianChart(
//                // --- FIX: Kustomisasi Layer Garis ---
//                rememberLineCartesianLayer(
//                    // Gunakan LineProvider untuk mendefinisikan setiap garis.
//                    lineProvider = LineCartesianLayer.LineProvider.series(
//                        // Buat definisi garis dengan `rememberLine`.
//                        rememberLine(
//                            // 1. Atur warna garis melalui `fill` di dalam `Line`.
//                            fill = LineCartesianLayer.LineFill.single(Fill.Black), // Ganti dengan `textColorGreen` Anda
//                            // 2. Efek gradien di bawah garis didefinisikan di `areaFill`.
//                            areaFill = LineCartesianLayer.AreaFill.single(
//                                fill = Fill(
//                                    brush = Brush.verticalGradient(
//                                        listOf(
//                                            Color.Green.copy(alpha = 0.4f), // Ganti dengan `textColorGreen` Anda
//                                            Color.Green.copy(alpha = 0.0f)  // Ganti dengan `textColorGreen` Anda
//                                        )
//                                    ).toShaderProvider()
//                                )
//                            ),
//                        )
//                    )
//                ),
//                // --- FIX: Kustomisasi Sumbu (Axis) ---
//                startAxis = rememberStartAxis(
//                    label = rememberTextComponent(color = Color.White), // Ganti dengan `textColorWhite` Anda
//                    axis = rememberLineComponent(color = Color.DarkGray), // Ganti dengan `secondary` Anda
//                    tick = rememberLineComponent(color = Color.DarkGray), // Ganti dengan `secondary` Anda
//                    guideline = null, // Sembunyikan garis panduan vertikal
//                ),
//                bottomAxis = rememberBottomAxis(
//                    label = rememberTextComponent(color = Color.White), // Ganti dengan `textColorWhite` Anda
//                    axis = rememberLineComponent(color = Color.DarkGray), // Ganti dengan `secondary` Anda
//                    tick = rememberLineComponent(color = Color.DarkGray), // Ganti dengan `secondary` Anda
//                    // Garis panduan horizontal yang lebih halus
//                    guideline = rememberLineComponent(
//                        color = Color.DarkGray, // Ganti dengan `secondary` Anda
//                        thickness = 1.dp
//                    )
//                ),
//            ),
//            // Mengatur state scroll agar chart otomatis bergerak ke data terbaru
//            scrollState = rememberVicoScrollState(
//                initialScroll = Scroll.Absolute.End,
//                autoScrollCondition = AutoScrollCondition.OnModelGrowth
//            ),
//            modelProducer = modelProducer,
//            modifier = modifier
//                .fillMaxWidth()
//                .height(200.dp)
//                .clip(RoundedCornerShape(20.dp))
//                .background(Color.DarkGray) // Ganti dengan `secondary` Anda
//                .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)
//        )
//    }
//}



