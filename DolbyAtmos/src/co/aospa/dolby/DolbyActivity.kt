/*
 * Copyright (C) 2023-24 Paranoid Android
 * Copyright (C) 2026 samakshkambxj
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby

import android.content.Intent
import android.media.audiofx.AudioEffect
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import co.aospa.dolby.ui.DolbyApp
import co.aospa.dolby.ui.DolbyViewModel
import co.aospa.dolby.ui.PAGE_EQUALIZER
import co.aospa.dolby.ui.theme.DolbyTheme

/**
 * Single entry point for the launcher icon, the Settings injection, the QS
 * tile preference shortcut and the system audio-effect panel.
 */
class DolbyActivity : ComponentActivity() {

    private val viewModel: DolbyViewModel by viewModels()

    /**
     * One-shot page request coming from the intent. Kept as Compose state so
     * [DolbyApp] can react to [onNewIntent] without recreating the activity.
     */
    private var pageRequest by mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (savedInstanceState == null) {
            pageRequest = pageForIntent(intent)
        }

        setContent {
            DolbyTheme {
                DolbyApp(
                    viewModel = viewModel,
                    pageRequest = pageRequest,
                    onPageRequestConsumed = { pageRequest = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pageRequest = pageForIntent(intent)
    }

    private fun pageForIntent(intent: Intent?): Int? =
        if (intent?.action == AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL) {
            PAGE_EQUALIZER
        } else {
            null
        }
}
