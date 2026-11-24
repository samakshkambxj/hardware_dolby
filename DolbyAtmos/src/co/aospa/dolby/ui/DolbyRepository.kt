/*
 * Copyright (C) 2026 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.ui

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.preference.PreferenceManager
import co.aospa.dolby.DolbyConstants
import co.aospa.dolby.DolbyController
import co.aospa.dolby.R

/**
 * Thin presentation data layer on top of [DolbyController].
 *
 * It keeps the SharedPreferences keys written by the old preference screen so the boot receiver and
 * the quick-settings tile continue to work unchanged.
 */
class DolbyRepository(context: Context) {

    private val appContext = context.applicationContext
    private val controller: DolbyController
        get() = DolbyController.getInstance(appContext)

    private val defaultPrefs: SharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(appContext)

    private val audioManager: AudioManager
        get() = appContext.getSystemService(AudioManager::class.java)!!

    fun profilePrefs(profile: Int): SharedPreferences =
        appContext.getSharedPreferences("profile_$profile", Context.MODE_PRIVATE)

    // ---------------------------------------------------------------------------------------------
    // Snapshot
    // ---------------------------------------------------------------------------------------------

    fun loadState(): DolbyUiState {
        val profile = runCatching { controller.profile }.getOrDefault(0)
        val profileValues = intArray(R.array.dolby_profile_values)
        val profileEntries = stringArray(R.array.dolby_profile_entries)
        val profileKnown = profile in profileValues
        val profileName =
            profileValues.indexOf(profile).takeIf { it >= 0 }?.let { profileEntries.getOrNull(it) }
                ?: appContext.getString(R.string.dolby_profile_unknown)

        val prefs = profilePrefs(profile)
        val enabled =
            runCatching { controller.dsOn }
                .getOrElse { defaultPrefs.getBoolean(DolbyConstants.PREF_ENABLE, false) }

        val presetGains = readBandGains(profile)
        val ieq = runCatching { controller.getIeqPreset(profile) }.getOrDefault(0)

        return DolbyUiState(
            enabled = enabled,
            profile = profile,
            profileKnown = profileKnown,
            profileName = profileName,
            presetName = resolvePresetName(presetGains),
            ieq = ieq,
            speakerVirtualizer =
                runCatching { controller.getSpeakerVirtEnabled(profile) }.getOrDefault(false),
            headphoneVirtualizer =
                runCatching { controller.getHeadphoneVirtEnabled(profile) }.getOrDefault(false),
            stereoWidening =
                runCatching { controller.getStereoWideningAmount(profile) }.getOrDefault(0),
            dialogueEnhancer =
                runCatching { controller.getDialogueEnhancerAmount(profile) }.getOrDefault(0),
            bassEnhancer =
                prefs.getBoolean(
                    DolbyConstants.PREF_BASS,
                    runCatching { controller.getBassEnhancerEnabled(profile) }.getOrDefault(false),
                ),
            volumeLeveler =
                runCatching { controller.getVolumeLevelerEnabled(profile) }.getOrDefault(false),
            isOnSpeaker = isOnSpeaker(),
            stereoWideningSupported = stereoWideningSupported(),
            bassLevel = prefs.getInt(KEY_BASS_LEVEL, DEFAULT_BASS_LEVEL),
            bassCurve = prefs.getInt(KEY_BASS_CURVE, 0),
            midEnhancer = prefs.getBoolean(KEY_MID, false),
            midLevel = prefs.getInt(KEY_MID_LEVEL, DEFAULT_MID_LEVEL),
            trebleEnhancer = prefs.getBoolean(KEY_TREBLE, false),
            trebleLevel = prefs.getInt(KEY_TREBLE_LEVEL, DEFAULT_TREBLE_LEVEL),
            bandGains = presetGains,
            bandMode = BandMode.fromValue(prefs.getString(KEY_BAND_MODE, null)),
            profiles = buildProfiles(profileValues, profileEntries),
            ieqChoices = buildIeqChoices(),
            dialogueChoices = buildChoices(R.array.dolby_dialogue_entries, R.array.dolby_dialogue_values),
            stereoChoices = buildChoices(R.array.dolby_stereo_entries, R.array.dolby_stereo_values),
            bassCurveChoices =
                listOf(
                    LevelChoice(0, appContext.getString(R.string.dolby_bass_curve_balanced)),
                    LevelChoice(1, appContext.getString(R.string.dolby_bass_curve_sub_boost)),
                    LevelChoice(2, appContext.getString(R.string.dolby_bass_curve_punch)),
                ),
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Main settings
    // ---------------------------------------------------------------------------------------------

    fun setEnabled(enabled: Boolean) {
        runCatching { controller.setDsOnAndPersist(enabled) }
    }

    fun setProfile(profile: Int) {
        runCatching { controller.profile = profile }
    }

    fun setIeq(value: Int) {
        runCatching { controller.setIeqPreset(value) }
        profilePrefs(currentProfile()).edit().putString(DolbyConstants.PREF_IEQ, value.toString()).apply()
    }

    fun setSpeakerVirtualizer(enabled: Boolean) {
        runCatching { controller.setSpeakerVirtEnabled(enabled) }
        profilePrefs(currentProfile())
            .edit()
            .putBoolean(DolbyConstants.PREF_SPK_VIRTUALIZER, enabled)
            .apply()
    }

    fun setHeadphoneVirtualizer(enabled: Boolean) {
        runCatching { controller.setHeadphoneVirtEnabled(enabled) }
        profilePrefs(currentProfile())
            .edit()
            .putBoolean(DolbyConstants.PREF_HP_VIRTUALIZER, enabled)
            .apply()
    }

    fun setVolumeLeveler(enabled: Boolean) {
        runCatching { controller.setVolumeLevelerEnabled(enabled) }
        profilePrefs(currentProfile())
            .edit()
            .putBoolean(DolbyConstants.PREF_VOLUME, enabled)
            .apply()
    }

    fun setDialogueEnhancer(value: Int) {
        runCatching { controller.setDialogueEnhancerAmount(value) }
        profilePrefs(currentProfile())
            .edit()
            .putString(DolbyConstants.PREF_DIALOGUE, value.toString())
            .apply()
    }

    fun setStereoWidening(value: Int) {
        runCatching { controller.setStereoWideningAmount(value) }
        profilePrefs(currentProfile())
            .edit()
            .putString(DolbyConstants.PREF_STEREO, value.toString())
            .apply()
    }

    fun resetProfile() {
        val profile = currentProfile()
        runCatching { controller.resetProfileSpecificSettings() }
        // Re-apply the ROM defaults so the DSP does not stay flat.
        runCatching {
            controller.setDsOnAndPersist(defaultPrefs.getBoolean(DolbyConstants.PREF_ENABLE, true))
        }
        runCatching { controller.setPreset(controller.getPreset(profile), profile) }
    }

    fun isOnSpeaker(): Boolean {
        // getDevicesForAttributes() returns a List; keep the Result type instead of calling
        // getOrDefault(emptyArray()), which widens the value to Any and breaks member access.
        val devices =
            runCatching {
                    audioManager.getDevicesForAttributes(
                        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build()
                    )
                }
                .getOrElse { return true }
        if (devices.isEmpty()) return true
        val type = devices[0].type
        return type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER ||
            type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
    }

    fun outputName(): String =
        if (isOnSpeaker()) {
            appContext.getString(R.string.dolby_speaker)
        } else {
            appContext.getString(R.string.dolby_headphones)
        }

    fun versionName(): String =
        runCatching {
                appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName
            }
            .getOrNull()
            ?: "1.0"

    private fun stereoWideningSupported(): Boolean =
        appContext.resources.getBoolean(R.bool.dolby_stereo_widening_supported)

    // ---------------------------------------------------------------------------------------------
    // Equalizer
    // ---------------------------------------------------------------------------------------------

    fun presetNames(): List<String> =
        builtInPresetNames() + userPresets().keys.sorted()

    fun applyPreset(name: String) {
        val builtIn = builtInPresetNames().indexOf(name)
        val gains =
            when {
                builtIn >= 0 -> {
                    val values = stringArray(R.array.dolby_preset_values)
                    values.getOrNull(builtIn)
                }
                else -> userPresets()[name]
            } ?: return
        writeBandGains(parseGains(gains), currentProfile())
    }

    fun savePreset(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val context = appContext
        context
            .getSharedPreferences(PRESETS_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(trimmed, readBandGains(currentProfile()).joinToString(","))
            .apply()
    }

    fun deletePreset(name: String) {
        appContext
            .getSharedPreferences(PRESETS_PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(name)
            .apply()
    }

    fun isUserPreset(name: String): Boolean = userPresets().containsKey(name)

    fun setBandMode(mode: BandMode) {
        profilePrefs(currentProfile()).edit().putString(KEY_BAND_MODE, mode.label).apply()
    }

    /** Writes a full 20-band gain array (0.1 dB units) to the DSP and profile prefs. */
    fun setGains(gains: List<Int>) {
        writeBandGains(gains, currentProfile())
    }

    fun resetGains() {
        writeBandGains(List(DolbyUiState.BACKEND_BANDS) { 0 }, currentProfile())
    }

    /**
     * Replaces one visible band. In 10-band mode the hidden half-band between two bands is
     * interpolated so the DSP curve does not comb.
     */
    fun withGain(gains: List<Int>, visibleIndex: Int, tenths: Int, mode: BandMode): List<Int> {
        val indices = mode.backendIndices
        val backend = indices.getOrNull(visibleIndex) ?: return gains
        val result = gains.toMutableList()
        while (result.size < DolbyUiState.BACKEND_BANDS) result.add(0)
        result[backend] = tenths.coerceIn(-150, 150)
        if (mode == BandMode.TEN) {
            val left = backend
            val right = backend + 2
            if (right < result.size) {
                result[backend + 1] = ((result[left] + result[right]) / 2).coerceIn(-150, 150)
            }
        }
        return result
    }

    fun readBandGains(profile: Int = currentProfile()): List<Int> {
        val stored = profilePrefs(profile).getString(DolbyConstants.PREF_PRESET, null)
        val raw =
            if (!stored.isNullOrBlank()) {
                stored
            } else {
                runCatching { controller.getPreset(profile) }.getOrDefault("")
            }
        return parseGains(raw)
    }

    private fun writeBandGains(gains: List<Int>, profile: Int) {
        val normalized =
            gains.take(DolbyUiState.BACKEND_BANDS).toMutableList().apply {
                while (size < DolbyUiState.BACKEND_BANDS) add(0)
            }
        val serialized = normalized.joinToString(",")
        runCatching { controller.setPreset(serialized, profile) }
        profilePrefs(profile).edit().putString(DolbyConstants.PREF_PRESET, serialized).apply()
    }

    private fun resolvePresetName(gains: List<Int>): String {
        val serialized = gains.joinToString(",")
        val builtIn = stringArray(R.array.dolby_preset_values)
        val builtInIndex = builtIn.indexOf(serialized)
        if (builtInIndex >= 0) {
            return stringArray(R.array.dolby_preset_entries).getOrElse(builtInIndex) {
                appContext.getString(R.string.dolby_eq_custom)
            }
        }
        userPresets().entries.firstOrNull { it.value == serialized }?.let { return it.key }
        return appContext.getString(R.string.dolby_eq_custom)
    }

    // ---------------------------------------------------------------------------------------------
    // Audio tuning (bass / mid / treble), applied as offsets on the GEQ gains like the 17 branch.
    // ---------------------------------------------------------------------------------------------

    fun setBassEnhancer(enabled: Boolean) {
        val prefs = profilePrefs(currentProfile())
        if (prefs.getBoolean(KEY_BASS_ENABLED_KEY, false) == enabled) return
        prefs.edit().putBoolean(KEY_BASS_ENABLED_KEY, enabled).apply()
        runCatching { controller.setBassEnhancerEnabled(enabled, currentProfile()) }

        val level = prefs.getInt(KEY_BASS_LEVEL, DEFAULT_BASS_LEVEL)
        val curve = prefs.getInt(KEY_BASS_CURVE, 0)
        if (enabled && level > 0) {
            applyBassDelta(level, curve, +1)
        } else if (!enabled && level > 0) {
            applyBassDelta(level, curve, -1)
        }
    }

    fun setBassLevel(level: Int) {
        val clamped = level.coerceIn(0, 100)
        val prefs = profilePrefs(currentProfile())
        val previous = prefs.getInt(KEY_BASS_LEVEL, DEFAULT_BASS_LEVEL)
        if (previous == clamped) return
        val curve = prefs.getInt(KEY_BASS_CURVE, 0)
        val enabled = prefs.getBoolean(KEY_BASS_ENABLED_KEY, false)
        prefs.edit().putInt(KEY_BASS_LEVEL, clamped).apply()
        if (!enabled) return
        if (previous > 0) applyBassDelta(previous, curve, -1)
        if (clamped > 0) applyBassDelta(clamped, curve, +1)
    }

    fun setBassCurve(curve: Int) {
        val prefs = profilePrefs(currentProfile())
        val previous = prefs.getInt(KEY_BASS_CURVE, 0)
        if (previous == curve) return
        val level = prefs.getInt(KEY_BASS_LEVEL, DEFAULT_BASS_LEVEL)
        val enabled = prefs.getBoolean(KEY_BASS_ENABLED_KEY, false)
        prefs.edit().putInt(KEY_BASS_CURVE, curve).apply()
        if (!enabled || level <= 0) return
        applyBassDelta(level, previous, -1)
        applyBassDelta(level, curve, +1)
    }

    fun setMidEnhancer(enabled: Boolean) {
        val prefs = profilePrefs(currentProfile())
        if (prefs.getBoolean(KEY_MID, false) == enabled) return
        prefs.edit().putBoolean(KEY_MID, enabled).apply()
        val level = prefs.getInt(KEY_MID_LEVEL, DEFAULT_MID_LEVEL)
        applyBandRange(5..13, level, MID_GAIN_MULTIPLIER, if (enabled) +1 else -1)
    }

    fun setMidLevel(level: Int) {
        val clamped = level.coerceIn(0, 100)
        val prefs = profilePrefs(currentProfile())
        val previous = prefs.getInt(KEY_MID_LEVEL, DEFAULT_MID_LEVEL)
        if (previous == clamped) return
        val enabled = prefs.getBoolean(KEY_MID, false)
        prefs.edit().putInt(KEY_MID_LEVEL, clamped).apply()
        if (!enabled) return
        applyBandRange(5..13, previous, MID_GAIN_MULTIPLIER, -1)
        applyBandRange(5..13, clamped, MID_GAIN_MULTIPLIER, +1)
    }

    fun setTrebleEnhancer(enabled: Boolean) {
        val prefs = profilePrefs(currentProfile())
        if (prefs.getBoolean(KEY_TREBLE, false) == enabled) return
        prefs.edit().putBoolean(KEY_TREBLE, enabled).apply()
        val level = prefs.getInt(KEY_TREBLE_LEVEL, DEFAULT_TREBLE_LEVEL)
        applyBandRange(14..19, level, TREBLE_GAIN_MULTIPLIER, if (enabled) +1 else -1)
    }

    fun setTrebleLevel(level: Int) {
        val clamped = level.coerceIn(0, 100)
        val prefs = profilePrefs(currentProfile())
        val previous = prefs.getInt(KEY_TREBLE_LEVEL, DEFAULT_TREBLE_LEVEL)
        if (previous == clamped) return
        val enabled = prefs.getBoolean(KEY_TREBLE, false)
        prefs.edit().putInt(KEY_TREBLE_LEVEL, clamped).apply()
        if (!enabled) return
        applyBandRange(14..19, previous, TREBLE_GAIN_MULTIPLIER, -1)
        applyBandRange(14..19, clamped, TREBLE_GAIN_MULTIPLIER, +1)
    }

    private fun applyBassDelta(level: Int, curve: Int, direction: Int) {
        val weights = BASS_CURVES.getOrElse(curve) { BASS_CURVES[0] }
        val base = level * BASS_GAIN_MULTIPLIER
        updateGainArray { gains ->
            for (i in weights.indices) {
                if (i >= gains.size) break
                val delta = (base * weights[i] * direction).toInt()
                gains[i] = (gains[i] + delta).coerceIn(-150, 150)
            }
        }
    }

    private fun applyBandRange(
        range: IntRange,
        level: Int,
        multiplier: Float,
        direction: Int,
    ) {
        if (level <= 0) return
        val delta = (level * multiplier * direction).toInt()
        updateGainArray { gains ->
            for (i in range) {
                if (i >= gains.size) break
                gains[i] = (gains[i] + delta).coerceIn(-150, 150)
            }
        }
    }

    private inline fun updateGainArray(block: (MutableList<Int>) -> Unit) {
        val profile = currentProfile()
        val gains = readBandGains(profile).toMutableList()
        while (gains.size < DolbyUiState.BACKEND_BANDS) gains.add(0)
        block(gains)
        writeBandGains(gains, profile)
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    private fun currentProfile(): Int = runCatching { controller.profile }.getOrDefault(0)

    private fun builtInPresetNames(): List<String> = stringArray(R.array.dolby_preset_entries)

    private fun userPresets(): Map<String, String> =
        appContext
            .getSharedPreferences(PRESETS_PREFS, Context.MODE_PRIVATE)
            .all
            .mapNotNull { (key, value) -> (value as? String)?.let { key to it } }
            .toMap()

    private fun buildProfiles(values: IntArray, entries: List<String>): List<DolbyProfile> =
        values.mapIndexed { index, value ->
            DolbyProfile(
                value = value,
                name = entries.getOrElse(index) { value.toString() },
                iconRes = profileIcon(value),
            )
        }

    private fun profileIcon(value: Int): Int =
        when (value) {
            0 -> R.drawable.ic_auto_fix
            1 -> R.drawable.ic_movie
            2 -> R.drawable.ic_music_note
            4 -> R.drawable.ic_spatial
            else -> R.drawable.ic_tune
        }

    private fun buildIeqChoices(): List<IeqChoice> {
        val entries = stringArray(R.array.dolby_ieq_entries)
        val values = intArray(R.array.dolby_ieq_values)
        return values.mapIndexed { index, value ->
            IeqChoice(
                value = value,
                name = entries.getOrElse(index) { value.toString() },
                iconRes =
                    when (value) {
                        1 -> R.drawable.ic_ieq_balanced
                        2 -> R.drawable.ic_ieq_warm
                        3 -> R.drawable.ic_ieq_detailed
                        else -> R.drawable.ic_ieq_off
                    },
            )
        }
    }

    private fun buildChoices(entriesRes: Int, valuesRes: Int): List<LevelChoice> {
        val entries = stringArray(entriesRes)
        val values = intArray(valuesRes)
        return values.mapIndexed { index, value ->
            LevelChoice(value, entries.getOrElse(index) { value.toString() })
        }
    }

    private fun intArray(res: Int): IntArray =
        appContext.resources
            .getStringArray(res)
            .mapNotNull { it.trim().toIntOrNull() }
            .toIntArray()

    private fun stringArray(res: Int): List<String> =
        appContext.resources.getStringArray(res).toList()

    private fun parseGains(raw: String?): List<Int> {
        val parsed =
            raw
                ?.split(",")
                ?.mapNotNull { it.trim().toIntOrNull() }
                ?.takeIf { it.isNotEmpty() }
                ?: emptyList()
        return List(DolbyUiState.BACKEND_BANDS) { parsed.getOrElse(it) { 0 } }
    }

    private companion object {
        const val PRESETS_PREFS = "presets"

        const val KEY_BAND_MODE = "dolby_band_mode"
        const val KEY_BASS_ENABLED_KEY = DolbyConstants.PREF_BASS
        const val KEY_BASS_LEVEL = "dolby_bass_level"
        const val KEY_BASS_CURVE = "dolby_bass_curve"
        const val KEY_MID = "dolby_mid"
        const val KEY_MID_LEVEL = "dolby_mid_level"
        const val KEY_TREBLE = "dolby_treble"
        const val KEY_TREBLE_LEVEL = "dolby_treble_level"

        const val DEFAULT_BASS_LEVEL = 50
        const val DEFAULT_MID_LEVEL = 40
        const val DEFAULT_TREBLE_LEVEL = 30

        const val BASS_GAIN_MULTIPLIER = 1.4f
        const val MID_GAIN_MULTIPLIER = 1.3f
        const val TREBLE_GAIN_MULTIPLIER = 1.5f

        val BASS_CURVES =
            listOf(
                floatArrayOf(
                    1.00f, 1.00f, 0.95f, 0.90f, 0.80f, 0.70f, 0.55f, 0.40f, 0.25f, 0.15f,
                    0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f,
                ),
                floatArrayOf(
                    1.20f, 1.15f, 1.05f, 0.90f, 0.70f, 0.55f, 0.40f, 0.25f, 0.10f, 0.05f,
                    0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f,
                ),
                floatArrayOf(
                    0.90f, 0.95f, 1.00f, 1.00f, 0.90f, 0.75f, 0.60f, 0.45f, 0.30f, 0.20f,
                    0.10f, 0.05f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f,
                ),
            )
    }
}
