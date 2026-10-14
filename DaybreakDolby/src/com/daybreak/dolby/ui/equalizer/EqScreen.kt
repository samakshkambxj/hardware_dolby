/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daybreak.dolby.ui.equalizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.daybreak.dolby.R
import com.daybreak.dolby.ui.components.DayCard
import com.daybreak.dolby.ui.components.DayConfirmDialog
import com.daybreak.dolby.ui.components.DayScreen
import com.daybreak.dolby.ui.components.DaySectionHeader
import com.daybreak.dolby.ui.components.DaySlider
import com.daybreak.dolby.ui.components.DayTextDialog
import kotlin.math.log10
import kotlin.math.roundToInt

private val BAND_FREQS = listOf(32, 64, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)

private fun freqLabel(hz: Int): String =
    if (hz >= 1000) "${hz / 1000}k" else "$hz"

private fun xFrac(index: Int): Float {
    val min = log10(32f)
    val max = log10(16000f)
    return (log10(BAND_FREQS[index].toFloat()) - min) / (max - min)
}

@Composable
fun EqScreen(viewModel: EqViewModel) {
    val state by viewModel.uiState.collectAsState()

    var showNewDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    DayScreen {
        item {
            CurveCard(
                gains = state.gains10,
                onPreview = { index, gain -> viewModel.previewGain(index, gain) },
                onCommit = { viewModel.commitGains() },
            )
        }

        item { DaySectionHeader(text = stringResource(R.string.eq_preset)) }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.presets, key = { it.name }) { preset ->
                    FilterChip(
                        selected = !state.isCustom && preset.name == state.presetName,
                        onClick = { viewModel.setPreset(preset) },
                        label = { Text(preset.name) },
                    )
                }
                if (state.isCustom) {
                    item(key = "Custom") {
                        FilterChip(
                            selected = true,
                            onClick = {},
                            label = { Text(stringResource(R.string.eq_custom)) },
                        )
                    }
                }
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EqActionButton(
                    icon = Icons.Filled.Add,
                    description = stringResource(R.string.eq_new),
                    onClick = { showNewDialog = true },
                )
                if (state.isUserPreset) {
                    EqActionButton(
                        icon = Icons.Filled.Edit,
                        description = stringResource(R.string.eq_rename),
                        onClick = { showRenameDialog = true },
                    )
                    EqActionButton(
                        icon = Icons.Filled.Delete,
                        description = stringResource(R.string.eq_delete),
                        onClick = { showDeleteDialog = true },
                    )
                }
                EqActionButton(
                    icon = Icons.Filled.Refresh,
                    description = stringResource(R.string.eq_reset),
                    onClick = { viewModel.reset() },
                )
            }
        }

        item { DaySectionHeader(text = stringResource(R.string.eq_bass)) }
        item {
            BandGroupCard(
                gains = state.gains10.slice(0..2),
                offset = 0,
                onPreview = { i, g -> viewModel.previewGain(i, g) },
                onCommit = { viewModel.commitGains() },
            )
        }

        item { DaySectionHeader(text = stringResource(R.string.eq_mids)) }
        item {
            BandGroupCard(
                gains = state.gains10.slice(3..6),
                offset = 3,
                onPreview = { i, g -> viewModel.previewGain(i, g) },
                onCommit = { viewModel.commitGains() },
            )
        }

        item { DaySectionHeader(text = stringResource(R.string.eq_treble)) }
        item {
            BandGroupCard(
                gains = state.gains10.slice(7..9),
                offset = 7,
                onPreview = { i, g -> viewModel.previewGain(i, g) },
                onCommit = { viewModel.commitGains() },
            )
        }
    }

    if (showNewDialog) {
        DayTextDialog(
            title = stringResource(R.string.eq_new_title),
            initial = "",
            confirmLabel = stringResource(android.R.string.ok),
            dismissLabel = stringResource(android.R.string.cancel),
            onConfirm = {
                viewModel.saveAs(it)
                showNewDialog = false
            },
            onDismiss = { showNewDialog = false },
        )
    }

    if (showRenameDialog) {
        DayTextDialog(
            title = stringResource(R.string.eq_rename_title),
            initial = state.presetName,
            confirmLabel = stringResource(android.R.string.ok),
            dismissLabel = stringResource(android.R.string.cancel),
            onConfirm = {
                viewModel.renamePreset(state.presetName, it)
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false },
        )
    }

    if (showDeleteDialog) {
        DayConfirmDialog(
            title = stringResource(R.string.eq_delete_title),
            message = stringResource(R.string.eq_delete_message, state.presetName),
            confirmLabel = stringResource(android.R.string.ok),
            dismissLabel = stringResource(android.R.string.cancel),
            destructive = true,
            onConfirm = {
                viewModel.deletePreset(state.presetName)
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

@Composable
private fun EqActionButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun CurveCard(
    gains: List<Int>,
    onPreview: (Int, Int) -> Unit,
    onCommit: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val grid = onSurfaceVariant.copy(alpha = 0.25f)

    var dragGains by remember(gains) { mutableStateOf<FloatArray?>(null) }
    var dragIndex by remember { mutableIntStateOf(-1) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
            ) {
                Column(
                    modifier = Modifier.width(36.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    listOf("+10", "0", "-10").forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = onSurfaceVariant,
                        )
                    }
                }
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(200.dp)
                        .pointerInput(gains) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val w = size.width.toFloat()
                                    var best = 0
                                    var bestDist = Float.MAX_VALUE
                                    gains.indices.forEach { i ->
                                        val d = kotlin.math.abs(
                                            xFrac(i) * w - offset.x,
                                        )
                                        if (d < bestDist) {
                                            bestDist = d
                                            best = i
                                        }
                                    }
                                    dragIndex = best
                                    dragGains = FloatArray(gains.size) {
                                        gains[it].toFloat()
                                    }
                                },
                                onDragEnd = {
                                    val snapshot = dragGains
                                    if (snapshot != null) {
                                        snapshot.forEachIndexed { i, g ->
                                            val rounded = g.roundToInt().coerceIn(-100, 100)
                                            if (rounded != gains[i]) {
                                                onPreview(i, rounded)
                                            }
                                        }
                                        onCommit()
                                    }
                                    dragGains = null
                                    dragIndex = -1
                                },
                                onDragCancel = {
                                    dragGains = null
                                    dragIndex = -1
                                },
                                onDrag = { change, _ ->
                                    val current = dragGains
                                    val idx = dragIndex
                                    if (current != null && idx in current.indices) {
                                        val h = size.height.toFloat()
                                        val g = ((1f - change.position.y / h) * 200f - 100f)
                                            .roundToInt().coerceIn(-100, 100)
                                        current[idx] = g.toFloat()
                                        dragGains = current.copyOf()
                                    }
                                },
                            )
                        },
                ) {
                    val w = size.width
                    val h = size.height
                    fun xAt(i: Int) = xFrac(i) * w
                    fun yAt(g: Float) = h - ((g + 100f) / 200f) * h

                    val shown = dragGains?.toList() ?: gains.map { it.toFloat() }
                    val points = shown.mapIndexed { i, g ->
                        Offset(xAt(i), yAt(g))
                    }

                    listOf(100f, 0f, -100f).forEach { g ->
                        drawLine(
                            color = grid,
                            start = Offset(0f, yAt(g)),
                            end = Offset(w, yAt(g)),
                            strokeWidth = if (g == 0f) 2f else 1f,
                        )
                    }

                    val curve = Path().apply {
                        moveTo(points[0].x, points[0].y)
                        for (i in 1 until points.size) {
                            val prev = points[i - 1]
                            val curr = points[i]
                            quadraticBezierTo(
                                prev.x, prev.y,
                                (prev.x + curr.x) / 2f, (prev.y + curr.y) / 2f,
                            )
                        }
                        lineTo(points.last().x, points.last().y)
                    }
                    drawPath(
                        Path().apply {
                            addPath(curve)
                            lineTo(points.last().x, h)
                            lineTo(points.first().x, h)
                            close()
                        },
                        Brush.verticalGradient(
                            0f to primary.copy(alpha = 0.35f),
                            1f to Color.Transparent,
                        ),
                    )
                    drawPath(curve, primary, style = Stroke(width = 3.dp.toPx()))

                    points.forEach { p ->
                        drawCircle(primary, radius = 5.dp.toPx(), center = p)
                        drawCircle(Color.White, radius = 2.dp.toPx(), center = p)
                    }
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .padding(start = 36.dp, top = 4.dp),
            ) {
                BAND_FREQS.forEachIndexed { i, hz ->
                    Text(
                        text = freqLabel(hz),
                        style = MaterialTheme.typography.labelSmall,
                        color = onSurfaceVariant,
                        modifier = Modifier
                            .offset(x = maxWidth * xFrac(i) - 16.dp)
                            .width(32.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun BandGroupCard(
    gains: List<Int>,
    offset: Int,
    onPreview: (Int, Int) -> Unit,
    onCommit: () -> Unit,
) {
    DayCard {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            gains.forEachIndexed { i, gain ->
                BandSliderRow(
                    freqHz = BAND_FREQS[offset + i],
                    gain = gain,
                    onPreview = { onPreview(offset + i, it) },
                    onCommit = onCommit,
                )
            }
        }
    }
}

@Composable
private fun BandSliderRow(
    freqHz: Int,
    gain: Int,
    onPreview: (Int) -> Unit,
    onCommit: () -> Unit,
) {
    var pos by remember(gain) { mutableFloatStateOf(gain / 100f) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = freqLabel(freqHz),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.width(44.dp),
        )
        DaySlider(
            value = pos,
            valueRange = -1f..1f,
            steps = 0,
            onLiveChange = { pos = it },
            onCommit = {
                onPreview((pos * 100f).toInt())
                onCommit()
            },
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "%.1f".format(pos * 10f),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(52.dp),
        )
    }
}
