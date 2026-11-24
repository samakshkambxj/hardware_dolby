/*
 * Copyright (C) 2024-2026 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.geq.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.aospa.dolby.geq.data.BandGain
import kotlin.math.roundToInt

/**
 * One vertical equalizer band.
 *
 * Drag previews stay local to the tab (so the response curve follows the finger); only the final
 * value is committed to [EqualizerViewModel], which keeps SharedPreferences writes down to one per
 * gesture.
 */
@Composable
fun VerticalBand(
    bandGain: BandGain,
    onPreview: (Int) -> Unit,
    onCommit: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    var heightPx by remember { mutableFloatStateOf(1f) }
    val preview by rememberUpdatedState(onPreview)
    val commit by rememberUpdatedState(onCommit)
    val gain = bandGain.gain

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = formatGain(gain),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (gain > 0) colors.primary else colors.onSurfaceVariant,
            style = TextStyle(fontFeatureSettings = "tnum"),
            maxLines = 1,
        )

        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(138.dp)
                    .onSizeChanged { heightPx = it.height.toFloat().coerceAtLeast(1f) }
                    .pointerInput(Unit) {
                        fun valueFor(y: Float): Int =
                            ((0.5f - y / heightPx) * 200f).roundToInt().coerceIn(-100, 100)

                        var last = gain
                        detectDragGestures(
                            onDragStart = { offset ->
                                last = valueFor(offset.y)
                                preview(last)
                            },
                            onDrag = { change, _ ->
                                last = valueFor(change.position.y)
                                preview(last)
                                change.consume()
                            },
                            onDragEnd = { commit(last) },
                            onDragCancel = { commit(last) },
                        )
                    },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val trackWidth = 4.dp.toPx()
                val top = 6.dp.toPx()
                val bottom = size.height - 6.dp.toPx()
                val centerX = size.width / 2f
                val centerY = (top + bottom) / 2f
                val half = (bottom - top) / 2f

                drawRoundRect(
                    color = colors.outlineVariant,
                    topLeft = Offset(centerX - trackWidth / 2f, top),
                    size = Size(trackWidth, bottom - top),
                    cornerRadius = CornerRadius(trackWidth / 2f),
                )

                val thumbY = centerY - (gain / 100f) * half
                drawLine(
                    color = colors.primary,
                    start = Offset(centerX, centerY),
                    end = Offset(centerX, thumbY),
                    strokeWidth = trackWidth,
                    cap = StrokeCap.Round,
                )
                drawCircle(
                    color = colors.surfaceContainer,
                    radius = 8.dp.toPx(),
                    center = Offset(centerX, thumbY),
                )
                drawCircle(
                    color = colors.primary,
                    radius = 8.dp.toPx(),
                    center = Offset(centerX, thumbY),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }

        Text(
            text = formatFrequency(bandGain.band),
            fontSize = 8.5.sp,
            color = colors.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

private fun formatGain(gain: Int): String {
    val db = gain / 10f
    return if (db > 0f) "+%.1f".format(db) else "%.1f".format(db)
}

private fun formatFrequency(band: Int): String =
    if (band >= 1000) "${band / 1000}k" else band.toString()
