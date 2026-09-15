/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.components

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Handler
import android.os.Looper
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

@Composable
fun AnimatedEqualizerIconDynamic(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    size: Dp = 24.dp,
    barCount: Int = 5
) {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer_dynamic")
    
    val barHeights = List(barCount) { index ->
        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 800 + (index * 50),
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar_height_$index"
        )
    }

    Canvas(modifier = modifier.size(size)) {
        val canvasWidth = this.size.width
        val canvasHeight = this.size.height
        
        val barWidths = List(barCount) { index ->
            val normalizedPosition = index.toFloat() / (barCount - 1)
            val centerOffset = (normalizedPosition - 0.5f) * 2
            val widthFactor = 1.0f - (centerOffset * centerOffset).pow(0.6f)
            val scaledWidth = 0.5f + (widthFactor * 0.5f)
            scaledWidth
        }
        
        val totalWidthFactor = barWidths.sum() + (barCount - 1) * 0.3f
        val baseBarWidth = canvasWidth / totalWidthFactor
        
        var currentX = 0f
        
        barHeights.forEachIndexed { index, heightAnimation ->
            val barWidth = baseBarWidth * barWidths[index]
            val barHeight = canvasHeight * heightAnimation.value
            val y = canvasHeight - barHeight

            drawRoundRect(
                color = color,
                topLeft = Offset(currentX, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
            
            currentX += barWidth + (baseBarWidth * 0.3f)
        }
    }
}

private fun gaussian(x: Float, center: Float, sigma: Float): Float =
    exp(-((x - center) * (x - center)) / (2f * sigma * sigma))

@Composable
fun AnimatedWaveformBanner(
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    accentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    barCount: Int = 56,
    animated: Boolean = true
) {
    // Playback-aware motion (no permission needed): poll whether music is
    // actually playing and the current music-volume fraction, then ease the
    // banner between a calm idle drift and a fast, deep ripple. Smoothed so
    // play/pause never pops.
    val context = LocalContext.current
    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    var musicActive by remember { mutableStateOf(false) }
    var volumeFraction by remember { mutableFloatStateOf(0.5f) }

    // Event-driven playback state. The old isMusicActive poll is a sticky
    // global flag that stays true after pause/stop on several ROMs and
    // players, leaving the banner stuck at full energy. Playback callbacks
    // fire immediately on pause/stop with the live config list instead.
    DisposableEffect(audioManager) {
        fun refresh(configs: List<AudioPlaybackConfiguration>) {
            musicActive = configs.any { config ->
                config.isActive && (config.audioAttributes.usage == AudioAttributes.USAGE_MEDIA ||
                    config.audioAttributes.usage == AudioAttributes.USAGE_GAME)
            }
        }
        val callback = object : AudioManager.AudioPlaybackCallback() {
            override fun onPlaybackConfigChanged(configs: List<AudioPlaybackConfiguration>) {
                refresh(configs)
            }
        }
        audioManager.registerAudioPlaybackCallback(callback, Handler(Looper.getMainLooper()))
        runCatching { refresh(audioManager.activePlaybackConfigurations) }
        onDispose {
            runCatching { audioManager.unregisterAudioPlaybackCallback(callback) }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            volumeFraction = runCatching {
                val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    .coerceAtLeast(1)
                (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max)
                    .coerceIn(0f, 1f)
            }.getOrDefault(0.5f)
            delay(500)
        }
    }

    // 1 = full-energy playback, 0.25 = idle drift. Animated off = static.
    val energyTarget = when {
        !animated -> 0f
        musicActive -> 1f
        else -> 0.25f
    }
    val energy by animateFloatAsState(
        targetValue = energyTarget,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "waveform_energy"
    )

    // Phase is advanced manually per frame so speed can follow energy
    // smoothly (an infiniteTransition can't retime mid-flight).
    val speedRef = rememberUpdatedState(
        (0.9f + 2.3f * energy) * if (animated) 1f else 0f
    )
    var phase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = 0L
        val tau = (2f * Math.PI).toFloat()
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = (now - last) / 1_000_000_000f
                    phase = (phase + dt * speedRef.value) % tau
                }
                last = now
            }
        }
    }

    // Slow breathing swell stays alive even when idle so the card never
    // looks dead; playback energy scales how hard it breathes.
    val swellTransition = rememberInfiniteTransition(label = "waveform_swell")
    val swellBase by swellTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "swell_base"
    )
    val swell = swellBase * (0.35f + 0.65f * volumeFraction)

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        if (canvasWidth <= 0f || canvasHeight <= 0f) return@Canvas

        val centerY = canvasHeight / 2f
        val slotWidth = canvasWidth / barCount
        val barWidth = slotWidth * 0.42f
        val radius = barWidth / 2f
        val maxHalfHeight = centerY * 0.86f

        // Idle: tall, shallow ripple. Playing: deeper troughs + volume scale.
        val rippleBase = 0.9f - 0.22f * energy
        val rippleDepth = 0.1f + 0.22f * energy
        val levelScale = if (!animated) {
            0.9f
        } else {
            0.55f + 0.45f * energy * (0.4f + 0.6f * volumeFraction)
        }

        for (index in 0 until barCount) {
            val position = (index + 0.5f) / barCount
            val centerX = position * canvasWidth

            val envelope = (
                gaussian(position, 0.34f, 0.13f) * 0.95f +
                gaussian(position, 0.62f, 0.11f) * 0.78f +
                gaussian(position, 0.48f, 0.05f) * 0.45f +
                gaussian(position, 0.82f, 0.09f) * 0.30f +
                gaussian(position, 0.14f, 0.10f) * 0.26f
            ).coerceIn(0f, 1f)

            val ripple = if (animated) {
                rippleBase + rippleDepth * sin(phase + position * 14f)
            } else {
                0.85f
            }
            val level = envelope * ripple * if (animated) swell * levelScale / 0.9f else 0.9f
            val halfHeight = maxHalfHeight * level

            val alpha = (0.28f + envelope * 0.72f).coerceIn(0f, 1f)
            val color = if (envelope > 0.55f) {
                lerpColor(barColor, accentColor, (envelope - 0.55f) / 0.45f)
            } else {
                barColor
            }

            if (halfHeight <= radius * 1.15f) {
                drawCircle(
                    color = color.copy(alpha = alpha * 0.65f),
                    radius = radius * 0.72f,
                    center = Offset(centerX, centerY)
                )
            } else {
                drawRoundRect(
                    color = color.copy(alpha = alpha),
                    topLeft = Offset(centerX - radius, centerY - halfHeight),
                    size = Size(barWidth, halfHeight * 2f),
                    cornerRadius = CornerRadius(radius, radius)
                )
            }
        }
    }
}

private fun lerpColor(from: Color, to: Color, fraction: Float): Color {
    val t = fraction.coerceIn(0f, 1f)
    return Color(
        red = from.red + (to.red - from.red) * t,
        green = from.green + (to.green - from.green) * t,
        blue = from.blue + (to.blue - from.blue) * t,
        alpha = from.alpha + (to.alpha - from.alpha) * t
    )
}
