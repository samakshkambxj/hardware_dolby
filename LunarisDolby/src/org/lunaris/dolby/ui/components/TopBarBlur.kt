/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Full-bleed frosted header — deliberately NOT a pill.
 *
 * Bottom [FloatingNavToolbar] is a floating CircleShape liquid-glass pill
 * (translucent primaryContainer + linear edge + vertical sheen + glow).
 * This top bar uses the opposite language so the two never read as the same:
 * - Rectangle, edge-to-edge, no pill shape, no glow
 * - Frosted translucent base + horizontal aurora wash
 *   (primary tint left, tertiary tint right) instead of the pill's
 *   linear edge / vertical sheen
 * - 1dp top specular highlight + 1dp bottom hairline divider
 * - Flat rectangular soft shadow + short bottom fade scrim so content
 *   appears to slide under frosted glass
 *
 * True backdrop blur isn't available to Compose content in the same window
 * (RenderEffect blurs a composable's own pixels, not what's behind it),
 * so depth comes from translucency + aurora + hairline + fade.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LunarisGlassTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    expandedHeight: Dp = 64.dp
) {
    val scheme = MaterialTheme.colorScheme

    // Frosted base: mostly opaque so text stays legible over scrolling lists.
    val frostBase = Brush.verticalGradient(
        colors = listOf(
            scheme.surfaceContainerLow.copy(alpha = 0.94f),
            scheme.surfaceContainer.copy(alpha = 0.82f)
        )
    )
    // Aurora wash: horizontal tint, distinct from the pill's vertical sheen.
    val aurora = Brush.horizontalGradient(
        colors = listOf(
            scheme.primary.copy(alpha = 0.14f),
            Color.Transparent,
            Color.Transparent,
            scheme.tertiary.copy(alpha = 0.12f)
        )
    )
    val topHighlight = Color.White.copy(alpha = 0.20f)
    val hairline = Brush.horizontalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.28f),
            scheme.outlineVariant.copy(alpha = 0.65f),
            scheme.primary.copy(alpha = 0.35f)
        )
    )
    val bottomFade = Brush.verticalGradient(
        colors = listOf(
            scheme.surfaceContainer.copy(alpha = 0.35f),
            Color.Transparent
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RectangleShape,
                ambientColor = Color.Black.copy(alpha = 0.10f),
                spotColor = Color.Black.copy(alpha = 0.12f)
            )
            .background(frostBase, RectangleShape)
            .background(aurora, RectangleShape)
    ) {
        // Top specular line.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(topHighlight)
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
        // Bottom hairline divider + short fade scrim beneath it.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(9.dp)
                .background(bottomFade)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(hairline)
        )
    }
}
