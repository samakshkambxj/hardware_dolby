/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package org.dolbyatmos.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import org.dolbyatmos.R
import org.dolbyatmos.domain.models.DolbyUiState
import org.dolbyatmos.ui.components.BalanceCard
import org.dolbyatmos.ui.components.ExperimentalCard
import org.dolbyatmos.ui.components.ModernSettingSelector
import org.dolbyatmos.ui.components.ModernSettingSlider
import org.dolbyatmos.ui.components.ModernSettingSwitch
import org.dolbyatmos.ui.components.ModernSettingsCard
import org.dolbyatmos.ui.components.MonoCard
import org.dolbyatmos.ui.components.RegisterScrollToTop
import org.dolbyatmos.ui.components.StudioAmbientBackground
import org.dolbyatmos.ui.components.StudioHeader
import org.dolbyatmos.ui.components.StudioSectionHeader
import org.dolbyatmos.ui.components.StyledParticles
import org.dolbyatmos.ui.components.verticalBouncyEdge
import org.dolbyatmos.ui.viewmodel.DolbyViewModel
import org.dolbyatmos.utils.ToastHelper

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ModernAdvancedSettingsScreen(
    viewModel: DolbyViewModel,
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    val balance by viewModel.channelBalance.collectAsState()
    val balanceError by viewModel.balanceError.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(balanceError) {
        balanceError?.let {
            ToastHelper.showToast(context, it)
            viewModel.clearBalanceError()
        }
    }

    Scaffold(
        topBar = {
            StudioHeader(
                eyebrow = "Tuning",
                title = stringResource(R.string.dolby_category_adv_settings),
                subtitle = "Shape the engine, one layer at a time"
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            StudioAmbientBackground()
            StyledParticles(modifier = Modifier.padding(paddingValues))
            when (val state = uiState) {
                is DolbyUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is DolbyUiState.Success -> {
                    ModernAdvancedSettingsContent(
                        state = state,
                        viewModel = viewModel,
                        balance = balance,
                        onBalanceChange = { viewModel.setChannelBalance(it) },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
                is DolbyUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernAdvancedSettingsContent(
    state: DolbyUiState.Success,
    viewModel: DolbyViewModel,
    balance: Float,
    onBalanceChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scrollScope = rememberCoroutineScope()

    RegisterScrollToTop(
        visible = listState.firstVisibleItemIndex > 3,
        onTop = { scrollScope.launch { listState.animateScrollToItem(0) } },
    )

    fun match(vararg keywords: String): Boolean {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return true
        return keywords.any { it.lowercase().contains(q) }
    }

    val showEnhance = match(
        "bass", "mid", "treble", "curve", "enhancer", "audio tuning",
        "leveler", "volume", "loudness"
    )
    val showImmersion = state.settings.currentProfile != 0 && match(
        "surround", "virtualizer", "widening", "headphone", "speaker",
        "stage", "width", "dialogue", "voice", "speech", "clarity"
    )
    val showOutput = match("balance", "left", "right", "channel", "mono", "stereo", "hearing")
    val showExperimental = match("experimental", "tuning lab", "reverb", "height", "dap", "raw")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .verticalBouncyEdge(),
        state = listState,
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 8.dp,
            bottom = 124.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "search") {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = {
                            Text(
                                stringResource(R.string.advanced_search_hint),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        singleLine = true
                    )
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (!state.settings.enabled) {
            item(key = "disabled_notice") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.dolby_adv_settings_footer),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        if (state.settings.enabled && showEnhance) {
            item(key = "enhance_header") {
                StudioSectionHeader(
                    title = "Enhance",
                    eyebrow = "Profile tuning",
                    subtitle = "Tone and loudness for the current profile."
                )
            }

            if (match("bass", "mid", "treble", "curve", "enhancer", "audio tuning")) {
                item(key = "eq_settings") {
                    ModernSettingsCard(
                        title = stringResource(R.string.dolby_category_settings),
                        icon = Icons.Default.Tune
                    ) {
                        Column {
                            ModernSettingSwitch(
                                title = stringResource(R.string.dolby_bass_enhancer),
                                subtitle = stringResource(R.string.dolby_bass_enhancer_summary),
                                checked = state.profileSettings.bassLevel > 0,
                                onCheckedChange = { enabled ->
                                    if (enabled && state.profileSettings.bassLevel == 0) {
                                        viewModel.setBassLevel(50)
                                    } else if (!enabled) {
                                        viewModel.setBassLevel(0)
                                    }
                                },
                                icon = Icons.Default.MusicNote
                            )

                            AnimatedVisibility(visible = state.profileSettings.bassLevel > 0) {
                                Column {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    ModernSettingSelector(
                                        title = stringResource(R.string.dolby_bass_curve),
                                        currentValue = state.profileSettings.bassCurve,
                                        entries = R.array.dolby_bass_curve_entries,
                                        values = R.array.dolby_bass_curve_values,
                                        onValueChange = { viewModel.setBassCurve(it) },
                                        icon = Icons.Default.Equalizer
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    ModernSettingSlider(
                                        title = stringResource(R.string.dolby_bass_level),
                                        value = state.profileSettings.bassLevel,
                                        onValueChange = { viewModel.setBassLevel(it.toInt()) },
                                        valueRange = 0f..100f,
                                        steps = 19,
                                        valueLabel = { "$it%" }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Column {
                            ModernSettingSwitch(
                                title = stringResource(R.string.dolby_mid_enhancer),
                                subtitle = stringResource(R.string.dolby_mid_enhancer_summary),
                                checked = state.profileSettings.midLevel > 0,
                                onCheckedChange = { enabled ->
                                    if (enabled && state.profileSettings.midLevel == 0) {
                                        viewModel.setMidLevel(40)
                                    } else if (!enabled) {
                                        viewModel.setMidLevel(0)
                                    }
                                },
                                icon = Icons.Default.VolumeUp
                            )

                            AnimatedVisibility(visible = state.profileSettings.midLevel > 0) {
                                Column {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    ModernSettingSlider(
                                        title = stringResource(R.string.dolby_mid_level),
                                        value = state.profileSettings.midLevel,
                                        onValueChange = { viewModel.setMidLevel(it.toInt()) },
                                        valueRange = 0f..100f,
                                        steps = 19,
                                        valueLabel = { "$it%" }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Column {
                            ModernSettingSwitch(
                                title = stringResource(R.string.dolby_treble_enhancer),
                                subtitle = stringResource(R.string.dolby_treble_enhancer_summary),
                                checked = state.profileSettings.trebleLevel > 0,
                                onCheckedChange = { enabled ->
                                    if (enabled && state.profileSettings.trebleLevel == 0) {
                                        viewModel.setTrebleLevel(30)
                                    } else if (!enabled) {
                                        viewModel.setTrebleLevel(0)
                                    }
                                },
                                icon = Icons.Default.GraphicEq
                            )

                            AnimatedVisibility(visible = state.profileSettings.trebleLevel > 0) {
                                Column {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    ModernSettingSlider(
                                        title = stringResource(R.string.dolby_treble_level),
                                        value = state.profileSettings.trebleLevel,
                                        onValueChange = { viewModel.setTrebleLevel(it.toInt()) },
                                        valueRange = 0f..100f,
                                        steps = 19,
                                        valueLabel = { "$it%" }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (match("leveler", "volume", "loudness")) {
                item(key = "volume_leveler") {
                    ModernSettingsCard(
                        title = "Volume Leveler",
                        icon = Icons.Default.VolumeDown
                    ) {
                        ModernSettingSwitch(
                            title = stringResource(R.string.dolby_volume_leveler),
                            subtitle = stringResource(R.string.dolby_volume_leveler_summary),
                            checked = state.settings.volumeLevelerEnabled,
                            onCheckedChange = { viewModel.setVolumeLeveler(it) },
                            icon = Icons.Default.BarChart
                        )
                    }
                }
            }
        }

        if (state.settings.enabled && showImmersion) {
            item(key = "immersion_header") {
                StudioSectionHeader(
                    title = "Immersion",
                    eyebrow = "Space",
                    subtitle = "Width and dialogue, tuned to the active output."
                )
            }

            if (match("surround", "virtualizer", "widening", "headphone", "speaker", "stage", "width")) {
                item(key = "surround_virtualizer") {
                    ModernSettingsCard(
                        title = "Surround Virtualizer",
                        icon = Icons.Default.Headphones
                    ) {
                        if (state.isOnSpeaker) {
                            ModernSettingSwitch(
                                title = stringResource(R.string.dolby_spk_virtualizer),
                                subtitle = stringResource(R.string.dolby_spk_virtualizer_summary),
                                checked = state.profileSettings.speakerVirtualizerEnabled,
                                onCheckedChange = { viewModel.setSpeakerVirtualizer(it) },
                                icon = Icons.Default.Speaker
                            )

                            AnimatedVisibility(visible = state.profileSettings.speakerVirtualizerEnabled) {
                                Column {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    ModernSettingSlider(
                                        title = stringResource(R.string.dolby_hp_virtualizer_dolby_strength),
                                        value = state.profileSettings.stereoWideningAmount,
                                        onValueChange = { viewModel.setStereoWidening(it.toInt()) },
                                        valueRange = 4f..64f,
                                        steps = 59
                                    )
                                }
                            }
                        } else {
                            ModernSettingSwitch(
                                title = stringResource(R.string.dolby_hp_virtualizer),
                                subtitle = stringResource(R.string.dolby_hp_virtualizer_summary),
                                checked = state.profileSettings.headphoneVirtualizerEnabled,
                                onCheckedChange = { viewModel.setHeadphoneVirtualizer(it) },
                                icon = Icons.Default.Headphones
                            )

                            AnimatedVisibility(visible = state.profileSettings.headphoneVirtualizerEnabled) {
                                Column {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    ModernSettingSlider(
                                        title = stringResource(R.string.dolby_hp_virtualizer_dolby_strength),
                                        value = state.profileSettings.stereoWideningAmount,
                                        onValueChange = { viewModel.setStereoWidening(it.toInt()) },
                                        valueRange = 4f..64f,
                                        steps = 59
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (match("dialogue", "voice", "speech", "clarity")) {
                item(key = "dialogue_enhancement") {
                    ModernSettingsCard(
                        title = "Dialogue Enhancement",
                        icon = Icons.Default.RecordVoiceOver
                    ) {
                        ModernSettingSwitch(
                            title = stringResource(R.string.dolby_dialogue_enhancer),
                            subtitle = stringResource(R.string.dolby_dialogue_enhancer_summary),
                            checked = state.profileSettings.dialogueEnhancerEnabled,
                            onCheckedChange = { viewModel.setDialogueEnhancer(it) },
                            icon = Icons.Default.RecordVoiceOver
                        )

                        AnimatedVisibility(visible = state.profileSettings.dialogueEnhancerEnabled) {
                            Column {
                                Spacer(modifier = Modifier.height(16.dp))
                                ModernSettingSlider(
                                    title = stringResource(R.string.dolby_dialogue_enhancer_dolby_strength),
                                    value = state.profileSettings.dialogueEnhancerAmount,
                                    onValueChange = { viewModel.setDialogueEnhancerAmount(it.toInt()) },
                                    valueRange = 1f..12f,
                                    steps = 10
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showOutput) {
            item(key = "output_header") {
                StudioSectionHeader(
                    title = "Output",
                    eyebrow = "Channels",
                    subtitle = "Balance and mono compatibility."
                )
            }

            if (match("balance", "left", "right", "channel")) {
                item(key = "balance") {
                    BalanceCard(
                        balance = balance,
                        onBalanceChange = onBalanceChange
                    )
                }
            }

            if (match("mono", "stereo", "channel", "balance", "hearing")) {
                item(key = "mono") {
                    MonoCard()
                }
            }
        }

        if (state.settings.enabled && showExperimental) {
            item(key = "experimental_header") {
                StudioSectionHeader(
                    title = "Experimental",
                    eyebrow = "Raw DAP",
                    subtitle = "Only parameters your HAL answers to."
                )
            }

            item(key = "experimental") {
                ExperimentalCard(
                    labParams = state.profileSettings.labParams,
                    onParamChange = { paramId, value ->
                        viewModel.setLabParam(paramId, value)
                    },
                    onReset = { viewModel.resetLabParams() },
                    forceExpand = query.isNotEmpty()
                )
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
