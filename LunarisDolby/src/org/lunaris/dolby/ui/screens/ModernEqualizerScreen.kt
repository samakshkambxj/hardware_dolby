/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import org.lunaris.dolby.R
import org.lunaris.dolby.data.EasterEggs
import org.lunaris.dolby.data.autoeq.*
import org.lunaris.dolby.ui.components.*
import org.lunaris.dolby.ui.viewmodel.EqualizerViewModel
import org.lunaris.dolby.ui.viewmodel.DynamicsEqualizerViewModel
import org.lunaris.dolby.ui.viewmodel.EnhancementsViewModel
import org.lunaris.dolby.ui.viewmodel.VeynFxViewModel
import org.lunaris.dolby.audio.FrameworkEnhancementsEngine
import org.lunaris.dolby.domain.models.*
import org.lunaris.dolby.utils.*

enum class EqualizerViewMode {
    CURVE,
    SLIDERS
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ModernEqualizerScreen(
    viewModel: EqualizerViewModel,
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()
    var showAutoEqDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(EqualizerViewMode.CURVE) }
    val snackbarHost = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                // Keep action icons off the screen edge (M3 only insets 4.dp).
                modifier = Modifier.padding(end = 8.dp),
                title = { 
                    Text(
                        stringResource(R.string.dolby_preset),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    ) 
                },
                actions = {
                    IconButton(onClick = { showSaveDialog = true }) {
                        Icon(
                            Icons.Default.Save, 
                            contentDescription = "Save",
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
                    IconButton(onClick = { navController.navigate("import_export") }) {
                        Icon(
                            Icons.Default.ImportExport, 
                            contentDescription = "Import/Export",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { showAutoEqDialog = true }) {
                        Icon(
                            Icons.Default.Headphones,
                            contentDescription = "AutoEQ",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (uiState is EqualizerUiState.Success) {
                        val state = uiState as EqualizerUiState.Success
                        if (state.currentPreset.isUserDefined) {
                            IconButton(onClick = {
                                val deleted = state.currentPreset
                                viewModel.deletePreset(deleted)
                                scope.launch {
                                    val res = snackbarHost.showSnackbar(
                                        message = context.getString(R.string.preset_deleted),
                                        actionLabel = context.getString(R.string.undo),
                                        withDismissAction = true
                                    )
                                    if (res == SnackbarResult.ActionPerformed) {
                                        viewModel.restorePreset(deleted)
                                    }
                                }
                            }) {
                                Icon(
                                    Icons.Default.Delete, 
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
        StyledParticles(modifier = Modifier.padding(paddingValues))
        when (val state = uiState) {
            is EqualizerUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is EqualizerUiState.Success -> {
                ModernEqualizerContent(
                    state = state,
                    viewModel = viewModel,
                    viewMode = viewMode,
                    onViewModeChange = { viewMode = it },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is EqualizerUiState.Error -> {
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

    if (showSaveDialog) {
        SavePresetDialog(
            onSave = { name ->
                EasterEggs.checkPresetName(name)?.let { code ->
                    if (EasterEggs.isMaintainerCode(code)) {
                        EasterEggs.unlock(context, EasterEggs.BADGE_MAINTAINER)
                    } else {
                        EasterEggs.unlock(context, EasterEggs.BADGE_WORDSMITH)
                    }
                    scope.launch {
                        haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.HEAVY_CLICK)
                    }
                    ToastHelper.showToast(context, context.getString(EasterEggs.cheatMessageRes(code)))
                }
                val error = viewModel.savePreset(name)
                if (error == null) {
                    showSaveDialog = false
                }
                error
            },
            onDismiss = { showSaveDialog = false }
        )
    }

    if (showResetDialog) {
        ModernConfirmDialog(
            title = stringResource(R.string.dolby_geq_reset_gains),
            message = stringResource(R.string.dolby_geq_reset_gains_prompt),
            icon = Icons.Default.RestartAlt,
            onConfirm = {
                viewModel.resetGains()
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false }
        )
    }

    if (showAutoEqDialog) {
        AutoEqSelectionDialog(
            viewModel = viewModel,
            onDismiss = { showAutoEqDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ModernEqualizerContent(
    state: EqualizerUiState.Success,
    viewModel: EqualizerViewModel,
    viewMode: EqualizerViewMode,
    onViewModeChange: (EqualizerViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val isFlatPreset = state.currentPreset.name == stringResource(R.string.dolby_preset_default)
    val scrollState = rememberScrollState()
    val isBandModeCompatible = state.currentPreset.bandMode == state.bandMode
    val canEdit = isBandModeCompatible || isFlatPreset
    val isActive = canEdit && !isFlatPreset
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalBouncyEdge()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        EqualizerCard {
            ModernPresetSelector(
                presets = state.presets,
                currentPreset = state.currentPreset,
                onPresetSelected = { viewModel.setPreset(it) }
            )
        }
        
        BandModeSelector(
            currentMode = state.bandMode,
            onModeChange = { viewModel.setBandMode(it) }
        )
        
        if (!isBandModeCompatible && !isFlatPreset) {
            EqualizerCard(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.band_mode_mismatch),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "This ${state.currentPreset.bandMode.displayName} preset cannot be edited in ${state.bandMode.displayName} mode. " +
                                  "Switch to ${state.currentPreset.bandMode.displayName} or select a compatible preset.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
        
        EqualizerCard {
            Column(modifier = Modifier.padding(20.dp)) {
                EqualizerSectionHeader(
                    icon = Icons.Default.Visibility,
                    title = "Equalizer View",
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ViewModeTile(
                        title = "Curve",
                        icon = Icons.Default.ShowChart,
                        isSelected = viewMode == EqualizerViewMode.CURVE,
                        onClick = { onViewModeChange(EqualizerViewMode.CURVE) },
                        modifier = Modifier.weight(1f)
                    )
                    
                    ViewModeTile(
                        title = "Sliders",
                        icon = Icons.Default.Tune,
                        isSelected = viewMode == EqualizerViewMode.SLIDERS,
                        onClick = { onViewModeChange(EqualizerViewMode.SLIDERS) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        val viewTransitionSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
        AnimatedContent(
            targetState = viewMode,
            transitionSpec = {
                fadeIn(animationSpec = viewTransitionSpec) togetherWith
                fadeOut(animationSpec = viewTransitionSpec)
            },
            label = "equalizer_view_transition"
        ) { mode ->
            when (mode) {
                EqualizerViewMode.CURVE -> {
                    CurveViewContent(
                        state = state,
                        viewModel = viewModel,
                        canEdit = canEdit,
                        isActive = isActive
                    )
                }
                EqualizerViewMode.SLIDERS -> {
                    SlidersViewContent(
                        state = state,
                        viewModel = viewModel,
                        canEdit = canEdit
                    )
                }
            }
        }

        BandTunerCard(
            bandGains = state.bandGains,
            bandMode = state.bandMode,
            onGainChange = { index, gain ->
                if (canEdit) {
                    viewModel.setBandGain(index, gain)
                }
            },
            enabled = canEdit
        )

        DynamicsProcessingSection()

        VeynFxSection()

        Spacer(modifier = Modifier.height(70.dp))
    }
}

/**
 * Card wrapper for the equalizer page that honors Page Style > Cards and
 * Corners (shape, fill, outline, elevation) like [ModernSettingsCard] does,
 * instead of hardcoding extraLarge + surfaceContainerLow.
 */
@Composable
private fun EqualizerCard(
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    content: @Composable () -> Unit
) {
    val pageStyle by rememberPageStyle()
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = pageStyle.cardShape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor ?: pageStyle.cardContainer()
        ),
        border = pageStyle.cardBorder(),
        elevation = pageStyle.cardElevation()
    ) {
        content()
    }
}

@Composable
private fun CurveViewContent(
    state: EqualizerUiState.Success,
    viewModel: EqualizerViewModel,
    canEdit: Boolean,
    isActive: Boolean
) {
    EqualizerCard(
        modifier = Modifier.height(380.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (canEdit) "Interactive Frequency Response" 
                          else "Frequency Response (Read-only)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (canEdit) MaterialTheme.colorScheme.onSurface
                          else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (canEdit) MaterialTheme.colorScheme.secondaryContainer
                           else MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = "${state.bandMode.bandCount} bands",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (canEdit) MaterialTheme.colorScheme.onSecondaryContainer
                              else MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Text(
                text = if (canEdit) 
                    "Drag the control points to adjust gain (±15 dB) • ${getFrequencyRange(state.bandGains)}"
                else
                    "Read-only view • Band mode mismatch",
                style = MaterialTheme.typography.bodySmall,
                color = if (canEdit) MaterialTheme.colorScheme.onSurfaceVariant
                      else MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
            InteractiveFrequencyResponseCurve(
                bandGains = state.bandGains,
                onBandGainChange = { index, newGain ->
                    if (canEdit) {
                        viewModel.setBandGain(index, newGain)
                    }
                },
                isActive = isActive,
                isEditable = canEdit,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

@Composable
private fun SlidersViewContent(
    state: EqualizerUiState.Success,
    viewModel: EqualizerViewModel,
    canEdit: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        EqualizerCard(
            modifier = Modifier.height(180.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.frequency_response),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = getFrequencyRange(state.bandGains),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                FrequencyResponseCurve(
                    bandGains = state.bandGains,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }

        EqualizerCard(
            modifier = Modifier.height(380.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (canEdit) stringResource(R.string.dolby_geq_slider_label_gain)
                              else "${stringResource(R.string.dolby_geq_slider_label_gain)} (Read-only)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (canEdit) MaterialTheme.colorScheme.onSurface
                              else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (!canEdit) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = stringResource(R.string.locked),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(
                        items = state.bandGains,
                        key = { _, bandGain -> bandGain.frequency }
                    ) { index, bandGain ->
                        ModernEqualizerBand(
                            frequency = bandGain.frequency,
                            gain = bandGain.gain,
                            onGainChange = { newGain ->
                                if (canEdit) {
                                    viewModel.setBandGain(index, newGain)
                                }
                            },
                            enabled = canEdit
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EqualizerSectionHeader(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
                   else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BandTunerCard(
    bandGains: List<BandGain>,
    bandMode: BandMode,
    onGainChange: (Int, Int) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    if (bandGains.isEmpty()) return

    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()
    var selectedIndex by remember(bandMode, bandGains.size) { mutableIntStateOf(0) }
    val index = selectedIndex.coerceIn(0, bandGains.lastIndex)
    val band = bandGains[index]

    var sliderValue by remember(index, band.gain) { mutableFloatStateOf(band.gain / 10f) }
    var lastHapticStep by remember(index) { mutableIntStateOf(band.gain) }

    EqualizerCard(
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            EqualizerSectionHeader(
                icon = Icons.Default.GraphicEq,
                title = stringResource(R.string.band_tuner),
                enabled = enabled,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                text = stringResource(R.string.band_tuner_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BandStepButton(
                    icon = Icons.Default.ChevronLeft,
                    contentDescription = stringResource(R.string.band_tuner_previous),
                    enabled = enabled && index > 0,
                    onClick = { selectedIndex = index - 1 }
                )

                Text(
                    text = stringResource(
                        R.string.band_tuner_position,
                        index + 1,
                        bandGains.size
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                BandStepButton(
                    icon = Icons.Default.ChevronRight,
                    contentDescription = stringResource(R.string.band_tuner_next),
                    enabled = enabled && index < bandGains.lastIndex,
                    onClick = { selectedIndex = index + 1 }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(
                        R.string.band_tuner_label,
                        index + 1,
                        formatFrequency(band.frequency)
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (enabled) MaterialTheme.colorScheme.primaryContainer
                           else MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Text(
                        text = formatGain(sliderValue),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Slider(
                value = sliderValue,
                onValueChange = { newValue ->
                    val step = (newValue * 10).toInt()
                    if (step != lastHapticStep) {
                        scope.launch {
                            haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.TEXTURE_TICK)
                        }
                        lastHapticStep = step
                    }
                    sliderValue = newValue
                },
                onValueChangeFinished = {
                    onGainChange(index, (sliderValue * 10).toInt())
                },
                enabled = enabled,
                valueRange = -15f..15f,
                steps = 299,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    activeTickColor = MaterialTheme.colorScheme.onPrimary,
                    inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer,
                    inactiveTickColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    disabledThumbColor = MaterialTheme.colorScheme.outline,
                    disabledActiveTrackColor = MaterialTheme.colorScheme.outline,
                    disabledInactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BandAdjustButton(
                    label = stringResource(R.string.band_tuner_decrement),
                    enabled = enabled && band.gain > -150,
                    onClick = {
                        val newGain = (band.gain - 1).coerceAtLeast(-150)
                        sliderValue = newGain / 10f
                        onGainChange(index, newGain)
                    },
                    modifier = Modifier.weight(1f)
                )
                BandAdjustButton(
                    label = stringResource(R.string.band_tuner_reset),
                    enabled = enabled && band.gain != 0,
                    onClick = {
                        sliderValue = 0f
                        onGainChange(index, 0)
                    },
                    modifier = Modifier.weight(1f)
                )
                BandAdjustButton(
                    label = stringResource(R.string.band_tuner_increment),
                    enabled = enabled && band.gain < 150,
                    onClick = {
                        val newGain = (band.gain + 1).coerceAtMost(150)
                        sliderValue = newGain / 10f
                        onGainChange(index, newGain)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BandStepButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()

    Surface(
        onClick = {
            scope.launch {
                haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.CLICK)
            }
            onClick()
        },
        enabled = enabled,
        modifier = Modifier
            .size(width = 64.dp, height = 40.dp)
            .squishable(enabled = enabled, scaleDown = 0.9f),
        shape = MaterialTheme.shapes.extraLarge,
        color = if (enabled) MaterialTheme.colorScheme.secondaryContainer
               else MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp),
                tint = if (enabled) MaterialTheme.colorScheme.onSecondaryContainer
                      else MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun BandAdjustButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()

    Surface(
        onClick = {
            scope.launch {
                haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.TICK)
            }
            onClick()
        },
        enabled = enabled,
        modifier = modifier
            .height(48.dp)
            .squishable(enabled = enabled, scaleDown = 0.93f),
        shape = MaterialTheme.shapes.extraLarge,
        color = if (enabled) MaterialTheme.colorScheme.secondaryContainer
               else MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = if (enabled) MaterialTheme.colorScheme.onSecondaryContainer
                       else MaterialTheme.colorScheme.outline
            )
        }
    }
}

private fun formatFrequency(frequency: Int): String =
    if (frequency >= 1000) "%.1f kHz".format(frequency / 1000f) else "$frequency Hz"

private fun formatGain(gain: Float): String {
    val rounded = Math.round(gain * 10f) / 10f
    return if (rounded > 0f) "+%.1f dB".format(rounded) else "%.1f dB".format(rounded + 0f)
}

private fun getFrequencyRange(bandGains: List<BandGain>): String {
    if (bandGains.isEmpty()) return ""
    return "${formatFrequency(bandGains.minOf { it.frequency })} - " +
        formatFrequency(bandGains.maxOf { it.frequency })
}

@Composable
private fun ViewModeTile(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()
    
    Surface(
        onClick = {
            scope.launch {
                haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.CLICK)
            }
            onClick()
        },
        modifier = modifier
            .height(72.dp)
            .squishable(enabled = true, scaleDown = 0.93f),
        color = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (isSelected)
            MaterialTheme.colorScheme.onPrimaryContainer
        else
            MaterialTheme.colorScheme.onSurface,
        shape = if (isSelected)
            MaterialTheme.shapes.extraLarge
        else
            MaterialTheme.shapes.large,
        border = tileSelectionBorder(isSelected)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = if (isSelected)
                    MaterialTheme.shapes.extraLarge
                else
                    MaterialTheme.shapes.medium,
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun BandModeSelector(
    currentMode: BandMode,
    onModeChange: (BandMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()
    
    EqualizerCard(
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            EqualizerSectionHeader(
                icon = Icons.Default.Tune,
                title = stringResource(R.string.band_configuration),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Text(
                text = stringResource(R.string.choose_equalizer_precision),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BandMode.values().forEach { mode ->
                    BandModeTile(
                        mode = mode,
                        isSelected = currentMode == mode,
                        onClick = {
                            scope.launch {
                                haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.CLICK)
                            }
                            onModeChange(mode)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BandModeTile(
    mode: BandMode,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()
    
    Surface(
        onClick = {
            scope.launch {
                haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.CLICK)
            }
            onClick()
        },
        modifier = modifier
            .height(80.dp)
            .squishable(enabled = true, scaleDown = 0.93f),
        color = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (isSelected)
            MaterialTheme.colorScheme.onPrimaryContainer
        else
            MaterialTheme.colorScheme.onSurface,
        shape = if (isSelected)
            MaterialTheme.shapes.extraLarge
        else
            MaterialTheme.shapes.large,
        border = tileSelectionBorder(isSelected)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = if (isSelected)
                    MaterialTheme.shapes.extraLarge
                else
                    MaterialTheme.shapes.small,
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = mode.value,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = mode.displayName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernPresetSelector(
    presets: List<EqualizerPreset>,
    currentPreset: EqualizerPreset,
    onPresetSelected: (EqualizerPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.padding(20.dp)) {
        EqualizerSectionHeader(
            icon = Icons.Default.LibraryMusic,
            title = stringResource(R.string.dolby_geq_preset),
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { 
                scope.launch {
                    haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.TICK)
                }
                expanded = it 
            }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (currentPreset.isUserDefined) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = currentPreset.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            }
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                presets.forEach { preset ->
                    DropdownMenuItem(
                        text = { 
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    preset.name,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (preset.isUserDefined) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        onClick = {
                            scope.launch {
                                haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.CLICK)
                            }
                            onPresetSelected(preset)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ModernEqualizerBand(
    frequency: Int,
    gain: Int,
    onGainChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var sliderValue by remember(gain) { mutableFloatStateOf(gain / 10f) }
    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()
    var lastHapticValue by remember { mutableIntStateOf((gain / 10f).toInt()) }

    Column(
        modifier = modifier
            .width(64.dp)
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = if (enabled) MaterialTheme.colorScheme.primaryContainer
                   else MaterialTheme.colorScheme.surfaceContainerHighest
        ) {
            Text(
                text = "%.1f".format(sliderValue),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = { newValue ->
                if (enabled) {
                    val intValue = (newValue * 10).toInt() / 10
                    if (intValue != lastHapticValue) {
                        scope.launch {
                            haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.TEXTURE_TICK)
                        }
                        lastHapticValue = intValue
                    }
                    sliderValue = newValue
                }
            },
            onValueChangeFinished = {
                if (enabled) {
                    onGainChange((sliderValue * 10).toInt())
                }
            },
            enabled = enabled,
            valueRange = -15f..15f,
            modifier = Modifier
                .graphicsLayer {
                    rotationZ = 270f
                    transformOrigin = TransformOrigin(0f, 0f)
                }
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(
                        Constraints(
                            minWidth = constraints.minHeight,
                            maxWidth = constraints.maxHeight,
                            minHeight = constraints.minWidth,
                            maxHeight = constraints.maxHeight,
                        )
                    )
                    layout(placeable.height, placeable.width) {
                        placeable.place(-placeable.width, 0)
                    }
                }
                .weight(1f)
                .width(48.dp),
            colors = SliderDefaults.colors(
                thumbColor = if (enabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                activeTrackColor = if (enabled) MaterialTheme.colorScheme.primary
                                  else MaterialTheme.colorScheme.outline,
                inactiveTrackColor = if (enabled) MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                disabledThumbColor = MaterialTheme.colorScheme.outline,
                disabledActiveTrackColor = MaterialTheme.colorScheme.outline,
                disabledInactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        )
        Text(
            text = if (frequency >= 1000) "${frequency / 1000}k" else "$frequency",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
                   else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FrequencyResponseCurve(
    bandGains: List<BandGain>,
    modifier: Modifier = Modifier
) {
    val curveColor = MaterialTheme.colorScheme.primary
    val plotContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val plotInkColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(plotContainerColor)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2

        drawLine(
            color = plotInkColor.copy(alpha = 0.5f),
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 2f
        )

        for (i in 1..4) {
            val y = (height / 5) * i
            drawLine(
                color = plotInkColor.copy(alpha = 0.18f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
        }
        
        if (bandGains.size >= 2) {
            val path = Path()
            val stepX = width / (bandGains.size - 1)

            bandGains.forEachIndexed { index, bandGain ->
                val x = index * stepX
                val normalizedGain = (bandGain.gain / 150f).coerceIn(-1f, 1f)
                val y = centerY - (normalizedGain * centerY * 0.8f)
                
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    val prevX = (index - 1) * stepX
                    val prevGain = bandGains[index - 1].gain
                    val prevNormalizedGain = (prevGain / 150f).coerceIn(-1f, 1f)
                    val prevY = centerY - (prevNormalizedGain * centerY * 0.8f)
                    
                    val cpX1 = prevX + stepX * 0.4f
                    val cpY1 = prevY
                    val cpX2 = x - stepX * 0.4f
                    val cpY2 = y
                    
                    path.cubicTo(cpX1, cpY1, cpX2, cpY2, x, y)
                }
            }
            
            drawPath(
                path = path,
                color = curveColor,
                style = Stroke(width = 4f)
            )

            val fillPath = Path().apply {
                addPath(path)
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        curveColor.copy(alpha = 0.28f),
                        curveColor.copy(alpha = 0.04f)
                    )
                )
            )
        }
    }
}

@Composable
private fun SavePresetDialog(
    onSave: (String) -> String?,
    onDismiss: () -> Unit
) {
    var presetName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            ApplyDialogWindowBlur()
            Surface(
                modifier = Modifier.size(56.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        },
        title = {
            Text(
                stringResource(R.string.dolby_geq_new_preset),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            ) 
        },
        text = {
            Column {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { 
                        presetName = it
                        errorMessage = null
                    },
                    label = { 
                        Text(
                            stringResource(R.string.dolby_geq_preset_name),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ) 
                    },
                    isError = errorMessage != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        errorBorderColor = MaterialTheme.colorScheme.error,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        errorContainerColor = MaterialTheme.colorScheme.errorContainer
                    )
                )
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val error = onSave(presetName)
                    if (error != null) {
                        errorMessage = error
                    }
                },
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(stringResource(android.R.string.cancel))
            }
        },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoEqSelectionDialog(
    viewModel: EqualizerViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.initAutoEq(context)
        viewModel.updateSearchQuery("")
    }

    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredList by viewModel.filteredAutoEqList.collectAsState()
    val isLoading by viewModel.isSearchLoading.collectAsState()
    val activeAutoEqId by viewModel.currentAppliedAutoEqId.collectAsState()
    val listState = rememberLazyListState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            ApplyDialogWindowBlur()
            Text(
                text = stringResource(id = R.string.dolby_autoeq_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)) {

                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = { Text(stringResource(R.string.dolby_autoeq_search_hint)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.dolby_autoeq_clear))
                                }
                            } else {
                                Icon(Icons.Default.Search, contentDescription = stringResource(R.string.dolby_autoeq_search))
                            }
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                    if (searchQuery.isEmpty() && !activeAutoEqId.isNullOrEmpty()) {
                    val activeEntry = filteredList.find { it.id == activeAutoEqId }
                    if (activeEntry != null) {
                        Text(
                            text = stringResource(R.string.dolby_autoeq_currently_applied),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = activeEntry.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text(text = "${activeEntry.source} • ${activeEntry.measurementRig}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                Icon(Icons.Default.CheckCircle, contentDescription = stringResource(R.string.dolby_autoeq_active), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }

                if (isLoading && filteredList.isEmpty()) {
                    SkeletonRows(rows = 4)
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredList, key = { it.id }) { entry ->
                            val isSelected = entry.id == activeAutoEqId

                            Surface(
                                onClick = {
                                    viewModel.applyAutoEqProfileNetwork(context, entry)
                                    onDismiss()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = if (isSelected) MaterialTheme.shapes.extraLarge else MaterialTheme.shapes.large,
                                color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = entry.name,
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${entry.source} • ${entry.measurementRig}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = stringResource(R.string.dolby_autoeq_selected),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(stringResource(android.R.string.cancel))
            }
        },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun formatDynamicsFrequency(hz: Float): String {
    return if (hz >= 1000f) {
        val khz = hz / 1000f
        if (khz == khz.toInt().toFloat()) "${khz.toInt()} kHz" else "$khz kHz"
    } else {
        "${hz.toInt()} Hz"
    }
}

/**
 * One collapsible dynamics band: a pill row showing the band name plus a
 * live value summary, expanding to reveal its sliders. Collapsed by
 * default so the Bands/MBC tabs stay compact; expansion survives
 * rotation via [saveKey].
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CollapsibleDynamicsBand(
    title: String,
    summary: String,
    saveKey: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by rememberSaveable(saveKey) { mutableStateOf(false) }
    val chevronAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "band_chevron"
    )

    Surface(
        onClick = { expanded = !expanded },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = if (expanded)
            MaterialTheme.colorScheme.secondaryContainer
        else
            MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (expanded)
            MaterialTheme.colorScheme.onSecondaryContainer
        else
            MaterialTheme.colorScheme.onSurface,
        border = tileSelectionBorder(expanded)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (expanded)
                            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer { rotationZ = chevronAngle }
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(
                    animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
                ) + fadeIn(
                    animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
                ),
                exit = shrinkVertically(
                    animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
                ) + fadeOut(
                    animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
                )
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    content()
                }
            }
        }
    }
}

/**
 * Disabled placeholder for an FX block whose framework effect is not
 * available on this device, so the FX tab never looks mysteriously
 * empty — unsupported is visible as unsupported.
 */
@Composable
private fun UnsupportedFxRow(
    title: String,
    modifier: Modifier = Modifier
) {
    ModernSettingSwitch(
        title = title,
        subtitle = "Not supported on this device",
        checked = false,
        onCheckedChange = {},
        enabled = false,
        modifier = modifier
    )
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun DynamicsProcessingSection(
    dynamicsVm: DynamicsEqualizerViewModel = viewModel(),
    enhVm: EnhancementsViewModel = viewModel()
) {
    val state by dynamicsVm.uiState.collectAsState()
    val fxState by enhVm.uiState.collectAsState()
    val spectrum by dynamicsVm.spectrum.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var presetName by remember { mutableStateOf("") }
    val tabs = listOf("Bands", "MBC", "Limiter", "FX")

    EqualizerCard {
        Column(modifier = Modifier.padding(20.dp)) {
            EqualizerSectionHeader(
                icon = Icons.Default.GraphicEq,
                title = "Dynamics processing",
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Independent pre-EQ, multiband compressor, limiter and FX",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            ModernSettingSwitch(
                title = "Enable dynamics",
                subtitle = "Runs alongside the Dolby effect",
                checked = state.enabled,
                onCheckedChange = { dynamicsVm.setEnabled(it) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            SpectrumView(bars = spectrum, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                items(state.presetNames, key = { it }) { name ->
                    AssistChip(
                        onClick = { dynamicsVm.loadPreset(name) },
                        label = { Text(name) },
                        shape = CircleShape,
                        trailingIcon = {
                            IconButton(onClick = { dynamicsVm.deletePreset(name) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete $name",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    )
                }
                item {
                    IconButton(onClick = { showSaveDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Save preset")
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            SecondaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            when (selectedTab) {
                0 -> {
                    ModernSettingSlider(
                        title = "Preamp",
                        value = state.preampDb.toInt(),
                        valueRange = -20f..20f,
                        steps = 39,
                        onValueChange = { dynamicsVm.setPreamp(it) },
                        valueLabel = { "$it dB" }
                    )
                    state.bandFrequencies.forEachIndexed { index, freq ->
                        val gain = state.bandGains.getOrElse(index) { 0f }.toInt()
                        Spacer(modifier = Modifier.height(8.dp))
                        key("dynamics_eq_band_$index") {
                            CollapsibleDynamicsBand(
                                title = formatDynamicsFrequency(freq),
                                summary = "$gain dB",
                                saveKey = "dynamics_eq_band_$index"
                            ) {
                                ModernSettingSlider(
                                    title = "Gain",
                                    value = gain,
                                    valueRange = -20f..20f,
                                    steps = 39,
                                    onValueChange = { dynamicsVm.setBandGain(index, it) },
                                    valueLabel = { "$it dB" }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { dynamicsVm.resetBands() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset bands")
                    }
                }
                1 -> {
                    ModernSettingSwitch(
                        title = "Multiband compressor",
                        subtitle = "3-band dynamics control (Low / Mid / High)",
                        checked = state.mbcEnabled,
                        onCheckedChange = { dynamicsVm.setMbcEnabled(it) }
                    )
                    if (state.mbcEnabled) {
                        state.mbcBands.forEachIndexed { index, band ->
                            Spacer(modifier = Modifier.height(8.dp))
                            key("dynamics_mbc_band_$index") {
                                CollapsibleDynamicsBand(
                                    title = "${band.label} band",
                                    summary = "${band.threshold.toInt()} dB · ${band.ratio.toInt()}:1",
                                    saveKey = "dynamics_mbc_band_$index"
                                ) {
                                    ModernSettingSlider(
                                        title = "Threshold",
                                        value = band.threshold.toInt(),
                                        valueRange = -60f..0f,
                                        steps = 59,
                                        onValueChange = { dynamicsVm.setMbcThreshold(index, it) },
                                        valueLabel = { "$it dB" }
                                    )
                                    ModernSettingSlider(
                                        title = "Ratio",
                                        value = band.ratio.toInt(),
                                        valueRange = 1f..20f,
                                        steps = 18,
                                        onValueChange = { dynamicsVm.setMbcRatio(index, it) },
                                        valueLabel = { "${it}:1" }
                                    )
                                    ModernSettingSlider(
                                        title = "Attack",
                                        value = band.attackMs.toInt(),
                                        valueRange = 1f..200f,
                                        steps = 198,
                                        onValueChange = { dynamicsVm.setMbcAttack(index, it) },
                                        valueLabel = { "$it ms" }
                                    )
                                    ModernSettingSlider(
                                        title = "Release",
                                        value = band.releaseMs.toInt(),
                                        valueRange = 10f..1000f,
                                        steps = 98,
                                        onValueChange = { dynamicsVm.setMbcRelease(index, it) },
                                        valueLabel = { "$it ms" }
                                    )
                                }
                            }
                        }
                    }
                    if (state.mbcEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { dynamicsVm.resetMbc() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Reset MBC")
                        }
                    }
                }
                2 -> {
                    ModernSettingSwitch(
                        title = "Limiter",
                        subtitle = "Final-stage output limiter, prevents clipping",
                        checked = state.limiterEnabled,
                        onCheckedChange = { dynamicsVm.setLimiterEnabled(it) }
                    )
                    if (state.limiterEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ModernSettingSlider(
                            title = "Threshold",
                            value = state.limiterThreshold.toInt(),
                            valueRange = -30f..0f,
                            steps = 29,
                            onValueChange = { dynamicsVm.setLimiterThreshold(it) },
                            valueLabel = { "$it dB" }
                        )
                        ModernSettingSlider(
                            title = "Ratio",
                            value = state.limiterRatio.toInt(),
                            valueRange = 1f..20f,
                            steps = 18,
                            onValueChange = { dynamicsVm.setLimiterRatio(it) },
                            valueLabel = { "${it}:1" }
                        )
                        ModernSettingSlider(
                            title = "Release",
                            value = state.limiterRelease.toInt(),
                            valueRange = 1f..1000f,
                            steps = 98,
                            onValueChange = { dynamicsVm.setLimiterRelease(it) },
                            valueLabel = { "$it ms" }
                        )
                        ModernSettingSlider(
                            title = "Post gain",
                            value = state.limiterPostGain.toInt(),
                            valueRange = -20f..20f,
                            steps = 39,
                            onValueChange = { dynamicsVm.setLimiterPostGain(it) },
                            valueLabel = { "$it dB" }
                        )
                    }
                    if (state.limiterEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { dynamicsVm.resetLimiter() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Reset limiter")
                        }
                    }
                }
                else -> {
                    Text(
                        text = "Framework effects — they stack with Dolby and Dynamics, combine to taste",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    if (fxState.bassSupported) {
                        ModernSettingSwitch(
                            title = "Bass boost",
                            subtitle = "Extra low-end on top of the Dolby bass enhancer",
                            checked = fxState.bassEnabled,
                            onCheckedChange = { enhVm.setBassEnabled(it) }
                        )
                        if (fxState.bassEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            ModernSettingSlider(
                                title = "Strength",
                                value = fxState.bassStrengthPercent,
                                valueRange = 0f..100f,
                                steps = 99,
                                onValueChange = { enhVm.setBassStrength(it) },
                                valueLabel = { "$it%" }
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    } else {
                        UnsupportedFxRow(title = "Bass boost")
                    }
                    if (fxState.virtualizerSupported) {
                        ModernSettingSwitch(
                            title = "Virtualizer",
                            subtitle = "Headphone widening, alongside the Dolby virtualizers",
                            checked = fxState.virtualizerEnabled,
                            onCheckedChange = { enhVm.setVirtualizerEnabled(it) }
                        )
                        if (fxState.virtualizerEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            ModernSettingSlider(
                                title = "Strength",
                                value = fxState.virtualizerStrengthPercent,
                                valueRange = 0f..100f,
                                steps = 99,
                                onValueChange = { enhVm.setVirtualizerStrength(it) },
                                valueLabel = { "$it%" }
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    } else {
                        UnsupportedFxRow(title = "Virtualizer")
                    }
                    if (fxState.reverbSupported) {
                        ModernSettingSwitch(
                            title = "Reverb",
                            subtitle = "Room simulation on the output mix",
                            checked = fxState.reverbEnabled,
                            onCheckedChange = { enhVm.setReverbEnabled(it) }
                        )
                        if (fxState.reverbEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                itemsIndexed(FrameworkEnhancementsEngine.REVERB_PRESET_NAMES) { index, name ->
                                    val selected = fxState.reverbPreset == index
                                    AssistChip(
                                        onClick = { enhVm.setReverbPreset(index) },
                                        label = { Text(name) },
                                        shape = CircleShape,
                                        leadingIcon = if (selected) {
                                            {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        } else null
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    } else {
                        UnsupportedFxRow(title = "Reverb")
                    }
                    if (fxState.loudnessSupported) {
                        ModernSettingSwitch(
                            title = "Loudness",
                            subtitle = "Output gain alongside the Dolby volume leveler",
                            checked = fxState.loudnessEnabled,
                            onCheckedChange = { enhVm.setLoudnessEnabled(it) }
                        )
                        if (fxState.loudnessEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            ModernSettingSlider(
                                title = "Gain",
                                value = fxState.loudnessGainHalfDb,
                                valueRange = 0f..20f,
                                steps = 19,
                                onValueChange = { enhVm.setLoudnessGainHalfDb(it) },
                                valueLabel = { "%.1f dB".format(it / 2f) }
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    } else {
                        UnsupportedFxRow(title = "Loudness")
                    }
                    OutlinedButton(
                        onClick = { enhVm.resetFx() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset FX")
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = {
                ApplyDialogWindowBlur()
                Text("Save preset")
            },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("Preset name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    dynamicsVm.savePreset(presetName)
                    presetName = ""
                    showSaveDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            }
        )
    }
}

/**
 * Stage-A controller UI for the VeynFx DSP chain (VeynFx-native
 * effects: convolver, crossfeed, exciter, tube, AGC, compressor,
 * widener, surround, spatial, multiband compressor). Each block is
 * enable-gated and collapsed by default, reusing
 * [CollapsibleDynamicsBand].
 *
 * Blocks already covered by Dolby/Dynamics/framework FX (EQ, bass,
 * limiter, MBC, FIR, reverb) are intentionally not duplicated here.
 * Until libveynfxaidl lands in the device tree the section shows a
 * driver-missing state with a recheck action instead of failing.
 */

/**
 * Empty state for [VeynFxSection] when the native driver is absent:
 * status badge, what is missing, and a recheck action for after the
 * driver is flashed — instead of a bare paragraph of text.
 */
@Composable
private fun VeynFxMissingDriver(
    onRecheck: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.errorContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "VeynFx driver not found",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "This section needs the VeynFx native effect (libveynfxaidl) " +
                "registered in audio_effects.xml. Flash a build that includes " +
                "it, then check again.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onRecheck,
            shape = MaterialTheme.shapes.large
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Check again")
        }
    }
}
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VeynFxSection(
    veynVm: VeynFxViewModel = viewModel()
) {
    val s by veynVm.uiState.collectAsState()
    val irPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) veynVm.loadIr(uri)
    }

    EqualizerCard {
        Column(modifier = Modifier.padding(20.dp)) {
            EqualizerSectionHeader(
                icon = Icons.Default.GraphicEq,
                title = "VeynFx DSP",
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Native Viper-style chain: convolver, crossfeed, exciter, tube, AGC and more",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            if (!s.available) {
                VeynFxMissingDriver(onRecheck = { veynVm.recheckAvailability() })
            } else {
            ModernSettingSwitch(
                title = "Enable VeynFx",
                subtitle = "Master switch for the whole chain",
                checked = s.masterEnabled,
                onCheckedChange = { veynVm.setMasterEnabled(it) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            ModernSettingSlider(
                title = "Output gain",
                value = s.outputGain,
                valueRange = 0f..200f,
                steps = 99,
                onValueChange = { veynVm.setOutputGain(it) },
                valueLabel = { "$it%" }
            )
            Spacer(modifier = Modifier.height(8.dp))
            key("veyn_widener") {
                CollapsibleDynamicsBand(
                    title = "Stereo widener",
                    summary = if (s.widenerEnabled) "On · ${s.widenerWidth}%" else "Off",
                    saveKey = "veyn_widener"
                ) {
                    ModernSettingSwitch(
                        title = "Stereo widener",
                        subtitle = "Width enhancement alongside the Dolby widener",
                        checked = s.widenerEnabled,
                        onCheckedChange = { veynVm.setWidenerEnabled(it) }
                    )
                    if (s.widenerEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ModernSettingSlider(
                            title = "Width",
                            value = s.widenerWidth,
                            valueRange = 0f..200f,
                            steps = 99,
                            onValueChange = { veynVm.setWidenerWidth(it) },
                            valueLabel = { "$it%" }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            key("veyn_tube") {
                CollapsibleDynamicsBand(
                    title = "Tube simulator",
                    summary = if (s.tubeEnabled) "On · Drive ${s.tubeDrive}" else "Off",
                    saveKey = "veyn_tube"
                ) {
                    ModernSettingSwitch(
                        title = "Tube simulator",
                        subtitle = "Warm saturation",
                        checked = s.tubeEnabled,
                        onCheckedChange = { veynVm.setTubeEnabled(it) }
                    )
                    if (s.tubeEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ModernSettingSlider(
                            title = "Drive",
                            value = s.tubeDrive,
                            valueRange = 0f..100f,
                            steps = 99,
                            onValueChange = { veynVm.setTubeDrive(it) },
                            valueLabel = { "$it%" }
                        )
                        ModernSettingSlider(
                            title = "Mix",
                            value = s.tubeMix,
                            valueRange = 0f..100f,
                            steps = 99,
                            onValueChange = { veynVm.setTubeMix(it) },
                            valueLabel = { "$it%" }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            key("veyn_exciter") {
                CollapsibleDynamicsBand(
                    title = "Exciter",
                    summary = if (s.exciterEnabled) "On · Drive ${s.exciterDrive}" else "Off",
                    saveKey = "veyn_exciter"
                ) {
                    ModernSettingSwitch(
                        title = "Exciter",
                        subtitle = "Harmonic brightness",
                        checked = s.exciterEnabled,
                        onCheckedChange = { veynVm.setExciterEnabled(it) }
                    )
                    if (s.exciterEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ModernSettingSlider(
                            title = "Drive",
                            value = s.exciterDrive,
                            valueRange = 0f..100f,
                            steps = 99,
                            onValueChange = { veynVm.setExciterDrive(it) },
                            valueLabel = { "$it" }
                        )
                        ModernSettingSlider(
                            title = "Blend",
                            value = s.exciterBlend,
                            valueRange = 0f..100f,
                            steps = 99,
                            onValueChange = { veynVm.setExciterBlend(it) },
                            valueLabel = { "$it" }
                        )
                        ModernSettingSlider(
                            title = "Frequency",
                            value = s.exciterFreq,
                            valueRange = 1000f..20000f,
                            steps = 189,
                            onValueChange = { veynVm.setExciterFreq(it) },
                            valueLabel = { formatFrequency(it) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            key("veyn_xfeed") {
                CollapsibleDynamicsBand(
                    title = "Crossfeed",
                    summary = if (s.xfeedEnabled) "On · ${s.xfeedLevel}%" else "Off",
                    saveKey = "veyn_xfeed"
                ) {
                    ModernSettingSwitch(
                        title = "Crossfeed",
                        subtitle = "Speaker-like imaging on headphones",
                        checked = s.xfeedEnabled,
                        onCheckedChange = { veynVm.setXfeedEnabled(it) }
                    )
                    if (s.xfeedEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ModernSettingSlider(
                            title = "Level",
                            value = s.xfeedLevel,
                            valueRange = 0f..100f,
                            steps = 99,
                            onValueChange = { veynVm.setXfeedLevel(it) },
                            valueLabel = { "$it%" }
                        )
                        ModernSettingSlider(
                            title = "Cutoff",
                            value = s.xfeedCutoff,
                            valueRange = 200f..5000f,
                            steps = 47,
                            onValueChange = { veynVm.setXfeedCutoff(it) },
                            valueLabel = { formatFrequency(it) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            key("veyn_agc") {
                CollapsibleDynamicsBand(
                    title = "Auto gain",
                    summary = if (s.agcEnabled) "On · ${s.agcTargetDb} dB" else "Off",
                    saveKey = "veyn_agc"
                ) {
                    ModernSettingSwitch(
                        title = "Auto gain",
                        subtitle = "Automatic level control",
                        checked = s.agcEnabled,
                        onCheckedChange = { veynVm.setAgcEnabled(it) }
                    )
                    if (s.agcEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ModernSettingSlider(
                            title = "Target",
                            value = s.agcTargetDb,
                            valueRange = -30f..0f,
                            steps = 29,
                            onValueChange = { veynVm.setAgcTargetDb(it) },
                            valueLabel = { "$it dB" }
                        )
                        ModernSettingSlider(
                            title = "Max gain",
                            value = s.agcMaxGainDb,
                            valueRange = 0f..30f,
                            steps = 29,
                            onValueChange = { veynVm.setAgcMaxGainDb(it) },
                            valueLabel = { "$it dB" }
                        )
                        ModernSettingSlider(
                            title = "Speed",
                            value = s.agcSpeed,
                            valueRange = 0f..100f,
                            steps = 99,
                            onValueChange = { veynVm.setAgcSpeed(it) },
                            valueLabel = { "$it" }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            key("veyn_comp") {
                CollapsibleDynamicsBand(
                    title = "Compressor",
                    summary = if (s.compEnabled) "On · ${s.compThresholdDb} dB" else "Off",
                    saveKey = "veyn_comp"
                ) {
                    ModernSettingSwitch(
                        title = "Compressor",
                        subtitle = "Single-band dynamics control",
                        checked = s.compEnabled,
                        onCheckedChange = { veynVm.setCompEnabled(it) }
                    )
                    if (s.compEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ModernSettingSlider(
                            title = "Threshold",
                            value = s.compThresholdDb,
                            valueRange = -60f..0f,
                            steps = 59,
                            onValueChange = { veynVm.setCompThresholdDb(it) },
                            valueLabel = { "$it dB" }
                        )
                        ModernSettingSlider(
                            title = "Ratio",
                            value = s.compRatio,
                            valueRange = 1f..20f,
                            steps = 18,
                            onValueChange = { veynVm.setCompRatio(it) },
                            valueLabel = { "${it}:1" }
                        )
                        ModernSettingSlider(
                            title = "Attack",
                            value = s.compAttackMs,
                            valueRange = 1f..200f,
                            steps = 198,
                            onValueChange = { veynVm.setCompAttackMs(it) },
                            valueLabel = { "$it ms" }
                        )
                        ModernSettingSlider(
                            title = "Release",
                            value = s.compReleaseMs,
                            valueRange = 10f..1000f,
                            steps = 98,
                            onValueChange = { veynVm.setCompReleaseMs(it) },
                            valueLabel = { "$it ms" }
                        )
                        ModernSettingSlider(
                            title = "Knee",
                            value = s.compKneeDb,
                            valueRange = 0f..30f,
                            steps = 29,
                            onValueChange = { veynVm.setCompKneeDb(it) },
                            valueLabel = { "$it dB" }
                        )
                        ModernSettingSlider(
                            title = "Makeup",
                            value = s.compMakeupDb,
                            valueRange = 0f..24f,
                            steps = 23,
                            onValueChange = { veynVm.setCompMakeupDb(it) },
                            valueLabel = { "$it dB" }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            key("veyn_surr") {
                CollapsibleDynamicsBand(
                    title = "Surround",
                    summary = if (s.surrEnabled) "On · ${s.surrWidth}%" else "Off",
                    saveKey = "veyn_surr"
                ) {
                    ModernSettingSwitch(
                        title = "Surround",
                        subtitle = "Diffuse surround widening",
                        checked = s.surrEnabled,
                        onCheckedChange = { veynVm.setSurrEnabled(it) }
                    )
                    if (s.surrEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ModernSettingSlider(
                            title = "Delay",
                            value = s.surrDelay,
                            valueRange = 0f..100f,
                            steps = 99,
                            onValueChange = { veynVm.setSurrDelay(it) },
                            valueLabel = { "$it%" }
                        )
                        ModernSettingSlider(
                            title = "Width",
                            value = s.surrWidth,
                            valueRange = 0f..100f,
                            steps = 99,
                            onValueChange = { veynVm.setSurrWidth(it) },
                            valueLabel = { "$it%" }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            key("veyn_spat") {
                CollapsibleDynamicsBand(
                    title = "Spatial",
                    summary = if (s.spatEnabled) "On · ${s.spatWidth}%" else "Off",
                    saveKey = "veyn_spat"
                ) {
                    ModernSettingSwitch(
                        title = "Spatial",
                        subtitle = "HRTF binaural spatialization",
                        checked = s.spatEnabled,
                        onCheckedChange = { veynVm.setSpatEnabled(it) }
                    )
                    if (s.spatEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ModernSettingSlider(
                            title = "Width",
                            value = s.spatWidth,
                            valueRange = 0f..100f,
                            steps = 99,
                            onValueChange = { veynVm.setSpatWidth(it) },
                            valueLabel = { "$it%" }
                        )
                        ModernSettingSlider(
                            title = "Blend",
                            value = s.spatBlend,
                            valueRange = 0f..100f,
                            steps = 99,
                            onValueChange = { veynVm.setSpatBlend(it) },
                            valueLabel = { "$it%" }
                        )
                        Text(
                            text = "HRTF profile",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val hrtfLabels = listOf("Default", "Alt 1", "Alt 2")
                            items(hrtfLabels.size) { index ->
                                val selected = s.spatHrtf == index
                                AssistChip(
                                    onClick = { veynVm.setSpatHrtf(index) },
                                    label = { Text(hrtfLabels[index]) },
                                    shape = CircleShape,
                                    leadingIcon = if (selected) {
                                        {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            key("veyn_conv") {
                CollapsibleDynamicsBand(
                    title = "Convolver",
                    summary = if (s.convEnabled) {
                        "On · " + s.convIrName.ifEmpty { "${s.convMix}%" }
                    } else "Off",
                    saveKey = "veyn_conv"
                ) {
                    ModernSettingSwitch(
                        title = "Convolver",
                        subtitle = "Impulse-response reverb",
                        checked = s.convEnabled,
                        onCheckedChange = { veynVm.setConvEnabled(it) }
                    )
                    if (s.convEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ModernSettingSlider(
                            title = "Mix",
                            value = s.convMix,
                            valueRange = 0f..100f,
                            steps = 99,
                            onValueChange = { veynVm.setConvMix(it) },
                            valueLabel = { "$it%" }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { irPicker.launch("audio/*") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    if (s.convIrName.isEmpty()) "Load IR file"
                                    else s.convIrName
                                )
                            }
                            if (s.convIrName.isNotEmpty()) {
                                IconButton(onClick = { veynVm.clearIr() }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Clear IR",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            key("veyn_mcomp") {
                CollapsibleDynamicsBand(
                    title = "Multiband comp",
                    summary = if (s.mcompEnabled) "On · 4 bands" else "Off",
                    saveKey = "veyn_mcomp"
                ) {
                    ModernSettingSwitch(
                        title = "Multiband comp",
                        subtitle = "4-band compressor with crossovers",
                        checked = s.mcompEnabled,
                        onCheckedChange = { veynVm.setMcompEnabled(it) }
                    )
                    if (s.mcompEnabled) {
                        val bandLabels = listOf("Low", "Low-mid", "High-mid", "High")
                        s.mcompBands.forEachIndexed { index, band ->
                            Spacer(modifier = Modifier.height(8.dp))
                            key("veyn_mcomp_band_$index") {
                                CollapsibleDynamicsBand(
                                    title = "${bandLabels.getOrElse(index) { "Band $index" }} band",
                                    summary = "${band.thresholdDb} dB · ${band.ratio}:1",
                                    saveKey = "veyn_mcomp_band_$index"
                                ) {
                                    ModernSettingSlider(
                                        title = "Threshold",
                                        value = band.thresholdDb,
                                        valueRange = -60f..0f,
                                        steps = 59,
                                        onValueChange = { veynVm.setMcompThresholdDb(index, it) },
                                        valueLabel = { "$it dB" }
                                    )
                                    ModernSettingSlider(
                                        title = "Ratio",
                                        value = band.ratio,
                                        valueRange = 1f..20f,
                                        steps = 18,
                                        onValueChange = { veynVm.setMcompRatio(index, it) },
                                        valueLabel = { "${it}:1" }
                                    )
                                    ModernSettingSlider(
                                        title = "Attack",
                                        value = band.attackMs,
                                        valueRange = 1f..200f,
                                        steps = 198,
                                        onValueChange = { veynVm.setMcompAttackMs(index, it) },
                                        valueLabel = { "$it ms" }
                                    )
                                    ModernSettingSlider(
                                        title = "Release",
                                        value = band.releaseMs,
                                        valueRange = 10f..1000f,
                                        steps = 98,
                                        onValueChange = { veynVm.setMcompReleaseMs(index, it) },
                                        valueLabel = { "$it ms" }
                                    )
                                    ModernSettingSlider(
                                        title = "Makeup",
                                        value = band.makeupDb,
                                        valueRange = -12f..12f,
                                        steps = 23,
                                        onValueChange = { veynVm.setMcompMakeupDb(index, it) },
                                        valueLabel = { "$it dB" }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Crossovers",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        s.mcompXovers.forEachIndexed { index, hz ->
                            ModernSettingSlider(
                                title = "Crossover ${index + 1}",
                                value = hz,
                                valueRange = 100f..12000f,
                                steps = 118,
                                onValueChange = { veynVm.setMcompXover(index, it) },
                                valueLabel = { formatFrequency(it) }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { veynVm.resetAll() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset VeynFx")
            }
            }
        }
    }
}

