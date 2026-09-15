/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.components

import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider

/**
 * Real background blur for any dialog.
 *
 * Call once inside the dialog's content (any AlertDialog slot, or the first
 * line inside a Dialog {} block). The composable resolves the *dialog*
 * window via [DialogWindowProvider] and enables the platform
 * blur-behind layer:
 *
 * - API 31+ (S): FLAG_BLUR_BEHIND + blurBehindRadius. The wallpaper /
 *   app content behind the dialog is blurred by SurfaceFlinger, so the
 *   credits / AutoEQ / confirm panels float over frosted glass.
 * - Below S: gracefully falls back to dim only (no-op blur flag).
 *
 * Why a helper instead of Modifier.blur: Modifier.blur / RenderEffect blurs
 * a composable's *own* pixels, never what's behind its window. Dialogs live
 * in their own window, so only the window-level blurBehindRadius can blur
 * the app content underneath them.
 *
 * @param blurRadiusPx 0..150, platform clamps. 80-100 reads as frosted glass
 * without smearing text behind into mush.
 * @param dimAmount 0f (no dim) .. 1f (pitch black). Kept modest so the blur
 * shows through instead of a flat black veil.
 */
@Composable
fun ApplyDialogWindowBlur(
    blurRadiusPx: Int = 90,
    dimAmount: Float = 0.40f
) {
    val view = LocalView.current
    SideEffect {
        // LocalView inside AlertDialog/Dialog content is hosted in the dialog
        // window, whose parent implements DialogWindowProvider.
        val window = (view.parent as? DialogWindowProvider)?.window
            ?: run {
                // Fallback: walk up ContextWrapper chain for a Dialog window.
                var ctx: android.content.Context? = view.context
                var found: android.view.Window? = null
                while (ctx != null && found == null) {
                    if (ctx is android.app.Dialog) {
                        found = ctx.window
                    }
                    ctx = (ctx as? android.content.ContextWrapper)?.baseContext
                }
                found
            }
        window?.let {
            try {
                it.setDimAmount(dimAmount)
            } catch (_: Exception) {
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    it.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    val attrs = it.attributes
                    attrs.blurBehindRadius = blurRadiusPx.coerceIn(0, 150)
                    it.attributes = attrs
                } catch (_: Exception) {
                    // OEM stripped blur-behind; dim above still applies.
                }
            }
        }
    }
}
