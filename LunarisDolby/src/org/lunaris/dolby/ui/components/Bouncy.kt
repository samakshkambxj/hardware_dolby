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
import androidx.compose.ui.draw.clipToBounds
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

    /**
     * Edge-stretch return for scroll containers only.
     *
     * Fast + nearly critical: single small overshoot, settles in ~250-350ms.
     * The old MediumBouncy/Medium spec oscillated for ~800ms+, which is why
     * a fast fling felt "stuck" then released late.
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
 * Fast-scroll fixes vs the old version:
 * - `offset.stop()` runs synchronously on every new edge delta, so an
 *   in-flight spring-back never fights the finger (the old "stuck" feeling).
 * - Diminishing resistance as [target] approaches [maxStretchPx], so a fast
 *   fling bends instead of slamming into a hard cap and sitting there.
 * - Snap is posted with UNDISPATCHED so translation follows the finger the
 *   same frame instead of lagging a dispatch behind.
 * - Clipped to the list bounds ([clipToBounds]): the stretch translation can
 *   never paint over the top bar or status bar. The old unclipped version let
 *   rows bleed on top of the "Volume" title and clock icons at full stretch,
 *   then visibly snap back on release.
 * - Pre-scroll only consumes the finger travel actually needed to pull the
 *   stretch back to rest; any remainder is handed to the list. The old
 *   version swallowed the whole drag while `target != 0`, so a tiny residual
 *   stretch froze scrolling until it decayed (list felt stuck, then jumped).
 * - Single [settleJob] (cancel-previous) so finger-lift + fling can't launch
 *   two competing animateTo coroutines on the same Animatable.
 * - Over-stretch guard: every mutation is serialized through one snap owner
 *   plus a generation token, so a stale snap can never resume after release
 *   and yank the offset back out (the "stretched past the limit gets stuck"
 *   bug). Redundant snaps at the cap are skipped entirely, and a watchdog
 *   forces exact rest after the return spring.
 * - Return spring is fast/critically-damped ([BouncySpecs.overscroll]) and
 *   carries fling velocity, so release feels instant, not late + wobbly.
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
            var settleJob: kotlinx.coroutines.Job? = null
            var snapJob: kotlinx.coroutines.Job? = null
            // Owner token bumped on every mutation. Suspended snaps capture
            // the token at launch and abort if a newer mutation (or the
            // release settle) has since taken ownership of the offset.
            var generation = 0

            private fun resistance(): Float {
                val t = (kotlin.math.abs(target) / maxStretchPx).coerceIn(0f, 1f)
                // 1.0 at rest -> ~0.15 at the cap: fast scrolls bend, never slam.
                return 1f - t * t * 0.85f
            }

            private fun stretchBy(delta: Float) {
                // Kill a running spring-back instantly: finger owns the offset now.
                // Previous snap is cancelled first so only the latest delta owns
                // the Animatable; UNDISPATCHED stop+snap then follows the finger
                // the same frame (no dispatch lag).
                settleJob?.cancel()
                snapJob?.cancel()
                val prev = target
                target = (target + delta * stretchFactor * resistance())
                    .coerceIn(-maxStretchPx, maxStretchPx)
                // Past the cap, repeated deltas resolve to the same target the
                // offset already holds: skip the snap instead of churning the
                // Animatable mutex with stale work that could resume post-release.
                if (target == prev && offset.value == target && !offset.isRunning) return
                val settled = target
                val myGen = ++generation
                snapJob = scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
                    try {
                        offset.stop()
                    } catch (_: Exception) {
                    }
                    if (myGen != generation) return@launch
                    try {
                        offset.snapTo(settled)
                    } catch (_: kotlinx.coroutines.CancellationException) {
                        // Superseded by a newer stretch/settle.
                    }
                }
            }

            fun settle(velocity: Float = 0f) {
                if (target == 0f && offset.value == 0f && !offset.isRunning) return
                target = 0f
                snapJob?.cancel()
                settleJob?.cancel()
                val myGen = ++generation
                val startVelocity = velocity.coerceIn(-2500f, 2500f)
                settleJob = scope.launch {
                    try {
                        if (myGen != generation) return@launch
                        offset.animateTo(
                            targetValue = 0f,
                            animationSpec = BouncySpecs.overscroll,
                            initialVelocity = startVelocity
                        )
                    } catch (_: kotlinx.coroutines.CancellationException) {
                        // Superseded by a new stretch/settle; finger owns the offset.
                        return@launch
                    }
                    // Watchdog: if anything displaced the offset after the
                    // spring (stale snap, missed fling), force exact rest.
                    try {
                        if (myGen == generation && offset.value != 0f && !offset.isRunning) {
                            offset.snapTo(0f)
                        }
                    } catch (_: Exception) {
                    }
                }
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y != 0f) {
                    // Only stretch the leftover the list couldn't consume, and
                    // ignore tiny jitter so normal scroll stays 1:1.
                    if (kotlin.math.abs(available.y) < 0.5f) return Offset.Zero
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
                // While stretched, consume only the finger travel needed to
                // pull back to rest; the remainder scrolls the list. Never
                // swallow a whole drag on a residual stretch (scroll freeze).
                if (target != 0f && source == NestedScrollSource.Drag && available.y != 0f) {
                    val pullingHome =
                        (target > 0f && available.y < 0f) ||
                            (target < 0f && available.y > 0f)
                    if (pullingHome) {
                        val denom = stretchFactor * resistance()
                        if (denom > 1e-6f) {
                            val need = -target / denom
                            if (kotlin.math.abs(need) >= kotlin.math.abs(available.y)) {
                                stretchBy(available.y)
                                return Offset(0f, available.y)
                            }
                            // Reach rest exactly, hand the rest to the list.
                            target = 0f
                            snapJob?.cancel()
                            val myGen = ++generation
                            snapJob = scope.launch(
                                start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED
                            ) {
                                try {
                                    offset.stop()
                                } catch (_: Exception) {
                                }
                                if (myGen != generation) return@launch
                                try {
                                    offset.snapTo(0f)
                                } catch (_: kotlinx.coroutines.CancellationException) {
                                }
                            }
                            return Offset(0f, need)
                        }
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                if (target != 0f || offset.value != 0f || offset.isRunning) {
                    // Carry a fraction of the leftover fling into the spring so
                    // a fast flick releases with momentum instead of hanging.
                    settle(velocity = available.y * 0.25f)
                    return available
                }
                return Velocity.Zero
            }
        }
    }
    // Spring back on finger lift even when no fling is dispatched.
    // NOTE: inside awaitEachGesture the receiver is AwaitPointerEventScope,
    // not a CoroutineScope, so we must use the outer `scope`.
    // clipToBounds keeps the stretch inside the list: it can never paint over
    // the Scaffold top bar or the status bar while overscrolled.
    this
        .nestedScroll(connection)
        .clipToBounds()
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
 * Mirrors [verticalBouncyEdge]: synchronous stop, diminishing resistance,
 * serialized snap owner + generation guard against over-stretch stuck,
 * single settle job, velocity-aware fast return.
 */
