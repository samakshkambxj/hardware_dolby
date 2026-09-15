/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.lunaris.dolby.R

@Composable
fun SpatialAudioCard(
    isSupported: Boolean,
    isAvailable: Boolean,
    isEnabled: Boolean,
    headTrackingAvailable: Boolean,
    headTrackingEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onHeadTrackingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    ModernSettingsCard(
        title = stringResource(R.string.spatial_title),
        icon = Icons.Default.SurroundSound,
        modifier = modifier
    ) {
        if (!isSupported) {
            ModernSettingSwitch(
                title = stringResource(R.string.spatial_enable),
                subtitle = stringResource(R.string.spatial_unsupported),
                checked = false,
                onCheckedChange = {},
                icon = Icons.Default.SurroundSound,
                enabled = false
            )
            return@ModernSettingsCard
        }
        Column {
            ModernSettingSwitch(
                title = stringResource(R.string.spatial_enable),
                subtitle = when {
                    isEnabled && isAvailable -> stringResource(R.string.spatial_enable_summary)
                    isEnabled && !isAvailable -> stringResource(R.string.spatial_enabled_no_output)
                    else -> stringResource(R.string.spatial_enable_summary)
                },
                checked = isEnabled,
                onCheckedChange = onEnabledChange,
                icon = Icons.Default.SurroundSound,
                enabled = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            // Dim when head tracking isn't available on this device/output,
            // or when spatial audio itself is off (tracker needs it).
            val headTrackingEnabledState = headTrackingAvailable && isEnabled
            ModernSettingSwitch(
                title = stringResource(R.string.spatial_headtracking),
                subtitle = if (headTrackingAvailable) {
                    stringResource(R.string.spatial_headtracking_summary)
                } else {
                    stringResource(R.string.spatial_unsupported)
                },
                checked = headTrackingEnabled && isEnabled,
                onCheckedChange = onHeadTrackingChange,
                icon = Icons.Default.Headphones,
                enabled = headTrackingEnabledState
            )
        }
    }
}
