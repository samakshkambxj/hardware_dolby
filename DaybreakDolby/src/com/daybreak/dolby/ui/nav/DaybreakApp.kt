/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daybreak.dolby.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.daybreak.dolby.ui.sound.SoundScreen
import com.daybreak.dolby.ui.sound.SoundViewModel

@Composable
fun DaybreakApp(soundViewModel: SoundViewModel) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Destination.Sound.route,
    ) {
        composable(Destination.Sound.route) {
            SoundScreen(viewModel = soundViewModel)
        }
    }
}
