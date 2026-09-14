/*
 * Copyright (C) 2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import org.lunaris.dolby.R
import org.lunaris.dolby.service.DolbyCommandReceiver
import org.lunaris.dolby.utils.ToastHelper

private data class AutomationCommand(val action: String, val hint: String)

/**
 * Lists the broadcast actions accepted by [DolbyCommandReceiver] so Tasker /
 * MacroDroid users can copy them. Tapping copy puts the action on the
 * clipboard; extras are described alongside.
 */
@Composable
fun AutomationCard(modifier: Modifier = Modifier) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val commands = listOf(
        AutomationCommand(
            DolbyCommandReceiver.ACTION_TOGGLE,
            stringResource(R.string.automation_hint_toggle)
        ),
        AutomationCommand(
            DolbyCommandReceiver.ACTION_SET_ENABLED,
            stringResource(R.string.automation_hint_enabled)
        ),
        AutomationCommand(
            DolbyCommandReceiver.ACTION_SET_PROFILE,
            stringResource(R.string.automation_hint_profile)
        ),
        AutomationCommand(
            DolbyCommandReceiver.ACTION_APPLY_SCENE,
            stringResource(R.string.automation_hint_scene)
        )
    )
    ModernSettingsCard(
        title = stringResource(R.string.automation),
        icon = Icons.Default.Code,
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.automation_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            commands.forEach { command ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = command.action.substringAfterLast(".action."),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = command.hint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(command.action))
                            ToastHelper.showToast(
                                context,
                                context.getString(R.string.automation_copied)
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.preset_copy_clipboard),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
