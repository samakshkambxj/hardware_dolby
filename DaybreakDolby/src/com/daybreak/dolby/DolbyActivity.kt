/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daybreak.dolby

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.daybreak.dolby.ui.equalizer.EqViewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.daybreak.dolby.ui.customize.rememberCustomize
import com.daybreak.dolby.ui.nav.DaybreakApp
import com.daybreak.dolby.ui.sound.SoundViewModel
import com.daybreak.dolby.ui.spatial.SpatialViewModel
import com.daybreak.dolby.ui.theme.DaybreakTheme
import com.daybreak.dolby.ui.volume.VolumeViewModel

class DolbyActivity : ComponentActivity() {

    private val soundViewModel: SoundViewModel by viewModels()
    private val eqViewModel: EqViewModel by viewModels()
    private val spatialViewModel: SpatialViewModel by viewModels()
    private val volumeViewModel: VolumeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val customize by rememberCustomize()
            DaybreakTheme(
                dynamicColor = customize.dynamicColor,
                accent = customize.accent,
                amoled = customize.amoled,
            ) {
                DaybreakApp(
                    soundViewModel = soundViewModel,
                    eqViewModel = eqViewModel,
                    spatialViewModel = spatialViewModel,
                    volumeViewModel = volumeViewModel,
                )
            }
        }
    }
}
