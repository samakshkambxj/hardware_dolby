/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.screens

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import android.os.SystemClock
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import org.lunaris.dolby.ui.components.FloatingNavToolbar
import org.lunaris.dolby.ui.viewmodel.AppProfileViewModel
import org.lunaris.dolby.ui.viewmodel.DolbyViewModel
import org.lunaris.dolby.ui.viewmodel.EqualizerViewModel

sealed class Screen(val route: String) {
    object Settings : Screen("settings")
    object Equalizer : Screen("equalizer")
    object Advanced : Screen("advanced")
    object AppProfiles : Screen("app_profiles")
    object ImportExport : Screen("import_export")
    object Onboarding : Screen("onboarding")
    object EasterEgg : Screen("easter_egg")
    object DapProbe : Screen("dap_probe")
    object About : Screen("about")
    object PageStyle : Screen("page_style")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainPagerScreen(
    dolbyViewModel: DolbyViewModel,
    equalizerViewModel: EqualizerViewModel,
    navController: NavController
) {
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { 4 }
    )
    val coroutineScope = rememberCoroutineScope()

    // Nav dots: Advanced when a Tuning Lab value was customized on the
    // current profile (a persisted lab pref exists — plain HAL reads no
    // longer write prefs), Equalizer when a custom/user preset is active.
    val dolbyState by dolbyViewModel.uiState.collectAsState()
    val eqState by equalizerViewModel.uiState.collectAsState()
    val advancedBadge =
        (dolbyState as? org.lunaris.dolby.domain.models.DolbyUiState.Success)
            ?.profileSettings?.labParams?.isNotEmpty() == true
    val equalizerBadge =
        (eqState as? org.lunaris.dolby.domain.models.EqualizerUiState.Success)
            ?.currentPreset?.let { it.isUserDefined || it.isCustom } == true

    val currentFakeRoute = when (pagerState.currentPage) {
        0 -> "settings"
        1 -> "equalizer"
        2 -> "advanced"
        3 -> "volume"
    else -> "settings"
    }

    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage == 1) {
            equalizerViewModel.loadEqualizer()
        }
    }

    // Bucketed swipe offset drives navbar blur re-snapshots: coarse enough
    // (~6 steps per swipe) to avoid recomposing per frame, fresh enough that
    // the blur never looks stale/delayed. Throttled to ~100ms in the view.
    val offsetBucket = try {
        (pagerState.currentPageOffsetFraction * 6).toInt()
    } catch (_: Exception) {
        0
    }

    // Vertical list scrolls never touch Android Views (LazyColumn scrolls by
    // recomposing), so the blur view cannot hear them via ViewTreeObserver.
    // Instead the root observes every nested scroll delta from the pages
    // below and bumps a tick — throttled here, throttled again (~100ms) in
    // the blur view, so scrolls refresh at ~8fps and never per frame.
    // The tick state is read only inside BottomNavOverlay (via blurKey),
    // so scroll ticks recompose just the pill — never the pager pages.
    val scrollTickState = remember { mutableIntStateOf(0) }
    val blurScrollConnection = remember {
        var lastTickMs = 0L
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val now = SystemClock.uptimeMillis()
                if (now - lastTickMs > 90L) {
                    lastTickMs = now
                    scrollTickState.intValue++
                }
                return Offset.Zero
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(blurScrollConnection)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> ModernDolbySettingsScreen(
                    viewModel = dolbyViewModel,
                    navController = navController
                )
                1 -> ModernEqualizerScreen(
                    viewModel = equalizerViewModel,
                    navController = navController
                )
                2 -> ModernAdvancedSettingsScreen(
                    viewModel = dolbyViewModel,
                    navController = navController
                )
                3 -> VolumeControlScreen()
            }
        }

        // Soft translucent fade only: the nav pill carries live blur, so this
        // must stay translucent — an opaque scrim would flatten the blur.
        BottomNavOverlay(
            currentRoute = currentFakeRoute,
            blurKey = {
                "${pagerState.currentPage}:$offsetBucket:${scrollTickState.intValue}"
            },
            advancedBadge = advancedBadge,
            equalizerBadge = equalizerBadge,
            onNavigate = { route ->
                coroutineScope.launch {
                    when (route) {
                        "settings" -> pagerState.animateScrollToPage(0)
                        "equalizer" -> pagerState.animateScrollToPage(1)
                        "advanced" -> pagerState.animateScrollToPage(2)
                        "volume" -> pagerState.animateScrollToPage(3)
                    }
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/**
 * Fade + floating pill. Separate restart scope so blur ticks (scrolls,
 * swipes) recompose only this overlay — the pager pages above never pay
 * for a blur refresh.
 */
@Composable
private fun BottomNavOverlay(
    currentRoute: String,
    blurKey: () -> Any,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    advancedBadge: Boolean = false,
    equalizerBadge: Boolean = false
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(130.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.45f)
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .safeDrawingPadding(),
            contentAlignment = Alignment.Center
        ) {
            FloatingNavToolbar(
                currentRoute = currentRoute,
                blurKey = blurKey(),
                advancedBadge = advancedBadge,
                equalizerBadge = equalizerBadge,
                onNavigate = onNavigate
            )
        }
    }
}

@Composable
fun DolbyNavHost(
    dolbyViewModel: DolbyViewModel,
    equalizerViewModel: EqualizerViewModel
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val startDestination = remember {
        if (isOnboardingDone(context)) "main_pager" else Screen.Onboarding.route
    }

    // Fade-only transitions on all four directions: the back-arrow button
    // calls navigateUp()/popBackStack() while an edge swipe drives the same
    // pop via the system gesture. Without explicit transitions the gesture
    // path falls back to the platform predictive-back animation (window
    // scale/shift), which looks unrelated to the in-app fade. Pinning all
    // four to the same fade keeps button-back and swipe-back identical.
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn(animationSpec = tween(250)) },
        exitTransition = { fadeOut(animationSpec = tween(200)) },
        popEnterTransition = { fadeIn(animationSpec = tween(250)) },
        popExitTransition = { fadeOut(animationSpec = tween(200)) }
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                viewModel = dolbyViewModel,
                navController = navController
            )
        }
        composable("main_pager") {
            MainPagerScreen(
                dolbyViewModel = dolbyViewModel,
                equalizerViewModel = equalizerViewModel,
                navController = navController
            )
        }
        
        composable(Screen.AppProfiles.route) {
            val context = LocalContext.current
            val appProfileViewModel: AppProfileViewModel = viewModel(
                factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(
                    context.applicationContext as android.app.Application
                )
            )
            
            AppProfileScreen(
                viewModel = appProfileViewModel,
                navController = navController
            )
        }
        
        composable(Screen.ImportExport.route) {
            PresetImportExportScreen(
                viewModel = equalizerViewModel,
                navController = navController
            )
        }

        composable(Screen.EasterEgg.route) {
            EasterEggScreen(
                dolbyViewModel = dolbyViewModel,
                equalizerViewModel = equalizerViewModel,
                navController = navController
            )
        }

        composable(Screen.DapProbe.route) {
            DapProbeScreen(
                viewModel = dolbyViewModel,
                navController = navController
            )
        }

        composable(Screen.About.route) {
            AboutScreen(
                navController = navController
            )
        }

        composable(Screen.PageStyle.route) {
            PageStyleScreen(
                navController = navController
            )
        }
    }
}
