/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.lunaris.dolby.R
import org.lunaris.dolby.domain.models.AudioDeviceCategory
import org.lunaris.dolby.domain.models.OutputDevice

@Composable
private fun outputCategoryLabel(category: AudioDeviceCategory): String {
    return when (category) {
        AudioDeviceCategory.SPEAKER -> stringResource(R.string.audio_output_speaker)
        AudioDeviceCategory.WIRED -> stringResource(R.string.audio_output_wired)
        AudioDeviceCategory.BLUETOOTH -> stringResource(R.string.audio_output_bluetooth)
        AudioDeviceCategory.USB -> stringResource(R.string.audio_output_usb)
        AudioDeviceCategory.OTHER -> stringResource(R.string.audio_output_unknown)
    }
}

/**
 * Connected-output picker.
 *
 * Lists live sinks; tapping one steers the system media route to it.
 * Only physically connected devices are listed (an unplugged jack can't be
 * forced), and the frosted dialog blur matches the other app dialogs.
 */
@Composable
fun AudioOutputDialog(
    devices: List<OutputDevice>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        ApplyDialogWindowBlur()
        BouncyPopIn(delayMillis = 0) {
            Card(
                modifier = modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.output_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.padding(top = 12.dp))
                    if (devices.isEmpty()) {
                        Text(
                            text = stringResource(R.string.output_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 340.dp),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(devices, key = { it.key }) { device ->
                                Surface(
                                    onClick = { onSelect(device.key) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .squishable(enabled = true, scaleDown = 0.97f),
                                    shape = if (device.isActive)
                                        MaterialTheme.shapes.extraLarge
                                    else
                                        MaterialTheme.shapes.large,
                                    color = if (device.isActive)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceContainerHighest,
                                    contentColor = if (device.isActive)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurface,
                                    border = tileSelectionBorder(device.isActive)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AudioDeviceIcon(
                                            category = device.category,
                                            active = device.isActive,
                                            size = 48.dp
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = device.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = if (device.isActive)
                                                    FontWeight.SemiBold
                                                else
                                                    FontWeight.Medium
                                            )
                                            Text(
                                                text = if (device.isActive)
                                                    stringResource(R.string.output_active)
                                                else
                                                    outputCategoryLabel(device.category),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (device.isActive)
                                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                                        alpha = 0.8f
                                                    )
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        RadioButton(
                                            selected = device.isActive,
                                            onClick = { onSelect(device.key) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
