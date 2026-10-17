/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 *
 * Ported from OriginSU ui/animation/predictiveback.
 * Dependency-free easing math shared by the navigation transition styles.
 */

package org.dolbyatmos.ui.animation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/**
 * The AOSP "emphasized" curve, built as two cubic-bezier segments joined at a
 * knot so a single [Easing] reproduces the two-stage accelerate/decelerate
 * shape. Used by the AOSP navigation style's 450ms settle.
 */
internal val FastOutExtraSlowIn: Easing = run {
    val knotX = 0.166666f
    val knotY = 0.4f
    val first = CubicBezierEasing(0.05f / knotX, 0f, 0.133333f / knotX, 0.06f / knotY)
    val second = CubicBezierEasing(
        (0.208333f - knotX) / (1f - knotX),
        (0.82f - knotY) / (1f - knotY),
        (0.25f - knotX) / (1f - knotX),
        (1f - knotY) / (1f - knotY),
    )
    Easing { fraction ->
        if (fraction < knotX) {
            knotY * first.transform(fraction / knotX)
        } else {
            knotY + (1f - knotY) * second.transform((fraction - knotX) / (1f - knotX))
        }
    }
}

/** Damped curve for entries that must not overshoot while the finger is down. */
internal val BackGestureEasing: Easing = CubicBezierEasing(0.1f, 0.1f, 0f, 1f)
