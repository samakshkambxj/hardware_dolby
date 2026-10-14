/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daybreak.dolby.ui.components

import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider

/**
 * Real background blur for dialogs, which live in their own window where
 * layer sampling cannot reach: enables the platform blur-behind layer.
 */
@Composable
fun ApplyDialogWindowBlur(
    blurRadiusPx: Int = 90,
    dimAmount: Float = 0.40f,
) {
    val view = LocalView.current
    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window
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
                }
            }
        }
    }
}
