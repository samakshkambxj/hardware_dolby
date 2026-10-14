/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daybreak.dolby

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.daybreak.dolby.ui.nav.DaybreakApp
import com.daybreak.dolby.ui.sound.SoundViewModel
import com.daybreak.dolby.ui.theme.DaybreakTheme

class DolbyActivity : ComponentActivity() {

    private val soundViewModel: SoundViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DaybreakTheme {
                DaybreakApp(soundViewModel = soundViewModel)
            }
        }
    }
}
