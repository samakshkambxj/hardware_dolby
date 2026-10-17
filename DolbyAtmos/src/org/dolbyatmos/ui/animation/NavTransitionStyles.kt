/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 *
 * Navigation transition styles ported from OriginSU
 * ui/animation/predictiveback.
 *
 * OriginSU drives these through the miuix-nav runtime (NavTransition /
 * navGraphicsTransition). That artifact is not part of this AOSP/Soong build,
 * so the same *motion* is reproduced here with androidx.compose.animation
 * transitions on the existing NavHost: identical durations, easings and scale
 * factors, applied to push/pop instead of a gesture-seeked surface.
 */

package org.dolbyatmos.ui.animation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut

/** Selectable navigation transition. Mirrors OriginSU's PredictiveBackAnimation. */
enum class NavAnimationStyle {
    /** Plain, quick cross-fade -- the pre-port behaviour. */
    NONE,

    /** AOSP emphasized settle: long, eased fade with no scale. */
    AOSP,

    /** Scale pop: content shrinks/grows with a FastOutSlowIn curve. */
    SCALE,

    /** Classic scale pop using the MIUI cubic curve. */
    CLASSIC
}

// OriginSU's ClassicNavTransition / ScaleNavTransition curves.
private val ClassicEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

private const val AOSP_DURATION = 450
private const val SCALE_DURATION = 200

private const val SCALE_ENTER = 0.92f
private const val SCALE_EXIT = 0.90f

/** Forward navigation: the incoming page. */
internal fun navEnterTransition(style: NavAnimationStyle): EnterTransition = when (style) {
    NavAnimationStyle.NONE -> fadeIn(tween(180))
    NavAnimationStyle.AOSP -> fadeIn(tween(AOSP_DURATION, easing = FastOutExtraSlowIn))
    NavAnimationStyle.SCALE -> fadeIn(tween(SCALE_DURATION, easing = FastOutSlowInEasing)) +
        scaleIn(initialScale = SCALE_ENTER, animationSpec = tween(SCALE_DURATION, easing = FastOutSlowInEasing))
    NavAnimationStyle.CLASSIC -> fadeIn(tween(SCALE_DURATION, easing = ClassicEasing)) +
        scaleIn(initialScale = SCALE_ENTER, animationSpec = tween(SCALE_DURATION, easing = ClassicEasing))
}

/** Forward navigation: the outgoing page. */
internal fun navExitTransition(style: NavAnimationStyle): ExitTransition = when (style) {
    NavAnimationStyle.NONE -> fadeOut(tween(150))
    NavAnimationStyle.AOSP -> fadeOut(tween(AOSP_DURATION, easing = FastOutExtraSlowIn))
    NavAnimationStyle.SCALE -> fadeOut(tween(SCALE_DURATION, easing = FastOutSlowInEasing)) +
        scaleOut(targetScale = SCALE_EXIT, animationSpec = tween(SCALE_DURATION, easing = FastOutSlowInEasing))
    NavAnimationStyle.CLASSIC -> fadeOut(tween(SCALE_DURATION, easing = ClassicEasing)) +
        scaleOut(targetScale = SCALE_EXIT, animationSpec = tween(SCALE_DURATION, easing = ClassicEasing))
}

/** Back navigation: the page being revealed underneath. */
internal fun navPopEnterTransition(style: NavAnimationStyle): EnterTransition = when (style) {
    NavAnimationStyle.NONE -> fadeIn(tween(180))
    NavAnimationStyle.AOSP -> fadeIn(tween(AOSP_DURATION, easing = FastOutExtraSlowIn))
    NavAnimationStyle.SCALE -> fadeIn(tween(SCALE_DURATION, easing = FastOutSlowInEasing)) +
        scaleIn(initialScale = SCALE_EXIT, animationSpec = tween(SCALE_DURATION, easing = FastOutSlowInEasing))
    NavAnimationStyle.CLASSIC -> fadeIn(tween(SCALE_DURATION, easing = ClassicEasing)) +
        scaleIn(initialScale = SCALE_EXIT, animationSpec = tween(SCALE_DURATION, easing = ClassicEasing))
}

/** Back navigation: the page sliding away -- the predictive-back visual. */
internal fun navPopExitTransition(style: NavAnimationStyle): ExitTransition = when (style) {
    NavAnimationStyle.NONE -> fadeOut(tween(180))
    NavAnimationStyle.AOSP -> fadeOut(tween(AOSP_DURATION, easing = FastOutExtraSlowIn))
    NavAnimationStyle.SCALE -> fadeOut(tween(SCALE_DURATION, easing = FastOutSlowInEasing)) +
        scaleOut(targetScale = SCALE_ENTER, animationSpec = tween(SCALE_DURATION, easing = FastOutSlowInEasing))
    NavAnimationStyle.CLASSIC -> fadeOut(tween(SCALE_DURATION, easing = ClassicEasing)) +
        scaleOut(targetScale = SCALE_EXIT, animationSpec = tween(SCALE_DURATION, easing = ClassicEasing))
}
