/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.data

import android.content.Context
import org.json.JSONObject

/**
 * Persists the framework-enhancements state (bass boost, virtualizer,
 * reverb, loudness) so it survives app/process restarts. Separate from
 * [DynamicsStateStore] and the named dynamics presets on purpose: those
 * predate FX and their JSON shape stays untouched.
 */
data class EnhancementsStateData(
    val bassEnabled: Boolean = false,
    val bassStrengthPercent: Int = 0,
    val virtualizerEnabled: Boolean = false,
    val virtualizerStrengthPercent: Int = 0,
    val reverbEnabled: Boolean = false,
    val reverbPreset: Int = 0,
    val loudnessEnabled: Boolean = false,
    val loudnessGainHalfDb: Int = 0
)

class EnhancementsStateStore(context: Context) {

    private val prefs =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): EnhancementsStateData? {
        val raw = prefs.getString(KEY_STATE, null) ?: return null
        return try {
            val json = JSONObject(raw)
            EnhancementsStateData(
                bassEnabled = json.optBoolean("bassEnabled", false),
                bassStrengthPercent = json.optInt("bassStrengthPercent", 0),
                virtualizerEnabled = json.optBoolean("virtualizerEnabled", false),
                virtualizerStrengthPercent = json.optInt("virtualizerStrengthPercent", 0),
                reverbEnabled = json.optBoolean("reverbEnabled", false),
                reverbPreset = json.optInt("reverbPreset", 0),
                loudnessEnabled = json.optBoolean("loudnessEnabled", false),
                loudnessGainHalfDb = json.optInt("loudnessGainHalfDb", 0)
            )
        } catch (e: Exception) {
            null
        }
    }

    fun save(state: EnhancementsStateData) {
        val json = JSONObject().apply {
            put("bassEnabled", state.bassEnabled)
            put("bassStrengthPercent", state.bassStrengthPercent)
            put("virtualizerEnabled", state.virtualizerEnabled)
            put("virtualizerStrengthPercent", state.virtualizerStrengthPercent)
            put("reverbEnabled", state.reverbEnabled)
            put("reverbPreset", state.reverbPreset)
            put("loudnessEnabled", state.loudnessEnabled)
            put("loudnessGainHalfDb", state.loudnessGainHalfDb)
        }
        prefs.edit().putString(KEY_STATE, json.toString()).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_STATE).apply()
    }

    companion object {
        private const val PREFS_NAME = "enhancements_state"
        private const val KEY_STATE = "state"
    }
}
