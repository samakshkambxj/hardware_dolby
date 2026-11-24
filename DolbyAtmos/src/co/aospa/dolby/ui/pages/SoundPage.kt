/*
 * Copyright (C) 2026 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.ui.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.aospa.dolby.R
import co.aospa.dolby.ui.DolbyUiState
import co.aospa.dolby.ui.DolbyViewModel
import co.aospa.dolby.ui.components.ChevronRow
import co.aospa.dolby.ui.components.DolbyTopBar
import co.aospa.dolby.ui.components.IconBubble
import co.aospa.dolby.ui.components.ProfileCarousel
import co.aospa.dolby.ui.components.RowDivider
import co.aospa.dolby.ui.components.SectionHeader
import co.aospa.dolby.ui.components.SettingsCard
import co.aospa.dolby.ui.components.SwitchRow

@Composable
fun SoundPage(state: DolbyUiState, viewModel: DolbyViewModel, modifier: Modifier = Modifier) {
    var showIeq by remember { mutableStateOf(false) }
    val controls = state.controlsEnabled
    val output =
        if (state.isOnSpeaker) stringResource(R.string.dolby_speaker)
        else stringResource(R.string.dolby_headphones)
    val enabledSummary =
        if (state.enabled) stringResource(R.string.dolby_on_output, output)
        else stringResource(R.string.dolby_off_output, output)
    val ieqChoice = state.ieqChoices.firstOrNull { it.value == state.ieq }

    Column(modifier = modifier.fillMaxWidth()) {
        DolbyTopBar(title = stringResource(R.string.dolby_title))

        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 120.dp)
        ) {
            SettingsCard {
                SwitchRow(
                    title = stringResource(R.string.dolby_enable),
                    summary = enabledSummary,
                    checked = state.enabled,
                    onCheckedChange = viewModel::setEnabled,
                    icon = R.drawable.ic_volume_up,
                )
            }

            Spacer(Modifier.height(12.dp))

            SettingsCard {
                ProfileCarousel(
                    profiles = state.profiles,
                    selectedValue = state.profile,
                    enabled = state.enabled && state.profileKnown,
                    onSelect = viewModel::setProfile,
                    onReset = viewModel::resetProfile,
                    modifier = Modifier.padding(vertical = 18.dp),
                )
            }

            SectionHeader(stringResource(R.string.dolby_section_sound))
            SettingsCard {
                ChevronRow(
                    title = stringResource(R.string.dolby_ieq),
                    summary = ieqChoice?.name ?: stringResource(R.string.dolby_unknown),
                    onClick = { showIeq = true },
                    enabled = controls,
                    icon = ieqChoice?.iconRes,
                )
                RowDivider()
                SwitchRow(
                    title = stringResource(R.string.dolby_spk_virtualizer),
                    checked = state.speakerVirtualizer,
                    onCheckedChange = viewModel::setSpeakerVirtualizer,
                    enabled = controls,
                )
                RowDivider()
                SwitchRow(
                    title = stringResource(R.string.dolby_hp_virtualizer),
                    summary =
                        if (state.isOnSpeaker) stringResource(R.string.dolby_connect_headphones)
                        else null,
                    checked = state.headphoneVirtualizer,
                    onCheckedChange = viewModel::setHeadphoneVirtualizer,
                    enabled = controls && !state.isOnSpeaker,
                )
                RowDivider()
                SwitchRow(
                    title = stringResource(R.string.dolby_volume_leveler),
                    checked = state.volumeLeveler,
                    onCheckedChange = viewModel::setVolumeLeveler,
                    enabled = controls,
                )
            }

            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconBubble(
                    icon = R.drawable.ic_info,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Bass, mid and treble tuning live on the Advanced page.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showIeq) {
        IeqDialog(
            state = state,
            onDismiss = { showIeq = false },
            onSelect = {
                viewModel.setIeq(it)
                showIeq = false
            },
        )
    }
}

@Composable
private fun IeqDialog(
    state: DolbyUiState,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dolby_ieq)) },
        text = {
            Column {
                state.ieqChoices.forEach { choice ->
                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                                .clickable { onSelect(choice.value) }
                                .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconBubble(
                            icon = choice.iconRes,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = choice.name,
                            modifier = Modifier.weight(1f).padding(start = 16.dp),
                            fontSize = 16.sp,
                        )
                        RadioButton(
                            selected = choice.value == state.ieq,
                            onClick = { onSelect(choice.value) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dolby_eq_cancel))
            }
        },
    )
}
