/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daybreak.dolby.ui.spatial

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.daybreak.dolby.R
import com.daybreak.dolby.ui.components.DayCard
import com.daybreak.dolby.ui.components.DayConfirmDialog
import com.daybreak.dolby.ui.components.DayOption
import com.daybreak.dolby.ui.components.DayRow
import com.daybreak.dolby.ui.components.DayScreen
import com.daybreak.dolby.ui.components.DaySectionHeader
import com.daybreak.dolby.ui.components.DaySelectDialog
import com.daybreak.dolby.ui.components.DaySwitchRow
import kotlinx.coroutines.launch

@Composable
fun SpatialScreen(viewModel: SpatialViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var stereoDialog by remember { mutableStateOf(false) }
    var dialogueDialog by remember { mutableStateOf(false) }
    var resetDialog by remember { mutableStateOf(false) }

    val stereoOptions = remember {
        context.resources.getStringArray(R.array.stereo_entries).toList()
    }
    val stereoValues = remember {
        context.resources.getStringArray(R.array.stereo_values).map { it.toInt() }
    }
    val dialogueOptions = remember {
        context.resources.getStringArray(R.array.dialogue_entries).toList()
    }
    val dialogueValues = remember {
        context.resources.getStringArray(R.array.dialogue_values).map { it.toInt() }
    }
    val stereoSummary = stereoOptions.getOrNull(stereoValues.indexOf(state.stereo))
    val dialogueSummary = dialogueOptions.getOrNull(dialogueValues.indexOf(state.dialogue))
    val headphonesHint = stringResource(R.string.spatial_headphones_hint)

    Box(Modifier.fillMaxSize()) {
        DayScreen {
            item { DaySectionHeader(text = stringResource(R.string.spatial_virtualizers)) }

            item {
                DayCard {
                    DaySwitchRow(
                        title = stringResource(R.string.spatial_speaker),
                        checked = state.spkVirt,
                        enabled = state.enabled,
                        onToggle = { viewModel.setSpeakerVirt(it) },
                        iconVector = Icons.Filled.Speaker,
                    )
                    DaySwitchRow(
                        title = stringResource(R.string.spatial_headphones),
                        summary = headphonesHint.takeIf { state.onSpeaker },
                        checked = state.hpVirt,
                        enabled = state.enabled && !state.onSpeaker,
                        onToggle = { viewModel.setHeadphoneVirt(it) },
                        iconVector = Icons.Filled.Headset,
                    )
                    DayRow(
                        title = stringResource(R.string.spatial_stereo),
                        summary = headphonesHint.takeIf { state.onSpeaker } ?: stereoSummary,
                        enabled = state.enabled && !state.onSpeaker,
                        onClick = { stereoDialog = true },
                        iconVector = Icons.Filled.SurroundSound,
                    )
                }
            }

            item { DaySectionHeader(text = stringResource(R.string.spatial_voice)) }

            item {
                DayCard {
                    DayRow(
                        title = stringResource(R.string.spatial_dialogue),
                        summary = dialogueSummary,
                        enabled = state.enabled,
                        onClick = { dialogueDialog = true },
                        iconVector = Icons.Filled.RecordVoiceOver,
                    )
                }
            }

            item {
                DayCard {
                    DayRow(
                        title = stringResource(R.string.spatial_reset),
                        enabled = state.enabled,
                        onClick = { resetDialog = true },
                        iconVector = Icons.Filled.Refresh,
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snack,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = 110.dp),
        )
    }

    if (stereoDialog) {
        DaySelectDialog(
            title = stringResource(R.string.spatial_stereo),
            options = stereoOptions.mapIndexed { i, label ->
                DayOption(stereoValues[i], label)
            },
            selected = state.stereo,
            onSelect = {
                viewModel.setStereo(it)
                stereoDialog = false
            },
            onDismiss = { stereoDialog = false },
            dismissLabel = context.getString(android.R.string.cancel),
        )
    }

    if (dialogueDialog) {
        DaySelectDialog(
            title = stringResource(R.string.spatial_dialogue),
            options = dialogueOptions.mapIndexed { i, label ->
                DayOption(dialogueValues[i], label)
            },
            selected = state.dialogue,
            onSelect = {
                viewModel.setDialogue(it)
                dialogueDialog = false
            },
            onDismiss = { dialogueDialog = false },
        )
    }

    if (resetDialog) {
        DayConfirmDialog(
            title = stringResource(R.string.spatial_reset_title),
            message = stringResource(R.string.spatial_reset_message),
            confirmLabel = stringResource(android.R.string.ok),
            dismissLabel = stringResource(android.R.string.cancel),
            destructive = true,
            onConfirm = {
                viewModel.resetProfile()
                scope.launch {
                    snack.showSnackbar(
                        context.getString(
                            R.string.spatial_reset_toast,
                            state.profileName ?: "",
                        ),
                    )
                }
            },
            onDismiss = { resetDialog = false },
        )
    }
}
