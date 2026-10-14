/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.components

import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import org.lunaris.dolby.ui.glass.LocalGlassBackdrop
import org.lunaris.dolby.utils.HapticFeedbackHelper
import org.lunaris.dolby.utils.rememberHapticFeedback

/** Scroll-to-top action published by long lists; rendered as a glass FAB overlay. */
class ScrollToTopState {
    var visible by mutableStateOf(false)
    var onTop: (() -> Unit)? = null
}

val LocalScrollToTop = staticCompositionLocalOf { ScrollToTopState() }

/**
 * Long lists call this with their own visibility signal; the pager-level
 * [ScrollToTopFabOverlay] renders the button outside the recorded layer,
 * so its glass background never self-samples.
 */
@Composable
fun RegisterScrollToTop(
    visible: Boolean,
    onTop: () -> Unit,
) {
    val holder = LocalScrollToTop.current
    val updatedOnTop by rememberUpdatedState(onTop)
    LaunchedEffect(visible) { holder.visible = visible }
    DisposableEffect(Unit) {
        holder.onTop = { updatedOnTop() }
        onDispose {
            holder.onTop = null
            holder.visible = false
        }
    }
}

@Composable
fun ScrollToTopFabOverlay(
    modifier: Modifier = Modifier,
) {
    val holder = LocalScrollToTop.current
    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()
    val scale by animateFloatAsState(
        targetValue = if (holder.visible) 1f else 0f,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioMediumBouncy,
        ),
        label = "scroll_top_scale",
    )
    if (scale <= 0.01f) return
    val backdrop = LocalGlassBackdrop.current
    val glass = backdrop != null &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    Box(
        modifier = modifier
            .size(56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = scale.coerceIn(0f, 1f)
            },
        contentAlignment = Alignment.Center,
    ) {
        if (glass) {
            LiquidGlassBackdrop(
                tint = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                blurRadiusPx = 16f,
                backdrop = backdrop,
                shape = CircleShape,
                modifier = Modifier.matchParentSize(),
            )
        }
        IconButton(
            onClick = {
                scope.launchHaptic(haptic)
                holder.onTop?.invoke()
            },
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private fun rememberCoroutineScope.launchHaptic(haptic: org.lunaris.dolby.utils.HapticFeedback) {
    launch {
        haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.CLICK)
    }
}
