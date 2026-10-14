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
}
