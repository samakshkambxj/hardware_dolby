/*
 * Copyright (C) 2024-2026 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.geq.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.aospa.dolby.R
import co.aospa.dolby.geq.data.BandGain
import co.aospa.dolby.geq.data.Preset

private val FREQUENCY_LABELS =
    listOf("32", "64", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

/**
 * Equalizer destination of the floating tab bar. Preset chips, a live response curve and the same
 * ten bands backed by the existing [EqualizerViewModel].
 */
@Composable
fun EqualizerTab(
    viewModel: EqualizerViewModel,
    profileName: String,
    scrollState: ScrollState = rememberScrollState(),
    modifier: Modifier = Modifier,
) {
    val presets by viewModel.presets.collectAsState()
    val preset by viewModel.preset.collectAsState()

    var showNewDialog by remember { mutableStateOf(false) }
    var renamePreset by remember { mutableStateOf<Preset?>(null) }
    var deletePreset by remember { mutableStateOf<Preset?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }
    var dragIndex by remember { mutableIntStateOf(-1) }
    var dragGain by remember { mutableIntStateOf(0) }

    val gains =
        preset.bandGains.mapIndexed { index, bandGain ->
            if (index == dragIndex) bandGain.copy(gain = dragGain) else bandGain
        }

    Column(
        modifier =
            modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
    ) {
        EqualizerHeader(
            onSave = { showNewDialog = true },
            onReset = {
                if (preset.isUserDefined) showResetDialog = true else viewModel.reset()
            },
        )

        Column(
            modifier =
                Modifier.weight(1f).verticalScroll(scrollState).padding(bottom = 104.dp),
        ) {
            PresetChips(
                presets = presets,
                currentName = preset.name,
                showCustom = presets.none { it.name == "Custom" },
                onSelect = viewModel::setPreset,
                onCustom = {
                    viewModel.setPreset(
                        Preset(name = "Custom", bandGains = preset.bandGains)
                    )
                },
                onNew = { showNewDialog = true },
                onRename = { renamePreset = it },
                onDelete = { deletePreset = it },
            )

            Spacer(Modifier.height(16.dp))

            ResponseCard(
                gains = gains,
                presetName = preset.name,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            Spacer(Modifier.height(12.dp))

            BandsCard(
                gains = gains,
                onPreview = { index, gain ->
                    dragIndex = index
                    dragGain = gain
                },
                onCommit = { index, gain ->
                    viewModel.setGain(index, gain)
                    dragIndex = -1
                },
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { showNewDialog = true },
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(50.dp),
                shape = CircleShape,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.save_as_24px),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.dolby_eq_save_preset),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Text(
                text = stringResource(id = R.string.dolby_eq_hint, profileName),
                modifier =
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                textAlign = TextAlign.Center,
                fontSize = 10.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showNewDialog) {
        PresetNameDialog(
            title = stringResource(id = R.string.dolby_geq_new_preset),
            onPresetNameSet = { viewModel.createNewPreset(name = it) },
            onDismissDialog = { showNewDialog = false },
        )
    }

    renamePreset?.let { presetToRename ->
        PresetNameDialog(
            title = stringResource(id = R.string.dolby_geq_rename_preset),
            presetName = presetToRename.name,
            onPresetNameSet = { viewModel.renamePreset(preset = presetToRename, name = it) },
            onDismissDialog = { renamePreset = null },
        )
    }

    deletePreset?.let { presetToDelete ->
        ConfirmationDialog(
            text = stringResource(id = R.string.dolby_geq_delete_preset_prompt),
            onConfirm = { viewModel.deletePreset(presetToDelete) },
            onDismiss = { deletePreset = null },
        )
    }

    if (showResetDialog) {
        ConfirmationDialog(
            text = stringResource(id = R.string.dolby_geq_reset_gains_prompt),
            onConfirm = { viewModel.reset() },
            onDismiss = { showResetDialog = false },
        )
    }
}

@Composable
private fun EqualizerHeader(onSave: () -> Unit, onReset: () -> Unit) {
    Row(
        modifier =
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(id = R.string.dolby_preset),
            fontSize = 21.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.2).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onSave) {
            Icon(
                painter = painterResource(id = R.drawable.save_as_24px),
                contentDescription = stringResource(id = R.string.dolby_geq_new_preset),
            )
        }
        IconButton(onClick = onReset) {
            Icon(
                painter = painterResource(id = R.drawable.reset_settings_24px),
                contentDescription = stringResource(id = R.string.dolby_geq_reset_gains),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PresetChips(
    presets: List<Preset>,
    currentName: String,
    showCustom: Boolean,
    onSelect: (Preset) -> Unit,
    onCustom: () -> Unit,
    onNew: () -> Unit,
    onRename: (Preset) -> Unit,
    onDelete: (Preset) -> Unit,
) {
    var menuFor by remember { mutableStateOf<String?>(null) }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 2.dp),
    ) {
        items(items = presets, key = { it.name }) { preset ->
            Box {
                PresetChip(
                    label = preset.name,
                    selected = preset.name == currentName,
                    onClick = { onSelect(preset) },
                    onLongClick =
                        if (preset.isUserDefined) {
                            { menuFor = preset.name }
                        } else null,
                )
                DropdownMenu(
                    expanded = menuFor == preset.name,
                    onDismissRequest = { menuFor = null },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.dolby_geq_rename_preset)) },
                        onClick = {
                            menuFor = null
                            onRename(preset)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.dolby_geq_delete_preset)) },
                        onClick = {
                            menuFor = null
                            onDelete(preset)
                        },
                    )
                }
            }
        }

        if (showCustom) {
            item(key = "custom") {
                PresetChip(
                    label = stringResource(id = R.string.dolby_eq_custom),
                    selected = currentName == "Custom",
                    onClick = onCustom,
                )
            }
        }

        item(key = "new") {
            Surface(
                modifier = Modifier.height(34.dp).clip(CircleShape).clickable(onClick = onNew),
                shape = CircleShape,
                color = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = stringResource(id = R.string.dolby_new),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val container by
        animateColorAsState(
            targetValue = if (selected) colors.primary else colors.surfaceContainer,
            label = "preset_chip_container",
        )
    val content by
        animateColorAsState(
            targetValue = if (selected) colors.onPrimary else colors.onSurfaceVariant,
            label = "preset_chip_content",
        )

    Surface(
        modifier =
            Modifier.height(34.dp)
                .clip(CircleShape)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = CircleShape,
        color = container,
        contentColor = content,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ResponseCard(
    gains: List<BandGain>,
    presetName: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = colors.surfaceContainer,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(id = R.string.dolby_eq_response),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                MetaPill(presetName)
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.width(24.dp).height(132.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End,
                ) {
                    AxisLabel("+10")
                    AxisLabel("0")
                    AxisLabel("-10")
                }
                Spacer(Modifier.width(6.dp))
                ResponseCurve(gains = gains, modifier = Modifier.weight(1f).height(132.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 30.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                FREQUENCY_LABELS.forEach { AxisLabel(it) }
            }
        }
    }
}

@Composable
private fun ResponseCurve(gains: List<BandGain>, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Canvas(modifier) {
        if (gains.isEmpty()) return@Canvas

        val amplitude = size.height * 0.33f
        val centerY = size.height / 2f
        val stepX = size.width / (gains.size - 1).coerceAtLeast(1)
        val gridColor = colors.outlineVariant.copy(alpha = 0.6f)

        listOf(
                centerY - amplitude,
                centerY - amplitude / 2f,
                centerY + amplitude / 2f,
                centerY + amplitude,
            )
            .forEach { y ->
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }

        drawLine(
            color = colors.outline.copy(alpha = 0.8f),
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)),
        )

        val points =
            gains.mapIndexed { index, bandGain ->
                Offset(
                    x = index * stepX,
                    y = centerY - (bandGain.gain / 100f) * amplitude,
                )
            }

        val line = Path()
        line.moveTo(points.first().x, points.first().y)
        for (index in 1 until points.size) {
            val previous = points[index - 1]
            val current = points[index]
            val midX = (previous.x + current.x) / 2f
            line.cubicTo(midX, previous.y, midX, current.y, current.x, current.y)
        }

        val fill =
            Path().apply {
                addPath(line)
                lineTo(points.last().x, size.height)
                lineTo(points.first().x, size.height)
                close()
            }

        drawPath(
            path = fill,
            brush =
                Brush.verticalGradient(
                    colors = listOf(colors.primary.copy(alpha = 0.22f), Color.Transparent),
                    startY = 0f,
                    endY = size.height,
                ),
        )
        drawPath(
            path = line,
            color = colors.primary,
            style =
                Stroke(
                    width = 2.4.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
        )

        points.forEach { point ->
            drawCircle(
                color = colors.surfaceContainer,
                radius = 3.4.dp.toPx(),
                center = point,
            )
            drawCircle(
                color = colors.primary,
                radius = 3.4.dp.toPx(),
                center = point,
                style = Stroke(width = 2.dp.toPx()),
            )
        }
    }
}

@Composable
private fun BandsCard(
    gains: List<BandGain>,
    onPreview: (Int, Int) -> Unit,
    onCommit: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = colors.surfaceContainer,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(id = R.string.dolby_eq_bands),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                MetaPill(stringResource(id = R.string.dolby_eq_db_range))
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                gains.forEachIndexed { index, bandGain ->
                    VerticalBand(
                        bandGain = bandGain,
                        onPreview = { onPreview(index, it) },
                        onCommit = { onCommit(index, it) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaPill(text: String) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = colors.surface,
        contentColor = colors.onSurfaceVariant,
        shadowElevation = 1.dp,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun AxisLabel(text: String) {
    Text(
        text = text,
        fontSize = 8.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
    )
}
