/*
 * Copyright (C) 2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.lunaris.dolby.R
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * System-wide left/right channel balance in [-1f, 1f]. The slider works in
 * percent; the value pill shows L/R/C plus magnitude.
 */
@Composable
fun BalanceCard(
    balance: Float,
    onBalanceChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    ModernSettingsCard(
        title = stringResource(R.string.balance),
        icon = Icons.Default.SurroundSound,
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ModernSettingSlider(
                title = stringResource(R.string.balance_summary),
                value = (balance * 100f).roundToInt(),
                onValueChange = { onBalanceChange(it / 100f) },
                valueRange = -100f..100f,
                steps = 39,
                valueLabel = { percent ->
                    val magnitude = abs(percent)
                    when {
                        percent < 0 -> "L $magnitude%"
                        percent > 0 -> "R $magnitude%"
                        else -> "C"
                    }
                }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { onBalanceChange(0f) },
                    enabled = balance != 0f
                ) {
                    Text(
                        text = stringResource(R.string.balance_center),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * System mono-audio switch backed by Settings.System.MASTER_MONO.
 * WRITE_SETTINGS is declared (and allowlisted for priv builds); when it
 * is not yet granted, tapping the row deep-links to the system
 * write-settings page instead of silently failing.
 */
@Composable
fun MonoCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val resolver = context.contentResolver
    var mono by remember {
        mutableStateOf(isMasterMonoEnabled(resolver))
    }

    ModernSettingsCard(
        title = "Mono audio",
        icon = Icons.Default.Hearing,
        modifier = modifier
    ) {
        ModernSettingSwitch(
            title = "Mono audio",
            subtitle = if (Settings.System.canWrite(context))
                "Merge left and right channels"
            else
                "Needs write access — tap to grant it",
            checked = mono,
            onCheckedChange = { enabled ->
                if (!Settings.System.canWrite(context)) {
                    runCatching {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    }
                    return@ModernSettingSwitch
                }
                if (setMasterMonoEnabled(resolver, enabled)) {
                    mono = enabled
                }
            }
        )
    }
}

private fun isMasterMonoEnabled(
    resolver: android.content.ContentResolver
): Boolean {
    return runCatching {
        Settings.System.getInt(resolver, Settings.System.MASTER_MONO, 0) == 1
    }.getOrDefault(false)
}

private fun setMasterMonoEnabled(
    resolver: android.content.ContentResolver,
    enabled: Boolean
): Boolean {
    return runCatching {
        Settings.System.putInt(
            resolver,
            Settings.System.MASTER_MONO,
            if (enabled) 1 else 0
        )
    }.getOrDefault(false)
}
