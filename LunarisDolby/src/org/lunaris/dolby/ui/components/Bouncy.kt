/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs
import kotlin.math.sign
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Shared motion system: one place for every spring in the app.
 *
 * Rules that keep the feel even:
 * - Press feedback never bounces (critical damping). Bounce on press reads
 *   as "stuck then released".
 * - Selection gets a single small overshoot, never a wobble.
 * - Entrances all travel the same distance at the same speed.
 * - The only real bounce lives in the scroll-edge stretch.
 */
object BouncySpecs {
    /** Press squish: snappy in/out, zero overshoot. */
    val press = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /** Selection pop: one gentle overshoot. */
    val pop = spring<Float>(
        dampingRatio = 0.85f,
        stiffness = Spring.StiffnessMedium
    )

    /** Card entrance: damped, uniform travel. */
    val enter = spring<Float>(
        dampingRatio = 0.9f,
        stiffness = Spring.StiffnessMediumLow
    )

    /** Entrance spec typed for slide offsets. */
    val enterOffset = spring<IntOffset>(
        dampingRatio = 0.9f,
        stiffness = Spring.StiffnessMediumLow
    )

    /** Entrance spec typed for expand/shrink sizes. */
    val enterSize = spring<IntSize>(
        dampingRatio = 0.9f,
        stiffness = Spring.StiffnessMediumLow
    )

    /**
     * Edge-stretch return spring: fast and nearly critical so release feels
     * instant (~250-350ms) instead of oscillating for close to a second.
     */
    val overscroll = spring<Float>(
        dampingRatio = 0.82f,
        stiffness = 1400f,
        visibilityThreshold = 0.5f
    )
}

/**
 * Play-once gate for entrance animations.
 *
 * Lazy items are recycled, so without this every scroll-away-and-back (or
 * page revisit) would replay the delay + enter and cards would visibly lag
 * behind the list. A stable [key] plays the entrance only the first time;
 * afterwards content is emitted directly.
 */
private val bouncySeenKeys = mutableSetOf<Any>()

private fun markBouncySeen(key: Any): Boolean = synchronized(bouncySeenKeys) {
    bouncySeenKeys.add(key)
}

@Composable
private fun rememberBouncyFirstSeen(key: Any?): Boolean {
    return remember(key) { key == null || markBouncySeen(key) }
}

/**
 * Press squish that behaves inside scrolling lists.
 *
 * Older press handlers only watched press/release pointer events, so a
 * scroll that started on a card left it stuck scaled-down (move/cancel never
 * reset the flag). This uses [awaitEachGesture] + [waitForUpOrCancellation],
 * so any drag, scroll or cancellation releases the squish immediately.
 */
fun Modifier.squishable(
    enabled: Boolean = true,
    scaleDown: Float = 0.93f
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) scaleDown else 1f,
        animationSpec = BouncySpecs.press,
        label = "squish_scale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                isPressed = true
                val up = waitForUpOrCancellation()
                isPressed = false
                if (up == null) return@awaitEachGesture
            }
        }
}

/**
 * Ripple-safe press alias kept so call sites can say what they mean.
 * Delegates to [squishable]; both behave identically.
 */
@Composable
fun Modifier.bouncyPress(
    enabled: Boolean = true,
    scaleDown: Float = 0.93f,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
): Modifier = squishable(enabled = enabled, scaleDown = scaleDown)

/**
 * Entrance for cards and sections: fade + short slide-up + tiny scale pop.
 *
 * Reserve [delayMillis] for the first paint of top-level sections only.
 * Never stagger rows inside a scrolling list — recycled rows would pop in
 * late mid-scroll and wreck smoothness.
 */
@Composable
fun BouncyPopIn(
    visible: Boolean = true,
    delayMillis: Int = 0,
    modifier: Modifier = Modifier,
    key: Any? = null,
    content: @Composable () -> Unit
) {
    val firstSeen = rememberBouncyFirstSeen(key)
    if (key != null && !firstSeen && visible) {
        content()
        return
    }
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
        enter = fadeIn(animationSpec = tween(180)) +
            slideInVertically(
                animationSpec = BouncySpecs.enterOffset,
                initialOffsetY = { fullHeight -> fullHeight / 12 }
            ) +
            scaleIn(
                animationSpec = BouncySpecs.enter,
                initialScale = 0.96f
            ),
        exit = fadeOut(animationSpec = tween(120)) + scaleOut(targetScale = 0.96f),
        label = "bouncy_pop_in"
    ) {
        content()
    }
}