fun Modifier.horizontalBouncyEdge(
    enabled: Boolean = true,
    maxStretchPx: Float = 120f,
    stretchFactor: Float = 0.32f
): Modifier = composed {
    if (!enabled) return@composed this
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val connection = remember(maxStretchPx, stretchFactor) {
        object : NestedScrollConnection {
            var target = 0f
            var settleJob: kotlinx.coroutines.Job? = null
            var snapJob: kotlinx.coroutines.Job? = null
            var generation = 0

            private fun resistance(): Float {
                val t = (kotlin.math.abs(target) / maxStretchPx).coerceIn(0f, 1f)
                return 1f - t * t * 0.85f
            }

            private fun stretchBy(delta: Float) {
                settleJob?.cancel()
                snapJob?.cancel()
                val prev = target
                target = (target + delta * stretchFactor * resistance())
                    .coerceIn(-maxStretchPx, maxStretchPx)
                if (target == prev && offset.value == target && !offset.isRunning) return
                val settled = target
                val myGen = ++generation
                snapJob = scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
                    try {
                        offset.stop()
                    } catch (_: Exception) {
                    }
                    if (myGen != generation) return@launch
                    try {
                        offset.snapTo(settled)
                    } catch (_: kotlinx.coroutines.CancellationException) {
                    }
                }
            }

            fun settle(velocity: Float = 0f) {
                if (target == 0f && offset.value == 0f && !offset.isRunning) return
                target = 0f
                snapJob?.cancel()
                settleJob?.cancel()
                val myGen = ++generation
                val startVelocity = velocity.coerceIn(-2500f, 2500f)
                settleJob = scope.launch {
                    try {
                        if (myGen != generation) return@launch
                        offset.animateTo(
                            targetValue = 0f,
                            animationSpec = BouncySpecs.overscroll,
                            initialVelocity = startVelocity
                        )
                    } catch (_: kotlinx.coroutines.CancellationException) {
                        return@launch
                    }
                    try {
                        if (myGen == generation && offset.value != 0f && !offset.isRunning) {
                            offset.snapTo(0f)
                        }
                    } catch (_: Exception) {
                    }
                }
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.x != 0f) {
                    if (kotlin.math.abs(available.x) < 0.5f) return Offset.Zero
                    stretchBy(available.x)
                    return Offset(available.x, 0f)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                if (target != 0f || offset.value != 0f || offset.isRunning) {
                    settle(velocity = available.x * 0.25f)
                    return available
                }
                return Velocity.Zero
            }
        }
    }
    this
        .nestedScroll(connection)
        .clipToBounds()
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
