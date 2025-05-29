package com.overdevx.arhybe.ui.components

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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.overdevx.arhybe.R
import com.overdevx.arhybe.ui.theme.background
import com.overdevx.arhybe.ui.theme.textColorBlack
import com.overdevx.arhybe.ui.theme.textColorGreen
import com.overdevx.arhybe.ui.theme.textColorRed
import com.overdevx.arhybe.ui.theme.textColorWhite

@Composable
fun StartComponent(modifier: Modifier = Modifier) {
    // State untuk toggle tampilan; false: tampil image; true: tampil progress bar
    val isTracking = remember { mutableStateOf(false) }

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
                    text = if (isTracking.value) "Tracking Berjalan" else "Mulai Tracking",
                    fontSize = 26.sp,
                    fontFamily = FontFamily(listOf(Font(R.font.sofia_bold))),
                    color = textColorBlack
                )
                Text(
                    text = if (isTracking.value) "Sisa Waktu: 03:45" else "Tracking data ~5 Menit",
                    fontSize = 16.sp,
                    fontFamily = FontFamily(listOf(Font(R.font.sofia_regular))),
                    color = textColorBlack
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            // Bagian kanan: tampil image atau circular progress bar, dengan aksi toggle
            if (isTracking.value) {
                Box(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .align(Alignment.CenterVertically)
                        .clickable { isTracking.value = false } // Klik progress -> kembali ke image awal
                ) {
                    CustomCircularProgressBar(
                        progress = 0.2f, // Misalnya progres 75%
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
                        .clickable { isTracking.value = true } // Klik image -> tampil progress bar
                )
            }
        }
    }
}
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun StartComponentPreview(modifier: Modifier = Modifier) {
    StartComponent(Modifier.background(textColorWhite))
}