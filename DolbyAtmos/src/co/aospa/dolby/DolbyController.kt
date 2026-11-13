/*
 * Copyright (C) 2023-24 Paranoid Android
 * Copyright (C) 2026 samakshkambxj
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioManager.AudioPlaybackCallback
import android.media.AudioPlaybackConfiguration
import android.os.Build
import android.os.Handler
import co.aospa.dolby.DolbyConstants.dlog
import co.aospa.dolby.DolbyConstants.DsParam

/**
 * Single gateway to the DAX3 effect and its persisted state.
 *
 * Every public mutator is synchronized because the effect is also driven by
 * the boot receiver, the QS tile and the summary provider. Reads fall back to
 * the saved preset when the HAL rejects a query so the UI always has values.
 */
class DolbyController private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val defaultPrefs: SharedPreferences = appContext.getSharedPreferences(
        "${appContext.packageName}_preferences",
        Context.MODE_PRIVATE,
    )
    private val audioManager = appContext.getSystemService(AudioManager::class.java)
    private val handler = Handler(appContext.mainLooper)
    private val stereoWideningSupported =
        appContext.resources.getBoolean(R.bool.dolby_stereo_widening_supported)

    private var dolbyEffect: DolbyAudioEffect? = null
    private var restoring = false
    @Volatile private var initialized = false
    @Volatile private var lastError: String? = null

    /** False when the DAX3 effect is missing on this device. */
    @Volatile var available: Boolean = true
        private set

    val error: String? get() = lastError

    private val playbackCallback =
        object : AudioPlaybackCallback() {
            override fun onPlaybackConfigChanged(configs: List<AudioPlaybackConfiguration>) {
                val isPlaying = configs.any {
                    it.playerState == AudioPlaybackConfiguration.PLAYER_STATE_STARTED
                }
                if (isPlaying) setCurrentProfile()
            }
        }

    private val audioDeviceCallback =
        object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) {
                setCurrentProfile()
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) {
                setCurrentProfile()
            }
        }

    private var registerCallbacks = false
        set(value) {
            if (field == value) return
            field = value
            try {
                if (value) {
                    audioManager?.registerAudioPlaybackCallback(playbackCallback, handler)
                    audioManager?.registerAudioDeviceCallback(audioDeviceCallback, handler)
                } else {
                    audioManager?.unregisterAudioPlaybackCallback(playbackCallback)
                    audioManager?.unregisterAudioDeviceCallback(audioDeviceCallback)
                }
            } catch (e: Exception) {
                dlog(TAG, "callback registration failed: ${e.message}")
            }
        }

    // ---------------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------------

    /**
     * Attaches the effect and restores every profile once. Safe to call from
     * any process entry point (activity, tile, boot receiver, provider).
     */
    fun ensureInitialized() {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val savedEnabled = defaultPrefs.getBoolean(DolbyConstants.PREF_ENABLE, true)
            val savedProfile = readSavedProfile()
            applyEnabled(savedEnabled, persist = false)
            restoreAllProfiles()
            applyProfile(savedProfile, persist = false)
            // The previous preference-based UI stored the profile as a string.
            // Rewrite it as an int so later reads cannot hit a ClassCastException.
            if (defaultPrefs.all[DolbyConstants.PREF_PROFILE] !is Int) {
                defaultPrefs.edit().putInt(DolbyConstants.PREF_PROFILE, savedProfile).apply()
            }
            initialized = true
            dlog(TAG, "initialized (enabled=$savedEnabled, profile=$profile)")
        }
    }

    fun onBootCompleted() {
        dlog(TAG, "onBootCompleted")
        initialized = false
        ensureInitialized()
    }

    @Synchronized
    private fun ensureEffect(): DolbyAudioEffect? {
        val current = dolbyEffect
        if (current != null) {
            try {
                if (current.hasControl()) return current
            } catch (_: Exception) {
                // fall through and recreate
            }
            try {
                current.release()
            } catch (_: Exception) {
            }
            dolbyEffect = null
        }
        if (!available) return null
        return try {
            DolbyAudioEffect(EFFECT_PRIORITY, 0).also {
                dolbyEffect = it
                if (!restoring) {
                    restoring = true
                    try {
                        restoreAllProfiles()
                    } finally {
                        restoring = false
                    }
                }
            }
        } catch (e: Exception) {
            dlog(TAG, "effect unavailable: ${e.message}")
            lastError = e.message
            available = false
            null
        }
    }

    private fun restoreAllProfiles() {
        val values = appContext.resources.getStringArray(R.array.dolby_profile_values)
        for (value in values) {
            value.toIntOrNull()?.let { restoreProfile(it) }
        }
    }

    override fun toString(): String = "DolbyController(available=$available)"

    // ---------------------------------------------------------------------
    // Master switch and profile
    // ---------------------------------------------------------------------

    var dsOn: Boolean
        get() = try {
            ensureEffect()?.dsOn ?: false
        } catch (e: Exception) {
            dlog(TAG, "getDsOn failed: ${e.message}")
            false
        }
        set(value) = applyEnabled(value, persist = false)

    var profile: Int
        get() = try {
            ensureEffect()?.profile ?: readSavedProfile()
        } catch (e: Exception) {
            readSavedProfile()
        }
        set(value) = applyProfile(value, persist = false)

    fun setDsOnAndPersist(enabled: Boolean) = applyEnabled(enabled, persist = true)

    fun setProfileAndPersist(profile: Int) = applyProfile(profile, persist = true)

    @Synchronized
    private fun applyEnabled(enabled: Boolean, persist: Boolean) {
        try {
            ensureEffect()?.let { effect ->
                effect.dsOn = enabled
                registerCallbacks = enabled
                if (enabled) setCurrentProfile()
            }
            if (persist) {
                defaultPrefs.edit().putBoolean(DolbyConstants.PREF_ENABLE, enabled).apply()
            }
        } catch (e: Exception) {
            dlog(TAG, "setDsOn($enabled) failed: ${e.message}")
        }
    }

    @Synchronized
    private fun applyProfile(profile: Int, persist: Boolean) {
        try {
            ensureEffect()?.profile = profile
            if (persist) {
                defaultPrefs.edit().putInt(DolbyConstants.PREF_PROFILE, profile).apply()
            }
        } catch (e: Exception) {
            dlog(TAG, "setProfile($profile) failed: ${e.message}")
        }
    }

    private fun setCurrentProfile() {
        applyProfile(readSavedProfile(), persist = false)
    }

    /** Reads the stored profile as an int, tolerating the legacy string form. */
    private fun readSavedProfile(): Int =
        when (val raw = defaultPrefs.all[DolbyConstants.PREF_PROFILE]) {
            is Int -> raw
            is Long -> raw.toInt()
            is String -> raw.toIntOrNull() ?: 0
            else -> 0
        }

    fun getProfileName(profile: Int = this.profile): String? {
        val values = appContext.resources.getStringArray(R.array.dolby_profile_values)
        val index = values.indexOf(profile.toString())
        if (index < 0) return null
        return appContext.resources.getStringArray(R.array.dolby_profile_entries)[index]
    }

    fun resetProfileSpecificSettings(profile: Int = this.profile) {
        synchronized(this) {
            try {
                ensureEffect()?.resetProfileSpecificSettings(profile)
            } catch (e: Exception) {
                dlog(TAG, "resetProfileSpecificSettings failed: ${e.message}")
            }
            appContext.deleteSharedPreferences(profilePrefsName(profile))
        }
    }

    // ---------------------------------------------------------------------
    // Per-profile persistence
    // ---------------------------------------------------------------------

    private fun profilePrefs(profile: Int): SharedPreferences =
        appContext.getSharedPreferences(profilePrefsName(profile), Context.MODE_PRIVATE)

    private fun profilePrefsName(profile: Int) = "profile_$profile"

    private fun restoreProfile(profile: Int) {
        val prefs = profilePrefs(profile)
        val effect = ensureEffect() ?: return
        try {
            effect.resetProfileSpecificSettings(profile)

            // GEQ first: the saved preset already contains any bass / mid /
            // treble contribution, so the tuning sliders are not re-applied.
            val preset = prefs.getString(DolbyConstants.PREF_PRESET, null)
            if (preset != null) {
                setPresetInternal(profile, decodeGains(preset))
            }

            effect.setDapParameter(
                DsParam.IEQ_PRESET,
                prefs.getString(
                    DolbyConstants.PREF_IEQ,
                    effect.getDapParameterInt(DsParam.IEQ_PRESET, profile).toString(),
                )?.toIntOrNull() ?: 0,
                profile,
            )
            effect.setDapParameter(
                DsParam.HEADPHONE_VIRTUALIZER,
                prefs.getBoolean(
                    DolbyConstants.PREF_HP_VIRTUALIZER,
                    effect.getDapParameterBool(DsParam.HEADPHONE_VIRTUALIZER, profile),
                ),
                profile,
            )
            effect.setDapParameter(
                DsParam.SPEAKER_VIRTUALIZER,
                prefs.getBoolean(
                    DolbyConstants.PREF_SPK_VIRTUALIZER,
                    effect.getDapParameterBool(DsParam.SPEAKER_VIRTUALIZER, profile),
                ),
                profile,
            )
            if (stereoWideningSupported) {
                effect.setDapParameter(
                    DsParam.STEREO_WIDENING_AMOUNT,
                    prefs.getString(
                        DolbyConstants.PREF_STEREO,
                        effect.getDapParameterInt(DsParam.STEREO_WIDENING_AMOUNT, profile).toString(),
                    )?.toIntOrNull() ?: 0,
                    profile,
                )
            }
            val dialogue = prefs.getString(
                DolbyConstants.PREF_DIALOGUE,
                effect.getDapParameterInt(DsParam.DIALOGUE_ENHANCER_AMOUNT, profile).toString(),
            )?.toIntOrNull() ?: 0
            effect.setDapParameter(DsParam.DIALOGUE_ENHANCER_ENABLE, dialogue > 0, profile)
            effect.setDapParameter(DsParam.DIALOGUE_ENHANCER_AMOUNT, dialogue, profile)

            // Levels are baked into the preset; only the derived enable flags
            // need to be pushed back to the HAL.
            effect.setDapParameter(
                DsParam.BASS_ENHANCER_ENABLE,
                readBassLevel(profile) > 0,
                profile,
            )
            effect.setDapParameter(
                DsParam.VOLUME_LEVELER_ENABLE,
                prefs.getBoolean(DolbyConstants.PREF_VOLUME, false),
                profile,
            )
        } catch (e: Exception) {
            dlog(TAG, "restoreProfile($profile) failed: ${e.message}")
        }
    }

    // ---------------------------------------------------------------------
    // Graphic EQ primitives
    // ---------------------------------------------------------------------

    fun getGains(profile: Int = this.profile): IntArray {
        val fromEffect = try {
            ensureEffect()?.getDapParameter(DsParam.GEQ_BAND_GAINS, profile)
        } catch (e: Exception) {
            dlog(TAG, "getGains failed: ${e.message}")
            null
        }
        if (fromEffect != null && fromEffect.size >= DolbyConstants.GEQ_BAND_COUNT) {
            return normalize(fromEffect)
        }
        if (fromEffect != null && fromEffect.size == DolbyConstants.DISPLAY_BAND_COUNT) {
            return displayToBackend(fromEffect)
        }
        val saved = profilePrefs(profile).getString(DolbyConstants.PREF_PRESET, null)
        return if (saved != null) decodeGains(saved) else IntArray(DolbyConstants.GEQ_BAND_COUNT)
    }

    fun setPreset(profile: Int, gains: IntArray) {
        if (isReleased()) return
        synchronized(this) {
            try {
                setPresetInternal(profile, normalize(gains))
            } catch (e: Exception) {
                dlog(TAG, "setPreset failed: ${e.message}")
            }
        }
    }

    private fun setPresetInternal(profile: Int, gains: IntArray) {
        ensureEffect()?.setDapParameter(DsParam.GEQ_BAND_GAINS, gains, profile)
        profilePrefs(profile)
            .edit()
            .putString(DolbyConstants.PREF_PRESET, gains.joinToString(","))
            .apply()
    }

    fun getPresetString(profile: Int = this.profile): String =
        getGains(profile).joinToString(",")

    private fun readGainsForTuning(profile: Int): IntArray = getGains(profile).copyOf()

    private fun persistGains(profile: Int, gains: IntArray) {
        profilePrefs(profile)
            .edit()
            .putString(DolbyConstants.PREF_PRESET, gains.joinToString(","))
            .apply()
    }

    // ---------------------------------------------------------------------
    // Bass / mid / treble tuning (ported from the Lunaris build)
    // ---------------------------------------------------------------------

    fun getBassLevel(profile: Int = this.profile): Int = readBassLevel(profile)

    fun getBassCurve(profile: Int = this.profile): Int =
        profilePrefs(profile).getInt(DolbyConstants.PREF_BASS_CURVE, 0)

    fun getMidLevel(profile: Int = this.profile): Int =
        profilePrefs(profile).getInt(DolbyConstants.PREF_MID_LEVEL, 0)

    fun getTrebleLevel(profile: Int = this.profile): Int =
        profilePrefs(profile).getInt(DolbyConstants.PREF_TREBLE_LEVEL, 0)

    /**
     * Bass used to be a plain on/off switch. When only that legacy flag exists,
     * migrate it to a mid-strength level so the UI and the tuning math agree.
     */
    private fun readBassLevel(profile: Int): Int {
        val prefs = profilePrefs(profile)
        if (prefs.contains(DolbyConstants.PREF_BASS_LEVEL)) {
            return prefs.getInt(DolbyConstants.PREF_BASS_LEVEL, 0)
        }
        if (prefs.getBoolean(DolbyConstants.PREF_BASS, false)) {
            prefs.edit().putInt(DolbyConstants.PREF_BASS_LEVEL, 50).apply()
            return 50
        }
        return 0
    }

    fun setBassLevel(profile: Int, level: Int) {
        if (isReleased()) return
        require(level in 0..100) { "Bass level must be 0..100" }
        synchronized(this) {
            try {
                val prefs = profilePrefs(profile)
                val previous = readBassLevel(profile)
                if (previous == level) return

                prefs.edit().putInt(DolbyConstants.PREF_BASS_LEVEL, level).apply()
                ensureEffect()?.setDapParameter(DsParam.BASS_ENHANCER_ENABLE, level > 0, profile)
                prefs.edit().putBoolean(DolbyConstants.PREF_BASS, level > 0).apply()

                val gains = readGainsForTuning(profile)
                val curve = prefs.getInt(DolbyConstants.PREF_BASS_CURVE, 0)
                if (previous > 0) applyBassCurve(gains, previous, curve, -1)
                if (level > 0) applyBassCurve(gains, level, curve, 1)
                ensureEffect()?.setDapParameter(DsParam.GEQ_BAND_GAINS, gains, profile)
                persistGains(profile, gains)
            } catch (e: Exception) {
                dlog(TAG, "setBassLevel failed: ${e.message}")
            }
        }
    }

    fun setBassCurve(profile: Int, curve: Int) {
        if (isReleased()) return
        synchronized(this) {
            try {
                val prefs = profilePrefs(profile)
                val previous = prefs.getInt(DolbyConstants.PREF_BASS_CURVE, 0)
                if (previous == curve) return
                val level = prefs.getInt(DolbyConstants.PREF_BASS_LEVEL, 0)
                prefs.edit().putInt(DolbyConstants.PREF_BASS_CURVE, curve).apply()
                if (level <= 0) return

                val gains = readGainsForTuning(profile)
                applyBassCurve(gains, level, previous, -1)
                applyBassCurve(gains, level, curve, 1)
                ensureEffect()?.setDapParameter(DsParam.GEQ_BAND_GAINS, gains, profile)
                persistGains(profile, gains)
            } catch (e: Exception) {
                dlog(TAG, "setBassCurve failed: ${e.message}")
            }
        }
    }

    private fun applyBassCurve(gains: IntArray, level: Int, curve: Int, direction: Int) {
        val weights = BASS_CURVES.getOrElse(curve) { BASS_CURVES[0] }
        val baseGain = level * BASS_GAIN_MULTIPLIER
        for (i in weights.indices) {
            if (i >= gains.size) break
            val weighted = (baseGain * weights[i] * direction).toInt()
            gains[i] = (gains[i] + weighted).coerceIn(
                -DolbyConstants.GAIN_LIMIT,
                DolbyConstants.GAIN_LIMIT,
            )
        }
    }

    fun setMidLevel(profile: Int, level: Int) {
        if (isReleased()) return
        require(level in 0..100) { "Mid level must be 0..100" }
        synchronized(this) {
            try {
                val prefs = profilePrefs(profile)
                val previous = prefs.getInt(DolbyConstants.PREF_MID_LEVEL, 0)
                if (previous == level) return
                prefs.edit().putInt(DolbyConstants.PREF_MID_LEVEL, level).apply()
                prefs.edit().putBoolean(DolbyConstants.PREF_MID, level > 0).apply()

                val gains = readGainsForTuning(profile)
                if (previous > 0) {
                    val previousGain = (previous * MID_GAIN_MULTIPLIER).toInt()
                    for (i in MID_RANGE) {
                        if (i < gains.size) gains[i] = (gains[i] - previousGain).coerceIn(
                            -DolbyConstants.GAIN_LIMIT,
                            DolbyConstants.GAIN_LIMIT,
                        )
                    }
                }
                if (level > 0) {
                    val gain = (level * MID_GAIN_MULTIPLIER).toInt()
                    for (i in MID_RANGE) {
                        if (i < gains.size) gains[i] = (gains[i] + gain).coerceIn(
                            -DolbyConstants.GAIN_LIMIT,
                            DolbyConstants.GAIN_LIMIT,
                        )
                    }
                }
                ensureEffect()?.setDapParameter(DsParam.GEQ_BAND_GAINS, gains, profile)
                persistGains(profile, gains)
            } catch (e: Exception) {
                dlog(TAG, "setMidLevel failed: ${e.message}")
            }
        }
    }

    fun setTrebleLevel(profile: Int, level: Int) {
        if (isReleased()) return
        require(level in 0..100) { "Treble level must be 0..100" }
        synchronized(this) {
            try {
                val prefs = profilePrefs(profile)
                val previous = prefs.getInt(DolbyConstants.PREF_TREBLE_LEVEL, 0)
                if (previous == level) return
                prefs.edit().putInt(DolbyConstants.PREF_TREBLE_LEVEL, level).apply()
                prefs.edit().putBoolean(DolbyConstants.PREF_TREBLE, level > 0).apply()

                val gains = readGainsForTuning(profile)
                if (previous > 0) {
                    val previousGain = (previous * TREBLE_GAIN_MULTIPLIER).toInt()
                    for (i in TREBLE_RANGE) {
                        if (i < gains.size) gains[i] = (gains[i] - previousGain).coerceIn(
                            -DolbyConstants.GAIN_LIMIT,
                            DolbyConstants.GAIN_LIMIT,
                        )
                    }
                }
                if (level > 0) {
                    val gain = (level * TREBLE_GAIN_MULTIPLIER).toInt()
                    for (i in TREBLE_RANGE) {
                        if (i < gains.size) gains[i] = (gains[i] + gain).coerceIn(
                            -DolbyConstants.GAIN_LIMIT,
                            DolbyConstants.GAIN_LIMIT,
                        )
                    }
                }
                ensureEffect()?.setDapParameter(DsParam.GEQ_BAND_GAINS, gains, profile)
                persistGains(profile, gains)
            } catch (e: Exception) {
                dlog(TAG, "setTrebleLevel failed: ${e.message}")
            }
        }
    }

    // ---------------------------------------------------------------------
    // Remaining DAP settings
    // ---------------------------------------------------------------------

    fun getIeqPreset(profile: Int = this.profile): Int = readInt(DsParam.IEQ_PRESET, profile)
    fun setIeqPreset(profile: Int, value: Int) =
        writeInt(DsParam.IEQ_PRESET, profile, value, DolbyConstants.PREF_IEQ)

    fun getHeadphoneVirtualizer(profile: Int = this.profile): Boolean =
        readBool(DsParam.HEADPHONE_VIRTUALIZER, profile)

    fun setHeadphoneVirtualizer(profile: Int, value: Boolean) =
        writeBool(DsParam.HEADPHONE_VIRTUALIZER, profile, value, DolbyConstants.PREF_HP_VIRTUALIZER)

    fun getSpeakerVirtualizer(profile: Int = this.profile): Boolean =
        readBool(DsParam.SPEAKER_VIRTUALIZER, profile)

    fun setSpeakerVirtualizer(profile: Int, value: Boolean) =
        writeBool(DsParam.SPEAKER_VIRTUALIZER, profile, value, DolbyConstants.PREF_SPK_VIRTUALIZER)

    fun getBassEnhancerEnabled(profile: Int = this.profile): Boolean =
        readBool(DsParam.BASS_ENHANCER_ENABLE, profile)

    fun setBassEnhancerEnabled(profile: Int, value: Boolean) {
        writeBool(DsParam.BASS_ENHANCER_ENABLE, profile, value, DolbyConstants.PREF_BASS)
        if (!value) {
            profilePrefs(profile).edit().putInt(DolbyConstants.PREF_BASS_LEVEL, 0).apply()
        }
    }

    fun getVolumeLevelerEnabled(profile: Int = this.profile): Boolean =
        readBool(DsParam.VOLUME_LEVELER_ENABLE, profile)

    fun setVolumeLevelerEnabled(profile: Int, value: Boolean) =
        writeBool(DsParam.VOLUME_LEVELER_ENABLE, profile, value, DolbyConstants.PREF_VOLUME)

    fun getStereoWidening(profile: Int = this.profile): Int =
        if (!stereoWideningSupported) 0 else readInt(DsParam.STEREO_WIDENING_AMOUNT, profile)

    fun setStereoWidening(profile: Int, value: Int) {
        if (!stereoWideningSupported) return
        writeInt(DsParam.STEREO_WIDENING_AMOUNT, profile, value, DolbyConstants.PREF_STEREO)
    }

    fun getDialogueEnhancer(profile: Int = this.profile): Int {
        val enabled = readBool(DsParam.DIALOGUE_ENHANCER_ENABLE, profile)
        return if (enabled) readInt(DsParam.DIALOGUE_ENHANCER_AMOUNT, profile) else 0
    }

    fun setDialogueEnhancer(profile: Int, value: Int) {
        synchronized(this) {
            try {
                ensureEffect()?.apply {
                    setDapParameter(DsParam.DIALOGUE_ENHANCER_ENABLE, value > 0, profile)
                    setDapParameter(DsParam.DIALOGUE_ENHANCER_AMOUNT, value, profile)
                }
                profilePrefs(profile)
                    .edit()
                    .putString(DolbyConstants.PREF_DIALOGUE, value.toString())
                    .apply()
            } catch (e: Exception) {
                dlog(TAG, "setDialogueEnhancer failed: ${e.message}")
            }
        }
    }

    private fun readInt(param: DsParam, profile: Int): Int = try {
        ensureEffect()?.getDapParameterInt(param, profile) ?: 0
    } catch (e: Exception) {
        dlog(TAG, "readInt($param) failed: ${e.message}")
        0
    }

    private fun readBool(param: DsParam, profile: Int): Boolean = try {
        ensureEffect()?.getDapParameterBool(param, profile) ?: false
    } catch (e: Exception) {
        dlog(TAG, "readBool($param) failed: ${e.message}")
        false
    }

    private fun writeInt(param: DsParam, profile: Int, value: Int, prefKey: String) {
        synchronized(this) {
            try {
                ensureEffect()?.setDapParameter(param, value, profile)
                profilePrefs(profile).edit().putString(prefKey, value.toString()).apply()
            } catch (e: Exception) {
                dlog(TAG, "writeInt($param=$value) failed: ${e.message}")
            }
        }
    }

    private fun writeBool(param: DsParam, profile: Int, value: Boolean, prefKey: String) {
        synchronized(this) {
            try {
                ensureEffect()?.setDapParameter(param, value, profile)
                profilePrefs(profile).edit().putBoolean(prefKey, value).apply()
            } catch (e: Exception) {
                dlog(TAG, "writeBool($param=$value) failed: ${e.message}")
            }
        }
    }

    // ---------------------------------------------------------------------
    // Output routing helpers
    // ---------------------------------------------------------------------

    fun isOnSpeaker(): Boolean {
        return try {
            val devices: List<AudioDeviceInfo>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager?.getDevicesForAttributes(ATTRIBUTES_MEDIA)
            } else {
                audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)?.toList()
            }
            devices?.any { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER } ?: true
        } catch (e: Exception) {
            dlog(TAG, "isOnSpeaker failed: ${e.message}")
            true
        }
    }

    fun getOutputName(): String {
        return try {
            val devices: List<AudioDeviceInfo>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager?.getDevicesForAttributes(ATTRIBUTES_MEDIA)
            } else {
                audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)?.toList()
            }
            val device = devices?.firstOrNull() ?: return appContext.getString(R.string.dolby_unknown)
            when (device.type) {
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Speaker"
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Headphones"
                AudioDeviceInfo.TYPE_USB_HEADSET -> "USB headset"
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth"
                AudioDeviceInfo.TYPE_BLE_HEADSET -> "Bluetooth LE"
                AudioDeviceInfo.TYPE_BLE_SPEAKER -> "Bluetooth speaker"
                else -> device.productName?.toString()?.ifBlank { "Audio device" } ?: "Audio device"
            }
        } catch (e: Exception) {
            dlog(TAG, "getOutputName failed: ${e.message}")
            appContext.getString(R.string.dolby_unknown)
        }
    }

    fun isStereoWideningSupported() = stereoWideningSupported

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private fun isReleased() = !available

    private fun normalize(gains: IntArray): IntArray {
        val out = IntArray(DolbyConstants.GEQ_BAND_COUNT)
        for (i in out.indices) {
            out[i] = (gains.getOrNull(i) ?: 0).coerceIn(
                -DolbyConstants.GAIN_LIMIT,
                DolbyConstants.GAIN_LIMIT,
            )
        }
        return out
    }

    private fun decodeGains(value: String): IntArray {
        val parsed = value.split(",").mapNotNull { it.trim().toIntOrNull() }
        return normalize(if (parsed.size == DolbyConstants.DISPLAY_BAND_COUNT) {
            displayToBackend(parsed.toIntArray())
        } else {
            parsed.toIntArray()
        })
    }

    /** Expands the 10 visible gains to the 20 bands the DAP expects. */
    fun displayToBackend(display: IntArray): IntArray {
        val out = IntArray(DolbyConstants.GEQ_BAND_COUNT)
        for (i in out.indices) {
            out[i] = if (i % 2 == 1 && i < DolbyConstants.GEQ_BAND_COUNT - 1) {
                (display.getOrElse((i - 1) / 2) { 0 } + display.getOrElse((i + 1) / 2) { 0 }) / 2
            } else {
                display.getOrElse(i / 2) { 0 }
            }
        }
        return out
    }

    companion object {
        private const val TAG = "DolbyController"
        private const val EFFECT_PRIORITY = 100

        private const val BASS_GAIN_MULTIPLIER = 1.4f
        private const val MID_GAIN_MULTIPLIER = 1.3f
        private const val TREBLE_GAIN_MULTIPLIER = 1.5f

        private val MID_RANGE = 5..13
        private val TREBLE_RANGE = 14..19

        private val ATTRIBUTES_MEDIA =
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build()

        private val BASS_CURVES = listOf(
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

        @Volatile private var instance: DolbyController? = null

        fun getInstance(context: Context): DolbyController =
            instance ?: synchronized(this) {
                instance ?: DolbyController(context).also { instance = it }
            }
    }
}
