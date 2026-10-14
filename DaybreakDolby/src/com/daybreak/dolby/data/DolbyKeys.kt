/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daybreak.dolby.data

/**
 * Every persisted key in one place. No raw strings anywhere else.
 * Files: one global, one per profile, one for user presets.
 */
object DolbyKeys {
    const val FILE_GLOBAL = "daybreak"
    const val FILE_PROFILE_PREFIX = "daybreak_profile_"
    const val FILE_PRESETS = "daybreak_presets"

    const val ENABLED = "enabled"
    const val PROFILE = "profile"

    const val PRESET = "preset"
    const val IEQ = "ieq"
    const val HP_VIRTUALIZER = "hp_virtualizer"
    const val SPK_VIRTUALIZER = "spk_virtualizer"
    const val STEREO = "stereo"
    const val DIALOGUE = "dialogue"
    const val BASS = "bass"
    const val LEVELER = "leveler"

    fun profileFile(profile: Int): String = "$FILE_PROFILE_PREFIX$profile"
}
