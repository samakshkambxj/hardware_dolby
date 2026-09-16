/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.components

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.lunaris.dolby.domain.models.AudioDeviceCategory

private fun glyphFor(category: AudioDeviceCategory) = when (category) {
    AudioDeviceCategory.SPEAKER -> Icons.Default.VolumeUp
    AudioDeviceCategory.WIRED -> Icons.Default.Headphones
    AudioDeviceCategory.BLUETOOTH -> Icons.Default.Bluetooth
    AudioDeviceCategory.USB -> Icons.Default.Usb
    AudioDeviceCategory.OTHER -> Icons.Default.Speaker
}

@Composable
private fun categoryBase(category: AudioDeviceCategory): Color {
    val scheme = MaterialTheme.colorScheme
    return when (category) {
        AudioDeviceCategory.SPEAKER -> scheme.primary
        AudioDeviceCategory.WIRED -> scheme.secondary
        AudioDeviceCategory.BLUETOOTH -> scheme.tertiary
        AudioDeviceCategory.USB -> scheme.secondary
        AudioDeviceCategory.OTHER -> scheme.outline
    }
}

@Composable
private fun rememberIconAnimationsDisabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        } catch (_: Exception) {
            false
        }
    }
}

/**
 * Glossy "3D" device tile with an embossed glyph.
 *
 * Depth is faked the cheap way: light-to-dark vertical body gradient,
 * top-left specular bloom, darkened base and a bright rim. When [active],
 * a breathing halo plus two expanding ripple rings play (single shared
 * infinite transition, zero allocations in the draw scope). Static when
 * idle or when animator-duration-scale is 0.
 */
@Composable
fun AudioDeviceIcon(
    category: AudioDeviceCategory,
    active: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp
) {
    val base = categoryBase(category)
    val animationsDisabled = rememberIconAnimationsDisabled()
    val animated = active && !animationsDisabled

    // Conditional composition is fine here: entering/leaving the branch
    // starts/stops the transition instead of ticking for idle icons.
    var progress = 0.5f
    var spin = 0f
    if (animated) {
        val transition = rememberInfiniteTransition(label = "device_icon")
        val animatedProgress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "ripple"
        )
        progress = animatedProgress
        val animatedSpin by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "spin"
        )
        spin = animatedSpin
    }

    val tileTop = lerp(base, Color.White, 0.38f)
    val tileBottom = lerp(base, Color.Black, 0.32f)

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { rotationZ = spin },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val minDim = this.size.minDimension
            val center = Offset(minDim / 2f, minDim / 2f)
            val tileR = minDim * 0.30f
            val tileSize = tileR * 2f
            val tileTopLeft = Offset(center.x - tileR, center.y - tileR)
            val corner = CornerRadius(tileR * 0.62f, tileR * 0.62f)

            // Breathing halo behind the tile for the active route.
            if (active) {
                val pulse = if (animated) {
                    0.5f + 0.5f * kotlin.math.sin(progress * 2f * kotlin.math.PI.toFloat())
                } else {
                    0.5f
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            base.copy(alpha = 0.34f + 0.12f * pulse),
                            Color.Transparent
                        ),
                        center = center,
                        radius = minDim * 0.5f
                    ),
                    radius = minDim * 0.5f,
                    center = center
                )
            }

            // Expanding ripple rings for the active route.
            if (animated) {
                for (i in 0 until 2) {
                    val phase = (progress + i * 0.5f) % 1f
                    drawCircle(
                        color = base.copy(alpha = (1f - phase) * 0.55f),
                        radius = tileR + phase * (minDim * 0.48f - tileR),
                        center = center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 2.dp.toPx()
                        )
                    )
                }
            }

            // Tile body: light crown -> base -> dark base (fake volume).
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(tileTop, base, tileBottom)
                ),
                topLeft = tileTopLeft,
                size = androidx.compose.ui.geometry.Size(tileSize, tileSize),
                cornerRadius = corner
            )
            // Darkened base edge for thickness.
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.28f)),
                    startY = center.y,
                    endY = center.y + tileR
                ),
                topLeft = tileTopLeft,
                size = androidx.compose.ui.geometry.Size(tileSize, tileSize),
                cornerRadius = corner
            )
            // Specular bloom, top-left (light source).
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.55f),
                        Color.White.copy(alpha = 0f)
                    ),
                    center = Offset(center.x - tileR * 0.38f, center.y - tileR * 0.42f),
                    radius = tileR * 0.85f
                ),
                radius = tileR * 0.85f,
                center = Offset(center.x - tileR * 0.38f, center.y - tileR * 0.42f)
            )
            // Bright rim.
            drawRoundRect(
                color = Color.White.copy(alpha = 0.35f),
                topLeft = tileTopLeft,
                size = androidx.compose.ui.geometry.Size(tileSize, tileSize),
                cornerRadius = corner,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 1.dp.toPx()
                )
            )
        }
        // Embossed glyph: dark offset copy under the white face.
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = glyphFor(category),
                contentDescription = null,
                tint = Color.Black.copy(alpha = 0.35f),
                modifier = Modifier
                    .size(size * 0.30f)
                    .offset(x = 0.dp, y = 1.dp)
            )
            Icon(
                imageVector = glyphFor(category),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size * 0.30f)
            )
        }
    }
}
