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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.overdevx.arhybe.R
import com.overdevx.arhybe.ui.theme.secondary
import com.overdevx.arhybe.ui.theme.textColorBlack
import com.overdevx.arhybe.ui.theme.textColorGreen
import com.overdevx.arhybe.ui.theme.textColorWhite
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.Canvas
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults

@Composable
fun DiagnosaComponent(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    imageResId: Int,
    progress: Float,
    subtitleColor: Color,
   ) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(secondary)
            .height(100.dp)
    )
    {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
        ) {
            Row(modifier = Modifier.padding(start = 16.dp, end = 16.dp)) {
                Box(
                    modifier = Modifier
                        .size(75.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(textColorWhite)

                ) {
                    Image(
                        painter = painterResource(id = imageResId),
                        contentDescription = null,
                        modifier = Modifier
                            .size(50.dp)
                            .align(Alignment.Center)


                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(start = 16.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontFamily = FontFamily(listOf(Font(R.font.sofia_semibold))),
                        color = textColorWhite
                    )
                    Text(
                        text = subtitle,
                        fontSize = 16.sp,
                        fontFamily = FontFamily(listOf(Font(R.font.sofia_semibold))),
                        color = subtitleColor
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            CustomCircularProgressBar(
                progress =progress,
                color = subtitleColor,
                trackColor = textColorWhite,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .align(Alignment.CenterVertically)
            )
        }
    }
}

@Composable
fun CustomCircularProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 60.dp,
    strokeWidth: Dp = 8.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.primary,
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Progress indicator deterministik
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(size),
            color = color,
            strokeWidth = strokeWidth,
            trackColor = trackColor,
        )
        // Tampilkan nilai progress dalam persen di tengah
        Text(
            text = "${(progress * 100).toInt()}%",
            fontSize = 12.sp,
            fontFamily = FontFamily(listOf(Font(R.font.sofia_semibold))),
            color = trackColor
        )
    }
}