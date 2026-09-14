/*
 * Copyright (C) 2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.lunaris.dolby.R
import org.lunaris.dolby.domain.models.Scene

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSceneCard(
    currentDeviceName: String,
    currentDeviceKey: String?,
    scenes: List<Scene>,
    deviceScenes: Map<String, String>,
    onAssign: (String) -> Unit,
    onClear: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedId by remember(deviceScenes, currentDeviceKey, scenes) {
        mutableStateOf(
            currentDeviceKey?.let { deviceScenes[it] }
                ?: scenes.firstOrNull()?.id
        )
    }
    val selectedName = scenes.find { it.id == selectedId }?.name
        ?: stringResource(R.string.device_scene_none)

    ModernSettingsCard(
        title = stringResource(R.string.device_scene_title),
        icon = Icons.Default.Devices,
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.device_scene_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = stringResource(R.string.device_scene_current_device, currentDeviceName),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (scenes.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.device_scene_choose)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        scenes.forEach { scene ->
                            DropdownMenuItem(
                                text = { Text(scene.name) },
                                onClick = {
                                    selectedId = scene.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Button(
                    onClick = { selectedId?.let { onAssign(it) } },
                    enabled = currentDeviceKey != null && selectedId != null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.device_scene_assign))
                }
            }

            if (deviceScenes.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                deviceScenes.forEach { (deviceKey, sceneId) ->
                    val sceneName = scenes.find { it.id == sceneId }?.name ?: sceneId
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(
                                R.string.device_scene_assigned, deviceKey, sceneName
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onClear(deviceKey) }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.device_scene_clear),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
