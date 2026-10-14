/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daybreak.dolby.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.daybreak.dolby.R
import com.daybreak.dolby.ui.equalizer.EqScreen
import com.daybreak.dolby.ui.equalizer.EqViewModel
import com.daybreak.dolby.ui.sound.SoundScreen
import com.daybreak.dolby.ui.sound.SoundViewModel

private data class Tab(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
)

private val TABS = listOf(
    Tab(Destination.Sound.route, R.string.tab_sound, Icons.Filled.Tune),
    Tab(Destination.Equalizer.route, R.string.tab_equalizer, Icons.Filled.GraphicEq),
)

@Composable
fun DaybreakApp(
    soundViewModel: SoundViewModel,
    eqViewModel: EqViewModel,
) {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            NavigationBar {
                val entry by navController.currentBackStackEntryAsState()
                TABS.forEach { tab ->
                    val selected = entry?.destination?.hierarchy?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
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
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Sound.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Destination.Sound.route) {
                SoundScreen(viewModel = soundViewModel)
            }
            composable(Destination.Equalizer.route) {
                EqScreen(viewModel = eqViewModel)
            }
        }
    }
}
