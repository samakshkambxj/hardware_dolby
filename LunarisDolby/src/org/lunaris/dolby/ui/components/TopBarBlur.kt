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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Scroll-aware real-blur header — deliberately NOT a pill.
 *
 * Bottom [FloatingNavToolbar] is the floating element. This top bar stays
 * neutral and calm so the two never compete:
 *
 * - Rectangle, edge-to-edge, no glow, no saturated aurora.
 * - REAL backdrop blur ([RealBlurBackdrop]): the scrolling rows behind the
 *   bar are snapshotted and GPU-blurred, the same frosted-glass read as the
 *   audio-output dialog background — not a glow, not a flat tint.
 * - A neutral surface veil over the blur (0.42 -> 0.60 with scroll) keeps
 *   the title legible; a 1dp outlineVariant hairline fades in with scroll.
 * - No bottom scrim: with live blur the rows genuinely melt under glass,
 *   so the old 28dp fade is unnecessary.
 *
 * @param scrollFraction 0f (top) .. 1f (scrolled). Pass
 * [rememberTopBarScrollFraction] output to make the bar react to scroll;
 * defaults to 0f (resting state) so existing call sites keep compiling.
 * The bucketed fraction also drives blur re-snapshots while scrolling.
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

    // Neutral veil over REAL blur: legible at rest, denser when scrolled.
    val veilAlpha = 0.42f + 0.18f * scrolled
    val hairlineAlpha = 0.10f + 0.38f * scrolled
    val veil = scheme.surfaceContainer.copy(alpha = veilAlpha)
    val hairline = scheme.outlineVariant.copy(alpha = hairlineAlpha)

    Box(modifier = modifier.fillMaxWidth()) {
        // Live backdrop blur of the rows sliding underneath.
        RealBlurBackdrop(
            tint = veil,
            blurRadiusPx = 28f,
            updateKey = (scrollFraction.coerceIn(0f, 1f) * 40).toInt(),
            modifier = Modifier.matchParentSize()
        )
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
