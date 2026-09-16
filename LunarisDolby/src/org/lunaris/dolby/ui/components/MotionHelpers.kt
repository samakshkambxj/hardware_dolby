/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.components

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * True when the user disabled animations system-wide (animator duration
 * scale 0 — the only reliable reduced-motion proxy before the platform
 * reduce-motion toggle). Same signal FloatingParticles already honors.
 */
@Composable
fun rememberReducedMotion(): Boolean {
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
 * Live "audio actually playing" flag via playback callbacks (no polling:
 * isMusicActive sticks on several ROMs). Shared by the waveform banner
 * logic and the top-bar live dot.
 */
@Composable
fun rememberIsAudioPlaying(): Boolean {
    val context = LocalContext.current
    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    var playing by remember { mutableStateOf(false) }
    DisposableEffect(audioManager) {
        fun refresh(configs: List<AudioPlaybackConfiguration>) {
            playing = configs.any { config ->
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
    return playing
}

/** Shimmer-less skeleton rows: breathing alpha, no infinite motion cost. */
@Composable
fun SkeletonRows(
    rows: Int = 3,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(rows) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (index == 0) 20.dp else 56.dp)
                    .alpha(alpha)
                    .clip(if (index == 0) CircleShape else MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            )
            if (index == 0) Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
