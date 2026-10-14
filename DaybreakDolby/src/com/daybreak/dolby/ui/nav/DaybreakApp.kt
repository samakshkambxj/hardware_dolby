/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daybreak.dolby.ui.nav

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.SpatialAudio
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.daybreak.dolby.R
import com.daybreak.dolby.ui.equalizer.EqScreen
import com.daybreak.dolby.ui.equalizer.EqViewModel
import com.daybreak.dolby.ui.glass.FloatingBottomBar
import com.daybreak.dolby.ui.glass.FloatingBottomBarItem
import com.daybreak.dolby.ui.glass.LocalGlassBackdrop
import com.daybreak.dolby.ui.sound.SoundScreen
import com.daybreak.dolby.ui.sound.SoundViewModel
import com.daybreak.dolby.ui.spatial.SpatialScreen
import com.daybreak.dolby.ui.spatial.SpatialViewModel
import com.daybreak.dolby.ui.volume.VolumeScreen
import com.daybreak.dolby.ui.volume.VolumeViewModel
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

private data class Tab(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
)

private val TABS = listOf(
    Tab(Destination.Sound.route, R.string.tab_sound, Icons.Filled.Tune),
    Tab(Destination.Equalizer.route, R.string.tab_equalizer, Icons.Filled.GraphicEq),
    Tab(Destination.Spatial.route, R.string.tab_spatial, Icons.Filled.SpatialAudio),
    Tab(Destination.Volume.route, R.string.tab_volume, Icons.Filled.VolumeUp),
)

@Composable
fun DaybreakApp(
    soundViewModel: SoundViewModel,
    eqViewModel: EqViewModel,
    spatialViewModel: SpatialViewModel,
    volumeViewModel: VolumeViewModel,
) {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val selectedIndex = TABS.indexOfFirst { tab ->
        entry?.destination?.hierarchy?.any { it.route == tab.route } == true
    }.coerceAtLeast(0)

    // Backdrop root: pager content records into this layer; the pill and
    // any overlay sample it. The pill lives outside the recorded box.
    val useBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val backdrop = if (useBlur) {
        rememberLayerBackdrop()
    } else {
        null
    }

    CompositionLocalProvider(LocalGlassBackdrop provides backdrop) {
        Scaffold(
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    FloatingBottomBar(
                        selectedIndex = selectedIndex,
                        onSelected = { index ->
                            navController.navigate(TABS[index].route) {
                                popUpTo(Destination.Sound.route)
                                launchSingleTop = true
                            }
                        },
                        tabsCount = TABS.size,
                        backdrop = backdrop,
                    ) { activateTab ->
                        TABS.forEachIndexed { index, tab ->
                            val selected = index == selectedIndex
                            FloatingBottomBarItem(
                                selected = selected,
                                onClick = { activateTab(index) },
                            ) {
                                val contentColor = LocalContentColor.current
                                CompositionLocalProvider(
                                    LocalContentColor provides if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        contentColor
                                    },
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = stringResource(tab.labelRes),
                                        modifier = Modifier.size(24.dp),
                                    )
                                    Text(
                                        text = stringResource(tab.labelRes),
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                    )
                                }
                            }
                        }
                    }
                }
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .then(
                        if (backdrop != null) Modifier.layerBackdrop(backdrop)
                        else Modifier
                    ),
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Destination.Sound.route,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    composable(Destination.Sound.route) {
                        SoundScreen(viewModel = soundViewModel)
                    }
                    composable(Destination.Equalizer.route) {
                        EqScreen(viewModel = eqViewModel)
                    }
                    composable(Destination.Spatial.route) {
                        SpatialScreen(viewModel = spatialViewModel)
                    }
                    composable(Destination.Volume.route) {
                        VolumeScreen(viewModel = volumeViewModel)
                    }
                }
            }
        }
    }
}
