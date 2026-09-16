/*
 * Copyright (C) 2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import org.lunaris.dolby.R
import org.lunaris.dolby.data.DolbyRepository
import org.lunaris.dolby.data.EasterEggs
import org.lunaris.dolby.domain.models.BandGain
import org.lunaris.dolby.domain.models.BandMode
import org.lunaris.dolby.domain.models.DolbyUiState
import org.lunaris.dolby.domain.models.EqualizerPreset
import org.lunaris.dolby.domain.models.EqualizerUiState
import org.lunaris.dolby.ui.components.ModernConfirmDialog
import org.lunaris.dolby.ui.components.ModernSettingsCard
import org.lunaris.dolby.ui.components.squishable
import org.lunaris.dolby.ui.components.tileSelectionBorder
import org.lunaris.dolby.ui.components.verticalBouncyEdge
import org.lunaris.dolby.ui.viewmodel.DolbyViewModel
import org.lunaris.dolby.ui.viewmodel.EqualizerViewModel
import org.lunaris.dolby.utils.HapticFeedbackHelper
import org.lunaris.dolby.utils.ToastHelper
import org.lunaris.dolby.utils.rememberHapticFeedback

private data class BadgeMeta(
    val id: String,
    val icon: ImageVector,
    val titleRes: Int,
    val descRes: Int
)

private val BADGE_METAS = listOf(
    BadgeMeta(
        EasterEggs.BADGE_PERSISTENT,
        Icons.Default.TouchApp,
        R.string.egg_badge_persistent,
        R.string.egg_badge_persistent_desc
    ),
    BadgeMeta(
        EasterEggs.BADGE_MASHER,
        Icons.Default.SportsEsports,
        R.string.egg_badge_masher,
        R.string.egg_badge_masher_desc
    ),
    BadgeMeta(
        EasterEggs.BADGE_WORDSMITH,
        Icons.Default.Edit,
        R.string.egg_badge_wordsmith,
        R.string.egg_badge_wordsmith_desc
    ),
    BadgeMeta(
        EasterEggs.BADGE_LIZARD,
        Icons.Default.NightsStay,
        R.string.egg_badge_lizard,
        R.string.egg_badge_lizard_desc
    ),
    BadgeMeta(
        EasterEggs.BADGE_MAINTAINER,
        Icons.Default.Star,
        R.string.egg_badge_maintainer,
        R.string.egg_badge_maintainer_desc
    )
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EasterEggScreen(
    dolbyViewModel: DolbyViewModel,
    equalizerViewModel: EqualizerViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()

    val badges by EasterEggs.badges.collectAsState()
    val taps by EasterEggs.logoTaps.collectAsState()
    val opens by EasterEggs.appOpens.collectAsState()
    val dolbyState by dolbyViewModel.uiState.collectAsState()
    val eqState by equalizerViewModel.uiState.collectAsState()

    var showResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        EasterEggs.visitLounge(context)
    }

    val profiles = stringArrayResource(R.array.dolby_profile_entries)
    val profileValues = stringArrayResource(R.array.dolby_profile_values)
    val currentProfileName = (dolbyState as? DolbyUiState.Success)
        ?.settings?.currentProfile
        ?.let { value ->
            val index = profileValues.indexOfFirst { it.toIntOrNull() == value }
            profiles.getOrNull(index)
        }
    val bandModeName = (eqState as? EqualizerUiState.Success)
        ?.bandMode?.displayName

    Scaffold(
        topBar = {
            TopAppBar(
                // Keep action icons off the screen edge (M3 only insets 4.dp).
                modifier = Modifier.padding(end = 8.dp),
                title = {
                    Text(
                        stringResource(R.string.egg_lounge_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalBouncyEdge()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ModernSettingsCard(
                title = stringResource(R.string.egg_lounge_title),
                icon = Icons.Default.NightsStay
            ) {
                Text(
                    text = stringResource(R.string.egg_lounge_sub),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ModernSettingsCard(
                title = stringResource(R.string.egg_badges_title),
                icon = Icons.Default.TouchApp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BADGE_METAS.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { meta ->
                                BadgeTile(
                                    meta = meta,
                                    unlocked = badges.contains(meta.id),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Keep the grid rectangular when the row is short.
                            repeat(2 - row.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            ModernSettingsCard(
                title = stringResource(R.string.egg_stats_title),
                icon = Icons.Default.SportsEsports
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatRow(
                        label = stringResource(R.string.egg_stat_taps),
                        value = taps.toString()
                    )
                    StatRow(
                        label = stringResource(R.string.egg_stat_opens),
                        value = opens.toString()
                    )
                    StatRow(
                        label = stringResource(R.string.egg_stat_badges),
                        value = "${badges.size} / ${EasterEggs.ALL_BADGES.size}"
                    )
                    bandModeName?.let {
                        StatRow(
                            label = stringResource(R.string.egg_stat_band),
                            value = it
                        )
                    }
                    currentProfileName?.let {
                        StatRow(
                            label = stringResource(R.string.egg_stat_profile),
                            value = it
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val state = eqState as? EqualizerUiState.Success ?: return@Button
                    scope.launch {
                        haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.CLICK)
                    }
                    val error = equalizerViewModel.saveImportedPreset(
                        EqualizerPreset(
                            name = context.getString(R.string.egg_secret_preset),
                            bandGains = midnightSnackGains(state.bandMode),
                            isUserDefined = true,
                            bandMode = state.bandMode
                        )
                    )
                    ToastHelper.showToast(
                        context,
                        error ?: context.getString(R.string.egg_summoned)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Text(stringResource(R.string.egg_summon))
            }

            TextButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Text(
                    stringResource(R.string.egg_reset),
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showResetDialog) {
        ModernConfirmDialog(
            title = stringResource(R.string.egg_reset_title),
            message = stringResource(R.string.egg_reset_message),
            icon = Icons.Default.Delete,
            onConfirm = {
                EasterEggs.reset(context)
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false }
        )
    }
}

@Composable
private fun BadgeTile(
    meta: BadgeMeta,
    unlocked: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.squishable(enabled = false),
        color = if (unlocked)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (unlocked)
            MaterialTheme.colorScheme.onPrimaryContainer
        else
            MaterialTheme.colorScheme.onSurface,
        shape = if (unlocked)
            MaterialTheme.shapes.extraLarge
        else
            MaterialTheme.shapes.large,
        border = tileSelectionBorder(unlocked)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (unlocked) meta.icon else Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = if (unlocked)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (unlocked) stringResource(meta.titleRes)
                else stringResource(R.string.egg_badge_locked),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (unlocked) stringResource(meta.descRes)
                else stringResource(R.string.egg_badge_locked_desc),
                style = MaterialTheme.typography.bodySmall,
                color = if (unlocked)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** Smiley curve: warm lows, scooped mids, sparkling highs. */
private fun midnightSnackGains(mode: BandMode): List<BandGain> {
    val frequencies = when (mode) {
        BandMode.TEN_BAND -> DolbyRepository.BAND_FREQUENCIES_10
        BandMode.FIFTEEN_BAND -> DolbyRepository.BAND_FREQUENCIES_15
        BandMode.TWENTY_BAND -> DolbyRepository.BAND_FREQUENCIES_20
    }
    return frequencies.mapIndexed { index, frequency ->
        val t = if (frequencies.size > 1) index / (frequencies.size - 1f) else 0.5f
        val v = 2f * t - 1f
        BandGain(
            frequency = frequency,
            gain = (-40 + 120f * v * v).toInt().coerceIn(-150, 150)
        )
    }
}
