/*
 * Copyright (C) 2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.lunaris.dolby.ui.glass.LocalGlassBackdrop
import org.lunaris.dolby.ui.glass.liquid.lens
import org.lunaris.dolby.ui.glass.liquid.vibrancy
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.highlight.BloomStroke
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import kotlin.math.max

/**
 * Liquid-glass navbar backdrop (LIQUID style only).
 *
 * Unlike [RealBlurBackdrop] (root snapshot + RenderEffect), this samples a
 * live layer of the content behind the pill through the miuix-blur engine:
 * refraction lens, vibrancy, and a specular highlight — no snapshot loop,
 * no staleness ticks, no per-frame root redraws.
 *
 * Callers must fall back to [RealBlurBackdrop] when [backdrop] is null or
 * the device is below API 33.
 */
@Composable
fun LiquidGlassBackdrop(
    tint: Color,
    blurRadiusPx: Float,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
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
                        shape = { CircleShape },
                        effects = {
                            padding = maxOf(padding, 40.dp.toPx())
                            vibrancy()
                            val radius = blurRadiusPx.coerceIn(1f, 64f)
                            blur(radius, radius)
                            lens(
                                refractionHeight = 24.dp.toPx(),
                                refractionAmount = 24.dp.toPx(),
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
        RealBlurBackdrop(
            tint = tint,
            blurRadiusPx = blurRadiusPx,
            modifier = modifier,
        )
    }
}
