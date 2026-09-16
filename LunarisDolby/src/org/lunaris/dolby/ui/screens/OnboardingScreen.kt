/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.screens

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import org.lunaris.dolby.R
import org.lunaris.dolby.ui.components.BouncyPopIn
import org.lunaris.dolby.ui.components.DolbyLogo
import org.lunaris.dolby.ui.components.ModernSettingSwitch
import org.lunaris.dolby.ui.viewmodel.DolbyViewModel
import org.lunaris.dolby.utils.ToastHelper

private data class OnboardingPage(
    val icon: ImageVector,
    val titleRes: Int,
    val bodyRes: Int
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
    val pages = listOf(
        OnboardingPage(
            icon = Icons.Default.Tune,
            titleRes = R.string.onboarding_welcome_title,
            bodyRes = R.string.onboarding_welcome_body
        ),
        OnboardingPage(
            icon = Icons.Default.Interests,
            titleRes = R.string.onboarding_scenes_title,
            bodyRes = R.string.onboarding_scenes_body
        ),
        OnboardingPage(
            icon = Icons.Default.GraphicEq,
            titleRes = R.string.onboarding_eq_title,
            bodyRes = R.string.onboarding_eq_body
        ),
        OnboardingPage(
            icon = Icons.Default.SurroundSound,
            titleRes = R.string.onboarding_pro_title,
            bodyRes = R.string.onboarding_pro_body
        )
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })
    var dolbyOn by remember { mutableStateOf(true) }

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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (pagerState.currentPage < pages.size - 1) {
                    TextButton(onClick = { finish() }) {
                        Text(stringResource(R.string.onboarding_skip))
                    }
                } else {
                    Spacer(modifier = Modifier.height(40.dp))
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
                        if (index == 0) {
                            DolbyLogo(modifier = Modifier.size(96.dp))
                        } else {
                            Surface(
                                modifier = Modifier.size(96.dp),
                                shape = MaterialTheme.shapes.extraLarge,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    androidx.compose.material3.Icon(
                                        imageVector = page.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            text = stringResource(page.titleRes),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(page.bodyRes),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (index == pages.size - 1) {
                            Spacer(modifier = Modifier.height(24.dp))
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pages.size) { i ->
                    val selected = i == pagerState.currentPage
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(
                                width = if (selected) 28.dp else 8.dp,
                                height = 8.dp
                            ),
                        shape = CircleShape,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        }
                    ) {}
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage > 0) {
                    TextButton(onClick = {
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

private const val PREFS_NAME = "dolby_prefs"
internal const val KEY_ONBOARDING_DONE = "onboarding_completed"

/** True once the user has finished (or skipped) the first-run tutorial. */
fun isOnboardingDone(context: Context): Boolean {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_ONBOARDING_DONE, false)
}
