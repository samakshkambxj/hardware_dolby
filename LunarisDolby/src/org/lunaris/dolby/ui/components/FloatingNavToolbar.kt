/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import kotlinx.coroutines.launch
import org.lunaris.dolby.R
import org.lunaris.dolby.utils.*

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FloatingNavToolbar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    /** Re-snapshots the blur when this changes; a ticker covers scrolling. */
    blurKey: Any = Unit
) {
    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()
    
    val isHomeSelected = currentRoute == "settings"
    val isVolumeSelected = currentRoute == "volume"
    val isEqualizerSelected = currentRoute == "equalizer"
    val isAdvancedSelected = currentRoute == "advanced"

    // Real-blur pill: the list behind the bar is snapshotted and GPU-blurred
    // (same frosted read as the audio-output dialog), under a neutral veil.
    // No white specular edge, no primary-tinted glow, no sheen — those were
    // decoration that read as glow next to true blur. Selection behavior and
    // pill shape are unchanged.
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
                blurRadiusPx = 26f,
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
    isEqualizer: Boolean = false
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
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer {
                                    scaleX = iconBounce
                                    scaleY = iconBounce
                                }
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
