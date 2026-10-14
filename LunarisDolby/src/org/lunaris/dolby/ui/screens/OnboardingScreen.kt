/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.screens

import android.content.Context
import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Interests
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import org.lunaris.dolby.R
import org.lunaris.dolby.ui.components.AnimatedEqualizerIconDynamic
import org.lunaris.dolby.ui.components.BouncyPopIn
import org.lunaris.dolby.ui.components.DolbyLogo
import org.lunaris.dolby.ui.components.FloatingParticles
import org.lunaris.dolby.ui.components.LiquidGlassBackdrop
import org.lunaris.dolby.ui.components.ModernSettingSwitch
import org.lunaris.dolby.ui.components.rememberReducedMotion
import org.lunaris.dolby.ui.glass.LocalGlassBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import org.lunaris.dolby.ui.viewmodel.DolbyViewModel
import org.lunaris.dolby.utils.HapticFeedbackHelper
import org.lunaris.dolby.utils.ToastHelper
import org.lunaris.dolby.utils.rememberHapticFeedback

private enum class PreviewKind {
    LOGO,
    SCENE_CHIPS,
    EQ_ANIMATED,
    PRO_CHIPS,
}

private data class OnboardingPage(
    val icon: ImageVector,
    val titleRes: Int,
    val bodyRes: Int,
    val preview: PreviewKind,
)

