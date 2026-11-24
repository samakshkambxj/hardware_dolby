/*
 * Copyright (C) 2026 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import co.aospa.dolby.ui.components.FloatingNav
import co.aospa.dolby.ui.components.navItems
import co.aospa.dolby.ui.pages.AdvancedPage
import co.aospa.dolby.ui.pages.EqualizerPage
import co.aospa.dolby.ui.pages.SoundPage
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DolbyApp(viewModel: DolbyViewModel) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val state = viewModel.state

    Box(
        modifier =
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                when (page) {
                    0 -> SoundPage(state, viewModel)
                    1 -> EqualizerPage(state, viewModel)
                    else -> AdvancedPage(state, viewModel)
                }
            }
        }

        FloatingNav(
            items = navItems,
            selectedIndex = pagerState.currentPage,
            onSelected = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
        )
    }

    DisposableEffect(viewModel) {
        viewModel.registerDeviceCallback()
        onDispose { viewModel.unregisterDeviceCallback() }
    }
}
