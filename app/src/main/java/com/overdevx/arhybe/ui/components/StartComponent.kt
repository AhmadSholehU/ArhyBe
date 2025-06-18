package com.overdevx.arhybe.ui.components

import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.overdevx.arhybe.R
import com.overdevx.arhybe.ui.theme.textColorBlack
import com.overdevx.arhybe.ui.theme.textColorRed
import com.overdevx.arhybe.ui.theme.textColorWhite
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.dotlottie.dlplayer.Mode
import com.overdevx.arhybe.viewmodel.HomeViewModel
import com.lottiefiles.dotlottie.core.compose.ui.DotLottieAnimation
import com.lottiefiles.dotlottie.core.util.DotLottieSource
import com.overdevx.arhybe.BluetoothDestination
import com.overdevx.arhybe.ui.theme.primary
import com.overdevx.arhybe.ui.theme.secondary
import com.overdevx.arhybe.viewmodel.BluetoothViewModelAdvance
import com.overdevx.arhybe.viewmodel.TrackingPhase

@Composable
fun StartComponent(
    modifier: Modifier = Modifier,
    navController: NavController,
    trackingPhase: TrackingPhase,
    bluetoothViewModel: BluetoothViewModelAdvance,
    onStartClicked: () -> Unit
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val isTracking by viewModel.isTracking.collectAsState()
    // --- MODIFIKASI: Ambil state fase pelacakan ---
    val trackingPhase by viewModel.trackingPhase.collectAsStateWithLifecycle()

    val ecgStatus by viewModel.ecgStatus.collectAsState()
    val isWifiProvisioned by bluetoothViewModel.isWifiProvisioned.collectAsStateWithLifecycle() // Ambil status provisioning

    // --- Menentukan teks utama dan subteks berdasarkan fase ---
    val mainText = when (trackingPhase) {
        TrackingPhase.IDLE -> "Mulai Tracking"
        TrackingPhase.TRACKING -> "Tracking Berjalan"
        TrackingPhase.PROCESSING -> "Memproses Data..."
    }

    val subText = when (trackingPhase) {
        TrackingPhase.IDLE -> "Perekaman data ~5 menit"
        TrackingPhase.TRACKING, TrackingPhase.PROCESSING -> {
            if (ecgStatus != null && !ecgStatus!!.ready) {
                val duration = ecgStatus!!.durationSec
                val minutes = (duration / 60).toInt()
                val seconds = (duration % 60).toInt()
                val durText = String.format("%02d:%02d", minutes, seconds)
                "Mengumpulkan data: $durText"
            } else if (ecgStatus?.ready == true) {
                "Siap diproses"
            }
            else {
                "Menginisialisasi..."
            }
        }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .padding(20.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(primary)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Kolom teks di sebelah kiri
            Column(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1f) // Beri bobot agar bisa melebar
            ) {
                Text(
                    text = mainText,
                    fontSize = 26.sp,
                    fontFamily = FontFamily(listOf(Font(R.font.sofia_bold))),
                    color = textColorWhite
                )
                Text(
                    text = subText,
                    fontSize = 16.sp,
                    fontFamily = FontFamily(listOf(Font(R.font.sofia_semibold))),
                    color = textColorWhite
                )
            }

            // Spacer tidak lagi diperlukan jika menggunakan weight pada Column

            // Tombol di sebelah kanan
            Crossfade(
                targetState = trackingPhase,
                label = "TrackingButtonCrossfade",
                modifier = Modifier.align(Alignment.CenterVertically)
            ) { phase ->
                Box(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (phase) {
                        TrackingPhase.IDLE -> {
                            Image(
                                painter = painterResource(id = R.drawable.ic_mulai),
                                contentDescription = "Mulai Tracking",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable {
                                        if (isWifiProvisioned) { // Cek status provisioning
                                            Log.d(
                                                "StartComponent",
                                                "WiFi sudah terkonfigurasi. Memulai tracking."
                                            )
                                            viewModel.toggleTracking() // Aksi untuk start jika sudah provisioned
                                        } else {
                                            Log.d(
                                                "StartComponent",
                                                "WiFi belum terkonfigurasi. Navigasi ke BluetoothScreen."
                                            )
                                            onStartClicked()
                                        }
                                    }
                            )
                        }
                        TrackingPhase.TRACKING -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .border(1.dp, secondary, CircleShape)
                                    .background(textColorWhite)
                                    .clickable { onStartClicked() },
                                contentAlignment = Alignment.Center
                            ) {
                                DotLottieAnimation(
                                    source = DotLottieSource.Asset("waitingdata.lottie"),
                                    autoplay = true, loop = true, speed = 1f,
                                    modifier = Modifier.size(50.dp)
                                )
                            }
                        }
                        TrackingPhase.PROCESSING -> {
                            // Tampilkan indikator loading dan non-aktifkan klik
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(textColorWhite.copy(alpha = 0.5f)), // Sedikit transparan
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(30.dp),
                                    color = primary,
                                    strokeWidth = 3.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun StartComponentPreview(modifier: Modifier = Modifier) {
//    StartComponent(Modifier.background(textColorWhite))
}