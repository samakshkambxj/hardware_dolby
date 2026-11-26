/*
 * Copyright (C) 2024-2026 Lunaris AOSP
 *           (C) 2026 samakshkambxj
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import co.aospa.dolby.R
import kotlin.math.exp
import kotlin.math.sin

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun Modifier.expressivePress(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    scaleDown: Float = 0.96f,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by
        animateFloatAsState(
            targetValue = if (pressed && enabled) scaleDown else 1f,
            animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
            label = "expressive_press_scale",
        )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
private fun DolbyLogo(
    modifier: Modifier = Modifier,
    leftColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    rightColor: Color = MaterialTheme.colorScheme.primary,
) {
    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_dolby_logo_left),
            contentDescription = stringResource(R.string.dolby_title),
            tint = leftColor,
            modifier = Modifier.fillMaxSize(),
        )
        Icon(
            painter = painterResource(R.drawable.ic_dolby_logo_right),
            contentDescription = null,
            tint = rightColor,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DolbyMainCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    statusText: String? = null,
    outputDeviceText: String? = null,
    outputDeviceIcon: ImageVector? = null,
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }

    val containerColor by
        animateColorAsState(
            targetValue =
                if (enabled) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
            animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
            label = "dolby_power_container",
        )
    val contentColor by
        animateColorAsState(
            targetValue =
                if (enabled) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
            label = "dolby_power_content",
        )
    val logoContainerColor by
        animateColorAsState(
            targetValue =
                if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
            animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
            label = "dolby_logo_bg",
        )
    val logoTintColor by
        animateColorAsState(
            targetValue =
                if (enabled) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
            label = "dolby_logo_tint",
        )
    val waveBarColor by
        animateColorAsState(
            targetValue =
                if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                },
            animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
            label = "dolby_wave_bar",
        )
    val waveAccentColor by
        animateColorAsState(
            targetValue =
                if (enabled) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                },
            animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
            label = "dolby_wave_accent",
        )
    val logoScale by
        animateFloatAsState(
            targetValue = if (enabled) 1f else 0.94f,
            animationSpec =
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            label = "dolby_logo_scale",
        )
    val logoRotation by
        animateFloatAsState(
            targetValue = if (enabled) 0f else -8f,
            animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
            label = "dolby_logo_rotation",
        )

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .expressivePress(interactionSource = interactionSource, scaleDown = 0.98f),
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor,
        contentColor = contentColor,
        border =
            if (!enabled) {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            } else {
                null
            },
    ) {
        Column(
            modifier =
                Modifier.toggleable(
                        value = enabled,
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        role = Role.Switch,
                        onValueChange = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            onEnabledChange(it)
                        },
                    )
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Surface(
                    shape =
                        if (enabled) {
                            MaterialShapes.Cookie9Sided.toShape()
                        } else {
                            RoundedCornerShape(24.dp)
                        },
                    color = logoContainerColor,
                    modifier =
                        Modifier.size(72.dp).graphicsLayer {
                            scaleX = logoScale
                            scaleY = logoScale
                            rotationZ = logoRotation
                        },
                ) {
                    DolbyLogo(
                        modifier = Modifier.padding(18.dp),
                        leftColor = logoTintColor,
                        rightColor = logoTintColor,
                    )
                }
                AnimatedWaveformBanner(
                    modifier = Modifier.weight(1f).height(64.dp),
                    barColor = waveBarColor,
                    accentColor = waveAccentColor,
                    barCount = 28,
                    animated = enabled,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.dolby_enable),
                        style = MaterialTheme.typography.headlineSmallEmphasized,
                    )
                    val statusMotionScheme = MaterialTheme.motionScheme
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AnimatedContent(
                            targetState =
                                statusText
                                    ?: stringResource(
                                        if (enabled) R.string.dolby_on else R.string.dolby_off
                                    ),
                            transitionSpec = {
                                fadeIn(statusMotionScheme.fastEffectsSpec()) togetherWith
                                    fadeOut(statusMotionScheme.fastEffectsSpec())
                            },
                            label = "dolby_status_text",
                        ) { label ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLargeEmphasized,
                                color =
                                    if (enabled) {
                                        contentColor
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                            )
                        }
                        if (outputDeviceText != null && outputDeviceIcon != null) {
                            Surface(
                                shape = CircleShape,
                                color =
                                    if (enabled) {
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                            alpha = 0.12f
                                        )
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    },
                                contentColor =
                                    if (enabled) {
                                        contentColor
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                            ) {
                                Row(
                                    modifier =
                                        Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = outputDeviceIcon,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                    )
                                    Text(
                                        text = outputDeviceText,
                                        style = MaterialTheme.typography.labelSmallEmphasized,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = null,
                    thumbContent = {
                        val thumbMotionScheme = MaterialTheme.motionScheme
                        AnimatedContent(
                            targetState = enabled,
                            transitionSpec = {
                                (scaleIn(thumbMotionScheme.fastSpatialSpec()) +
                                        fadeIn(thumbMotionScheme.fastEffectsSpec()))
                                    .togetherWith(
                                        scaleOut(thumbMotionScheme.fastSpatialSpec()) +
                                            fadeOut(thumbMotionScheme.fastEffectsSpec())
                                    )
                            },
                            label = "hero_switch_icon",
                        ) { isChecked ->
                            Icon(
                                imageVector =
                                    if (isChecked) Icons.Rounded.Check else Icons.Rounded.Close,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                            )
                        }
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AnimatedWaveformBanner(
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    accentColor: Color = MaterialTheme.colorScheme.tertiary,
    barCount: Int = 30,
    animated: Boolean = true,
) {
    val activityScale by
        animateFloatAsState(
            targetValue = if (animated) 1f else 0.32f,
            animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
            label = "waveform_activity",
        )

    val phase: Float
    val swell: Float
    if (animated) {
        val infiniteTransition = rememberInfiniteTransition(label = "waveform_banner")
        val animatedPhase by
            infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = (2f * Math.PI).toFloat(),
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(durationMillis = 2800, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart,
                    ),
                label = "waveform_phase",
            )
        val animatedSwell by
            infiniteTransition.animateFloat(
                initialValue = 0.84f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(durationMillis = 1850, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                label = "waveform_swell",
            )
        phase = animatedPhase
        swell = animatedSwell
    } else {
        phase = 0f
        swell = 0.9f
    }

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        if (canvasWidth <= 0f || canvasHeight <= 0f) return@Canvas

        val centerY = canvasHeight / 2f
        val slotWidth = canvasWidth / barCount
        val barWidth = slotWidth * 0.44f
        val radius = barWidth / 2f
        val maxHalfHeight = centerY * 0.88f

        for (index in 0 until barCount) {
            val position = (index + 0.5f) / barCount
            val centerX = position * canvasWidth

            val envelope =
                (gaussian(position, 0.30f, 0.13f) * 0.95f +
                        gaussian(position, 0.62f, 0.12f) * 0.82f +
                        gaussian(position, 0.46f, 0.06f) * 0.48f +
                        gaussian(position, 0.83f, 0.09f) * 0.34f +
                        gaussian(position, 0.12f, 0.09f) * 0.28f)
                    .coerceIn(0f, 1f)

            val ripple =
                if (animated) {
                    0.70f + 0.30f * sin(phase + position * 13f)
                } else {
                    0.85f
                }
            val level = (envelope * ripple * swell * activityScale).coerceIn(0f, 1f)
            val halfHeight = maxHalfHeight * level

            val alpha = (0.30f + envelope * 0.70f).coerceIn(0f, 1f)
            val color =
                if (envelope > 0.52f) {
                    lerpColor(barColor, accentColor, (envelope - 0.52f) / 0.48f)
                } else {
                    barColor
                }

            if (halfHeight <= radius * 1.15f) {
                drawCircle(
                    color = color.copy(alpha = alpha * 0.65f),
                    radius = radius * 0.8f,
                    center = Offset(centerX, centerY),
                )
            } else {
                drawRoundRect(
                    color = color.copy(alpha = alpha),
                    topLeft = Offset(centerX - radius, centerY - halfHeight),
                    size = Size(barWidth, halfHeight * 2f),
                    cornerRadius = CornerRadius(radius, radius),
                )
            }
        }
    }
}

private fun gaussian(x: Float, center: Float, sigma: Float): Float =
    exp(-((x - center) * (x - center)) / (2f * sigma * sigma))

private fun lerpColor(from: Color, to: Color, fraction: Float): Color {
    val t = fraction.coerceIn(0f, 1f)
    return Color(
        red = from.red + (to.red - from.red) * t,
        green = from.green + (to.green - from.green) * t,
        blue = from.blue + (to.blue - from.blue) * t,
        alpha = from.alpha + (to.alpha - from.alpha) * t,
    )
}
