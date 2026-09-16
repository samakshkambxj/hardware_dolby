/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.lunaris.dolby.R
import org.lunaris.dolby.utils.*

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FloatingNavToolbar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    /** Bump to re-snapshot the navbar blur (e.g. bucketed pager offset). */
    blurKey: Any = Unit,
    /** Dot on Advanced when a Tuning Lab value was customized. */
    advancedBadge: Boolean = false,
    /** Dot on Equalizer when a custom/user preset is active. */
    equalizerBadge: Boolean = false
) {
    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()
    
    val isHomeSelected = currentRoute == "settings"
    val isVolumeSelected = currentRoute == "volume"
    val isEqualizerSelected = currentRoute == "equalizer"
    val isAdvancedSelected = currentRoute == "advanced"
    
    // Navbar-only real blur: the list behind the pill is snapshotted and
    // GPU-blurred (see RealBlurBackdrop). Neutral veil + hairline, no glow,
    // so it reads as frosted glass like the dialogs.
    val barTint = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f)
    val barEdge = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    val onContainerColor = MaterialTheme.colorScheme.onPrimaryContainer
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .padding(
                    top = FloatingToolbarDefaults.ScreenOffset,
                    bottom = FloatingToolbarDefaults.ScreenOffset
                )
                .shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.20f),
                    spotColor = Color.Black.copy(alpha = 0.20f)
                )
                .clip(CircleShape)
        ) {
            RealBlurBackdrop(
                tint = barTint,
                blurRadiusPx = 20f,
                updateKey = blurKey,
                modifier = Modifier.matchParentSize()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(width = 1.dp, color = barEdge, shape = CircleShape)
            )
            HorizontalFloatingToolbar(
                expanded = true,
                colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(
                    toolbarContainerColor = Color.Transparent,
                    toolbarContentColor = onContainerColor
                ),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
            NavToolbarItem(
                icon = Icons.Default.Home,
                label = stringResource(R.string.home),
                selected = isHomeSelected,
                primaryColor = primaryColor,
                onPrimaryColor = onPrimaryColor,
                containerColor = Color.Transparent,
                onContainerColor = onContainerColor,
                onClick = {
                    scope.launch {
                        haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.CLICK)
                    }
                    onNavigate("settings")
                }
            )
            
            NavToolbarItem(
                icon = Icons.Default.GraphicEq,
                label = stringResource(R.string.equalizer),
                selected = isEqualizerSelected,
                isEqualizer = true,
                badge = equalizerBadge,
                primaryColor = primaryColor,
                onPrimaryColor = onPrimaryColor,
                containerColor = Color.Transparent,
                onContainerColor = onContainerColor,
                onClick = {
                    scope.launch {
                        haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.CLICK)
                    }
                    onNavigate("equalizer")
                }
            )
            
            NavToolbarItem(
                icon = Icons.Default.Settings,
                label = stringResource(R.string.advanced),
                selected = isAdvancedSelected,
                badge = advancedBadge,
                motion = NavIconMotion.Spin,
                primaryColor = primaryColor,
                onPrimaryColor = onPrimaryColor,
                containerColor = Color.Transparent,
                onContainerColor = onContainerColor,
                onClick = {
                    scope.launch {
                        haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.CLICK)
                    }
                    onNavigate("advanced")
                }
            )

            NavToolbarItem(
                icon = Icons.Default.VolumeUp,
                label = stringResource(R.string.volume),
                selected = isVolumeSelected,
                motion = NavIconMotion.Pulse,
                primaryColor = primaryColor,
                onPrimaryColor = onPrimaryColor,
                containerColor = Color.Transparent,
                onContainerColor = onContainerColor,
                onClick = {
                    scope.launch {
                        haptic.performHaptic(
                            HapticFeedbackHelper.HapticIntensity.CLICK
                        )
                    }
                    onNavigate("volume")
                }
            )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NavToolbarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    primaryColor: Color,
    onPrimaryColor: Color,
    containerColor: Color,
    onContainerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEqualizer: Boolean = false,
    badge: Boolean = false,
    motion: NavIconMotion = NavIconMotion.Bob
) {
    val currentSelectionKey = remember(selected) { selected }
    val iconBounce = rememberBouncySelectedScale(selected)

    ToggleButton(
        checked = selected,
        onCheckedChange = { onClick() },
        colors = ToggleButtonDefaults.toggleButtonColors(
            containerColor = containerColor,
            contentColor = onContainerColor,
            checkedContainerColor = primaryColor,
            checkedContentColor = onPrimaryColor
        ),
        shapes = ToggleButtonDefaults.shapes(
            androidx.compose.foundation.shape.CircleShape,
            androidx.compose.foundation.shape.CircleShape,
            androidx.compose.foundation.shape.CircleShape
        ),
        modifier = modifier.height(56.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.animateContentSize(
                animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
            )
        ) {
            key(currentSelectionKey) {
                Box(contentAlignment = Alignment.Center) {
                    Crossfade(
                        targetState = isEqualizer,
                        animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
                        label = "icon_transition_$label"
                    ) { isEq ->
                        if (isEq) {
                            AnimatedEqualizerIconDynamic(
                                modifier = if (selected) Modifier
                                    .graphicsLayer {
                                        scaleX = iconBounce
                                        scaleY = iconBounce
                                    } else Modifier.semantics {
                                    contentDescription = label
                                },
                                color = if (selected) onPrimaryColor else onContainerColor,
                                size = 24.dp
                            )
                        } else {
                            AnimatedNavIcon(
                                icon = icon,
                                contentDescription = label,
                                motion = motion,
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer {
                                        scaleX = iconBounce
                                        scaleY = iconBounce
                                    }
                            )
                        }
                    }
                    if (badge) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiary)
                        )
                    }
                }
            }
            
            AnimatedVisibility(
                visible = selected,
                enter = expandHorizontally(
                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                    expandFrom = Alignment.Start
                ) + fadeIn(
                    animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
                ),
                exit = shrinkHorizontally(
                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                    shrinkTowards = Alignment.Start
                ) + fadeOut(
                    animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
                ),
                label = "text_visibility_$label"
            ) {
                Text(
                    text = label,
                    fontSize = 16.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.padding(start = ButtonDefaults.IconSpacing)
                )
            }
        }
    }
}

