/*
 * Copyright (C) 2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists the live dynamics-processing state (master switch, preamp, band
 * gains, MBC and limiter settings) so it survives app/process restarts.
 * This is separate from [DynamicsEqualizerPresetRepository], which only
 * stores explicitly saved named presets.
 */
data class DynamicsStateData(
    val enabled: Boolean = false,
    val preampDb: Float = 0f,
    val bandGains: List<Float> = emptyList(),
    val mbcEnabled: Boolean = false,
    val mbcThresholds: List<Float> = emptyList(),
    val mbcRatios: List<Float> = emptyList(),
    val mbcAttacks: List<Float> = emptyList(),
    val mbcReleases: List<Float> = emptyList(),
    val limiterEnabled: Boolean = false,
    val limiterThreshold: Float = -1f,
    val limiterRatio: Float = 10f,
    val limiterRelease: Float = 50f,
    val limiterPostGain: Float = 0f
)

class DynamicsStateStore(context: Context) {

    private val prefs =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): DynamicsStateData? {
        val raw = prefs.getString(KEY_STATE, null) ?: return null
        return try {
            val json = JSONObject(raw)

            fun floatList(key: String): List<Float> {
                val arr = json.optJSONArray(key) ?: JSONArray()
                return (0 until arr.length()).map { arr.getDouble(it).toFloat() }
            }

            DynamicsStateData(
                enabled = json.optBoolean("enabled", false),
                preampDb = json.optDouble("preampDb", 0.0).toFloat(),
                bandGains = floatList("bandGains"),
                mbcEnabled = json.optBoolean("mbcEnabled", false),
                mbcThresholds = floatList("mbcThresholds"),
                mbcRatios = floatList("mbcRatios"),
                mbcAttacks = floatList("mbcAttacks"),
                mbcReleases = floatList("mbcReleases"),
                limiterEnabled = json.optBoolean("limiterEnabled", false),
                limiterThreshold = json.optDouble("limiterThreshold", -1.0).toFloat(),
                limiterRatio = json.optDouble("limiterRatio", 10.0).toFloat(),
                limiterRelease = json.optDouble("limiterRelease", 50.0).toFloat(),
                limiterPostGain = json.optDouble("limiterPostGain", 0.0).toFloat()
            )
        } catch (e: Exception) {
            null
        }
    }

    fun save(state: DynamicsStateData) {
        fun jsonArray(values: List<Float>): JSONArray {
            val arr = JSONArray()
            values.forEach { arr.put(it.toDouble()) }
            return arr
        }

        val json = JSONObject().apply {
            put("enabled", state.enabled)
            put("preampDb", state.preampDb.toDouble())
            put("bandGains", jsonArray(state.bandGains))
            put("mbcEnabled", state.mbcEnabled)
            put("mbcThresholds", jsonArray(state.mbcThresholds))
            put("mbcRatios", jsonArray(state.mbcRatios))
            put("mbcAttacks", jsonArray(state.mbcAttacks))
            put("mbcReleases", jsonArray(state.mbcReleases))
            put("limiterEnabled", state.limiterEnabled)
            put("limiterThreshold", state.limiterThreshold.toDouble())
            put("limiterRatio", state.limiterRatio.toDouble())
            put("limiterRelease", state.limiterRelease.toDouble())
            put("limiterPostGain", state.limiterPostGain.toDouble())
        }
        prefs.edit().putString(KEY_STATE, json.toString()).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_STATE).apply()
    }

    companion object {
        private const val PREFS_NAME = "dynamics_state"
        private const val KEY_STATE = "state"
    }
}
