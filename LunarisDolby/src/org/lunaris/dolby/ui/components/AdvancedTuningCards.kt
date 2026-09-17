/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.lunaris.dolby.DolbyConstants
import org.lunaris.dolby.R
import org.lunaris.dolby.data.DolbyCodecSupport

/**
 * Collapsed-by-default holder for the raw-DAP controls below. Lives last
 * in Advanced settings so experimental sliders don't push the everyday
 * cards down; auto-expands while the Advanced search filter is active.
 */
@Composable
fun ExperimentalCard(
    labParams: Map<Int, Int>,
    onParamChange: (paramId: Int, value: Int) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    forceExpand: Boolean = false
) {
    val pageStyle by rememberPageStyle()
    var expanded by remember { mutableStateOf(false) }
    val open = expanded || forceExpand
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = pageStyle.cardShape,
        colors = CardDefaults.cardColors(
            containerColor = pageStyle.cardContainer()
        ),
        border = pageStyle.cardBorder(),
        elevation = pageStyle.cardElevation()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsCardIcon(icon = Icons.Default.Science)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.experimental_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.experimental_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (open) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
            AnimatedVisibility(
                visible = open,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.tuning_lab),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    TuningLabContent(
                        labParams = labParams,
                        onParamChange = onParamChange,
                        onReset = onReset
                    )
                    if (ReverbHeightContent(
                            labParams = labParams,
                            onParamChange = onParamChange
                        )
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

/**
 * Experimental raw DAP controls. Only IDs the HAL answers to are shown —
 * support is probed live in [org.lunaris.dolby.data.DolbyRepository].
 * These IDs have no public semantics: change one at a time and listen.
 */
@Composable
fun ColumnScope.TuningLabContent(
    labParams: Map<Int, Int>,
    onParamChange: (paramId: Int, value: Int) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.tuning_lab_summary),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (labParams.isEmpty()) {
            Text(
                text = stringResource(R.string.tuning_lab_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                labParams.toSortedMap().forEach { (paramId, value) ->
                    ModernSettingSlider(
                        title = stringResource(R.string.tuning_lab_param, paramId),
                        value = value,
                        onValueChange = { onParamChange(paramId, it.toInt()) },
                        valueRange = DolbyConstants.LAB_PARAM_MIN.toFloat()..
                            DolbyConstants.LAB_PARAM_MAX.toFloat(),
                        steps = DolbyConstants.LAB_PARAM_MAX -
                            DolbyConstants.LAB_PARAM_MIN - 1,
                        valueLabel = { "$it" }
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(
                onClick = onReset,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.tuning_lab_reset))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.tuning_lab_footer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Experimental Reverb & Height section. Binds the candidate IDs from
 * [DolbyConstants.REVERB_PARAM_ID] / [DolbyConstants.HEIGHT_PARAM_ID] out
 * of the already-probed lab map — an ID the HAL rejects is simply absent
 * and its slider hidden. Returns false (renders nothing) when neither
 * candidate is live.
 */
@Composable
fun ReverbHeightContent(
    labParams: Map<Int, Int>,
    onParamChange: (paramId: Int, value: Int) -> Unit,
    modifier: Modifier = Modifier
): Boolean {
    val reverb = labParams[DolbyConstants.REVERB_PARAM_ID]
    val height = labParams[DolbyConstants.HEIGHT_PARAM_ID]
    if (reverb == null && height == null) return false
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.reverb_height_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.reverb_height_summary),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (reverb != null) {
                ModernSettingSlider(
                    title = stringResource(
                        R.string.reverb_candidate,
                        DolbyConstants.REVERB_PARAM_ID
                    ),
                    value = reverb,
                    onValueChange = {
                        onParamChange(DolbyConstants.REVERB_PARAM_ID, it.toInt())
                    },
                    valueRange = DolbyConstants.LAB_PARAM_MIN.toFloat()..
                        DolbyConstants.LAB_PARAM_MAX.toFloat(),
                    steps = DolbyConstants.LAB_PARAM_MAX -
                        DolbyConstants.LAB_PARAM_MIN - 1,
                    valueLabel = { "$it" }
                )
            }
            if (height != null) {
                ModernSettingSlider(
                    title = stringResource(
                        R.string.height_candidate,
                        DolbyConstants.HEIGHT_PARAM_ID
                    ),
                    value = height,
                    onValueChange = {
                        onParamChange(DolbyConstants.HEIGHT_PARAM_ID, it.toInt())
                    },
                    valueRange = DolbyConstants.LAB_PARAM_MIN.toFloat()..
                        DolbyConstants.LAB_PARAM_MAX.toFloat(),
                    steps = DolbyConstants.LAB_PARAM_MAX -
                        DolbyConstants.LAB_PARAM_MIN - 1,
                    valueLabel = { "$it" }
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.reverb_height_footer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    return true
}

/**
 * Read-only Dolby decoder capabilities from the live MediaCodecList
 * (wired via media_codecs_dolby_audio.xml): AC-3 / E-AC-3 / E-AC-3 JOC
 * (Atmos) / AC-4.
 */
@Composable
fun CodecInfoCard(
    codecs: List<DolbyCodecSupport>?,
    modifier: Modifier = Modifier
) {
    ModernSettingsCard(
        title = stringResource(R.string.codecs_title),
        icon = Icons.Default.Audiotrack,
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.codecs_summary),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (codecs == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                SpinningDolbyLogo()
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                codecs.forEach { codec ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (codec.supported)
                                Icons.Default.CheckCircle
                            else
                                Icons.Default.RemoveCircle,
                            contentDescription = null,
                            tint = if (codec.supported)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = codec.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = codec.decoderName ?: codec.mime,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = stringResource(
                                    if (codec.supported)
                                        R.string.codecs_supported
                                    else
                                        R.string.codecs_missing
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (codec.supported)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            codec.maxChannels?.let { channels ->
                                Text(
                                    text = stringResource(
                                        R.string.codecs_channels, channels
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "audio/ac3 · audio/eac3 · audio/eac3-joc · audio/ac4",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
