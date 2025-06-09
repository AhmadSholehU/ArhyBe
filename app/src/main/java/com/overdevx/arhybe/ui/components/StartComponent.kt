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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.overdevx.arhybe.viewmodel.BluetoothViewModel
import com.overdevx.arhybe.viewmodel.BluetoothViewModelAdvance

@Composable
fun StartComponent(modifier: Modifier = Modifier, navController: NavController, bluetoothViewModel: BluetoothViewModelAdvance, ) {
    val viewModel: HomeViewModel = hiltViewModel()
    val isTracking by viewModel.isTracking.collectAsState()
    val ecgStatus by viewModel.ecgStatus.collectAsState()
    val isWifiProvisioned by bluetoothViewModel.isWifiProvisioned.collectAsStateWithLifecycle() // Ambil status provisioning

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .padding(20.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(textColorWhite)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Kolom teks di sebelah kiri
            Column(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = if (isTracking) "Tracking Berjalan" else "Mulai Tracking",
                    fontSize = 26.sp,
                    fontFamily = FontFamily(listOf(Font(R.font.sofia_bold))),
                    color = textColorBlack
                )
                if (isTracking) {
                    if (ecgStatus != null) {
                        if (ecgStatus!!.ready) {
                            Text(
                                text = "Siap diproses",
                                fontSize = 16.sp,
                                fontFamily = FontFamily(listOf(Font(R.font.sofia_semibold))),
                                color = textColorBlack
                            )
                        } else {
                            // Tampilkan durasi saat ini
                            val duration = ecgStatus!!.durationSec
                            // Ubah durasi menjadi mm:ss
                            val minutes = (duration / 60).toInt()
                            val seconds = (duration % 60).toInt()
                            val durText = String.format("%02d:%02d", minutes, seconds)

                            Text(
                                text = "Mengumpulkan data: $durText",
                                fontSize = 16.sp,
                                fontFamily = FontFamily(listOf(Font(R.font.sofia_semibold))),
                                color = textColorBlack
                            )
                        }
                    } else {
                        // Belum ada data status pertama kali
                        Text(
                            text = "Mengumpulkan data…",
                            fontSize = 16.sp,
                            fontFamily = FontFamily(listOf(Font(R.font.sofia_semibold))),
                            color = textColorBlack
                        )
                    }
                } else {
                    Text(
                        text = "Tracking data ~5 Menit",
                        fontSize = 16.sp,
                        fontFamily = FontFamily(listOf(Font(R.font.sofia_semibold))),
                        color = textColorBlack
                    )
                }

            }

            Spacer(modifier = Modifier.weight(1f))

            Crossfade(targetState = isTracking, label = "Crossfade") { tracking ->
                Box(modifier = Modifier.fillMaxHeight()) {
                    if (tracking) {
                        Box(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(60.dp)
                                .align(Alignment.Center)
                                .clip(CircleShape)
                                .border(1.dp, textColorBlack, CircleShape)
                                .clickable {
                                    viewModel.toggleTracking()
                                }
                        ) {
                            DotLottieAnimation(
                                source = DotLottieSource.Asset("waitingdata.lottie"),
                                autoplay = true,
                                loop = true,
                                speed = 1f,
                                useFrameInterpolation = false,
                                playMode = Mode.FORWARD,
                                modifier = Modifier.size(60.dp)
                                    .align(Alignment.Center)
                            )
                        }
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.ic_mulai),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(60.dp)
                                .align(Alignment.Center)
                                .clickable {
                                    if (isWifiProvisioned) { // Cek status provisioning
                                        Log.d("StartComponent", "WiFi sudah terkonfigurasi. Memulai tracking.")
                                        viewModel.toggleTracking() // Aksi untuk start jika sudah provisioned
                                    } else {
                                        Log.d("StartComponent", "WiFi belum terkonfigurasi. Navigasi ke BluetoothScreen.")
                                        navController.navigate(BluetoothDestination) // Arahkan ke screen provisioning
                                    }
                                }
                        )
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