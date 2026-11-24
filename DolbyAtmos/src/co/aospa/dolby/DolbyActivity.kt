/*
 * Copyright (C) 2023-24 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import co.aospa.dolby.ui.DolbyApp
import co.aospa.dolby.ui.DolbyViewModel
import co.aospa.dolby.ui.theme.DolbyTheme

class DolbyActivity : ComponentActivity() {

    private lateinit var viewModel: DolbyViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = DolbyViewModel(this)
        setContent { DolbyTheme { DolbyApp(viewModel) } }
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) viewModel.refresh()
    }
}
