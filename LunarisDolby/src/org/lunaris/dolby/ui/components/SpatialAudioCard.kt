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
            // Grey out exactly like head tracking when the spatializer
            // isn't available on the current output: the switch shows the
            // live state (preference AND availability) and can't be flipped
            // until a supported output connects.
            val spatialActive = isEnabled && isAvailable
            ModernSettingSwitch(
                title = stringResource(R.string.spatial_enable),
                subtitle = when {
                    spatialActive -> stringResource(R.string.spatial_enable_summary)
                    isEnabled -> stringResource(R.string.spatial_unavailable)
                    else -> stringResource(R.string.spatial_enable_summary)
                },
                checked = spatialActive,
                onCheckedChange = onEnabledChange,
                icon = Icons.Default.SurroundSound,
                enabled = isAvailable
            )
            Spacer(modifier = Modifier.height(12.dp))
            // Dim when head tracking isn't available on this device/output,
            // or when spatial audio itself isn't live (tracker needs it).
            val headTrackingEnabledState = headTrackingAvailable && spatialActive
            ModernSettingSwitch(
                title = stringResource(R.string.spatial_headtracking),
                subtitle = if (headTrackingAvailable) {
                    stringResource(R.string.spatial_headtracking_summary)
                } else {
                    stringResource(R.string.spatial_unsupported)
                },
                checked = headTrackingEnabled && spatialActive,
                onCheckedChange = onHeadTrackingChange,
                icon = Icons.Default.Headphones,
                enabled = headTrackingEnabledState
            )
        }
    }
}
