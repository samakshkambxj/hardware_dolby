/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.lunaris.dolby.DolbyConstants
import org.lunaris.dolby.R
import org.lunaris.dolby.data.DolbyCodecSupport

/**
 * Experimental raw DAP controls. Only IDs the HAL answers to are shown —
 * support is probed live in [org.lunaris.dolby.data.DolbyRepository].
 * These IDs have no public semantics: change one at a time and listen.
 */
@Composable
fun TuningLabCard(
    labParams: Map<Int, Int>,
    onParamChange: (paramId: Int, value: Int) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModernSettingsCard(
        title = stringResource(R.string.tuning_lab),
        icon = Icons.Default.Science,
        modifier = modifier
    ) {
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
 * Experimental Reverb & Height card. Binds the candidate IDs from
 * [DolbyConstants.REVERB_PARAM_ID] / [DolbyConstants.HEIGHT_PARAM_ID] out
 * of the already-probed lab map — an ID the HAL rejects is simply absent
 * and its slider hidden. Renders nothing when neither candidate is live.
 */
@Composable
fun ReverbHeightCard(
    labParams: Map<Int, Int>,
    onParamChange: (paramId: Int, value: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val reverb = labParams[DolbyConstants.REVERB_PARAM_ID]
    val height = labParams[DolbyConstants.HEIGHT_PARAM_ID]
    if (reverb == null && height == null) return
    ModernSettingsCard(
        title = stringResource(R.string.reverb_height_title),
        icon = Icons.Default.Waves,
        modifier = modifier
    ) {
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
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.codecs_loading),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
