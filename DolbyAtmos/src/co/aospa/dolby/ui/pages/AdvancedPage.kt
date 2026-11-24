/*
 * Copyright (C) 2026 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.ui.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.aospa.dolby.R
import co.aospa.dolby.ui.DolbyUiState
import co.aospa.dolby.ui.DolbyViewModel
import co.aospa.dolby.ui.LevelChoice
import co.aospa.dolby.ui.components.ChevronRow
import co.aospa.dolby.ui.components.DolbyTopBar
import co.aospa.dolby.ui.components.IconBubble
import co.aospa.dolby.ui.components.InfoRow
import co.aospa.dolby.ui.components.RowDivider
import co.aospa.dolby.ui.components.SectionHeader
import co.aospa.dolby.ui.components.SettingsCard
import co.aospa.dolby.ui.components.SwitchRow

@Composable
fun AdvancedPage(state: DolbyUiState, viewModel: DolbyViewModel, modifier: Modifier = Modifier) {
    var showBassCurve by remember { mutableStateOf(false) }
    var showDialogue by remember { mutableStateOf(false) }
    var showStereo by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    val controls = state.controlsEnabled

    val bassCurve = state.bassCurveChoices.firstOrNull { it.value == state.bassCurve }
    val dialogue = state.dialogueChoices.firstOrNull { it.value == state.dialogueEnhancer }
    val stereo = state.stereoChoices.firstOrNull { it.value == state.stereoWidening }
    val output =
        if (state.isOnSpeaker) stringResource(R.string.dolby_speaker)
        else stringResource(R.string.dolby_headphones)

    Column(modifier = modifier.fillMaxWidth()) {
        DolbyTopBar(title = stringResource(R.string.dolby_page_advanced))

        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 120.dp)
        ) {
            SectionHeader(stringResource(R.string.dolby_section_audio_tuning))
            SettingsCard {
                SwitchRow(
                    title = stringResource(R.string.dolby_bass_enhancer),
                    summary = stringResource(R.string.dolby_bass_enhancer_desc),
                    checked = state.bassEnhancer,
                    onCheckedChange = viewModel::setBassEnhancer,
                    enabled = controls,
                    icon = R.drawable.ic_music_note,
                )
                if (state.bassEnhancer) {
                    Column(modifier = Modifier.padding(start = 60.dp, end = 20.dp)) {
                        CurveField(
                            label = stringResource(R.string.dolby_bass_curve),
                            value = bassCurve?.label ?: "",
                            enabled = controls,
                            onClick = { showBassCurve = true },
                        )
                        LevelSlider(
                            label = stringResource(R.string.dolby_bass_level),
                            value = state.bassLevel,
                            enabled = controls,
                            onChange = viewModel::setBassLevel,
                            onFinished = viewModel::commitBassLevel,
                        )
                    }
                }

                RowDivider()

                SwitchRow(
                    title = stringResource(R.string.dolby_mid_enhancer),
                    summary = stringResource(R.string.dolby_mid_enhancer_desc),
                    checked = state.midEnhancer,
                    onCheckedChange = viewModel::setMidEnhancer,
                    enabled = controls,
                    icon = R.drawable.ic_volume_up,
                )
                if (state.midEnhancer) {
                    Column(modifier = Modifier.padding(start = 60.dp, end = 20.dp)) {
                        LevelSlider(
                            label = stringResource(R.string.dolby_mid_level),
                            value = state.midLevel,
                            enabled = controls,
                            onChange = viewModel::setMidLevel,
                            onFinished = viewModel::commitMidLevel,
                        )
                    }
                }

                RowDivider()

                SwitchRow(
                    title = stringResource(R.string.dolby_treble_enhancer),
                    summary = stringResource(R.string.dolby_treble_enhancer_desc),
                    checked = state.trebleEnhancer,
                    onCheckedChange = viewModel::setTrebleEnhancer,
                    enabled = controls,
                    icon = R.drawable.ic_graphic_eq,
                )
                if (state.trebleEnhancer) {
                    Column(modifier = Modifier.padding(start = 60.dp, end = 20.dp)) {
                        LevelSlider(
                            label = stringResource(R.string.dolby_treble_level),
                            value = state.trebleLevel,
                            enabled = controls,
                            onChange = viewModel::setTrebleLevel,
                            onFinished = viewModel::commitTrebleLevel,
                        )
                    }
                }
            }

            SectionHeader(stringResource(R.string.dolby_section_playback))
            SettingsCard {
                ChevronRow(
                    title = stringResource(R.string.dolby_dialogue_enhancer),
                    summary = dialogue?.label,
                    onClick = { showDialogue = true },
                    enabled = controls,
                    icon = R.drawable.ic_chat,
                )
                RowDivider()
                ChevronRow(
                    title = stringResource(R.string.dolby_stereo_widening),
                    summary =
                        if (state.isOnSpeaker || !state.stereoWideningSupported) {
                            stringResource(R.string.dolby_connect_headphones)
                        } else {
                            stereo?.label
                        },
                    onClick = { showStereo = true },
                    enabled = controls && !state.isOnSpeaker && state.stereoWideningSupported,
                    icon = R.drawable.ic_spatial,
                )
            }

            SectionHeader(stringResource(R.string.dolby_section_device))
            SettingsCard {
                InfoRow(
                    title = stringResource(R.string.dolby_audio_output),
                    summary = output,
                    icon =
                        if (state.isOnSpeaker) R.drawable.ic_speaker
                        else R.drawable.ic_headphones,
                )
                RowDivider()
                InfoRow(
                    title = stringResource(R.string.dolby_dolby_status),
                    summary =
                        if (state.enabled) {
                            stringResource(R.string.dolby_status_active, state.profileName)
                        } else {
                            stringResource(R.string.dolby_off)
                        },
                    icon = R.drawable.ic_check,
                )
            }

            SectionHeader(stringResource(R.string.dolby_section_maintenance))
            SettingsCard {
                ChevronRow(
                    title = stringResource(R.string.dolby_reset_current_profile),
                    onClick = { showReset = true },
                    enabled = controls,
                    icon = R.drawable.ic_refresh,
                )
                RowDivider()
                ChevronRow(
                    title = stringResource(R.string.dolby_about),
                    summary = stringResource(R.string.dolby_version, viewModel.repository.versionName()),
                    onClick = { showAbout = true },
                    icon = R.drawable.ic_info,
                )
            }

            Spacer(Modifier.height(18.dp))
            Text(
                text = "Settings apply to the current profile and audio output.",
                modifier = Modifier.padding(horizontal = 8.dp),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showBassCurve) {
        ChoiceDialog(
            title = stringResource(R.string.dolby_bass_curve),
            choices = state.bassCurveChoices,
            selected = state.bassCurve,
            onDismiss = { showBassCurve = false },
            onSelect = {
                viewModel.setBassCurve(it)
                showBassCurve = false
            },
        )
    }
    if (showDialogue) {
        ChoiceDialog(
            title = stringResource(R.string.dolby_dialogue_enhancer),
            choices = state.dialogueChoices,
            selected = state.dialogueEnhancer,
            onDismiss = { showDialogue = false },
            onSelect = {
                viewModel.setDialogueEnhancer(it)
                showDialogue = false
            },
        )
    }
    if (showStereo) {
        ChoiceDialog(
            title = stringResource(R.string.dolby_stereo_widening),
            choices = state.stereoChoices,
            selected = state.stereoWidening,
            onDismiss = { showStereo = false },
            onSelect = {
                viewModel.setStereoWidening(it)
                showStereo = false
            },
        )
    }
    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text(stringResource(R.string.dolby_reset_current_profile)) },
            text = { Text(stringResource(R.string.dolby_geq_reset_gains_prompt)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetProfile()
                        showReset = false
                    }
                ) {
                    Text(stringResource(R.string.dolby_eq_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showReset = false }) {
                    Text(stringResource(R.string.dolby_eq_cancel))
                }
            },
        )
    }
    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text(stringResource(R.string.dolby_about)) },
            text = { Text(stringResource(R.string.dolby_version, viewModel.repository.versionName())) },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) {
                    Text(stringResource(R.string.dolby_eq_close))
                }
            },
        )
    }
}

@Composable
private fun CurveField(
    label: String,
    value: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun LevelSlider(
    label: String,
    value: Int,
    enabled: Boolean,
    onChange: (Int) -> Unit,
    onFinished: () -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.dolby_percent, value),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color =
                    if (enabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            onValueChangeFinished = onFinished,
            valueRange = 0f..100f,
            steps = 99,
            enabled = enabled,
        )
    }
}

@Composable
private fun ChoiceDialog(
    title: String,
    choices: List<LevelChoice>,
    selected: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                choices.forEach { choice ->
                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                                .clickable { onSelect(choice.value) }
                                .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = choice.label, modifier = Modifier.weight(1f), fontSize = 16.sp)
                        RadioButton(
                            selected = choice.value == selected,
                            onClick = { onSelect(choice.value) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dolby_eq_cancel)) }
        },
    )
}