/** Idle motion style for a nav-pill icon, complementing the equalizer bars. */
enum class NavIconMotion {
    /** Gentle float + tilt (home). */
    Bob,
    /** Slow continuous rotation (settings gear). */
    Spin,
    /** Breathing scale + wobble (volume speaker). */
    Pulse
}

/**
 * Static vector icon with a looping idle motion in the spirit of the animated
 * equalizer icon. Runs at the same gentle energy selected or not — selection
 * already gets the bounce scale + expanding pill from the caller.
 */
@Composable
private fun AnimatedNavIcon(
    icon: ImageVector,
    contentDescription: String?,
    motion: NavIconMotion,
    modifier: Modifier = Modifier
) {
    // Reduced motion: static icon, no looping transitions.
    if (rememberReducedMotion()) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = modifier
        )
        return
    }
    when (motion) {
        NavIconMotion.Spin -> {
            val spin = rememberInfiniteTransition(label = "nav_icon_spin")
            val angle by spin.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 9000, easing = LinearEasing)
                ),
                label = "angle"
            )
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = modifier.graphicsLayer { rotationZ = angle }
            )
        }
        NavIconMotion.Pulse -> {
            val pulse = rememberInfiniteTransition(label = "nav_icon_pulse")
            val scale by pulse.animateFloat(
                initialValue = 1f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )
            val wobble by pulse.animateFloat(
                initialValue = -6f,
                targetValue = 6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1600, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "wobble"
            )
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = modifier.graphicsLayer {
                    scaleX *= scale
                    scaleY *= scale
                    rotationZ = wobble
                }
            )
        }
        NavIconMotion.Bob -> {
            // Note: this tree's Compose keeps the initialValue/targetValue
            // overload for animateFloat but not for animateDp, so the bob
            // runs on animateDpAsState driven by a toggling target instead.
            var bobUp by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                while (true) {
                    delay(1500)
                    bobUp = !bobUp
                }
            }
            val y by animateDpAsState(
                targetValue = if (bobUp) (-2.5).dp else 0.dp,
                animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
                label = "bob"
            )
            val bob = rememberInfiniteTransition(label = "nav_icon_bob")
            val tilt by bob.animateFloat(
                initialValue = -5f,
                targetValue = 5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 2100, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "tilt"
            )
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = modifier
                    .offset(y = y)
                    .graphicsLayer { rotationZ = tilt }
            )
        }
    }
}
