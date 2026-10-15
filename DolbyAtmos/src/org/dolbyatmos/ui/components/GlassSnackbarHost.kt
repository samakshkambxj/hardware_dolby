/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package org.dolbyatmos.ui.components

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dolbyatmos.ui.glass.LocalGlassBackdrop
import top.yukonga.miuix.kmp.blur.Backdrop

/**
 * Pager-wide snackbar host. Screens post here instead of owning a host,
 * so confirmations render in the overlay [GlassSnackbarHost] (liquid
 * glass on API 33+, plain snackbar below).
 */
val LocalGlassSnackbar = staticCompositionLocalOf<SnackbarHostState?> { null }

@Composable
fun GlassSnackbarOverlay(
    hostState: SnackbarHostState,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
        snackbar = { data -> GlassSnackbar(data = data, backdrop = backdrop) },
    )
}

@Composable
private fun GlassSnackbar(
    data: SnackbarData,
    backdrop: Backdrop?,
) {
    val visuals = data.visuals
    val glass = backdrop != null &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    if (!glass) {
        androidx.compose.material3.Snackbar(snackbarData = data)
        return
    }
    Box(modifier = Modifier.fillMaxWidth()) {
        LiquidGlassBackdrop(
            tint = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
            blurRadiusPx = 16f,
            backdrop = backdrop,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.matchParentSize(),
        )
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = visuals.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            visuals.actionLabel?.let { label ->
                TextButton(onClick = { data.performAction() }) {
                    Text(
                        text = label,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
