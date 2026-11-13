/*
 * Copyright (C) 2026 samakshkambxj
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.aospa.dolby.R
import co.aospa.dolby.ui.components.EmptyStateCard
import co.aospa.dolby.ui.components.GainCurve
import co.aospa.dolby.ui.components.QuickToggleTile
import co.aospa.dolby.ui.components.SectionLabel
import co.aospa.dolby.ui.components.SettingsIcon

@Composable
fun HomeScreen(
    state: DolbyUiState,
    onToggleEnabled: (Boolean) -> Unit,
    onProfileSelected: (Int) -> Unit,
    onBassToggle: (Boolean) -> Unit,
    onLevelerToggle: (Boolean) -> Unit,
    onDialogueToggle: (Boolean) -> Unit,
    onVirtualizerToggle: (Boolean) -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenAdvanced: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = state.enabled && state.available
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { HeroCard(state = state, onToggleEnabled = onToggleEnabled) }

        if (!state.available) {
            item {
                EmptyStateCard(
                    title = stringResource(R.string.home_unavailable_title),
                    body = stringResource(R.string.home_unavailable_body),
                )
            }
        }

        item {
            SectionLabel(
                text = stringResource(R.string.dolby_profile_title),
                modifier = Modifier.alpha(if (enabled) 1f else 0.45f),
            )
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 8.dp),
            ) {
                items(state.profiles, key = { it.id }) { profile ->
                    FilterChip(
                        selected = state.profile == profile.id,
                        onClick = { onProfileSelected(profile.id) },
                        enabled = enabled,
                        label = { Text(profile.name) },
                    )
                }
            }
        }

        item { SectionLabel(text = stringResource(R.string.home_quick_controls)) }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickToggleTile(
                        title = stringResource(R.string.dolby_bass_enhancer),
                        subtitle = onOff(state.bassLevel > 0),
                        icon = Icons.Rounded.MusicNote,
                        checked = state.bassLevel > 0,
                        enabled = enabled,
                        onClick = { onBassToggle(state.bassLevel == 0) },
                        modifier = Modifier.weight(1f),
                    )
                    QuickToggleTile(
                        title = stringResource(R.string.dolby_volume_leveler),
                        subtitle = onOff(state.volumeLeveler),
                        icon = Icons.Rounded.VolumeDown,
                        checked = state.volumeLeveler,
                        enabled = enabled,
                        onClick = { onLevelerToggle(!state.volumeLeveler) },
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickToggleTile(
                        title = stringResource(R.string.dolby_dialogue_enhancer),
                        subtitle = onOff(state.dialogue > 0),
                        icon = Icons.Rounded.RecordVoiceOver,
                        checked = state.dialogue > 0,
                        enabled = enabled,
                        onClick = { onDialogueToggle(state.dialogue == 0) },
                        modifier = Modifier.weight(1f),
                    )
                    QuickToggleTile(
                        title = if (state.onSpeaker) {
                            stringResource(R.string.dolby_spk_virtualizer)
                        } else {
                            stringResource(R.string.dolby_hp_virtualizer)
                        },
                        subtitle = onOff(state.activeVirtualizer),
                        icon = if (state.onSpeaker) Icons.Rounded.SurroundSound
                        else Icons.Rounded.Headphones,
                        checked = state.activeVirtualizer,
                        enabled = enabled,
                        onClick = { onVirtualizerToggle(!state.activeVirtualizer) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        item { SectionLabel(text = stringResource(R.string.home_shortcuts)) }

        item {
            EqualizerShortcut(
                state = state,
                enabled = enabled,
                onClick = onOpenEqualizer,
            )
        }

        item {
            ShortcutCard(
                title = stringResource(R.string.advanced_title),
                subtitle = stringResource(R.string.home_advanced_summary),
                icon = Icons.Rounded.Tune,
                enabled = enabled,
                onClick = onOpenAdvanced,
            )
        }
    }
}

@Composable
private fun HeroCard(state: DolbyUiState, onToggleEnabled: (Boolean) -> Unit) {
    val shape = RoundedCornerShape(30.dp)
    val brush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.secondaryContainer,
        )
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(brush),
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_dolby),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.dolby_title).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.6.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = if (state.enabled) {
                            stringResource(R.string.home_status_on)
                        } else {
                            stringResource(R.string.home_status_off)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                    )
                }
                Switch(
                    checked = state.enabled,
                    onCheckedChange = onToggleEnabled,
                    enabled = state.available,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = state.profileName.ifBlank {
                        stringResource(R.string.dolby_profile_dynamic)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = " · ",
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                )
                Icon(
                    imageVector = Icons.Rounded.Equalizer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = state.outputName.ifBlank {
                        stringResource(R.string.dolby_unknown)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun EqualizerShortcut(
    state: DolbyUiState,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(enabled = enabled) { onClick() },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SettingsIcon(Icons.Rounded.GraphicEq)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.dolby_preset),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = state.presetName.ifBlank {
                            stringResource(R.string.dolby_preset_default)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            GainCurve(
                gains = state.displayGains,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
            )
        }
    }
}

@Composable
private fun ShortcutCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(enabled = enabled) { onClick() },
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
            SettingsIcon(icon)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun onOff(value: Boolean): String =
    stringResource(if (value) R.string.dolby_on else R.string.dolby_off)
