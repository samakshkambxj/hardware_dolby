/*
 * Copyright (C) 2024 Paranoid Android
 * Copyright (C) 2026 samakshkambxj
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.data

import android.content.Context
import android.content.SharedPreferences
import co.aospa.dolby.DolbyConstants
import co.aospa.dolby.DolbyConstants.dlog
import co.aospa.dolby.DolbyController
import co.aospa.dolby.R

/**
 * Built-in and user equalizer presets, stored per Dolby profile.
 *
 * The DAP works on 20 bands; built-in arrays may be authored with either 10 or
 * 20 values and are expanded to 20 on load.
 */
class EqualizerRepository(context: Context) {

    private val appContext = context.applicationContext
    private val controller = DolbyController.getInstance(appContext)
    private val presetsPrefs: SharedPreferences =
        appContext.getSharedPreferences(DolbyConstants.PREF_FILE_PRESETS, Context.MODE_PRIVATE)

    val builtInPresets: List<Preset> by lazy {
        val names = appContext.resources.getStringArray(R.array.dolby_preset_entries)
        val values = appContext.resources.getStringArray(R.array.dolby_preset_values)
        names.indices.mapNotNull { index ->
            val gains = parseGains(values.getOrNull(index) ?: return@mapNotNull null)
            Preset(name = names[index], gains = gains)
        }
    }

    val defaultPreset: Preset get() = builtInPresets.first()

    fun userPresets(): List<Preset> =
        presetsPrefs.all.mapNotNull { (name, value) ->
            val gains = parseGains(value as? String ?: return@mapNotNull null)
            Preset(name = name, gains = gains, isUserDefined = true)
        }.sortedBy { it.name.lowercase() }

    fun allPresets(): List<Preset> = userPresets() + builtInPresets

    fun currentGains(profile: Int): List<Int> =
        controller.getGains(profile).toList()

    fun displayGains(gains: List<Int>): List<Int> =
        DolbyConstants.DISPLAY_INDICES.map { gains.getOrElse(it) { 0 } }

    fun applyDisplayGains(profile: Int, display: List<Int>) {
        controller.setPreset(profile, controller.displayToBackend(display.toIntArray()))
    }

    fun applyPreset(profile: Int, preset: Preset) {
        controller.setPreset(profile, preset.gains.toIntArray())
    }

    fun activePreset(profile: Int, presets: List<Preset> = allPresets()): Preset {
        val gains = currentGains(profile)
        return presets.firstOrNull { it.matches(gains) }
            ?: Preset(
                name = appContext.getString(R.string.dolby_geq_preset_custom),
                gains = gains,
            )
    }

    fun savePreset(name: String, gains: List<Int>) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        presetsPrefs.edit().putString(trimmed, encodeGains(gains)).apply()
        dlog(TAG, "saved preset $trimmed")
    }

    fun deletePreset(name: String) {
        presetsPrefs.edit().remove(name).apply()
    }

    fun renamePreset(oldName: String, newName: String, gains: List<Int>) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        presetsPrefs.edit().remove(oldName).putString(trimmed, encodeGains(gains)).apply()
    }

    fun nameExists(name: String, exclude: String? = null): Boolean {
        val candidate = name.trim()
        return allPresets().any {
            it.name.equals(candidate, ignoreCase = true) &&
                !it.name.equals(exclude, ignoreCase = true)
        }
    }

    private fun parseGains(raw: String): List<Int> {
        val parsed = raw.split(",").mapNotNull { it.trim().toIntOrNull() }
        val normalized = when (parsed.size) {
            DolbyConstants.GEQ_BAND_COUNT -> parsed
            DolbyConstants.DISPLAY_BAND_COUNT ->
                controller.displayToBackend(parsed.toIntArray()).toList()
            else -> List(DolbyConstants.GEQ_BAND_COUNT) { 0 }
        }
        return normalized.map {
            it.coerceIn(-DolbyConstants.GAIN_LIMIT, DolbyConstants.GAIN_LIMIT)
        }
    }

    private fun encodeGains(gains: List<Int>): String =
        List(DolbyConstants.GEQ_BAND_COUNT) { index ->
            (gains.getOrNull(index) ?: 0).coerceIn(
                -DolbyConstants.GAIN_LIMIT,
                DolbyConstants.GAIN_LIMIT,
            )
        }.joinToString(",")

    private companion object {
        const val TAG = "EqualizerRepository"
    }
}