/** Subtle scale pop for selection icons (toolbar, badges). */
@Composable
fun rememberBouncySelectedScale(selected: Boolean): Float {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.9f,
        animationSpec = BouncySpecs.pop,
        label = "bouncy_selected_scale"
    )
    return scale
}

/**
 * Item wrapper for lazy lists: no delay, no slide (slides inside lazy
 * layouts cause remeasurement jank). Just fade + a tiny scale, played once
 * per [key].
 */
@Composable
fun LazyItemScope.BouncyListItem(
    modifier: Modifier = Modifier,
    key: Any? = null,
    content: @Composable () -> Unit
) {
    val firstSeen = rememberBouncyFirstSeen(key)
    if (key != null && !firstSeen) {
        content()
        return
    }
    AnimatedVisibility(
        visible = true,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(180)) +
            scaleIn(animationSpec = BouncySpecs.enter, initialScale = 0.96f),
        exit = fadeOut(animationSpec = tween(120)) + scaleOut(targetScale = 0.96f),
        label = "bouncy_list_item"
    ) {
        content()
    }
}

/**
 * Springy edge-stretch for vertical scroll containers.
 *
 * Apply OUTSIDE the scroll modifier:
 * ```
 * Modifier.verticalBouncyEdge().verticalScroll(state)
 * LazyColumn(Modifier.verticalBouncyEdge(), state = ...)
 * ```
 *
 * Behavior notes (each one fixes a stuck/jumpy edge from before):
 * - A new edge delta stops any in-flight return spring first, so the spring
 *   never fights the finger.
 * - Resistance grows as the stretch approaches [maxStretchPx]: fast flings
 *   bend instead of slamming into a hard cap.
 * - Stretched content is clipped to the list bounds, so rows can never paint
 *   over the top bar while stretched.
 * - Pre-scroll only consumes the travel needed to pull the stretch back to
 *   rest; the remainder goes to the list, so scrolling never freezes on a
 *   tiny residual stretch.
 * - One settle job (cancel-previous): finger-lift and fling can't launch two
 *   competing animations on the same value.
 * - A generation token serializes mutations, so a stale snap can never yank
 *   the offset back out after release, and a watchdog snaps exactly to rest.
 * - The return spring carries fling velocity, so release feels instant.
 */
fun Modifier.verticalBouncyEdge(
    enabled: Boolean = true,
    maxStretchPx: Float = 140f,
    stretchFactor: Float = 0.32f
): Modifier = composed {
    if (!enabled) return@composed this
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val connection = remember(maxStretchPx, stretchFactor) {
        object : NestedScrollConnection {
            var target = 0f
            var settleJob: Job? = null
            var generation = 0

            private fun resistance(): Float {
                val t = (abs(target) / maxStretchPx).coerceIn(0f, 1f)
                return 1f - 0.85f * t
            }

            private fun snapToTarget(token: Int) {
                if (token != generation) return
                settleJob?.cancel()
                settleJob = scope.launch {
                    offset.snapTo(target)
                    if (token == generation && abs(offset.value) < 0.5f && target != 0f) {
                        target = 0f
                        offset.snapTo(0f)
                    }
                }
            }

            private fun settle(velocity: Float) {
                generation += 1
                val token = generation
                target = 0f
                settleJob?.cancel()
                settleJob = scope.launch {
                    offset.stop()
                    offset.animateTo(
                        targetValue = 0f,
                        animationSpec = BouncySpecs.overscroll,
                        initialVelocity = velocity * stretchFactor
                    )
                    if (token == generation) offset.snapTo(0f)
                }
            }

            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (target == 0f || available.y == 0f) return Offset.Zero
                // Retract only: consume finger travel moving against the
                // stretch. Remainder flows to the list so scrolling never
                // freezes on residual stretch.
                if (target.sign != available.y.sign) {
                    val take = available.y.coerceIn(
                        minimumValue = if (target > 0f) -target else available.y,
                        maximumValue = if (target < 0f) -target else available.y
                    )
                    if (take != 0f) {
                        generation += 1
                        target += take
                        if (target.sign != available.y.sign && abs(target) < 0.5f) {
                            target = 0f
                        }
                        snapToTarget(generation)
                        return Offset(0f, take)
                    }
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val y = available.y
                if (y == 0f) return Offset.Zero
                generation += 1
                offset.stop()
                val delta = y * stretchFactor * resistance()
                target = (target + delta).coerceIn(-maxStretchPx, maxStretchPx)
                snapToTarget(generation)
                return Offset(0f, y)
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                if (target == 0f) return Velocity.Zero
                settle(available.y)
                return available
            }
        }
    }
    this
        .clipToBounds()
        .graphicsLayer { translationY = offset.value }
        .nestedScroll(connection)
}
