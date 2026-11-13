/*
 * Copyright (C) 2026 samakshkambxj
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.ui

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.aospa.dolby.DolbyConstants
import co.aospa.dolby.R
import co.aospa.dolby.ui.components.GainCurve
import co.aospa.dolby.ui.components.VerticalFader

@Composable
fun EqualizerScreen(
    state: DolbyUiState,
    viewModel: DolbyViewModel,
    modifier: Modifier = Modifier,
) {
    val enabled = state.enabled && state.available
    var menuExpanded by remember { mutableStateOf(false) }
    var showSave by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (enabled) 1f else 0.5f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.GraphicEq,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(
                    R.string.advanced_editing_profile,
                    state.profileName.ifBlank {
                        stringResource(R.string.dolby_profile_dynamic)
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { menuExpanded = true },
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = state.presetName.ifBlank {
                            stringResource(R.string.dolby_preset_default)
                        },
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        textAlign = TextAlign.Start,
                    )
                    Icon(
                        imageVector = Icons.Rounded.ArrowDropDown,
                        contentDescription = null,
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    state.presets.forEach { preset ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(preset.name)
                                    if (preset.isUserDefined) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.dolby_geq_yours),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            },
                            onClick = {
                                menuExpanded = false
                                viewModel.applyPreset(preset)
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = { showSave = true },
                enabled = enabled,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.dolby_geq_save_preset),
                )
            }
            if (state.userPresetActive) {
                IconButton(onClick = { showRename = true }, enabled = enabled) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = stringResource(R.string.dolby_geq_rename_preset),
                    )
                }
                IconButton(onClick = { showDelete = true }, enabled = enabled) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.dolby_geq_delete_preset),
                    )
                }
            }
            IconButton(onClick = { showReset = true }, enabled = enabled) {
                Icon(
                    imageVector = Icons.Rounded.RestartAlt,
                    contentDescription = stringResource(R.string.dolby_geq_reset_gains),
                )
            }
        }

        GainCurve(
            gains = state.displayGains,
            interactive = enabled,
            onGainChange = { index, gain -> viewModel.previewGain(index, gain) },
            onGainChangeFinished = { viewModel.commitGains() },
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .alpha(if (enabled) 1f else 0.5f),
        )

        Text(
            text = stringResource(R.string.dolby_geq_drag_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (enabled) 1f else 0.5f),
            textAlign = TextAlign.Center,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (enabled) 1f else 0.5f),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            state.displayGains.forEachIndexed { index, gain ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = formatGain(gain),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (gain == 0) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    VerticalFader(
                        value = gain,
                        enabled = enabled,
                        onValueChange = { viewModel.previewGain(index, it) },
                        onValueChangeFinished = { viewModel.commitGains() },
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = DolbyConstants.frequencyLabel(
                            DolbyConstants.DISPLAY_FREQUENCIES.getOrElse(index) { 0 }
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.dolby_geq_slider_label_gain),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (enabled) 1f else 0.5f),
            textAlign = TextAlign.Center,
        )
    }

    if (showSave) {
        PresetNameDialog(
            title = stringResource(R.string.dolby_geq_save_preset),
            initial = "",
            validate = { viewModel.validatePresetName(it) },
            onDismiss = { showSave = false },
            onSubmit = { name ->
                viewModel.savePreset(name)
                showSave = false
            },
        )
    }

    if (showRename) {
        PresetNameDialog(
            title = stringResource(R.string.dolby_geq_rename_preset),
            initial = state.presetName,
            validate = { viewModel.validatePresetName(it, exclude = state.presetName) },
            onDismiss = { showRename = false },
            onSubmit = { name ->
                viewModel.renamePreset(name)
                showRename = false
            },
        )
    }

    if (showDelete) {
        ConfirmDialog(
            title = stringResource(R.string.dolby_geq_delete_preset),
            body = stringResource(R.string.dolby_geq_delete_preset_prompt),
            confirmLabel = stringResource(R.string.action_delete),
            onDismiss = { showDelete = false },
            onConfirm = {
                viewModel.deletePreset()
                showDelete = false
            },
        )
    }

    if (showReset) {
        ConfirmDialog(
            title = stringResource(R.string.dolby_geq_reset_gains),
            body = stringResource(R.string.dolby_geq_reset_gains_prompt),
            confirmLabel = stringResource(R.string.action_reset),
            onDismiss = { showReset = false },
            onConfirm = {
                viewModel.resetEqualizer()
                showReset = false
            },
        )
    }
}

@Composable
private fun PresetNameDialog(
    title: String,
    initial: String,
    validate: (String) -> PresetNameError?,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    var error by remember { mutableStateOf<PresetNameError?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (error != null) error = null
                    },
                    singleLine = true,
                    label = { Text(stringResource(R.string.dolby_geq_preset_name)) },
                    isError = error != null,
                )
                error?.let {
                    Text(
                        text = it.message(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val failure = validate(name)
                    if (failure != null) {
                        error = failure
                    } else {
                        onSubmit(name)
                    }
                }
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun PresetNameError.message(): String = when (this) {
    PresetNameError.EMPTY -> stringResource(R.string.dolby_geq_preset_name_empty)
    PresetNameError.EXISTS -> stringResource(R.string.dolby_geq_preset_name_exists)
    PresetNameError.TOO_LONG -> stringResource(R.string.dolby_geq_preset_name_too_long)
}

@Composable
private fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

private fun formatGain(gain: Int): String {
    val db = gain / 10f
    return when {
        db == 0f -> "0"
        db > 0f -> "+%.1f".format(db)
        else -> "%.1f".format(db)
    }
}
