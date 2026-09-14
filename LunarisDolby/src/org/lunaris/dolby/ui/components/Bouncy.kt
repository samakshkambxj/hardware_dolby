/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Velocity

/**
 * Shared motion specs.
 *
 * Old specs used MediumBouncy / LowBouncy springs for *everything* (press,
 * selection, large-card entrance). Low stiffness + low damping = long,
 * multi-oscillation wobble that looked uneven, and big cards overshot hard.
 *
 * New rules for an even feel:
 * - press: no bounce at all (damping = 1), medium stiffness. Snappy, no wobble.
 * - selection pop: single subtle overshoot (damping 0.85), never LowBouncy.
 * - entrance: high damping + small slide/scale deltas so every card moves
 *   the same distance at the same speed.
 * - overscroll: the ONLY place a real bounce lives (edge stretch).
 */
object BouncySpecs {
    /** Press squish: crisp in/out, zero overshoot so it never looks stuck. */
    val press = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /** Selection pop: one gentle overshoot instead of a wobble. */
    val pop = spring<Float>(
        dampingRatio = 0.85f,
        stiffness = Spring.StiffnessMedium
    )

    /** Card entrance: damped, same travel for every surface. */
    val enter = spring<Float>(
        dampingRatio = 0.9f,
        stiffness = Spring.StiffnessMediumLow
    )

    /** Same damping as [enter] but typed for slide/expand (IntOffset). */
    val enterOffset = spring<IntOffset>(
        dampingRatio = 0.9f,
        stiffness = Spring.StiffnessMediumLow
    )

    /** Same damping as [enter] but typed for expand/shrink (IntSize). */
    val enterSize = spring<IntSize>(
        dampingRatio = 0.9f,
        stiffness = Spring.StiffnessMediumLow
    )

    /** Edge-stretch bounce for scroll containers only. */
    val overscroll = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
}

/**
 * Play-once gate for entrance animations.
 *
 * Lazy items are recycled: scrolling away and back (or navigating away and
 * back) recomposes them, and without this every [BouncyPopIn]/[BouncyListItem]
 * would replay its delay + enter animation — cards visibly lagging behind
 * the scroll, or a full staggered cascade on every page visit.
 * Pass a stable `key` (matching the Lazy `item(key = ...)` key) and the
 * entrance plays only the first time that key is composed; afterwards the
 * content is emitted directly with no animation.
 */
private val bouncySeenKeys = mutableSetOf<Any>()

private fun markBouncySeen(key: Any): Boolean = synchronized(bouncySeenKeys) {
    if (bouncySeenKeys.contains(key)) {
        false
    } else {
        bouncySeenKeys.add(key)
        true
    }
}

@Composable
private fun rememberBouncyFirstSeen(key: Any?): Boolean {
    // Single remember call (hook order stays stable); null key = always animate.
    return remember(key) { key == null || markBouncySeen(key) }
}

/**
 * Press bounce that is safe inside scrolling lists.
 *
 * The old version used `awaitPointerEventScope { awaitPointerEvent() }` and
 * only watched Press/Release. That stayed `isPressed = true` while scrolling
 * (Move/Cancel never reset it) and fought with `clickable` ripples.
 *
 * This version uses awaitEachGesture + waitForUpOrCancellation, so a scroll
 * gesture cancels the squish instead of leaving the card stuck scaled-down.
 */
fun Modifier.squishable(
    enabled: Boolean = true,
    scaleDown: Float = 0.93f
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by androidx.compose.animation.core.animateFloatAsState(
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
                // up == null means drag/scroll/cancel -> always release.
                isPressed = false
                if (up == null) return@awaitEachGesture
            }
        }
}

/**
 * Ripple-safe press bounce kept for API compatibility.
 * Previously this took an [interactionSource] that was never connected to the
 * host Surface/clickable, so it never animated. It now delegates to the
 * gesture-based [squishable] so both names behave identically.
 */
@Composable
fun Modifier.bouncyPress(
    enabled: Boolean = true,
    scaleDown: Float = 0.93f,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
): Modifier = squishable(enabled = enabled, scaleDown = scaleDown)

/**
 * Even entrance for cards/rows: fade + short slide-up + tiny scale pop.
 *
 * - Same travel for every item (1/12 height, 0.96 scale) so nothing jumps.
 * - Fade uses a short tween so text never flashes.
 * - [delayMillis] should only be used for the first screen paint
 *   (top-level sections). NEVER stagger per-row inside a scrolling LazyColumn
 *   — recycled rows would pop in late while scrolling and wreck smoothness.
 */
