/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.lunaris.dolby.R
import org.lunaris.dolby.ui.glass.FloatingBottomBar
import org.lunaris.dolby.ui.glass.FloatingBottomBarItem
import org.lunaris.dolby.ui.glass.LocalGlassBackdrop
import org.lunaris.dolby.utils.HapticFeedbackHelper
import org.lunaris.dolby.utils.rememberHapticFeedback

private data class GlassDestination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
)

private val GLASS_DESTINATIONS = listOf(
    GlassDestination("settings", R.string.home, Icons.Default.Home),
    GlassDestination("equalizer", R.string.equalizer, Icons.Default.GraphicEq),
    GlassDestination("advanced", R.string.advanced, Icons.Default.Settings),
    GlassDestination("volume", R.string.volume, Icons.Default.VolumeUp),
)

/**
 * Pure Apple-style navbar: floating liquid-glass pill with a sliding
 * selection indicator, icon + label tabs, and drag physics. Alternative to
 * [FloatingNavToolbar]; picked via Customization > Navbar > Design.
 */
@Composable
fun LiquidGlassNavbar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    /** Dot on Advanced when a Tuning Lab value was customized. */
    advancedBadge: Boolean = false,
    /** Dot on Equalizer when a custom/user preset is active. */
    equalizerBadge: Boolean = false,
    /** Prismatic fringe on the pill edge. */
    rainbow: Boolean = true,
    /** Refraction strength 0..1 for the pill lenses. */
    intensity: Float = 0.6f,
    /** Veil alpha over the pill container. */
    veilAlpha: Float = 0.4f,
) {
    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()

    val selectedIndex = GLASS_DESTINATIONS
        .indexOfFirst { it.route == currentRoute }
        .coerceAtLeast(0)

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        FloatingBottomBar(
            selectedIndex = selectedIndex,
            onSelected = { index ->
                scope.launch {
                    haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.CLICK)
                }
                onNavigate(GLASS_DESTINATIONS[index].route)
            },
            tabsCount = GLASS_DESTINATIONS.size,
            backdrop = LocalGlassBackdrop.current,
            rainbow = rainbow,
            intensity = intensity,
            veilAlpha = veilAlpha,
        ) { activateTab ->
            GLASS_DESTINATIONS.forEachIndexed { index, destination ->
                val selected = index == selectedIndex
                val badge = when (destination.route) {
                    "advanced" -> advancedBadge
                    "equalizer" -> equalizerBadge
                    else -> false
                }
                FloatingBottomBarItem(
                    selected = selected,
                    onClick = { activateTab(index) },
                    modifier = Modifier.defaultMinSize(minWidth = 76.dp),
                ) {
                    val contentColor = LocalContentColor.current
                    CompositionLocalProvider(
                        LocalContentColor provides if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            contentColor
                        },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = stringResource(destination.labelRes),
                                modifier = Modifier.size(24.dp),
                            )
                            if (badge) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 4.dp, y = (-4).dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.tertiary),
                                )
                            }
                        }
                        Text(
                            text = stringResource(destination.labelRes),
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }
    }
}
