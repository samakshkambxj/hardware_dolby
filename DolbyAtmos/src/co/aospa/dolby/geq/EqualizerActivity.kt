/*
 * Copyright (C) 2024-26 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.geq

import co.aospa.dolby.ui.DolbyBaseActivity
import co.aospa.dolby.ui.DolbyTab

/**
 * Standalone entry point kept for the historical "Graphic equalizer" intent. It opens the same
 * Compose host with the Equalizer tab selected.
 */
class EqualizerActivity : DolbyBaseActivity() {

    override val initialTab = DolbyTab.Equalizer
}