@Composable
fun BouncyPopIn(
    visible: Boolean = true,
    delayMillis: Int = 0,
    modifier: Modifier = Modifier,
    key: Any? = null,
    content: @Composable () -> Unit
) {
    // Play-once: recycled / revisited items render instantly instead of
    // replaying the delay + enter while scrolling or paging.
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

/** Subtle scale for selection icons (toolbar, carousel check badge). */
@Composable
fun rememberBouncySelectedScale(selected: Boolean): Float {
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (selected) 1f else 0.9f,
        animationSpec = BouncySpecs.pop,
        label = "bouncy_selected_scale"
    )
    return scale
}

/**
 * List-safe item wrapper: NO delay, NO slide (slides inside Lazy layouts
 * cause remeasurement jank). Just fade + tiny scale.
 * (Deliberately no animateItem: the platform Compose in this tree predates
 * LazyItemScope.animateItem, so we avoid it for compatibility.)
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
 *   Modifier.verticalBouncyEdge().verticalScroll(state)
 *   LazyColumn(Modifier.verticalBouncyEdge(), state = ...)
 *
 * Unconsumed scroll delta at the list ends stretches content (a fraction of
 * the finger travel, capped), then springs back with [BouncySpecs.overscroll].
 * Normal scrolling is untouched (we return Zero until the edge), so fling
 * smoothness is preserved.
 *
 * Implementation note: the stretch target is accumulated synchronously in a
 * plain var and only the Animatable write is posted async, guarded by a
 * generation counter. (Computing the next value from `offset.value` and
 * posting `snapTo` used to read a stale value under rapid scroll events, so
 * deltas collapsed and the stretch visibly lagged behind the finger.)
 */
fun Modifier.verticalBouncyEdge(
    enabled: Boolean = true,
    maxStretchPx: Float = 180f,
    stretchFactor: Float = 0.45f
): Modifier = composed {
    if (!enabled) return@composed this
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val connection = remember(maxStretchPx, stretchFactor) {
        object : NestedScrollConnection {
            var target = 0f
            var generation = 0

            private fun stretchBy(delta: Float) {
                generation += 1
                val snapshot = generation
                target = (target + delta * stretchFactor)
                    .coerceIn(-maxStretchPx, maxStretchPx)
                val settled = target
                scope.launch {
                    // A newer gesture event wins; stale snaps never clobber it.
                    if (snapshot == generation) offset.snapTo(settled)
                }
            }

            fun settle() {
                if (target == 0f && offset.value == 0f) return
                generation += 1
                target = 0f
                scope.launch { offset.animateTo(0f, BouncySpecs.overscroll) }
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y != 0f) {
                    stretchBy(available.y)
                    // Consume the edge delta so it becomes stretch, not fling.
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // While stretched, eat drag that pulls back toward rest so the
                // content follows the finger home instead of scrolling underneath.
                if (target != 0f && source == NestedScrollSource.Drag) {
                    val pullingHome =
                        (target > 0f && available.y < 0f) ||
                            (target < 0f && available.y > 0f)
                    if (pullingHome) {
                        stretchBy(available.y)
                        return Offset(0f, available.y)
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                if (target != 0f || offset.value != 0f) {
                    settle()
                    return available
                }
                return Velocity.Zero
            }
        }
    }
    // Spring back on finger lift even when no fling is dispatched.
    // NOTE: inside awaitEachGesture the receiver is AwaitPointerEventScope,
    // not a CoroutineScope, so we must use the outer `scope`.
    this
        .nestedScroll(connection)
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                waitForUpOrCancellation()
                connection.settle()
            }
        }
        .graphicsLayer { translationY = offset.value }
}

/**
 * Same edge-stretch for horizontal rows (e.g. sleep-timer chip strip).
 */
fun Modifier.horizontalBouncyEdge(
    enabled: Boolean = true,
    maxStretchPx: Float = 160f,
    stretchFactor: Float = 0.45f
): Modifier = composed {
    if (!enabled) return@composed this
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val connection = remember(maxStretchPx, stretchFactor) {
        object : NestedScrollConnection {
            var target = 0f
            var generation = 0

            private fun stretchBy(delta: Float) {
                generation += 1
                val snapshot = generation
                target = (target + delta * stretchFactor)
                    .coerceIn(-maxStretchPx, maxStretchPx)
                val settled = target
                scope.launch {
                    if (snapshot == generation) offset.snapTo(settled)
                }
            }

            fun settle() {
                if (target == 0f && offset.value == 0f) return
                generation += 1
                target = 0f
                scope.launch { offset.animateTo(0f, BouncySpecs.overscroll) }
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.x != 0f) {
                    stretchBy(available.x)
                    return Offset(available.x, 0f)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                if (target != 0f || offset.value != 0f) {
                    settle()
                    return available
                }
                return Velocity.Zero
            }
        }
    }
    this
        .nestedScroll(connection)
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                waitForUpOrCancellation()
                connection.settle()
            }
        }
        .graphicsLayer { translationX = offset.value }
}
