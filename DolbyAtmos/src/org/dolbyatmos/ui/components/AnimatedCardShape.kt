/*
 * Copyright 2024 The Android Open Source Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.dolbyatmos.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Shape

/**
 * Stiff, low-visibility spring so the four corner radii travel together and
 * settle quickly. Hoisted to a single instance: [rememberAnimatedShape] keys
 * its state on the spec, so a fresh `spring(...)` per recomposition would
 * reset the animation every frame.
 */
private val CardShapeSpec = spring<Float>(
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = 0.5f,
)

/**
 * Card corner shape that morphs smoothly instead of snapping when the user
 * changes Page Style (corner style). Thin wrapper over the vendored
 * [rememberAnimatedShape] (AOSP material3 internals, via OriginSU).
 */
@Composable
fun rememberAnimatedCardShape(): Shape {
    val pageStyle by rememberPageStyle()
    return rememberAnimatedShape(
        currentShape = pageStyle.cardShape,
        animationSpec = CardShapeSpec,
    )
}
