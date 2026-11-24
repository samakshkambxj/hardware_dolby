/*
 * Copyright (C) 2026 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.aospa.dolby.R
import co.aospa.dolby.ui.BandMode
import co.aospa.dolby.ui.DolbyUiState
import co.aospa.dolby.ui.DolbyViewModel
import co.aospa.dolby.ui.components.DolbyTopBar
import co.aospa.dolby.ui.components.FaderBand
import co.aospa.dolby.ui.components.IconAction
import co.aospa.dolby.ui.components.IconBubble
import co.aospa.dolby.ui.components.MiniCurve
import co.aospa.dolby.ui.components.ResponseCurve
import co.aospa.dolby.ui.components.SettingsCard

private enum class EqView {
    CURVE,
    SLIDERS,
}

@Composable
fun EqualizerPage(state: DolbyUiState, viewModel: DolbyViewModel, modifier: Modifier = Modifier) {
    var view by remember { mutableStateOf(EqView.CURVE) }
    var tunerIndex by remember { mutableIntStateOf(0) }
    var showPresets by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    var showSave by remember { mutableStateOf(false) }
    var showAutoEq by remember { mutableStateOf(false) }
    var showImportExport by remember { mutableStateOf(false) }

    val bandCount = state.bandCount
    val safeIndex = tunerIndex.coerceIn(0, (bandCount - 1).coerceAtLeast(0))
    val gains = state.visibleGains
    val frequencies = state.visibleFrequencies
    val enabled = state.controlsEnabled
    val userPreset = viewModel.isUserPreset(state.presetName)

    Column(modifier = modifier.fillMaxWidth()) {
        DolbyTopBar(title = stringResource(R.string.dolby_page_equalizer)) {
            IconAction(
                icon = R.drawable.ic_save,
                contentDescription = stringResource(R.string.dolby_eq_save_preset),
                onClick = { showSave = true },
                enabled = enabled,
            )
            IconAction(
                icon = R.drawable.ic_refresh,
                contentDescription = stringResource(R.string.dolby_geq_reset_gains),
                onClick = viewModel::resetGains,
                enabled = enabled,
            )
            Box {
                IconAction(
                    icon = R.drawable.ic_more_vert,
                    contentDescription = "More options",
                    onClick = { showMore = true },
                )
                DropdownMenu(expanded = showMore, onDismissRequest = { showMore = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.dolby_eq_import_export)) },
                        onClick = {
                            showMore = false
                            showImportExport = true
                        },
                        leadingIcon = {
                            IconBubble(R.drawable.ic_swap, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.dolby_eq_autoeq)) },
                        onClick = {
                            showMore = false
                            showAutoEq = true
                        },
                        leadingIcon = {
                            IconBubble(R.drawable.ic_auto_fix, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.dolby_eq_delete_preset)) },
                        enabled = userPreset,
                        onClick = {
                            showMore = false
                            viewModel.deletePreset(state.presetName)
                        },
                        leadingIcon = {
                            IconBubble(R.drawable.ic_delete, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                    )
                }
            }
        }

        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 120.dp)
        ) {
            // Preset ---------------------------------------------------------------------------
            SettingsCard {
                Column(modifier = Modifier.padding(20.dp)) {
                    CardHeader(
                        icon = R.drawable.ic_music_note,
                        title = stringResource(R.string.dolby_eq_preset),
                    )
                    Spacer(Modifier.height(12.dp))
                    Box {
                        Row(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .clickable(enabled = enabled) { showPresets = true }
                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = state.presetName,
                                modifier = Modifier.weight(1f),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            Icon(
                                painter = painterResource(id = R.drawable.ic_chevron_right),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp).graphicsLayer { rotationZ = 90f },
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        DropdownMenu(expanded = showPresets, onDismissRequest = { showPresets = false }) {
                            viewModel.presetNames.forEach { name ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        showPresets = false
                                        viewModel.applyPreset(name)
                                    },
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Band configuration ----------------------------------------------------------------
            SettingsCard {
                Column(modifier = Modifier.padding(20.dp)) {
                    CardHeader(
                        icon = R.drawable.ic_tune,
                        title = stringResource(R.string.dolby_eq_band_configuration),
                    )
                    Text(
                        text = stringResource(R.string.dolby_eq_band_configuration_desc),
                        modifier = Modifier.padding(top = 4.dp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BandMode.values().forEach { mode ->
                            ModeTile(
                                number = mode.label,
                                label = stringResource(R.string.dolby_eq_band_count, mode.bands),
                                selected = mode == state.bandMode,
                                enabled = enabled,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    viewModel.setBandMode(mode)
                                    tunerIndex = tunerIndex.coerceAtMost(mode.bands - 1)
                                },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // View toggle -----------------------------------------------------------------------
            SettingsCard {
                Column(modifier = Modifier.padding(20.dp)) {
                    CardHeader(
                        icon = R.drawable.ic_show_chart,
                        title = stringResource(R.string.dolby_eq_view),
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ViewTile(
                            icon = R.drawable.ic_show_chart,
                            label = stringResource(R.string.dolby_eq_view_curve),
                            selected = view == EqView.CURVE,
                            modifier = Modifier.weight(1f),
                            onClick = { view = EqView.CURVE },
                        )
                        ViewTile(
                            icon = R.drawable.ic_tune,
                            label = stringResource(R.string.dolby_eq_view_sliders),
                            selected = view == EqView.SLIDERS,
                            modifier = Modifier.weight(1f),
                            onClick = { view = EqView.SLIDERS },
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (view == EqView.CURVE) {
                SettingsCard {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.dolby_eq_interactive_response),
                                modifier = Modifier.weight(1f),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Chip(stringResource(R.string.dolby_eq_band_chip, bandCount))
                        }
                        Text(
                            text =
                                stringResource(
                                    R.string.dolby_eq_interactive_response_desc,
                                    "${DolbyUiState.formatFrequency(frequencies.first())} Hz – " +
                                        "${DolbyUiState.formatFrequency(frequencies.last())} Hz",
                                ),
                            modifier = Modifier.padding(top = 4.dp),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        ResponseCurve(
                            gains = gains,
                            modifier = Modifier.padding(top = 10.dp),
                            height = 200.dp,
                            interactive = enabled,
                            onGainChange = viewModel::previewGain,
                            onGainChangeFinished = viewModel::commitGains,
                        )
                    }
                }
            } else {
                SettingsCard {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.dolby_eq_frequency_response),
                                modifier = Modifier.weight(1f),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Chip(
                                "${DolbyUiState.formatFrequency(frequencies.first())} Hz – " +
                                    "${DolbyUiState.formatFrequency(frequencies.last())} Hz"
                            )
                        }
                        MiniCurve(gains = gains, modifier = Modifier.padding(top = 12.dp))
                    }
                }

                Spacer(Modifier.height(12.dp))

                SettingsCard {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.dolby_eq_gain),
                                modifier = Modifier.weight(1f),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Chip(stringResource(R.string.dolby_eq_band_chip, bandCount))
                        }
                        Row(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .padding(top = 12.dp)
                                    .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            gains.forEachIndexed { index, gain ->
                                FaderBand(
                                    gainTenths = gain,
                                    frequency =
                                        DolbyUiState.formatFrequency(frequencies[index]) +
                                            if (frequencies[index] < 1000) "" else "",
                                    onChange = { tenths ->
                                        if (enabled) viewModel.previewGain(index, tenths)
                                    },
                                    onFinished = {
                                        if (enabled) viewModel.commitGains()
                                    },
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Band tuner ------------------------------------------------------------------------
            SettingsCard {
                Column(modifier = Modifier.padding(20.dp)) {
                    CardHeader(
                        icon = R.drawable.ic_graphic_eq,
                        title = stringResource(R.string.dolby_eq_band_tuner),
                    )
                    Text(
                        text = stringResource(R.string.dolby_eq_band_tuner_desc),
                        modifier = Modifier.padding(top = 4.dp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        StepButton(
                            icon = R.drawable.ic_arrow_left,
                            contentDescription = stringResource(R.string.dolby_eq_previous_band),
                            enabled = enabled && safeIndex > 0,
                            onClick = { tunerIndex = (safeIndex - 1).coerceAtLeast(0) },
                        )
                        Text(
                            text = stringResource(R.string.dolby_eq_band_position, safeIndex + 1, bandCount),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        StepButton(
                            icon = R.drawable.ic_arrow_right,
                            contentDescription = stringResource(R.string.dolby_eq_next_band),
                            enabled = enabled && safeIndex < bandCount - 1,
                            onClick = { tunerIndex = (safeIndex + 1).coerceAtMost(bandCount - 1) },
                        )
                    }

                    val tunerGain = gains.getOrElse(safeIndex) { 0 }
                    val tunerFrequency = frequencies.getOrElse(safeIndex) { 0 }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text =
                                stringResource(
                                    R.string.dolby_eq_band_label,
                                    safeIndex + 1,
                                    DolbyUiState.formatFrequency(tunerFrequency) + " Hz",
                                ),
                            modifier = Modifier.weight(1f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                text = "${DolbyUiState.formatGain(tunerGain)} dB",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }

                    Slider(
                        value = tunerGain / 10f,
                        onValueChange = { value ->
                            if (enabled) viewModel.previewGain(safeIndex, (value * 10f).toInt())
                        },
                        onValueChangeFinished = {
                            if (enabled) viewModel.commitGains()
                        },
                        valueRange = -15f..15f,
                        steps = 299,
                        enabled = enabled,
                        modifier = Modifier.padding(top = 8.dp),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilledTonalButton(
                            onClick = {
                                viewModel.previewGain(safeIndex, (tunerGain - 1).coerceIn(-150, 150))
                                viewModel.commitGains()
                            },
                            enabled = enabled,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.dolby_eq_decrement))
                        }
                        FilledTonalButton(
                            onClick = {
                                viewModel.previewGain(safeIndex, 0)
                                viewModel.commitGains()
                            },
                            enabled = enabled,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.dolby_eq_zero))
                        }
                        FilledTonalButton(
                            onClick = {
                                viewModel.previewGain(safeIndex, (tunerGain + 1).coerceIn(-150, 150))
                                viewModel.commitGains()
                            },
                            enabled = enabled,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.dolby_eq_increment))
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(
                text = "Dynamics processing is intentionally not part of this screen.",
                modifier = Modifier.padding(horizontal = 8.dp),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showSave) {
        SavePresetDialog(
            onDismiss = { showSave = false },
            onSave = { name ->
                viewModel.savePreset(name)
                showSave = false
            },
        )
    }
    if (showAutoEq) {
        AutoEqDialog(
            names = viewModel.presetNames,
            onDismiss = { showAutoEq = false },
            onSelect = {
                viewModel.applyPreset(it)
                showAutoEq = false
            },
        )
    }
    if (showImportExport) {
        InfoDialog(
            title = stringResource(R.string.dolby_eq_import_export_title),
            message = stringResource(R.string.dolby_eq_import_export_desc),
            onDismiss = { showImportExport = false },
        )
    }
}

@Composable
private fun CardHeader(icon: Int, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = title,
            modifier = Modifier.padding(start = 10.dp),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun Chip(text: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun ModeTile(
    number: String,
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier =
            modifier
                .height(80.dp)
                .clip(shape)
                .background(
                    if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHigh
                )
                .then(
                    if (selected) {
                        Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), shape)
                    } else {
                        Modifier
                    }
                )
                .clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier =
                Modifier.size(32.dp)
                    .clip(RoundedCornerShape(if (selected) 14.dp else 10.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color =
                    if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = label,
            modifier = Modifier.padding(top = 6.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color =
                if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ViewTile(
    icon: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier =
            modifier
                .height(72.dp)
                .clip(shape)
                .background(
                    if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHigh
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier.size(40.dp)
                    .clip(RoundedCornerShape(if (selected) 14.dp else 12.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint =
                    if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = label,
            modifier = Modifier.padding(start = 12.dp),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color =
                if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun StepButton(
    icon: Int,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier.width(64.dp)
                .height(40.dp)
                .clip(CircleShape)
                .background(
                    if (enabled) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHigh
                )
                .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp),
            tint =
                if (enabled) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
        )
    }
}

@Composable
private fun SavePresetDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dolby_eq_save_preset)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(stringResource(R.string.dolby_geq_preset_name)) },
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }, enabled = name.trim().isNotEmpty()) {
                Text(stringResource(R.string.dolby_eq_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dolby_eq_cancel)) }
        },
    )
}

@Composable
private fun AutoEqDialog(
    names: List<String>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dolby_eq_autoeq_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(R.string.dolby_eq_autoeq_desc),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                names.forEach { name ->
                    Text(
                        text = name,
                        modifier =
                            Modifier.fillMaxWidth()
                                .clickable { onSelect(name) }
                                .padding(vertical = 12.dp),
                        fontSize = 16.sp,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dolby_eq_close)) }
        },
    )
}

@Composable
private fun InfoDialog(title: String, message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dolby_eq_close)) }
        },
    )
}
