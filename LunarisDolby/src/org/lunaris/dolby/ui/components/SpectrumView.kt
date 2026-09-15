/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp

@Composable
fun SpectrumView(
    bars: FloatArray,
    modifier: Modifier = Modifier
) {
    val barColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        drawSpectrum(bars, barColor.copy(alpha = 0.9f), trackColor.copy(alpha = 0.3f))
    }
}

private fun DrawScope.drawSpectrum(
    bars: FloatArray,
    barColor: androidx.compose.ui.graphics.Color,
    trackColor: androidx.compose.ui.graphics.Color
) {
    if (bars.isEmpty()) return
    val gap = 3.dp.toPx()
    val totalGap = gap * (bars.size - 1)
    val barWidth = ((size.width - totalGap) / bars.size).coerceAtLeast(1f)

    for (i in bars.indices) {
        val x = i * (barWidth + gap)
        val heightRatio = bars[i].coerceIn(0f, 1f)
        val barHeight = size.height * heightRatio
        val top = size.height - barHeight

        drawRect(
            color = trackColor,
            topLeft = Offset(x, 0f),
            size = Size(barWidth, size.height)
        )
        drawRect(
            color = barColor,
            topLeft = Offset(x, top),
            size = Size(barWidth, barHeight)
        )
    }
}
