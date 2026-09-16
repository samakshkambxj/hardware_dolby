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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Particle(
    val x: Float, // 0..1 normalized
    val y: Float, // 0..1 normalized
    val radiusDp: Float, // 1.5..5
    val baseAlpha: Float, // 0.10..0.24
    val phase: Float, // 0..2PI twinkle offset
    val driftDp: Float, // 16..48 px travel
    val driftDir: Float, // angle in radians (mostly upward)
    val speedFactor: Float, // 0.5..1.0 per-particle speed multiplier
    val colorIndex: Int // 0 primary, 1 secondary, 2 tertiary
)

/**
 * Dense floating dust / bokeh rendered above the Scaffold background but
 * below cards and lists.
 *
 * Place as the FIRST child of the Scaffold content [Box] so scrolling content
 * draws over it, and pass the Scaffold [paddingValues] so particles stay
 * below the transparent top bar instead of drifting behind the title:
 *
 * ```
 * Box(Modifier.fillMaxSize()) {
 *     FloatingParticles(modifier = Modifier.padding(paddingValues))
 *     LazyColumn(...) { ... }
 * }
 * ```
 *
 * Cheap by design: one shared [rememberInfiniteTransition], ~[particleCount]
 * `drawCircle` calls per frame, no allocations in the draw scope, no blur
 * passes. Honors animator-duration-scale == 0 (accessibility / battery saver)
 * by rendering a static frame.
 */
@Composable
fun FloatingParticles(
    modifier: Modifier = Modifier,
    particleCount: Int = 90
) {
    if (particleCount <= 0) return

    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val density = LocalDensity.current

    // Static when animations are disabled system-wide.
    val animationsDisabled = remember(context) {
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

    // Stable layout: same particles across recompositions / theme changes.
    val particles = remember(particleCount) {
        val rng = Random(0xD01B7)
        List(particleCount) {
            Particle(
                x = rng.nextFloat(),
                y = rng.nextFloat(),
                radiusDp = 1.5f + rng.nextFloat() * 3.5f,
                baseAlpha = 0.10f + rng.nextFloat() * 0.14f,
                phase = (rng.nextFloat() * 2f * PI).toFloat(),
                driftDp = 16f + rng.nextFloat() * 32f,
                // Mostly upward drift (-PI/2) with slight horizontal wander.
                driftDir = (-PI / 2 + (rng.nextFloat() - 0.5f) * 1.1).toFloat(),
                speedFactor = 0.5f + rng.nextFloat() * 0.5f,
                colorIndex = rng.nextInt(3)
            )
        }
    }

    val scheme = MaterialTheme.colorScheme
    val palette = remember(scheme) {
        listOf(scheme.primary, scheme.secondary, scheme.tertiary)
    }
    // Slightly more visible in dark theme, subtler on light backgrounds.
    val alphaScale = if (darkTheme) 1.15f else 0.85f

    if (animationsDisabled) {
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val wPx = with(density) { maxWidth.toPx() }
            val hPx = with(density) { maxHeight.toPx() }
            Canvas(modifier = Modifier.fillMaxSize()) {
                for (p in particles) {
                    drawCircle(
                        color = palette[p.colorIndex],
                        radius = p.radiusDp * density.density,
                        center = Offset(
                            p.x * wPx,
                            p.y * hPx
                        ),
                        alpha = (p.baseAlpha * alphaScale).coerceIn(0f, 0.38f)
                    )
                }
            }
        }
        return
    }

    val transition = rememberInfiniteTransition(label = "particles")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drift"
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val twoPi = (2f * PI).toFloat()
            for (p in particles) {
                val t = (progress * twoPi * p.speedFactor + p.phase) % twoPi
                val driftPx = p.driftDp * density.density
                // Oscillate around the home position so particles never
                // clump at an edge over time.
                val dx = cos(p.driftDir) * driftPx * sin(t)
                val dy = sin(p.driftDir) * driftPx * (0.5f + 0.5f * sin(t))
                val twinkle = 0.65f + 0.35f * sin(t * 1.7f + p.phase)
                drawCircle(
                    color = palette[p.colorIndex],
                    radius = p.radiusDp * density.density,
                    center = Offset(
                        (p.x * wPx + dx).coerceIn(0f, wPx),
                        (p.y * hPx + dy).coerceIn(0f, hPx)
                    ),
                    alpha = (p.baseAlpha * alphaScale * twinkle).coerceIn(0f, 0.38f)
                )
            }
        }
    }
}
