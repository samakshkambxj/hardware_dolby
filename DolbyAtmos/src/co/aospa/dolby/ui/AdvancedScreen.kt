/*
 * Copyright (C) 2026 samakshkambxj
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import co.aospa.dolby.R
import co.aospa.dolby.ui.components.SectionLabel
import co.aospa.dolby.ui.components.SettingSliderRow
import co.aospa.dolby.ui.components.SettingSwitchRow
import co.aospa.dolby.ui.components.SettingsCard
import co.aospa.dolby.ui.components.SettingsIcon
import kotlin.math.abs

@Composable
fun AdvancedScreen(
    state: DolbyUiState,
    viewModel: DolbyViewModel,
    modifier: Modifier = Modifier,
) {
    val enabled = state.enabled && state.available
    val stereoEntries = stringArrayResource(R.array.dolby_stereo_entries)
    val stereoValues = stringArrayResource(R.array.dolby_stereo_values)
        .mapNotNull { it.toIntOrNull() }
        .ifEmpty { listOf(4, 24, 44, 64) }
    val dialogueEntries = stringArrayResource(R.array.dolby_dialogue_entries)
    val dialogueValues = stringArrayResource(R.array.dolby_dialogue_values)
        .mapNotNull { it.toIntOrNull() }
        .ifEmpty { listOf(0, 2, 6, 9, 12) }
    val bassCurveEntries = stringArrayResource(R.array.dolby_bass_curve_entries)
    val ieqEntries = stringArrayResource(R.array.dolby_ieq_entries)

    var showReset by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (enabled) 1f else 0.5f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
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
        }

        item { SectionLabel(text = stringResource(R.string.advanced_immersion)) }

        item {
            SettingsCard(
                title = stringResource(R.string.advanced_immersion),
                icon = Icons.Rounded.SurroundSound,
                modifier = Modifier.alpha(if (enabled) 1f else 0.45f),
            ) {
                SettingSwitchRow(
                    title = stringResource(R.string.dolby_spk_virtualizer),
                    subtitle = stringResource(R.string.dolby_spk_virtualizer_summary),
                    icon = Icons.Rounded.Speaker,
                    checked = state.spkVirtualizer,
                    enabled = enabled,
                    onCheckedChange = viewModel::setSpeakerVirtualizer,
                )
                SettingSwitchRow(
                    title = stringResource(R.string.dolby_hp_virtualizer),
                    subtitle = if (state.onSpeaker) {
                        stringResource(R.string.advanced_unavailable_on_speaker)
                    } else {
                        stringResource(R.string.dolby_hp_virtualizer_summary)
                    },
                    icon = Icons.Rounded.Headphones,
                    checked = state.hpVirtualizer,
                    enabled = enabled && !state.onSpeaker,
                    onCheckedChange = viewModel::setHeadphoneVirtualizer,
                )
                if (state.stereoSupported) {
                    val index = nearestIndex(stereoValues, state.stereoWidening)
                    SettingSliderRow(
                        title = stringResource(R.string.dolby_stereo_widening),
                        supporting = if (state.onSpeaker) {
                            stringResource(R.string.advanced_unavailable_on_speaker)
                        } else {
                            stringResource(R.string.dolby_stereo_widening_summary)
                        },
                        value = index.toFloat(),
                        valueRange = 0f..(stereoValues.size - 1).toFloat(),
                        steps = (stereoValues.size - 2).coerceAtLeast(0),
                        valueLabel = stereoEntries.getOrElse(index) { "" },
                        enabled = enabled && !state.onSpeaker,
                        onValueChange = {},
                        onValueChangeFinished = {
                            viewModel.setStereoWidening(it.toInt())
                        },
                    )
                }
            }
        }

        item { SectionLabel(text = stringResource(R.string.advanced_clarity)) }

        item {
            SettingsCard(
                title = stringResource(R.string.advanced_clarity),
                icon = Icons.Rounded.RecordVoiceOver,
                modifier = Modifier.alpha(if (enabled) 1f else 0.45f),
            ) {
                Text(
                    text = stringResource(R.string.dolby_ieq),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = stringResource(R.string.dolby_ieq_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 8.dp),
                ) {
                    items(ieqEntries.size) { index ->
                        FilterChip(
                            selected = state.ieq == index,
                            onClick = { viewModel.setIeq(index) },
                            enabled = enabled,
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(ieqIcon(index)),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            label = { Text(ieqEntries[index]) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val dialogueIndex = nearestIndex(dialogueValues, state.dialogue)
                SettingSliderRow(
                    title = stringResource(R.string.dolby_dialogue_enhancer),
                    supporting = stringResource(R.string.dolby_dialogue_summary),
                    value = dialogueIndex.toFloat(),
                    valueRange = 0f..(dialogueValues.size - 1).toFloat(),
                    steps = (dialogueValues.size - 2).coerceAtLeast(0),
                    valueLabel = dialogueEntries.getOrElse(dialogueIndex) { "" },
                    enabled = enabled,
                    onValueChange = {},
                    onValueChangeFinished = { viewModel.setDialogue(it.toInt()) },
                )
            }
        }

        item { SectionLabel(text = stringResource(R.string.advanced_tuning)) }

        item {
            SettingsCard(
                title = stringResource(R.string.advanced_tuning),
                icon = Icons.Rounded.GraphicEq,
                modifier = Modifier.alpha(if (enabled) 1f else 0.45f),
            ) {
                TuningBlock(
                    title = stringResource(R.string.dolby_bass_enhancer),
                    subtitle = stringResource(R.string.dolby_bass_enhancer_summary),
                    icon = Icons.Rounded.MusicNote,
                    level = state.bassLevel,
                    fallback = 50,
                    enabled = enabled,
                    extra = {
                        if (state.bassLevel > 0) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(end = 8.dp),
                            ) {
                                items(bassCurveEntries.size) { index ->
                                    FilterChip(
                                        selected = state.bassCurve == index,
                                        onClick = { viewModel.setBassCurve(index) },
                                        enabled = enabled,
                                        label = { Text(bassCurveEntries[index]) },
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    },
                    onLevelChange = viewModel::setBassLevel,
                    levelLabel = stringResource(R.string.dolby_bass_level),
                )

                TuningBlock(
                    title = stringResource(R.string.dolby_mid_enhancer),
                    subtitle = stringResource(R.string.dolby_mid_enhancer_summary),
                    icon = Icons.Rounded.VolumeUp,
                    level = state.midLevel,
                    fallback = 40,
                    enabled = enabled,
                    onLevelChange = viewModel::setMidLevel,
                    levelLabel = stringResource(R.string.dolby_mid_level),
                )

                TuningBlock(
                    title = stringResource(R.string.dolby_treble_enhancer),
                    subtitle = stringResource(R.string.dolby_treble_enhancer_summary),
                    icon = Icons.Rounded.GraphicEq,
                    level = state.trebleLevel,
                    fallback = 30,
                    enabled = enabled,
                    onLevelChange = viewModel::setTrebleLevel,
                    levelLabel = stringResource(R.string.dolby_treble_level),
                )
            }
        }

        item { SectionLabel(text = stringResource(R.string.advanced_dynamics)) }

        item {
            SettingsCard(
                title = stringResource(R.string.advanced_dynamics),
                icon = Icons.Rounded.VolumeDown,
                modifier = Modifier.alpha(if (enabled) 1f else 0.45f),
            ) {
                SettingSwitchRow(
                    title = stringResource(R.string.dolby_volume_leveler),
                    subtitle = stringResource(R.string.dolby_volume_leveler_summary),
                    icon = Icons.Rounded.VolumeDown,
                    checked = state.volumeLeveler,
                    enabled = enabled,
                    onCheckedChange = viewModel::setLevelerEnabled,
                )
            }
        }

        item { SectionLabel(text = stringResource(R.string.advanced_reset)) }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (enabled) 1f else 0.45f)
                    .clickable(enabled = enabled) { showReset = true },
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SettingsIcon(Icons.Rounded.RestartAlt)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.dolby_reset_profile),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(
                                R.string.advanced_reset_description,
                                state.profileName.ifBlank {
                                    stringResource(R.string.dolby_profile_dynamic)
                                },
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text(stringResource(R.string.dolby_reset_profile)) },
            text = {
                Text(
                    stringResource(
                        R.string.advanced_reset_description,
                        state.profileName.ifBlank {
                            stringResource(R.string.dolby_profile_dynamic)
                        },
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showReset = false
                        viewModel.resetProfile()
                    }
                ) {
                    Text(stringResource(R.string.action_reset))
                }
            },
            dismissButton = {
                TextButton(onClick = { showReset = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun TuningBlock(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    level: Int,
    fallback: Int,
    enabled: Boolean,
    levelLabel: String,
    onLevelChange: (Int) -> Unit,
    extra: @Composable (() -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SettingSwitchRow(
            title = title,
            subtitle = subtitle,
            icon = icon,
            checked = level > 0,
            enabled = enabled,
            onCheckedChange = { on ->
                onLevelChange(if (on) fallback else 0)
            },
        )
        AnimatedVisibility(visible = level > 0) {
            Column(modifier = Modifier.padding(start = 36.dp)) {
                extra?.invoke()
                SettingSliderRow(
                    title = levelLabel,
                    value = level.toFloat(),
                    valueRange = 0f..100f,
                    steps = 19,
                    valueLabel = "$level%",
                    enabled = enabled,
                    onValueChange = {},
                    onValueChangeFinished = { onLevelChange(it.toInt()) },
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

private fun nearestIndex(values: List<Int>, value: Int): Int {
    if (values.isEmpty()) return 0
    var best = 0
    var bestDelta = Int.MAX_VALUE
    values.forEachIndexed { index, candidate ->
        val delta = abs(candidate - value)
        if (delta < bestDelta) {
            bestDelta = delta
            best = index
        }
    }
    return best
}

private fun ieqIcon(index: Int): Int = when (index) {
    1 -> R.drawable.ic_ieq_balanced
    2 -> R.drawable.ic_ieq_warm
    3 -> R.drawable.ic_ieq_detailed
    else -> R.drawable.ic_ieq_off
}
