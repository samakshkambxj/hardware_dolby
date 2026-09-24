/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby

import android.util.Log

object DolbyConstants {

    const val TAG = "Dolby"
    
    const val PREF_ENABLE = "dolby_enable"
    const val PREF_PROFILE = "dolby_profile"
    const val PREF_PRESET = "dolby_preset"
    const val PREF_IEQ = "dolby_ieq"
    const val PREF_HP_VIRTUALIZER = "dolby_virtualizer"
    const val PREF_SPK_VIRTUALIZER = "dolby_spk_virtualizer"
    const val PREF_STEREO_WIDENING = "dolby_stereo_widening"
    const val PREF_DIALOGUE = "dolby_dialogue_enabled"
    const val PREF_DIALOGUE_AMOUNT = "dolby_dialogue_amount"
    const val PREF_BASS = "dolby_bass"
    const val PREF_BASS_LEVEL = "dolby_bass_level"
    const val PREF_BASS_CURVE = "dolby_bass_curve"
    const val PREF_MID = "dolby_mid"
    const val PREF_MID_LEVEL = "dolby_mid_level"
    const val PREF_TREBLE = "dolby_treble"
    const val PREF_TREBLE_LEVEL = "dolby_treble_level"
    const val PREF_VOLUME = "dolby_volume"
    const val PREF_VOLUME_AMOUNT = "dolby_volume_amount"
    const val PREF_PRESETS_MIGRATED = "presets_migrated"
    const val PREF_BAND_MODE = "dolby_band_mode"
    const val PREF_DEVICE_STATE_MEMORY = "device_state_memory_enabled"
    
    const val PREF_FILE_PRESETS = "presets"
    const val PREF_FILE_SCENES = "dolby_scenes"
    const val PREF_SLEEP_TIMER_DEADLINE = "sleep_timer_deadline"
    const val PREF_BALANCE = "dolby_balance"

    enum class DsParam(val id: Int, val length: Int = 1) {
        HEADPHONE_VIRTUALIZER(101),
        SPEAKER_VIRTUALIZER(102),
        VOLUME_LEVELER_ENABLE(103),
        IEQ_PRESET(104),
        DIALOGUE_ENHANCER_ENABLE(105),
        DIALOGUE_ENHANCER_AMOUNT(108),
        GEQ_BAND_GAINS(110, 20),
        BASS_ENHANCER_ENABLE(111),
        STEREO_WIDENING_AMOUNT(113),
        // Seen in Xiaomi DAP trees; codec range 0-10 (Dolby default 7).
        VOLUME_LEVELER_AMOUNT(116);

        override fun toString(): String = "${name}(${id})"
    }

    /**
     * Gap IDs around the known 101-116 params with no public semantics.
     * The OEM daxService and all community DAP trees only use the [DsParam]
     * set above; these IDs are exposed generically in the Tuning Lab,
     * gated per-device by a live HAL probe (unsupported IDs are hidden).
     * Values are kept in a conservative 0-16 range since the HAL-side
     * scaling of unknown IDs is unverified — listen after changing.
     *
     * When none of these answers, the repository falls back to sweeping
     * [LAB_SWEEP_RANGE] (minus known IDs) so DAP revisions that keep
     * their extras elsewhere still surface sliders.
     */
    val LAB_DAP_PARAM_IDS = listOf(106, 107, 109, 112, 114, 115, 117, 118)
    const val LAB_PARAM_MIN = 0
    const val LAB_PARAM_MAX = 16
    fun labParamPref(paramId: Int) = "dolby_lab_$paramId"

    /**
     * Fallback sweep range when none of [LAB_DAP_PARAM_IDS] answers (some
     * DAP revisions put their extras at other IDs). Same 100–130 window
     * the DAP probe debug screen sweeps. Known [DsParam] IDs are excluded
     * — they have real UI elsewhere and must never show up as raw sliders.
     */
    val LAB_SWEEP_RANGE = 100..130
    val LAB_SWEEP_EXCLUDE: Set<Int> by lazy { DsParam.entries.map { it.id }.toSet() }

    /**
     * Tuning Lab IDs promoted to the experimental Reverb & Height card.
     * Candidates only — no public semantics exist for gap IDs, so if the
     * DAP probe names a different live ID on your HAL, remap here and the
     * card follows. IDs the HAL rejects are hidden automatically.
     */
    const val REVERB_PARAM_ID = 114
    const val HEIGHT_PARAM_ID = 115

    fun dlog(tag: String, msg: String) {
        if (Log.isLoggable(TAG, Log.DEBUG) || Log.isLoggable(tag, Log.DEBUG)) {
            Log.d("$TAG-$tag", msg)
        }
    }
}
