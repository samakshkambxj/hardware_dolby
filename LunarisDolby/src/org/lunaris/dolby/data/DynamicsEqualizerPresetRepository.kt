/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class DynamicsPresetData(
    val name: String,
    val preampDb: Float,
    val bandGains: List<Float>,
    val mbcEnabled: Boolean,
    val mbcThresholds: List<Float>,
    val mbcRatios: List<Float>,
    val mbcAttacks: List<Float>,
    val mbcReleases: List<Float>,
    val limiterEnabled: Boolean,
    val limiterThreshold: Float,
    val limiterRatio: Float,
    val limiterRelease: Float,
    val limiterPostGain: Float
)

class DynamicsEqualizerPresetRepository(context: Context) {

    private val prefs = context.getSharedPreferences("dynamics_equalizer_presets", Context.MODE_PRIVATE)

    fun listPresetNames(): List<String> {
        val raw = prefs.getString(KEY_PRESET_NAMES, "[]") ?: "[]"
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { arr.getString(it) }
    }

    fun savePreset(preset: DynamicsPresetData) {
        val json = JSONObject().apply {
            put("name", preset.name)
            put("preampDb", preset.preampDb)
            put("bandGains", JSONArray(preset.bandGains))
            put("mbcEnabled", preset.mbcEnabled)
            put("mbcThresholds", JSONArray(preset.mbcThresholds))
            put("mbcRatios", JSONArray(preset.mbcRatios))
            put("mbcAttacks", JSONArray(preset.mbcAttacks))
            put("mbcReleases", JSONArray(preset.mbcReleases))
            put("limiterEnabled", preset.limiterEnabled)
            put("limiterThreshold", preset.limiterThreshold)
            put("limiterRatio", preset.limiterRatio)
            put("limiterRelease", preset.limiterRelease)
            put("limiterPostGain", preset.limiterPostGain)
        }

        prefs.edit().putString(presetKey(preset.name), json.toString()).apply()

        val names = listPresetNames().toMutableSet()
        names.add(preset.name)
        prefs.edit().putString(KEY_PRESET_NAMES, JSONArray(names.toList()).toString()).apply()
    }

    fun loadPreset(name: String): DynamicsPresetData? {
        val raw = prefs.getString(presetKey(name), null) ?: return null
        val json = JSONObject(raw)

        fun floatList(key: String): List<Float> {
            val arr = json.optJSONArray(key) ?: JSONArray()
            return (0 until arr.length()).map { arr.getDouble(it).toFloat() }
        }

        return DynamicsPresetData(
            name = json.getString("name"),
            preampDb = json.getDouble("preampDb").toFloat(),
            bandGains = floatList("bandGains"),
            mbcEnabled = json.getBoolean("mbcEnabled"),
            mbcThresholds = floatList("mbcThresholds"),
            mbcRatios = floatList("mbcRatios"),
            mbcAttacks = floatList("mbcAttacks"),
            mbcReleases = floatList("mbcReleases"),
            limiterEnabled = json.getBoolean("limiterEnabled"),
            limiterThreshold = json.getDouble("limiterThreshold").toFloat(),
            limiterRatio = json.getDouble("limiterRatio").toFloat(),
            limiterRelease = json.getDouble("limiterRelease").toFloat(),
            limiterPostGain = json.getDouble("limiterPostGain").toFloat()
        )
    }

    fun deletePreset(name: String) {
        prefs.edit().remove(presetKey(name)).apply()
        val names = listPresetNames().toMutableList()
        names.remove(name)
        prefs.edit().putString(KEY_PRESET_NAMES, JSONArray(names).toString()).apply()
    }

    private fun presetKey(name: String) = "preset_$name"

    companion object {
        private const val KEY_PRESET_NAMES = "preset_names"
    }
}
