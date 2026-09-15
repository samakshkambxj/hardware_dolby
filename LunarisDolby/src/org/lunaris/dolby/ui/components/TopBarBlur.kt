/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.ScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Scroll-aware tonal glass header — deliberately NOT a pill.
 *
 * Bottom [FloatingNavToolbar] is the colorful floating element (tinted pill +
 * glow). This top bar stays neutral and calm so the two never compete:
 *
 * - Rectangle, edge-to-edge, no glow, no saturated aurora.
 * - Tonal frost: surfaceContainerLow alpha ramps 0.55 -> 0.92 with scroll,
 *   plus a whisper of primary (0.03 -> 0.07) for depth without cartoon tint.
 * - Hairline uses outlineVariant only (no white specular strip); it fades in
 *   with scroll instead of sitting at full strength on a fresh page.
 * - Bottom scrim is a long soft fade (28dp) whose alpha follows scroll, so
 *   content visibly slides *under* glass instead of clipping on a hard line.
 * - Elevation animates 0dp -> 8dp with scroll for physical lift.
 *
 * Dialog-grade frost: matches the [ApplyDialogWindowBlur] frosted-glass read
 * (translucency + tonal veil + scrim). True backdrop blur is window-level
 * only (FLAG_BLUR_BEHIND) and unavailable to same-window Compose content —
 * a RenderEffect here would blur the bar's own text, not the rows behind it —
 * so the blur feel comes from a denser frost stack that melts rows under
 * glass instead of clipping them on a hard line.
 *
 * @param scrollFraction 0f (top) .. 1f (scrolled). Pass
 * [rememberTopBarScrollFraction] output to make the bar react to scroll;
 * defaults to 0f (resting state) so existing call sites keep compiling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LunarisGlassTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    expandedHeight: Dp = 64.dp,
    scrollFraction: Float = 0f
) {
    val scheme = MaterialTheme.colorScheme

    // Smooth the raw scroll fraction so fast flings don't make the bar flicker.
    val scrolled by animateFloatAsState(
        targetValue = scrollFraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 220),
        label = "topbar_scrolled"
    )

    // Resting: frosted like dialog blur-behind. Scrolled: near-solid for legibility.
    val baseAlpha = 0.70f + 0.27f * scrolled
    val veilAlpha = 0.05f + 0.05f * scrolled
    val depthAlpha = 0.10f + 0.10f * scrolled
    val hairlineAlpha = 0.10f + 0.38f * scrolled
    val scrimAlpha = 0.00f + 0.45f * scrolled
    val elevation = (scrolled * 8f).dp

    val frostBase = Brush.verticalGradient(
        colors = listOf(
            scheme.surfaceContainerLow.copy(alpha = (baseAlpha + 0.05f).coerceAtMost(1f)),
            scheme.surfaceContainerLow.copy(alpha = baseAlpha)
        )
    )
    // Whisper-thin tonal veil, vertical (light falls top-down), not the old
    // horizontal primary->tertiary aurora that read as cartoonish.
    val tonalVeil = Brush.verticalGradient(
        colors = listOf(
            scheme.primary.copy(alpha = veilAlpha),
            Color.Transparent
        )
    )
    // Second frost band (neutral depth) that emulates the dialog blur's
    // frosted thickness over scrolling rows.
    val frostDepth = Brush.verticalGradient(
        colors = listOf(
            scheme.surfaceContainerHighest.copy(alpha = depthAlpha),
            Color.Transparent
        )
    )
    val hairline = scheme.outlineVariant.copy(alpha = hairlineAlpha)
    val bottomScrim = Brush.verticalGradient(
        colors = listOf(
            scheme.surfaceContainer.copy(alpha = scrimAlpha),
            Color.Transparent
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = elevation,
                shape = RectangleShape,
                ambientColor = Color.Black.copy(alpha = 0.08f * scrolled),
                spotColor = Color.Black.copy(alpha = 0.10f * scrolled)
            )
            .background(frostBase, RectangleShape)
            .background(tonalVeil, RectangleShape)
            .background(frostDepth, RectangleShape)
    ) {
        TopAppBar(
            title = title,
            navigationIcon = navigationIcon,
            actions = actions,
            expandedHeight = expandedHeight,
            windowInsets = TopAppBarDefaults.windowInsets,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
                navigationIconContentColor = scheme.onSurface,
                titleContentColor = scheme.onSurface,
                actionIconContentColor = scheme.onSurface
            )
        )
        // Long soft fade so rows melt under the bar while scrolling.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(28.dp)
                .background(bottomScrim)
        )
        // Single 1dp tonal divider, opacity-driven (no white specular line).
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(hairline)
        )
    }
}

/**
 * 0f at the very top -> 1f after [fadeDistancePx] of travel.
 * Use with LazyColumn-based screens.
 */
@Composable
fun rememberTopBarScrollFraction(
    listState: LazyListState,
    fadeDistancePx: Float = 180f
): Float {
    return remember(listState) {
        derivedStateOf {
            val offset = listState.firstVisibleItemIndex * 120f +
                listState.firstVisibleItemScrollOffset.toFloat()
            (offset / fadeDistancePx).coerceIn(0f, 1f)
        }
    }.value
}

/** Same ramp for Column(Modifier.verticalScroll(...)) screens. */
@Composable
fun rememberTopBarScrollFraction(
    scrollState: ScrollState,
    fadeDistancePx: Float = 180f
): Float {
    return remember(scrollState) {
        derivedStateOf {
            (scrollState.value.toFloat() / fadeDistancePx).coerceIn(0f, 1f)
        }
    }.value
}
