/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package org.dolbyatmos.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import org.dolbyatmos.DolbyConstants
import org.dolbyatmos.R

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
        shape = rememberAnimatedCardShape(),
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
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { expanded = !expanded },
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
 * support is probed live in [org.dolbyatmos.data.DolbyRepository].
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
                    // Swept IDs can read back values above the conservative
                    // 0–16 lab window — clamp for the slider (writes stay in
                    // range) instead of crashing on an out-of-range thumb.
                    ModernSettingSlider(
                        title = stringResource(R.string.tuning_lab_param, paramId),
                        value = value.coerceIn(
                            DolbyConstants.LAB_PARAM_MIN,
                            DolbyConstants.LAB_PARAM_MAX
                        ),
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
                    value = reverb.coerceIn(
                        DolbyConstants.LAB_PARAM_MIN,
                        DolbyConstants.LAB_PARAM_MAX
                    ),
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
                    value = height.coerceIn(
                        DolbyConstants.LAB_PARAM_MIN,
                        DolbyConstants.LAB_PARAM_MAX
                    ),
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
