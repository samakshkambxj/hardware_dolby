/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daybreak.dolby.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.daybreak.dolby.ui.glass.LocalGlassBackdrop
import com.daybreak.dolby.ui.glass.liquid.lens
import com.daybreak.dolby.ui.glass.liquid.vibrancy
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.highlight.BloomStroke
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import kotlin.math.max

/**
 * Liquid-glass background sampling [backdrop]. Overlay-only: the surface
 * must live outside the subtree it samples, or the layer records itself.
 */
@Composable
fun LiquidGlassBackdrop(
    tint: Color,
    blurRadiusPx: Float,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    shape: Shape,
    rainbow: Boolean = true,
    intensity: Float = 0.6f,
) {
    val glass = backdrop?.takeIf {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }
    if (glass != null) {
        Box(modifier = modifier) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBackdrop(
                        backdrop = glass,
                        shape = { shape },
                        effects = {
                            padding = maxOf(padding, 40.dp.toPx())
                            vibrancy()
                            val radius = blurRadiusPx.coerceIn(1f, 64f)
                            blur(radius, radius)
                            lens(
                                refractionHeight = 24.dp.toPx(),
                                refractionAmount = (8f + 32f * intensity).dp.toPx(),
                                chromaticAberration = if (rainbow) 0.6f else 0f,
                            )
                        },
                        highlight = {
                            Highlight(style = BloomStroke.GlassStrokeMiddleLight)
                        },
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(tint)
            )
        }
    } else {
        Box(
            modifier = modifier.background(tint, shape),
        )
    }
}
