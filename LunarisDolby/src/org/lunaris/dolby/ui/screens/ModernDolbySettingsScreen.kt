/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.lunaris.dolby.R
import org.lunaris.dolby.data.SleepTimerState
import org.lunaris.dolby.domain.models.DolbyUiState
import org.lunaris.dolby.domain.models.Scene
import org.lunaris.dolby.ui.components.*
import org.lunaris.dolby.ui.viewmodel.DolbyViewModel
import org.lunaris.dolby.utils.ToastHelper

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ModernDolbySettingsScreen(
    viewModel: DolbyViewModel,
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    val scenes by viewModel.scenes.collectAsState()
    val sleepState by viewModel.sleepState.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }
    var showCreditsDialog by remember { mutableStateOf(false) }
    var showSaveSceneDialog by remember { mutableStateOf(false) }
    var showResetScenesDialog by remember { mutableStateOf(false) }
    var sceneName by remember { mutableStateOf("") }
    var sceneToDelete by remember { mutableStateOf<Scene?>(null) }
    val context = LocalContext.current
    val homeListState = rememberLazyListState()
    val homeScrollFraction = rememberTopBarScrollFraction(homeListState)

    Scaffold(
        topBar = {
            LunarisGlassTopBar(
                scrollFraction = homeScrollFraction,
                title = {
                    Column {
                        Text(
                            stringResource(R.string.dolby_title),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            stringResource(R.string.dolby_summary),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                expandedHeight = 92.dp,
                actions = {
                    IconButton(onClick = { showCreditsDialog = true }) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Credits",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(
                            Icons.Default.RestartAlt,
                            contentDescription = "Reset",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
        FloatingParticles()
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
                    listState = homeListState,
                    scenes = scenes,
                    sleepState = sleepState,
                    onApplyScene = { scene ->
                        viewModel.applyScene(scene)
                        ToastHelper.showToast(context, context.getString(R.string.scene_applied))
                    },
                    onSaveSceneClick = {
                        sceneName = ""
                        showSaveSceneDialog = true
                    },
                    onDeleteSceneClick = { sceneToDelete = it },
                    onResetScenesClick = { showResetScenesDialog = true },
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
    
    if (showCreditsDialog) {
        CreditsDialog(
            onDismiss = { showCreditsDialog = false }
        )
    }

    if (showSaveSceneDialog) {
        SaveSceneDialog(
            name = sceneName,
            onNameChange = { sceneName = it },
            onConfirm = {
                viewModel.saveScene(sceneName)
                showSaveSceneDialog = false
            },
            onDismiss = { showSaveSceneDialog = false }
        )
    }

    sceneToDelete?.let { scene ->
        ModernConfirmDialog(
            title = stringResource(R.string.scene_delete_title),
            message = stringResource(R.string.scene_delete_message, scene.name),
            icon = Icons.Default.Delete,
            onConfirm = {
                viewModel.deleteScene(scene.id)
                sceneToDelete = null
            },
            onDismiss = { sceneToDelete = null }
        )
    }

    if (showResetScenesDialog) {
        ModernConfirmDialog(
            title = stringResource(R.string.scene_reset_title),
            message = stringResource(R.string.scene_reset_message),
            icon = Icons.Default.RestartAlt,
            onConfirm = {
                viewModel.resetScenes()
                showResetScenesDialog = false
            },
            onDismiss = { showResetScenesDialog = false }
        )
    }
}

@Composable
private fun ModernDolbySettingsContent(
    state: DolbyUiState.Success,
    viewModel: DolbyViewModel,
    navController: NavController,
    listState: androidx.compose.foundation.lazy.LazyListState,
    scenes: List<Scene>,
    sleepState: SleepTimerState,
    onApplyScene: (Scene) -> Unit,
    onSaveSceneClick: () -> Unit,
    onDeleteSceneClick: (Scene) -> Unit,
    onResetScenesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .verticalBouncyEdge(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "main_card") {
            BouncyPopIn(delayMillis = 0, key = "home:main_card") {
                DolbyMainCard(
                    enabled = state.settings.enabled,
                    onEnabledChange = { viewModel.setDolbyEnabled(it) }
                )
            }
        }

        item(key = "device_card") {
            BouncyPopIn(delayMillis = 30, key = "home:device_card") {
                ActiveAudioDeviceCard(device = state.activeAudioDevice)
            }
        }

        item(key = "notif_card") {
            BouncyPopIn(delayMillis = 60, key = "home:notif_card") {
                NotificationListenerPermissionCard()
            }
        }

        item(key = "profile") {
            AnimatedVisibility(
                visible = state.settings.enabled,
                enter = fadeIn() + expandVertically(
                    animationSpec = BouncySpecs.enterSize
                ),
                exit = fadeOut() + shrinkVertically()
            ) {
                BouncyPopIn(delayMillis = 0, key = "home:profile") {
                    ModernProfileSelector(
                        currentProfile = state.settings.currentProfile,
                        onProfileChange = { viewModel.setProfile(it) }
                    )
                }
            }
        }

        item(key = "ieq") {
            AnimatedVisibility(
                visible = state.settings.enabled && state.settings.currentProfile != 0,
                enter = fadeIn() + expandVertically(
                    animationSpec = BouncySpecs.enterSize
                ),
                exit = fadeOut() + shrinkVertically()
            ) {
                BouncyPopIn(delayMillis = 0, key = "home:ieq") {
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
        }

        item(key = "scenes") {
            AnimatedVisibility(
                visible = state.settings.enabled,
                enter = fadeIn() + expandVertically(
                    animationSpec = BouncySpecs.enterSize
                ),
                exit = fadeOut() + shrinkVertically()
            ) {
                BouncyPopIn(delayMillis = 0, key = "home:scenes") {
                    SceneSection(
                        scenes = scenes,
                        onApply = onApplyScene,
                        onSaveClick = onSaveSceneClick,
                        onDeleteClick = onDeleteSceneClick,
                        onResetClick = onResetScenesClick,
                        hasCustomScenes = scenes.any { !it.isBuiltIn }
                    )
                }
            }
        }

        item(key = "sleep") {
            AnimatedVisibility(
                visible = state.settings.enabled,
                enter = fadeIn() + expandVertically(
                    animationSpec = BouncySpecs.enterSize
                ),
                exit = fadeOut() + shrinkVertically()
            ) {
                BouncyPopIn(delayMillis = 0, key = "home:sleep") {
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
                enter = fadeIn() + expandVertically(
                    animationSpec = BouncySpecs.enterSize
                ),
                exit = fadeOut() + shrinkVertically()
            ) {
                BouncyPopIn(delayMillis = 0, key = "home:app_profiles") {
                    AppProfileSettingsCard(
                        onManageClick = { navController.navigate("app_profiles") }
                    )
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(70.dp))
        }
    }
}
