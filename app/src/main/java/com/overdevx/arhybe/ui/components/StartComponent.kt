package com.overdevx.arhybe.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.overdevx.arhybe.viewmodel.HomeViewModel

@Composable
fun StartComponent(modifier: Modifier = Modifier) {
    val viewModel: HomeViewModel = hiltViewModel()
    val isTracking by viewModel.isTracking.collectAsState()
    val remainingTime by viewModel.remainingTime.collectAsState()

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
                Text(
                    text = if (isTracking) {
                        // Format mm:ss
                        val minutes = remainingTime / 60
                        val seconds = remainingTime % 60
                        String.format("Sisa Waktu: %02d:%02d", minutes, seconds)
                    } else {
                        "Tracking data ~5 Menit"
                    },
                    fontSize = 16.sp,
                    fontFamily = FontFamily(listOf(Font(R.font.sofia_regular))),
                    color = textColorBlack
                )
            }
            Spacer(modifier = Modifier.weight(1f))

            Crossfade(targetState = isTracking, label = "Crossfade") { tracking ->
                if (tracking) {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .align(Alignment.CenterVertically)
                            .clickable {
                                viewModel.toggleTracking()
                            }
                    ) {
                        CustomCircularProgressBar(
                            progress = (remainingTime / (5 * 60f)),
                            color = textColorRed,
                            trackColor = textColorBlack,
                            modifier = Modifier.size(70.dp)
                        )
                    }
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_mulai),
                        contentDescription = null,
                        modifier = Modifier
                            .size(70.dp)
                            .align(Alignment.CenterVertically)
                            .padding(end = 16.dp)
                            .clickable {
                                viewModel.toggleTracking()
                            }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun StartComponentPreview(modifier: Modifier = Modifier) {
    StartComponent(Modifier.background(textColorWhite))
}