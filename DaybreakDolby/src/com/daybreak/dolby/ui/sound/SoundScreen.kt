/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daybreak.dolby.ui.sound

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.daybreak.dolby.R
import com.daybreak.dolby.ui.components.DayCard
import com.daybreak.dolby.ui.components.DayHeroCard
import com.daybreak.dolby.ui.components.DayOption
import com.daybreak.dolby.ui.components.DayRow
import com.daybreak.dolby.ui.components.DayScreen
import com.daybreak.dolby.ui.components.DaySectionHeader
import com.daybreak.dolby.ui.components.DaySelectDialog
import com.daybreak.dolby.ui.components.DaySwitchRow
import com.daybreak.dolby.ui.components.ProfileGrid

@Composable
fun SoundScreen(viewModel: SoundViewModel) {
    val state by viewModel.uiState.collectAsState()
    var ieqDialog by remember { mutableStateOf(false) }

    DayScreen {
        item {
            DayHeroCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            text = state.profileName?.takeIf { state.enabled }
                                ?: stringResource(R.string.level_off),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = state.outputLabel,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    Switch(
                        checked = state.enabled,
                        onCheckedChange = { viewModel.setEnabled(it) },
                    )
                }
            }
        }

        item { DaySectionHeader(text = stringResource(R.string.sound_profiles)) }

        item {
            ProfileGrid(
                profile = state.profile,
                enabled = state.enabled,
                onSelect = { viewModel.setProfile(it) },
            )
        }

        item { DaySectionHeader(text = stringResource(R.string.sound_settings)) }

        item {
            DayCard {
                val context = LocalContext.current
                val ieqEntries = remember {
                    context.resources.getStringArray(R.array.ieq_entries).toList()
                }
                val ieqValues = remember {
                    context.resources.getStringArray(R.array.ieq_values).map { it.toInt() }
                }
                val ieqSummary = ieqEntries.getOrNull(ieqValues.indexOf(state.ieq))
                DayRow(
                    title = stringResource(R.string.sound_ieq),
                    summary = ieqSummary,
                    enabled = state.enabled,
                    onClick = { ieqDialog = true },
                )
                DaySwitchRow(
                    title = stringResource(R.string.sound_bass),
                    checked = state.bass,
                    enabled = state.enabled,
                    onToggle = { viewModel.setBass(it) },
                )
                DaySwitchRow(
                    title = stringResource(R.string.sound_leveler),
                    checked = state.leveler,
                    enabled = state.enabled,
                    onToggle = { viewModel.setLeveler(it) },
                )
            }
        }
    }

    if (ieqDialog) {
        val context = LocalContext.current
        val options = remember {
            val entries = context.resources.getStringArray(R.array.ieq_entries)
            val values = context.resources.getStringArray(R.array.ieq_values)
            entries.mapIndexed { i, label -> DayOption(values[i].toInt(), label) }
        }
        DaySelectDialog(
            title = stringResource(R.string.sound_ieq),
            options = options,
            selected = state.ieq,
            onSelect = {
                viewModel.setIeq(it)
                ieqDialog = false
            },
            onDismiss = { ieqDialog = false },
            dismissLabel = context.getString(android.R.string.cancel),
        )
    }
}

@Composable
private fun ProfileCard(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        onClick = onClick,
        enabled = enabled,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
fun ProfileGrid(
    profile: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
) {
    val context = LocalContext.current
    val entries = remember {
        context.resources.getStringArray(R.array.profile_entries).toList()
    }
    val values = remember {
        context.resources.getStringArray(R.array.profile_values).map { it.toInt() }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEachIndexed { i, label ->
                    val value = values[entries.indexOf(label)]
                    ProfileCard(
                        label = label,
                        selected = profile == value,
                        enabled = enabled,
                        onClick = { onSelect(value) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
