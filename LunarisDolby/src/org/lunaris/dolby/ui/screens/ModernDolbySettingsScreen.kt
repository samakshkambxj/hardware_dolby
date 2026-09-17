/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
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
    val dirtyProfiles by viewModel.dirtyProfiles.collectAsState()
    var showSaveSceneDialog by remember { mutableStateOf(false) }
    var showResetScenesDialog by remember { mutableStateOf(false) }
    var sceneName by remember { mutableStateOf("") }
    var showOutputDialog by remember { mutableStateOf(false) }
    val outputDevices by viewModel.outputDevices.collectAsState()
    val outputError by viewModel.outputError.collectAsState()
    val pageStyle by rememberPageStyle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(outputError) {
        outputError?.let {
            ToastHelper.showToast(context, it)
            viewModel.clearOutputError()
        }
    }

    Scaffold(
        topBar = {
            // Custom header row instead of TopAppBar's title slot: M3's
            // internal title inset does not line up with the 16dp body
            // padding, leaving the "Dolby Atmos" title offset from the
            // cards below. Explicit 16dp start padding here keeps header
            // text and body content on the same left edge on every M3
            // version. End stays at 8dp so the action icons never sit
            // flush with (or clipped by) the screen edge.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
                    .heightIn(min = 64.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = if (pageStyle.headerCentered) Alignment.CenterHorizontally
                    else Alignment.Start
                ) {
                    if (pageStyle.showTitle) {
                        Row(
                            modifier = if (pageStyle.headerCentered) Modifier.fillMaxWidth()
                            else Modifier,
                            horizontalArrangement = if (pageStyle.headerCentered) Arrangement.Center
                            else Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                pageStyle.headerTitle,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = if (pageStyle.headerCentered) TextAlign.Center
                                else TextAlign.Start
                            )
                            // Live dot: Dolby on AND audio actually
                            // playing — not just the master switch.
                            val playing = rememberIsAudioPlaying()
                            val live = (uiState as? DolbyUiState.Success)
                                ?.settings?.enabled == true && playing
                            if (live) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.FiberManualRecord,
                                    contentDescription = stringResource(
                                        R.string.dolby_live_dot
                                    ),
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }
                    if (pageStyle.showSubtitle) {
                        Text(
                            pageStyle.subtitleText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = if (pageStyle.headerCentered) TextAlign.Center
                            else TextAlign.Start,
                            modifier = if (pageStyle.headerCentered) Modifier.fillMaxWidth()
                            else Modifier
                        )
                    }
                }
                IconButton(onClick = { navController.navigate(Screen.Customization.route) }) {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = "Customization",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = { navController.navigate(Screen.About.route) }) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "About",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = {
                    // Direct reset — no confirm dialog, no toast.
                    viewModel.resetAllProfiles()
                }) {
                    Icon(
                        Icons.Default.RestartAlt,
                        contentDescription = "Reset",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
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
                    scenes = scenes,
                    sleepState = sleepState,
                    dirtyProfiles = dirtyProfiles,
                    snackbarHost = snackbarHost,
                    onOutputCardClick = {
                        viewModel.refreshOutputDevices()
                        showOutputDialog = true
                    },
                    onApplyScene = { scene ->
                        viewModel.applyScene(scene)
                        ToastHelper.showToast(context, context.getString(R.string.scene_applied))
                    },
                    onSaveSceneClick = {
                        sceneName = ""
                        showSaveSceneDialog = true
                    },
                    onDeleteSceneClick = { scene ->
                        if (!scene.isBuiltIn) {
                            viewModel.deleteScene(scene.id)
                            scope.launch {
                                val res = snackbarHost.showSnackbar(
                                    message = context.getString(R.string.scene_deleted),
                                    actionLabel = context.getString(R.string.undo),
                                    withDismissAction = true
                                )
                                if (res == SnackbarResult.ActionPerformed) {
                                    viewModel.restoreScene(scene)
                                }
                            }
                        }
                    },
                    onResetScenesClick = { showResetScenesDialog = true },
                    onExportSceneClick = { scene ->
                        viewModel.exportSceneJson(scene.id)?.let { json ->
                            clipboard.setText(AnnotatedString(json))
                            ToastHelper.showToast(
                                context,
                                context.getString(R.string.scene_export_copied)
                            )
                        }
                    },
                    onImportSceneClick = {
                        val text = clipboard.getText()?.text.orEmpty()
                        viewModel.importScenes(text) { count ->
                            ToastHelper.showToast(
                                context,
                                if (count > 0) {
                                    context.getString(R.string.scene_import_done, count)
                                } else {
                                    context.getString(R.string.scene_import_failed)
                                }
                            )
                        }
                    },
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

    if (showOutputDialog) {
        AudioOutputDialog(
            devices = outputDevices,
            onSelect = { key ->
                viewModel.selectOutputDevice(key)
                showOutputDialog = false
            },
            onDismiss = { showOutputDialog = false }
        )
    }
}

@Composable
private fun ModernDolbySettingsContent(
    state: DolbyUiState.Success,
    viewModel: DolbyViewModel,
    navController: NavController,
    scenes: List<Scene>,
    sleepState: SleepTimerState,
    dirtyProfiles: Set<Int>,
    snackbarHost: SnackbarHostState,
    onOutputCardClick: () -> Unit,
    onApplyScene: (Scene) -> Unit,
    onSaveSceneClick: () -> Unit,
    onDeleteSceneClick: (Scene) -> Unit,
    onResetScenesClick: () -> Unit,
    onExportSceneClick: (Scene) -> Unit,
    onImportSceneClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .verticalBouncyEdge(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "main_card") {
            BouncyPopIn(delayMillis = 0, key = "main_card") {
                DolbyMainCard(
                    enabled = state.settings.enabled,
                    onEnabledChange = { viewModel.setDolbyEnabled(it) },
                    onEasterEggUnlocked = { navController.navigate(Screen.EasterEgg.route) }
                )
            }
        }

        item(key = "device_card") {
            BouncyPopIn(delayMillis = 30, key = "device_card") {
                ActiveAudioDeviceCard(
                    device = state.activeAudioDevice,
                    onClick = onOutputCardClick
                )
            }
        }

        item(key = "notif_card") {
            BouncyPopIn(delayMillis = 60, key = "notif_card") {
                NotificationListenerPermissionCard()
            }
        }

        item(key = "profile_selector") {
            AnimatedVisibility(
                visible = state.settings.enabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
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

        item(key = "scene_section") {
            AnimatedVisibility(
                visible = state.settings.enabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                SceneSection(
                    scenes = scenes,
                    onApply = onApplyScene,
                    onSaveClick = onSaveSceneClick,
                    onDeleteClick = onDeleteSceneClick,
                    onResetClick = onResetScenesClick,
                    onExportClick = onExportSceneClick,
                    onImportClick = onImportSceneClick,
                    hasCustomScenes = scenes.any { !it.isBuiltIn }
                )
            }
        }

        item(key = "sleep_timer") {
            AnimatedVisibility(
                visible = state.settings.enabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                SleepTimerCard(
                    state = sleepState,
                    onStart = { viewModel.startSleepTimer(it) },
                    onCancel = { viewModel.cancelSleepTimer() }
                )
            }
        }

        item(key = "app_profiles") {
            AnimatedVisibility(
                visible = state.settings.enabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                AppProfileSettingsCard(
                    onManageClick = { navController.navigate("app_profiles") }
                )
            }
        }
        
        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(70.dp))
        }
    }
}
