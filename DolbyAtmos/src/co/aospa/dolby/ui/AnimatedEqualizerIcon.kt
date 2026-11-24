/*
 * Copyright (C) 2026 samakshkambxj
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.pow

/**
 * Five-bar equalizer glyph that keeps dancing, adapted from the 17 branch navbar.
 *
 * Bar heights breathe out of phase so the mark reads as live audio rather than a
 * static icon, while the widths taper towards the edges to keep the rounded
 * silhouette of the original vector.
 */
@Composable
internal fun AnimatedEqualizerIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 21.dp,
    barCount: Int = 5,
) {
    val transition = rememberInfiniteTransition()
    val barHeights =
        List(barCount) { index ->
            transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation =
                            tween(
                                durationMillis = 800 + index * 60,
                                easing = FastOutSlowInEasing,
                            ),
                        repeatMode = RepeatMode.Reverse,
                    ),
            )
        }

    Canvas(modifier.size(size)) {
        val canvasWidth = this.size.width
        val canvasHeight = this.size.height

        val barWidths =
            List(barCount) { index ->
                val normalized = index.toFloat() / (barCount - 1)
                val centerOffset = (normalized - 0.5f) * 2f
                val widthFactor = 1f - (centerOffset * centerOffset).pow(0.6f)
                0.5f + widthFactor * 0.5f
            }

        val totalWidthFactor = barWidths.sum() + (barCount - 1) * 0.3f
        val baseBarWidth = canvasWidth / totalWidthFactor

        var currentX = 0f
        barHeights.forEachIndexed { index, height ->
            val barWidth = baseBarWidth * barWidths[index]
            val barHeight = canvasHeight * height.value
            drawRoundRect(
                color = color,
                topLeft = Offset(currentX, canvasHeight - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
            currentX += barWidth + baseBarWidth * 0.3f
        }
    }
}
