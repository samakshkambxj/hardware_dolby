/*
 * Copyright (C) 2026 samakshkambxj
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import co.aospa.dolby.DolbyConstants
import kotlin.math.abs
import kotlin.math.roundToInt

private const val GAIN_LIMIT = DolbyConstants.GAIN_LIMIT

/**
 * Smooth response curve shared by the home shortcut and the equalizer page.
 * When [onGainChange] is set the points become draggable.
 */
@Composable
fun GainCurve(
    gains: List<Int>,
    modifier: Modifier = Modifier,
    interactive: Boolean = false,
    onGainChange: ((Int, Int) -> Unit)? = null,
    onGainChangeFinished: (() -> Unit)? = null,
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.tertiary
    val grid = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val background = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.35f)
    val currentOnGainChange by rememberUpdatedState(onGainChange)
    val currentOnGainChangeFinished by rememberUpdatedState(onGainChangeFinished)
    val draggable = interactive && onGainChange != null

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .then(
                if (draggable) {
                    Modifier.pointerInput(draggable) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                bandAt(offset.x, size.width.toFloat())?.let { index ->
                                    currentOnGainChange?.invoke(
                                        index,
                                        gainAt(offset.y, size.height.toFloat()),
                                    )
                                }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                bandAt(change.position.x, size.width.toFloat())?.let { index ->
                                    currentOnGainChange?.invoke(
                                        index,
                                        gainAt(change.position.y, size.height.toFloat()),
                                    )
                                }
                            },
                            onDragEnd = { currentOnGainChangeFinished?.invoke() },
                            onDragCancel = { currentOnGainChangeFinished?.invoke() },
                        )
                    }
                } else {
                    Modifier
                }
            )
    ) {
        val width = size.width
        val height = size.height
        val inset = 10.dp.toPx()
        val usableHeight = height - inset * 2

        fun pointAt(index: Int): Offset {
            val x = if (gains.size <= 1) width / 2 else width * index / (gains.size - 1).toFloat()
            val gain = gains.getOrElse(index) { 0 }.coerceIn(-GAIN_LIMIT, GAIN_LIMIT)
            val y = inset + usableHeight * (1f - (gain + GAIN_LIMIT) / (GAIN_LIMIT * 2f))
            return Offset(x, y)
        }

        // Zero line + quarter lines.
        listOf(-1f, -0.5f, 0f, 0.5f, 1f).forEach { fraction ->
            val y = inset + usableHeight * (0.5f - fraction * 0.5f)
            drawLine(
                color = if (fraction == 0f) grid.copy(alpha = 0.9f) else grid,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = if (fraction == 0f) 1.5.dp.toPx() else 1.dp.toPx(),
            )
        }

        val points = gains.indices.map { pointAt(it) }
        if (points.isEmpty()) return@Canvas

        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                val previous = points[i - 1]
                val current = points[i]
                val midX = (previous.x + current.x) / 2f
                cubicTo(midX, previous.y, midX, current.y, current.x, current.y)
            }
        }

        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(points.last().x, height)
            lineTo(points.first().x, height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(primary.copy(alpha = 0.38f), Color.Transparent),
                startY = 0f,
                endY = height,
            ),
        )
        drawPath(
            path = linePath,
            brush = Brush.horizontalGradient(listOf(primary, secondary)),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
        )

        points.forEachIndexed { index, point ->
            val gain = gains.getOrElse(index) { 0 }
            val accent = if (gain == 0) primary.copy(alpha = 0.55f) else secondary
            drawCircle(color = Color.White.copy(alpha = 0.9f), radius = 6.dp.toPx(), center = point)
            drawCircle(color = accent, radius = 4.dp.toPx(), center = point)
        }
    }
}

/** A thin vertical fader used by the 10-band equalizer. */
@Composable
fun VerticalFader(
    value: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChange: (Int) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    val currentChange by rememberUpdatedState(onValueChange)
    val currentFinish by rememberUpdatedState(onValueChangeFinished)
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val activeColor = MaterialTheme.colorScheme.primary
    val thumbColor = MaterialTheme.colorScheme.onPrimary

    BoxWithConstraints(
        modifier = modifier
            .width(30.dp)
            .height(170.dp)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    fun update(y: Float) {
                        val fraction = (1f - (y / size.height.toFloat())).coerceIn(0f, 1f)
                        val gain = ((fraction * 2f - 1f) * GAIN_LIMIT).roundToInt()
                        currentChange(gain.coerceIn(-GAIN_LIMIT, GAIN_LIMIT))
                    }
                    update(down.position.y)
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        update(change.position.y)
                        change.consume()
                    }
                    currentFinish()
                }
            },
        contentAlignment = Alignment.TopCenter,
    ) {
        val thumbSize = 18.dp
        val fraction = ((value.coerceIn(-GAIN_LIMIT, GAIN_LIMIT) + GAIN_LIMIT) /
            (GAIN_LIMIT * 2f)).coerceIn(0f, 1f)
        val thumbOffset = (maxHeight - thumbSize) * (1f - fraction)

        Box(
            modifier = Modifier
                .width(6.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(3.dp))
                .background(trackColor),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .width(6.dp)
                .fillMaxHeight(fraction.coerceAtLeast(0.001f))
                .clip(RoundedCornerShape(3.dp))
                .background(activeColor),
        )
        Box(
            modifier = Modifier
                .offset(y = thumbOffset)
                .size(thumbSize)
                .clip(CircleShape)
                .background(activeColor),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(thumbColor),
            )
        }
    }
}

private fun bandAt(x: Float, width: Float): Int? {
    if (width <= 0f) return null
    val count = DolbyConstants.DISPLAY_BAND_COUNT
    val index = (x / width * (count - 1)).roundToInt().coerceIn(0, count - 1)
    val center = width * index / (count - 1).toFloat()
    return if (abs(center - x) <= width / (count - 1) / 1.35f) index else null
}

private fun gainAt(y: Float, height: Float): Int {
    if (height <= 0f) return 0
    val fraction = (1f - (y / height)).coerceIn(0f, 1f)
    return ((fraction * 2f - 1f) * GAIN_LIMIT).roundToInt().coerceIn(-GAIN_LIMIT, GAIN_LIMIT)
}
