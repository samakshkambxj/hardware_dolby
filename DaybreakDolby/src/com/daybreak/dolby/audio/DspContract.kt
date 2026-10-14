/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daybreak.dolby.audio

import java.util.UUID

/**
 * Wire contract of the DAX effect HAL blob.
 *
 * Effect type, control parameter IDs, and per-profile DSP parameter IDs.
 * Values must match the blob exactly; everything above this layer works
 * with [DsParam] only.
 */
object DspContract {
    /** Effect type UUID of the DAX3 DAP effect. */
    val EFFECT_TYPE_DAP: UUID =
        UUID.fromString("9d4921da-8225-4f29-aefa-39537a04bcaa")

    const val PARAM_ENABLE = 0
    const val PARAM_CPDP_VALUES = 5
    const val PARAM_PROFILE = 0xA000000
    const val PARAM_SET_PROFILE_PARAMETER = 0x1000000
    const val PARAM_GET_PROFILE_PARAMETER = 0x1000005
    const val PARAM_RESET_PROFILE_SETTINGS = 0xC000000

    const val EFFECT_PRIORITY = 100

    /** Profiles in HAL id order: dynamic, movie, music, spatial. */
    val PROFILES = intArrayOf(0, 1, 2, 4)

    /** Backend graphic-EQ band count. */
    const val GEQ_BAND_COUNT = 20
}

/** Per-profile DSP parameters: HAL id + value length. */
enum class DsParam(val id: Int, val length: Int = 1) {
    HEADPHONE_VIRTUALIZER(101),
    SPEAKER_VIRTUALIZER(102),
    VOLUME_LEVELER_ENABLE(103),
    IEQ_PRESET(104),
    DIALOGUE_ENHANCER_ENABLE(105),
    DIALOGUE_ENHANCER_AMOUNT(108),
    GEQ_BAND_GAINS(110, 20),
    BASS_ENHANCER_ENABLE(111),
    STEREO_WIDENING_AMOUNT(113);

    override fun toString(): String = "${name}(${id})"
}
