/*
 * Copyright (C) 2026 samakshkambxj
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowCompat
import co.aospa.dolby.geq.ui.EqualizerViewModel

/**
 * Shared Compose host for the main Dolby activity and the standalone equalizer activity.
 *
 * The window is drawn edge to edge; each screen applies its own status/navigation bar insets and
 * the floating navigation bar sits above the gesture area.
 */
abstract class DolbyBaseActivity : ComponentActivity() {

    protected open val initialTab: DolbyTab = DolbyTab.Home

    private val dolbyViewModel: DolbyViewModel by viewModels { DolbyViewModel.Factory }
    private val equalizerViewModel: EqualizerViewModel by viewModels { EqualizerViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val dark = isSystemInDarkTheme()
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }

            DolbyTheme {
                DolbyApp(
                    dolbyViewModel = dolbyViewModel,
                    equalizerViewModel = equalizerViewModel,
                    initialTab = initialTab,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Pick up changes made by the quick settings tile or another process while we were away.
        dolbyViewModel.refresh()
        equalizerViewModel.reload()
    }
}
