/*
 * Copyright (C) 2024 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.geq.data

data class BandGain(val band: Int, var gain: Int = 0) {
    /** Lunaris calls this field `frequency`; keep both names for the ported UI. */
    val frequency: Int
        get() = band
}
