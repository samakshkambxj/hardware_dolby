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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.daybreak.dolby.R
import com.daybreak.dolby.ui.customize.BarDesign
import com.daybreak.dolby.ui.customize.BarStyle
import com.daybreak.dolby.ui.customize.CustomizeScreen
import com.daybreak.dolby.ui.customize.rememberCustomize
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
    // Destinations change only through the bar below, so local selection
    // state is always correct — no backstack observation needed.
    var selectedIndex by remember { mutableIntStateOf(0) }
    val customize by rememberCustomize()

    // Backdrop root: tab content records into this layer; the pill and
    // any overlay sample it. The bar lives outside the recorded box.
    val useBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val backdrop = if (useBlur) {
        rememberLayerBackdrop()
    } else {
        null
    }

    CompositionLocalProvider(LocalGlassBackdrop provides backdrop) {
        Scaffold(
            bottomBar = {
                if (customize.barDesign == BarDesign.STOCK) {
                    NavigationBar {
                        TABS.forEachIndexed { index, tab ->
                            val selected = index == selectedIndex
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    selectedIndex = index
                                    navController.navigate(tab.route) {
                                        popUpTo(Destination.Sound.route)
                                        launchSingleTop = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = stringResource(tab.labelRes),
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                            )
                        }
                    }
                } else {
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
                                selectedIndex = index
                                navController.navigate(TABS[index].route) {
                                    popUpTo(Destination.Sound.route)
                                    launchSingleTop = true
                                }
                            },
                            tabsCount = TABS.size,
                            backdrop = backdrop,
                            rainbow = customize.rainbow,
                            intensity = customize.glassIntensity,
                            veilAlpha = when (customize.barStyle) {
                                BarStyle.TRANSPARENT -> 0.25f
                                BarStyle.FROSTED -> customize.navTint
                                BarStyle.FILLED -> 1f
                                BarStyle.TONAL -> 1f
                                BarStyle.OUTLINED -> 0.15f
                            },
                            blurRadius = customize.navBlurRadius,
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
                        SoundScreen(
                            viewModel = soundViewModel,
                            onCustomize = {
                                navController.navigate(Destination.Customize.route)
                            },
                        )
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
                    composable(Destination.Customize.route) {
                        CustomizeScreen()
                    }
                }
            }
        }
    }
}
