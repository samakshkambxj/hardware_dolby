/*
 * Copyright (C) 2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.lunaris.dolby.R
import org.lunaris.dolby.data.SleepTimerState
import kotlin.math.ceil

private val SLEEP_MINUTES = listOf(0, 15, 30, 45, 60, 90)

@Composable
fun SleepTimerCard(
    state: SleepTimerState,
    onStart: (Int) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeMinutes = if (state.active) {
        ceil(state.remainingMs / 60_000.0).toInt().coerceAtLeast(1)
    } else {
        0
    }
    ModernSettingsCard(
        title = stringResource(R.string.sleep_timer),
        icon = Icons.Default.Timer,
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SLEEP_MINUTES.forEachIndexed { index, minutes ->
                    BouncyPopIn(delayMillis = (index * 30).coerceAtMost(150)) {
                        FilterChip(
                            selected = if (minutes == 0) !state.active else state.active && activeMinutes == minutes,
                            onClick = { if (minutes == 0) onCancel() else onStart(minutes) },
                            label = {
                                Text(
                                    if (minutes == 0) stringResource(R.string.sleep_timer_off)
                                    else stringResource(R.string.sleep_timer_minutes, minutes)
                                )
                            }
                        )
                    }
                }
            }
            if (state.active) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.sleep_timer_remaining, activeMinutes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onCancel) {
                        Text(stringResource(R.string.sleep_timer_off))
                    }
                }
            }
        }
    }
}
