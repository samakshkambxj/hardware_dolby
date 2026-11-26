/*
 * Copyright (C) 2024-2026 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.geq.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import co.aospa.dolby.DolbyConstants.Companion.PREF_PRESET
import co.aospa.dolby.DolbyConstants.Companion.dlog
import co.aospa.dolby.DolbyController
import co.aospa.dolby.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

class EqualizerRepository(private val context: Context) {

    private val dolbyController by lazy { DolbyController.getInstance(context) }

    // Preset is saved as a string of comma separated gains in SharedPreferences
    // and is unique to each profile ID. The DAP always has 20 values.
    private var profile = runCatching { dolbyController.profile }.getOrDefault(0)
    private var profileSharedPrefs =
        context.getSharedPreferences("profile_$profile", Context.MODE_PRIVATE)

    /** UI-only settings that are not tied to a Dolby profile. */
    private val settingsSharedPrefs by lazy {
        context.getSharedPreferences("equalizer_settings", Context.MODE_PRIVATE)
    }

    /**
     * Follows the active Dolby profile. The Home tab can change the profile while this repository
     * is alive, so the backing preference file has to be re-resolved instead of captured once.
     *
     * Re-resolving is unconditional because a profile reset deletes the file while keeping the
     * same profile id; an early return would leave this repository pointing at the deleted file.
     */
    fun updateProfile() {
        // Reading the profile goes through the effect, which can lose control while the activity
        // is in the background. Keep the last known profile instead of crashing the UI.
        val newProfile = runCatching { dolbyController.profile }.getOrDefault(profile)
        if (profile != newProfile) dlog(TAG, "updateProfile: $profile -> $newProfile")
        profile = newProfile
        profileSharedPrefs =
            context.getSharedPreferences("profile_$newProfile", Context.MODE_PRIVATE)
    }

    private val presetsSharedPrefs by lazy {
        context.getSharedPreferences("presets", Context.MODE_PRIVATE)
    }

    /** Band mode each user preset was created in, keyed by preset name. */
    private val presetModesSharedPrefs by lazy {
        context.getSharedPreferences("preset_band_modes", Context.MODE_PRIVATE)
    }

    val builtInPresets: List<Preset> by lazy {
        val names = context.resources.getStringArray(R.array.dolby_preset_entries)
        val presets = context.resources.getStringArray(R.array.dolby_preset_values)
        List(names.size) { index ->
            Preset(name = names[index], bandGains = deserializeGains(presets[index]))
        }
    }

    val defaultPreset by lazy { builtInPresets[0] } // Flat

    // User defined presets are stored in a SharedPreferences as
    // key - preset name
    // value - comma separated string of 20 gains
    val userPresets: Flow<List<Preset>> = callbackFlow {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
                dlog(TAG, "presetsSharedPrefs changed")
                trySend(
                    presetsSharedPrefs.all.mapNotNull { (key, value) ->
                        val gains = value as? String ?: return@mapNotNull null
                        Preset(
                            name = key,
                            bandGains = deserializeGains(gains),
                            isUserDefined = true,
                            bandMode =
                                BandMode.fromValue(
                                    presetModesSharedPrefs.getInt(
                                        key,
                                        BandMode.TEN_BAND.bandCount,
                                    )
                                ),
                        )
                    }
                )
            }

        presetsSharedPrefs.registerOnSharedPreferenceChangeListener(listener)
        dlog(TAG, "presetsSharedPrefs registered listener")
        // trigger an initial emission
        listener.onSharedPreferenceChanged(presetsSharedPrefs, null)

        awaitClose {
            presetsSharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
            dlog(TAG, "presetsSharedPrefs unregistered listener")
        }
    }

    fun getBandMode(): BandMode =
        BandMode.fromValue(
            settingsSharedPrefs.getInt(KEY_BAND_MODE, BandMode.TEN_BAND.bandCount)
        )

    fun setBandMode(mode: BandMode) {
        settingsSharedPrefs.edit().putInt(KEY_BAND_MODE, mode.bandCount).apply()
    }

    suspend fun getBandGains(): List<BandGain> =
        withContext(Dispatchers.IO) {
            // Read the saved gains first. The effect is only a fallback for profiles that have
            // never been touched, and querying it can fail while the effect has no control.
            val stored = profileSharedPrefs.getString(PREF_PRESET, null)
            val gains =
                if (stored.isNullOrEmpty()) {
                    runCatching { dolbyController.getPreset() }
                        .onFailure { Log.e(TAG, "Failed to read preset from effect", it) }
                        .getOrNull()
                } else {
                    stored
                }
            return@withContext if (gains.isNullOrEmpty()) {
                    defaultPreset.bandGains
                } else {
                    deserializeGains(gains)
                }
                .also { dlog(TAG, "getBandGains: $it") }
        }

    suspend fun setBandGains(bandGains: List<BandGain>) =
        withContext(Dispatchers.IO) {
            dlog(TAG, "setBandGains($bandGains)")
            val gains = serializeGains(bandGains)
            // Keep the preference write even if the effect is temporarily unavailable, so the
            // values are restored on the next boot instead of being silently dropped.
            runCatching { dolbyController.setPreset(gains) }
                .onFailure { Log.e(TAG, "Failed to apply preset to effect", it) }
            profileSharedPrefs.edit().putString(PREF_PRESET, gains).apply()
        }

    suspend fun addPreset(preset: Preset) =
        withContext(Dispatchers.IO) {
            dlog(TAG, "addPreset($preset)")
            // Write the mode first: the presets listener reads it as soon as the gains are stored.
            presetModesSharedPrefs
                .edit()
                .putInt(preset.name, preset.bandMode.bandCount)
                .apply()
            presetsSharedPrefs
                .edit()
                .putString(preset.name, serializeGains(preset.bandGains))
                .apply()
        }

    suspend fun removePreset(preset: Preset) =
        withContext(Dispatchers.IO) {
            dlog(TAG, "removePreset($preset)")
            presetsSharedPrefs.edit().remove(preset.name).apply()
            presetModesSharedPrefs.edit().remove(preset.name).apply()
        }

    private companion object {
        const val TAG = "EqRepository"
        const val KEY_BAND_MODE = "band_mode"
        const val GAIN_MIN = -100
        const val GAIN_MAX = 100

        fun deserializeGains(bandGains: String): List<BandGain> {
            val values = bandGains.split(",").mapNotNull { it.trim().toIntOrNull() }
            val twenty = values.toTwentyBandGains()
            return BandMode.TWENTY_BAND.frequencies.mapIndexed { index, frequency ->
                BandGain(
                    band = frequency,
                    gain = twenty.getOrElse(index) { 0 }.coerceIn(GAIN_MIN, GAIN_MAX),
                )
            }
        }

        fun serializeGains(bandGains: List<BandGain>): String {
            val values = bandGains.map { it.gain.coerceIn(GAIN_MIN, GAIN_MAX) }
            return values.toTwentyBandGains().joinToString(",")
        }

        fun List<Int>.toTwentyBandGains(): List<Int> =
            when (size) {
                20 -> this
                15 -> fifteenToTwentyBandGains()
                10 -> tenToTwentyBandGains()
                else -> {
                    Log.e(TAG, "Preset must have 10, 15 or 20 elements, has $size!")
                    List(20) { 0 }
                }
            }

        // The 10-band UI only edits every other backend value; the odd ones are averaged.
        fun List<Int>.tenToTwentyBandGains() =
            List(20) { index ->
                if (index % 2 == 1 && index < 19) {
                    (this[(index - 1) / 2] + this[(index + 1) / 2]) / 2
                } else {
                    this[index / 2]
                }
            }

        // The 15-band UI uses a fixed subset; the remaining slots are linearly interpolated.
        fun List<Int>.fifteenToTwentyBandGains(): List<Int> {
            val indices = BandMode.FIFTEEN_BAND.indices
            val result = MutableList(20) { 0 }
            indices.forEachIndexed { index, target ->
                result[target] = this.getOrElse(index) { 0 }
            }
            (0 until 20).filter { it !in indices }.forEach { index ->
                val previous = indices.filter { it < index }.maxOrNull() ?: 0
                val next = indices.filter { it > index }.minOrNull() ?: 19
                result[index] =
                    if (previous < index && next > index) {
                        val ratio = (index - previous).toFloat() / (next - previous)
                        (result[previous] + ratio * (result[next] - result[previous])).toInt()
                    } else {
                        result[previous]
                    }
            }
            return result
        }
    }
}
