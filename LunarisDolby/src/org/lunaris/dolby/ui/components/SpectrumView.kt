/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.lunaris.dolby.R

/**
 * Live output spectrum drawn as a smooth filled curve, in the same visual
 * language as the frequency response graphs. Incoming FFT bars are jittery
 * frame to frame, so values are eased toward each new capture instead of
 * snapping to it.
 */
@Composable
fun SpectrumView(
    bars: FloatArray,
    modifier: Modifier = Modifier
) {
    val curveColor = MaterialTheme.colorScheme.primary
    val plotContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val plotInkColor = MaterialTheme.colorScheme.onSurfaceVariant

    var smoothed by remember(bars.size) { mutableStateOf(FloatArray(bars.size)) }
    LaunchedEffect(bars) {
        val next = FloatArray(bars.size) { i ->
            val prev = smoothed.getOrElse(i) { 0f }
            val target = bars.getOrElse(i) { 0f }.coerceIn(0f, 1f)
            prev + (target - prev) * 0.45f
        }
        smoothed = next
    }
    val values = smoothed

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.dynamics_spectrum),
            style = MaterialTheme.typography.labelLarge,
            color = plotInkColor
        )
        Spacer(modifier = Modifier.height(8.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(104.dp)
                .clip(MaterialTheme.shapes.large)
                .background(plotContainerColor)
        ) {
            val width = size.width
            val height = size.height
            if (width <= 0f || height <= 0f || values.isEmpty()) return@Canvas

            val gridColor = plotInkColor.copy(alpha = 0.18f)
            for (i in 1..3) {
                val y = (height / 4) * i
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
            }

            val n = values.size
            fun xAt(i: Int): Float =
                if (n > 1) i / (n - 1f) * width else width / 2f
            fun yAt(i: Int): Float =
                height - values.getOrElse(i) { 0f }.coerceIn(0f, 1f) * height * 0.92f

            if (n == 1) {
                drawCircle(
                    color = curveColor,
                    radius = 4f,
                    center = Offset(xAt(0), yAt(0))
                )
                return@Canvas
            }

            val path = Path().apply {
                moveTo(xAt(0), yAt(0))
                for (i in 1 until n) {
                    val midX = (xAt(i - 1) + xAt(i)) / 2f
                    cubicTo(midX, yAt(i - 1), midX, yAt(i), xAt(i), yAt(i))
                }
            }

            drawPath(
                path = Path().apply {
                    addPath(path)
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                },
                brush = Brush.verticalGradient(
                    colors = listOf(
                        curveColor.copy(alpha = 0.35f),
                        curveColor.copy(alpha = 0.04f)
                    )
                )
            )

            drawPath(
                path = path,
                color = curveColor,
                style = Stroke(width = 4f)
            )
        }
    }
}
