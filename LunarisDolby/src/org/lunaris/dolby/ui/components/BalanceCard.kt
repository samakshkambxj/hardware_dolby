/*
 * Copyright (C) 2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
