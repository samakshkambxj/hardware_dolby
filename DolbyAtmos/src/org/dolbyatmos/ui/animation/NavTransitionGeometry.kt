/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 *
 * Ported from OriginSU ui/animation/predictiveback.
 *
 * Pixel-snapping helpers so a scaled/translated page edge lands on an exact
 * device pixel instead of a fraction of one -- otherwise the scaled content
 * shimmers during the transition.
 */

package org.dolbyatmos.ui.animation

import kotlin.math.roundToInt

internal fun snapScaleToPixelExtent(scale: Float, extent: Float): Float =
    if (extent > 0f) (scale * extent).roundToInt() / extent else scale

internal fun snapTranslationToPixelEdge(
    translation: Float,
    scale: Float,
    extent: Float,
    pivotFraction: Float = 0.5f,
): Float {
    if (extent <= 0f) return translation
    val scaledEdgeOffset = extent * pivotFraction * (1f - scale)
    return (translation + scaledEdgeOffset).roundToInt() - scaledEdgeOffset
}
