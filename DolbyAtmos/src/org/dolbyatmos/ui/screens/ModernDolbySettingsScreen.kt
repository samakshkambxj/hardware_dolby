/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package org.dolbyatmos.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.dolbyatmos.R
import org.dolbyatmos.data.SleepTimerState
import org.dolbyatmos.domain.models.DolbyUiState
import org.dolbyatmos.ui.components.ActiveAudioDeviceCard
import org.dolbyatmos.ui.components.AppProfileSettingsCard
import org.dolbyatmos.ui.components.BouncyPopIn
import org.dolbyatmos.ui.components.DolbyMainCard
import org.dolbyatmos.ui.components.ModernConfirmDialog
import org.dolbyatmos.ui.components.ModernIeqSelector
import org.dolbyatmos.ui.components.ModernProfileSelector
import org.dolbyatmos.ui.components.ModernSettingsCard
import org.dolbyatmos.ui.components.NotificationListenerPermissionCard
import org.dolbyatmos.ui.components.SleepTimerCard
import org.dolbyatmos.ui.components.StudioAmbientBackground
import org.dolbyatmos.ui.components.StudioHeader
import org.dolbyatmos.ui.components.StudioIconButton
import org.dolbyatmos.ui.components.StudioQuickTile
import org.dolbyatmos.ui.components.StudioSectionHeader
import org.dolbyatmos.ui.components.StyledParticles
import org.dolbyatmos.ui.components.rememberIsAudioPlaying
import org.dolbyatmos.ui.components.rememberPageStyle
import org.dolbyatmos.ui.components.verticalBouncyEdge
import org.dolbyatmos.ui.viewmodel.DolbyViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ModernDolbySettingsScreen(
    viewModel: DolbyViewModel,
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    val sleepState by viewModel.sleepState.collectAsState()
    val dirtyProfiles by viewModel.dirtyProfiles.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }
    val pageStyle by rememberPageStyle()

    Scaffold(
        topBar = {
            StudioHeader(
                eyebrow = stringResource(R.string.studio_eyebrow),
                title = if (pageStyle.showTitle) pageStyle.headerTitle else "",
                subtitle = if (pageStyle.showSubtitle) pageStyle.subtitleText else null,
                centerContent = pageStyle.headerCentered,
                actions = {
                    StudioIconButton(
                        icon = Icons.Default.Palette,
                        contentDescription = "Customization",
                        onClick = { navController.navigate(Screen.Customization.route) }
                    )
                    StudioIconButton(
                        icon = Icons.Default.Info,
                        contentDescription = "About",
                        onClick = { navController.navigate(Screen.About.route) }
                    )
                    StudioIconButton(
                        icon = Icons.Default.RestartAlt,
                        contentDescription = "Reset",
                        onClick = { showResetDialog = true }
                    )
                }
            )
        },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface
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
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = stringResource(R.string.loading),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                is DolbyUiState.Success -> {
                    ModernDolbySettingsContent(
                        state = state,
                        viewModel = viewModel,
                        navController = navController,
                        sleepState = sleepState,
                        dirtyProfiles = dirtyProfiles,
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

    if (showResetDialog) {
        ModernConfirmDialog(
            title = stringResource(R.string.dolby_reset_all),
            message = stringResource(R.string.dolby_reset_all_message),
            icon = Icons.Default.RestartAlt,
            onConfirm = {
                viewModel.resetAllProfiles()
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false }
        )
    }
}

@Composable
private fun ModernDolbySettingsContent(
    state: DolbyUiState.Success,
    viewModel: DolbyViewModel,
    navController: NavController,
    sleepState: SleepTimerState,
    dirtyProfiles: Set<Int>,
    modifier: Modifier = Modifier
) {
    val playing = rememberIsAudioPlaying()
    val live = state.settings.enabled && playing

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .verticalBouncyEdge(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 8.dp,
            bottom = 124.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item(key = "main_card") {
            BouncyPopIn(delayMillis = 0, key = "main_card") {
                DolbyMainCard(
                    enabled = state.settings.enabled,
                    live = live,
                    onEnabledChange = { viewModel.setDolbyEnabled(it) },
                    onEasterEggUnlocked = {
                        navController.navigate(Screen.EasterEgg.route)
                    }
                )
            }
        }

        item(key = "device_card") {
            BouncyPopIn(delayMillis = 30, key = "device_card") {
                ActiveAudioDeviceCard(
                    device = state.activeAudioDevice
                )
            }
        }

        item(key = "notif_card") {
            NotificationListenerPermissionCard()
        }

        item(key = "profile_selector") {
            AnimatedVisibility(
                visible = state.settings.enabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    StudioSectionHeader(
                        title = stringResource(R.string.dolby_profile_title),
                        eyebrow = "Sound profile"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    ModernProfileSelector(
                        currentProfile = state.settings.currentProfile,
                        onProfileChange = { viewModel.setProfile(it) },
                        dirtyProfiles = dirtyProfiles,
                        onResetProfile = { profile ->
                            viewModel.resetProfile(profile)
                        }
                    )
                }
            }
        }

        item(key = "ieq_selector") {
            AnimatedVisibility(
                visible = state.settings.enabled && state.settings.currentProfile != 0,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                ModernSettingsCard(
                    title = stringResource(R.string.dolby_ieq),
                    icon = Icons.Default.GraphicEq
                ) {
                    ModernIeqSelector(
                        currentPreset = state.profileSettings.ieqPreset,
                        onPresetChange = { viewModel.setIeqPreset(it) }
                    )
                }
            }
        }

        item(key = "quick_actions") {
            Column {
                StudioSectionHeader(
                    title = "Quick actions",
                    eyebrow = "Shortcuts"
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StudioQuickTile(
                        icon = Icons.Default.Apps,
                        title = "App profiles",
                        value = "Per-app sound rules",
                        onClick = { navController.navigate(Screen.AppProfiles.route) },
                        modifier = Modifier.weight(1f)
                    )
                    StudioQuickTile(
                        icon = Icons.Default.Palette,
                        title = "Customization",
                        value = "Dock, accent, motion",
                        onClick = { navController.navigate(Screen.Customization.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item(key = "sleep_timer") {
            AnimatedVisibility(
                visible = state.settings.enabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    StudioSectionHeader(
                        title = "Wind down",
                        eyebrow = "Sleep timer",
                        subtitle = "Fade Dolby out automatically."
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SleepTimerCard(
                        state = sleepState,
                        onStart = { viewModel.startSleepTimer(it) },
                        onCancel = { viewModel.cancelSleepTimer() }
                    )
                }
            }
        }

        item(key = "app_profiles") {
            AnimatedVisibility(
                visible = state.settings.enabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                AppProfileSettingsCard(
                    onManageClick = { navController.navigate(Screen.AppProfiles.route) }
                )
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
