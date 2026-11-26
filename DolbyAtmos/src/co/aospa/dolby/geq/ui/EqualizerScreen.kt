/*
 * Copyright (C) 2024-2026 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.geq.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Equalizer destination of the floating tab bar.
 *
 * The actual page is the Lunaris-style [ModernEqualizerScreen]; this wrapper only keeps the
 * historical call site (and the space reserved for the floating navigation bar) in one place.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun EqualizerTab(
    viewModel: EqualizerViewModel,
    profileName: String,
    scrollState: ScrollState = rememberScrollState(),
    modifier: Modifier = Modifier,
) {
    ModernEqualizerScreen(
        viewModel = viewModel,
        scrollState = scrollState,
        bottomPadding = 104.dp,
        modifier = modifier,
    )
}