/**
 * First-run tutorial. Shown once (flag in `dolby_prefs`); finish clears it
 * from the back stack so Back never returns here.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    viewModel: DolbyViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = rememberHapticFeedback()
    val pages = listOf(
        OnboardingPage(
            icon = Icons.Default.Tune,
            titleRes = R.string.onboarding_welcome_title,
            bodyRes = R.string.onboarding_welcome_body,
            preview = PreviewKind.LOGO,
        ),
        OnboardingPage(
            icon = Icons.Default.Interests,
            titleRes = R.string.onboarding_scenes_title,
            bodyRes = R.string.onboarding_scenes_body,
            preview = PreviewKind.SCENE_CHIPS,
        ),
        OnboardingPage(
            icon = Icons.Default.GraphicEq,
            titleRes = R.string.onboarding_eq_title,
            bodyRes = R.string.onboarding_eq_body,
            preview = PreviewKind.EQ_ANIMATED,
        ),
        OnboardingPage(
            icon = Icons.Default.SurroundSound,
            titleRes = R.string.onboarding_pro_title,
            bodyRes = R.string.onboarding_pro_body,
            preview = PreviewKind.PRO_CHIPS,
        )
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })
    var dolbyOn by remember { mutableStateOf(true) }

    fun tap() {
        scope.launch {
            haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.CLICK)
        }
    }

    fun finish() {
        viewModel.setDolbyEnabled(dolbyOn)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ONBOARDING_DONE, true)
            .apply()
        navController.navigate("main_pager") {
            popUpTo(Screen.Onboarding.route) { inclusive = true }
        }
    }

    // Own backdrop root: the Get started bar below samples this layer.
    val glassBackdrop = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberLayerBackdrop()
    } else {
        null
    }

    CompositionLocalProvider(LocalGlassBackdrop provides glassBackdrop) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (glassBackdrop != null) Modifier.layerBackdrop(glassBackdrop)
                    else Modifier
                ),
        ) {
        FloatingParticles(
            modifier = Modifier.fillMaxSize(),
            particleCount = 35,
        )
        Scaffold(
            containerColor = Color.Transparent,
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(
                            R.string.onboarding_page_counter,
                            pagerState.currentPage + 1,
                            pages.size,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (pagerState.currentPage < pages.size - 1) {
                        TextButton(onClick = {
                            tap()
                            finish()
                        }) {
                            Text(stringResource(R.string.onboarding_skip))
                        }
                    } else {
                        Spacer(modifier = Modifier.width(64.dp))
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { index ->
                    val page = pages[index]
                    BouncyPopIn(key = "onboarding_$index") {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            BouncyPopIn(key = "onboarding_visual_$index", delayMillis = 60) {
                                PageVisual(page = page)
                            }
                            Spacer(modifier = Modifier.height(28.dp))
                            BouncyPopIn(key = "onboarding_title_$index", delayMillis = 140) {
                                Text(
                                    text = stringResource(page.titleRes),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            BouncyPopIn(key = "onboarding_body_$index", delayMillis = 220) {
                                Text(
                                    text = stringResource(page.bodyRes),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                            if (index == pages.size - 1) {
                                Spacer(modifier = Modifier.height(24.dp))
                                BouncyPopIn(key = "onboarding_cta", delayMillis = 300) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Card(
                                            shape = MaterialTheme.shapes.extraLarge,
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                            )
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                ModernSettingSwitch(
                                                    title = stringResource(R.string.dolby_enable),
                                                    subtitle = stringResource(R.string.dolby_summary),
                                                    checked = dolbyOn,
                                                    onCheckedChange = { dolbyOn = it },
                                                    icon = Icons.Default.Tune
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        // Starter nudge: hear a difference in 10 seconds.
                                        OutlinedButton(
                                            onClick = {
                                                tap()
                                                viewModel.applySceneById("builtin_movie_night")
                                                ToastHelper.showToast(
                                                    context,
                                                    context.getString(R.string.scene_applied)
                                                )
                                            },
                                            shape = MaterialTheme.shapes.large,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Movie,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(stringResource(R.string.onboarding_try_scene))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pages.size) { i ->
                        val selected = i == pagerState.currentPage
                        val dotWidth by animateDpAsState(
                            targetValue = if (selected) 28.dp else 8.dp,
                            animationSpec = spring(
                                stiffness = Spring.StiffnessMediumLow,
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                            ),
                            label = "onboarding_dot_$i",
                        )
                        Surface(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(width = dotWidth, height = 8.dp),
                            shape = CircleShape,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            }
                        ) {}
                    }
                }
                Spacer(modifier = Modifier.height(110.dp))
            }
        }
        }

        // Glass action bar overlay: outside the recorded layer, so its
        // background never self-samples.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                if (glassBackdrop != null) {
                    LiquidGlassBackdrop(
                        tint = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                        blurRadiusPx = 16f,
                        backdrop = glassBackdrop,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.matchParentSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                RoundedCornerShape(24.dp),
                            ),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (pagerState.currentPage > 0) {
                        TextButton(onClick = {
                            tap()
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }) {
                            Text(stringResource(R.string.onboarding_back))
                        }
                    } else {
                        Spacer(modifier = Modifier.width(64.dp))
                    }
                    Button(onClick = {
                        tap()
                        if (pagerState.currentPage == pages.size - 1) {
                            finish()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    }) {
                        Text(
                            stringResource(
                                if (pagerState.currentPage == pages.size - 1) {
                                    R.string.onboarding_get_started
                                } else {
                                    R.string.onboarding_next
                                }
                            )
                        )
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun PageVisual(page: OnboardingPage) {
    when (page.preview) {
        PreviewKind.LOGO -> {
            DolbyLogo(modifier = Modifier.size(112.dp))
        }
        PreviewKind.SCENE_CHIPS -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                HeroIcon(page = page)
                Spacer(modifier = Modifier.height(20.dp))
                PreviewChips(labels = listOf("Movie Night", "Gaming", "Bass Boost"))
            }
        }
        PreviewKind.EQ_ANIMATED -> {
            Surface(
                modifier = Modifier.size(112.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (rememberReducedMotion()) {
                        Icon(
                            imageVector = page.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(52.dp)
                        )
                    } else {
                        AnimatedEqualizerIconDynamic(
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            size = 56.dp,
                            barCount = 7,
                        )
                    }
                }
            }
        }
        PreviewKind.PRO_CHIPS -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                HeroIcon(page = page)
                Spacer(modifier = Modifier.height(20.dp))
                PreviewChips(labels = listOf("Spatial", "Sleep timer", "Per-app"))
            }
        }
    }
}

@Composable
private fun HeroIcon(page: OnboardingPage) {
    Surface(
        modifier = Modifier.size(96.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
private fun PreviewChips(labels: List<String>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        modifier = Modifier.fillMaxWidth(),
    ) {
        labels.forEach { label ->
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}

private const val PREFS_NAME = "dolby_prefs"
internal const val KEY_ONBOARDING_DONE = "onboarding_completed"

/** True once the user has finished (or skipped) the first-run tutorial. */
fun isOnboardingDone(context: Context): Boolean {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_ONBOARDING_DONE, false)
}
