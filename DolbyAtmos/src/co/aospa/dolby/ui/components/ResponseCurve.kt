/*
 * Copyright (C) 2026 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.aospa.dolby.ui.DolbyUiState
import kotlin.math.roundToInt

/**
 * Interactive spline through the visible equalizer bands. Drag anywhere to move the nearest point;
 * the value is clamped to ±15 dB.
 */
@Composable
fun ResponseCurve(
    gains: List<Int>,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp,
    interactive: Boolean = false,
    showPoints: Boolean = true,
    onGainChange: (Int, Int) -> Unit = { _, _ -> },
    onGainChangeFinished: () -> Unit = {},
) {
    val primary = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.surfaceContainerLow
    val grid = MaterialTheme.colorScheme.outlineVariant

    // Canvas size captured at layout time so drag maths can resolve the band index.
    var curveWidth = 0f
    var curveHeight = 0f

    val dragModifier =
        if (interactive && gains.size > 1) {
            Modifier
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    curveWidth = placeable.width.toFloat()
                    curveHeight = placeable.height.toFloat()
                    layout(placeable.width, placeable.height) {
                        placeable.place(0, 0)
                    }
                }
                .pointerInput(gains.size) {
                    val padX = 14.dp.toPx()
                    val padY = 12.dp.toPx()
                    var activeBand = 0
                    fun update(y: Float) {
                        val usable = (curveHeight - padY * 2f).coerceAtLeast(1f)
                        val fraction = (1f - (y - padY) / usable).coerceIn(0f, 1f)
                        val tenths = ((fraction * 2f - 1f) * 150f).roundToInt()
                        onGainChange(activeBand, tenths)
                    }
                    detectDragGestures(
                        onDragStart = { offset ->
                            val step = (curveWidth - padX * 2f) / (gains.size - 1)
                            activeBand =
                                ((offset.x - padX) / step).roundToInt().coerceIn(0, gains.lastIndex)
                            update(offset.y)
                        },
                        onDrag = { change, _ -> update(change.position.y) },
                        onDragEnd = { onGainChangeFinished() },
                        onDragCancel = { onGainChangeFinished() },
                    )
                }
        } else {
            Modifier
        }

    Canvas(modifier = modifier.fillMaxWidth().height(height).then(dragModifier)) {
        if (gains.size < 2) return@Canvas
        val padX = 14.dp.toPx()
        val padY = 12.dp.toPx()
        val usableW = size.width - padX * 2f
        val usableH = size.height - padY * 2f
        val step = usableW / (gains.size - 1)
        fun y(gain: Int) = padY + (1f - (gain + 150f) / 300f) * usableH
        fun x(index: Int) = padX + index * step

        val zeroY = y(0)
        for (gain in intArrayOf(150, 0, -150)) {
            drawLine(
                color = if (gain == 0) primary.copy(alpha = 0.30f) else primary.copy(alpha = 0.10f),
                start = Offset(0f, y(gain)),
                end = Offset(size.width, y(gain)),
                strokeWidth = 1.dp.toPx(),
                pathEffect =
                    if (gain == 0) PathEffect.dashPathEffect(floatArrayOf(6f, 8f)) else null,
            )
        }
        if (interactive) {
            gains.indices.forEach { index ->
                drawLine(
                    color = grid.copy(alpha = 0.35f),
                    start = Offset(x(index), padY),
                    end = Offset(x(index), size.height - padY),
                    strokeWidth = 1f,
                )
            }
        }

        val line = Path().apply { moveTo(x(0), y(gains[0])) }
        for (index in 1 until gains.size) {
            val x0 = x(index - 1)
            val x1 = x(index)
            val mid = (x0 + x1) / 2f
            line.cubicTo(mid, y(gains[index - 1]), mid, y(gains[index]), x1, y(gains[index]))
        }

        val area =
            Path().apply {
                addPath(line)
                lineTo(x(gains.lastIndex), size.height)
                lineTo(x(0), size.height)
                close()
            }
        drawPath(
            path = area,
            brush =
                Brush.verticalGradient(
                    colors = listOf(primary.copy(alpha = 0.24f), primary.copy(alpha = 0f)),
                    startY = padY,
                    endY = size.height,
                ),
        )
        drawPath(
            path = line,
            color = primary,
            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round),
        )

        if (showPoints) {
            gains.indices.forEach { index ->
                drawCircle(color = background, radius = 4.2.dp.toPx(), center = Offset(x(index), y(gains[index])))
                drawCircle(
                    color = primary,
                    radius = 3.2.dp.toPx(),
                    center = Offset(x(index), y(gains[index])),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }
        // Keep the zero line visible above the fill.
        drawLine(
            color = primary.copy(alpha = 0.18f),
            start = Offset(0f, zeroY),
            end = Offset(size.width, zeroY),
            strokeWidth = 1f,
        )
    }
}

/** A single vertical fader used by the sliders view. */
@Composable
fun FaderBand(
    gainTenths: Int,
    frequency: String,
    onChange: (Int) -> Unit,
    onFinished: () -> Unit,
) {
    val value = (gainTenths / 150f).coerceIn(-1f, 1f)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = DolbyUiState.formatGain(gainTenths),
            fontSize = 10.sp,
            color =
                if (gainTenths == 0) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        Slider(
            value = value,
            onValueChange = { onChange((it * 150f).roundToInt()) },
            onValueChangeFinished = onFinished,
            valueRange = -1f..1f,
            colors =
                SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
            modifier =
                Modifier.graphicsLayer {
                        rotationZ = 270f
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
                    .layout { measurable, constraints ->
                        val placeable =
                            measurable.measure(
                                Constraints(
                                    minWidth = constraints.minHeight,
                                    maxWidth = constraints.maxHeight,
                                    minHeight = constraints.minWidth,
                                    maxHeight = constraints.maxWidth,
                                )
                            )
                        layout(placeable.height, placeable.width) {
                            placeable.place(-placeable.width, 0)
                        }
                    }
                    .width(180.dp)
                    .height(36.dp)
                    .padding(horizontal = 10.dp),
        )
        Text(
            text = frequency,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Small read-only curve used above the sliders. */
@Composable
fun MiniCurve(gains: List<Int>, modifier: Modifier = Modifier) {
    ResponseCurve(
        gains = gains,
        modifier = modifier.padding(horizontal = 4.dp),
        height = 82.dp,
        interactive = false,
        showPoints = false,
    )
}
