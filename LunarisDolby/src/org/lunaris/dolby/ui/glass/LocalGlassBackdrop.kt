/*
 * Copyright (C) 2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.glass

import androidx.compose.runtime.staticCompositionLocalOf
import top.yukonga.miuix.kmp.blur.Backdrop

/**
 * Backdrop sampled by the liquid-glass navbar. Provided near the nav-host
 * root (API 33+ only); null means RealBlurBackdrop falls back to its
 * snapshot-based AndroidView blur.
 */
val LocalGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }
