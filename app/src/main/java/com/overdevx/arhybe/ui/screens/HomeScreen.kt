package com.overdevx.arhybe.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.overdevx.arhybe.ui.components.StartComponent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect

import androidx.compose.runtime.remember

import androidx.compose.ui.geometry.Offset

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.overdevx.arhybe.R
import com.overdevx.arhybe.ui.components.DiagnosaComponent
import com.overdevx.arhybe.ui.theme.textColorBlack
import com.overdevx.arhybe.ui.theme.textColorGreen
import com.overdevx.arhybe.ui.theme.textColorWhite
import com.overdevx.arhybe.ui.theme.textColorYellow
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries

@Composable
fun HomeScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),

    ) {
        Column {
            StartComponent()
            JetpackComposeBasicLineChart(modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Hasil Diagnosa",
                fontSize = 20.sp,
                fontFamily = FontFamily(listOf(Font(R.font.sofia_semibold))),
                color = textColorWhite,
                modifier = Modifier.padding(start = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            DiagnosaComponent(
                title = "Jenis Aritmia",
                subtitle = "Normal",
                imageResId = R.drawable.ic_aritmia,
                progress = 0.75f,
                subtitleColor = textColorGreen
            )
            Spacer(modifier = Modifier.height(16.dp))
            DiagnosaComponent(
                title = "Level Kecemasan",
                subtitle = "Medium",
                imageResId = R.drawable.ic_cemas,
                progress = 0.6f,
                subtitleColor = textColorYellow
            )
        }
    }
}
@Composable
private fun JetpackComposeBasicLineChart(
    modelProducer: CartesianChartModelProducer,
    modifier: Modifier = Modifier,
) {
    CartesianChartHost(
        chart =
        rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(),
        ),
        modelProducer = modelProducer,
        modifier = modifier,
    )
}

@Composable
fun JetpackComposeBasicLineChart(modifier: Modifier = Modifier) {
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(Unit) {
        modelProducer.runTransaction {
            // Learn more: https://patrykandpatrick.com/vmml6t.
            lineSeries { series(13, 8, 7, 12, 0, 1, 15, 14, 0, 11, 6, 12, 0, 11, 12, 11) }
        }
    }
    JetpackComposeBasicLineChart(modelProducer, modifier)
}


