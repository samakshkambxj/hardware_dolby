/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Shared bouncy motion specs. All springs — no tweens — so press,
 * selection and entrance all feel springy but still follow M3 Expressive.
 */
object BouncySpecs {
    /** Generic press squish: medium-bouncy, snappy return. */
    val press = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    /** Icon / badge pop: extra bounce on select. */
    val pop = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    /** Card entrance: slightly damped so large surfaces don't overshoot hard. */
    val enter = spring<Float>(
        dampingRatio = 0.75f,
        stiffness = Spring.StiffnessMediumLow
    )
}

/**
 * Ripple-safe press bounce. Unlike the old pointerInput-based squish, this
 * observes press state so ripples/indications from clickable/onClick keep
 * working — it only adds a springy scale layer.
 */
@Composable
fun Modifier.bouncyPress(
    enabled: Boolean = true,
    scaleDown: Float = 0.93f,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed && enabled) scaleDown else 1f,
        animationSpec = BouncySpecs.press,
        label = "bouncy_press"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Springy entrance for cards/rows. Wrap list content or hero cards in this
 * to get fade + slide-up + scale pop with a per-item [delayMillis] for
 * effortless stagger (index * 35ms works well).
 */
@Composable
fun BouncyPopIn(
    visible: Boolean = true,
    delayMillis: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var shown by remember { mutableStateOf(delayMillis <= 0) }
    LaunchedEffect(visible, delayMillis) {
        if (!visible) {
            shown = false
        } else if (delayMillis > 0) {
            delay(delayMillis.toLong())
            shown = true
        } else {
            shown = true
        }
    }
    AnimatedVisibility(
        visible = visible && shown,
        modifier = modifier,
        enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
            slideInVertically(
                animationSpec = spring(
                    dampingRatio = 0.75f,
                    stiffness = Spring.StiffnessMediumLow
                ),
                initialOffsetY = { fullHeight -> fullHeight / 6 }
            ) +
            scaleIn(
                animationSpec = BouncySpecs.enter,
                initialScale = 0.92f
            ),
        exit = fadeOut() + scaleOut(targetScale = 0.95f),
        label = "bouncy_pop_in"
    ) {
        content()
    }
}

/** Bouncy scale for selection icons (toolbar, carousel check badge). */
@Composable
fun rememberBouncySelectedScale(selected: Boolean): Float {
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (selected) 1f else 0.85f,
        animationSpec = BouncySpecs.pop,
        label = "bouncy_selected_scale"
    )
    return scale
}
