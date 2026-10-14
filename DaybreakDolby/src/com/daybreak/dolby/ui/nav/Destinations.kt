/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daybreak.dolby.ui.nav

/** Every destination, exhaustive. No raw route strings outside this file. */
sealed interface Destination {
    val route: String

    data object Sound : Destination {
        override val route = "sound"
    }

    data object Equalizer : Destination {
        override val route = "equalizer"
    }

    data object Spatial : Destination {
        override val route = "spatial"
    }

    data object Volume : Destination {
        override val route = "volume"
    }
}
