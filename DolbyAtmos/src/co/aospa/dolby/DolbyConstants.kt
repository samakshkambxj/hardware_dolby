/*
 * Copyright (C) 2023-24 Paranoid Android
 * Copyright (C) 2026 samakshkambxj
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby

import android.util.Log

object DolbyConstants {

    const val TAG = "DolbyAtmos"

    /** DAP parameters exposed by the DAX3 effect. */
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

        override fun toString(): String = "${name}($id)"
    }

    // Default-preference keys.
    const val PREF_ENABLE = "dolby_enable"
    const val PREF_PROFILE = "dolby_profile"
    const val PREF_PRESET = "dolby_preset"
    const val PREF_IEQ = "dolby_ieq"
    const val PREF_HP_VIRTUALIZER = "dolby_virtualizer"
    const val PREF_SPK_VIRTUALIZER = "dolby_spk_virtualizer"
    const val PREF_STEREO = "dolby_stereo"
    const val PREF_DIALOGUE = "dolby_dialogue"
    const val PREF_BASS = "dolby_bass"
    const val PREF_VOLUME = "dolby_volume"
    const val PREF_RESET = "dolby_reset"
    const val PREF_BASS_LEVEL = "dolby_bass_level"
    const val PREF_BASS_CURVE = "dolby_bass_curve"
    const val PREF_MID = "dolby_mid"
    const val PREF_MID_LEVEL = "dolby_mid_level"
    const val PREF_TREBLE = "dolby_treble"
    const val PREF_TREBLE_LEVEL = "dolby_treble_level"

    const val PREF_FILE_PRESETS = "presets"

    /** Keys that live in the per-profile preference file. */
    val PROFILE_SPECIFIC_PREFS = setOf(
        PREF_PRESET,
        PREF_IEQ,
        PREF_HP_VIRTUALIZER,
        PREF_SPK_VIRTUALIZER,
        PREF_STEREO,
        PREF_DIALOGUE,
        PREF_BASS,
        PREF_VOLUME,
        PREF_BASS_LEVEL,
        PREF_BASS_CURVE,
        PREF_MID,
        PREF_MID_LEVEL,
        PREF_TREBLE,
        PREF_TREBLE_LEVEL,
    )

    /** The DAP stores 20 bands; the app exposes the 10 even ones. */
    const val GEQ_BAND_COUNT = 20
    const val DISPLAY_BAND_COUNT = 10
    const val GAIN_LIMIT = 150

    /** UI frequencies for the 10 visible bands. */
    val DISPLAY_FREQUENCIES = listOf(32, 64, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)

    val DISPLAY_INDICES = listOf(0, 2, 4, 6, 8, 10, 12, 14, 16, 18)

    fun frequencyLabel(frequency: Int): String =
        if (frequency >= 1000) "${frequency / 1000}k" else frequency.toString()

    fun dlog(tag: String, msg: String) {
        if (Log.isLoggable(TAG, Log.DEBUG) || Log.isLoggable(tag, Log.DEBUG)) {
            Log.d("$TAG-$tag", msg)
        }
    }
}
