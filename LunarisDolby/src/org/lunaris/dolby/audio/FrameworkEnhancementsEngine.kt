/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.util.Log

/**
 * Framework sound-enhancement effects on the global output mix
 * (session 0): bass boost, headphone virtualizer, preset reverb and
 * loudness enhancer.
 *
 * Each effect type is instantiated at most once here — nothing else in
 * the app creates these framework types (the Dolby virtualizers are
 * vendor DAP parameters, and DynamicsProcessing lives in
 * [DynamicsEqualizerEngine]), so there is no duplicate-instance
 * conflict on the session. They stack with Dolby/Dynamics processing
 * by design; the UI says so.
 */
class FrameworkEnhancementsEngine(
    private val sessionId: Int = 0
) {
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var reverb: PresetReverb? = null
    private var loudness: LoudnessEnhancer? = null
    private var isReleased = false

    // UI-facing state, percent unless noted. Applied to the live effect
    // whenever its setter runs (no-ops while unsupported).
    var bassEnabled: Boolean = false
        private set
    var bassStrengthPercent: Int = 0
        private set
    var virtualizerEnabled: Boolean = false
        private set
    var virtualizerStrengthPercent: Int = 0
        private set
    var reverbEnabled: Boolean = false
        private set
    var reverbPreset: Int = PRESET_NONE
        private set
    var loudnessEnabled: Boolean = false
        private set
    var loudnessGainHalfDb: Int = 0
        private set

    val bassSupported: Boolean get() = bassBoost != null
    val virtualizerSupported: Boolean get() = virtualizer != null
    val reverbSupported: Boolean get() = reverb != null
    val loudnessSupported: Boolean get() = loudness != null

    fun init() {
        if (isReleased) return
        if (bassBoost == null) {
            bassBoost = create { BassBoost(0, sessionId) }
            bassBoost?.enabled = false
        }
        if (virtualizer == null) {
            virtualizer = create { Virtualizer(0, sessionId) }
            virtualizer?.enabled = false
        }
        if (reverb == null) {
            reverb = create { PresetReverb(0, sessionId) }
            reverb?.enabled = false
        }
        if (loudness == null) {
            loudness = create { LoudnessEnhancer(sessionId) }
            loudness?.enabled = false
        }
    }

    private inline fun <T> create(block: () -> T): T? {
        return try {
            block()
        } catch (e: Exception) {
            Log.w(TAG, "Enhancement effect unavailable: ${e.message}")
            null
        }
    }

    fun setBassEnabled(enabled: Boolean) {
        bassEnabled = enabled
        runCatching { bassBoost?.enabled = enabled }
    }

    /** Percent 0..100, mapped to the 0..1000 per-mille strength. */
    fun setBassStrength(percent: Int) {
        bassStrengthPercent = percent.coerceIn(0, 100)
        runCatching {
            bassBoost?.setStrength((bassStrengthPercent * 10).toShort())
        }
    }

    fun setVirtualizerEnabled(enabled: Boolean) {
        virtualizerEnabled = enabled
        runCatching { virtualizer?.enabled = enabled }
    }

    /** Percent 0..100, mapped to the 0..1000 per-mille strength. */
    fun setVirtualizerStrength(percent: Int) {
        virtualizerStrengthPercent = percent.coerceIn(0, 100)
        runCatching {
            virtualizer?.setStrength((virtualizerStrengthPercent * 10).toShort())
        }
    }

    fun setReverbEnabled(enabled: Boolean) {
        reverbEnabled = enabled
        runCatching { reverb?.enabled = enabled }
    }

    /** Preset index into [REVERB_PRESET_NAMES] (0 = off/none). */
    fun setReverbPreset(preset: Int) {
        reverbPreset = preset.coerceIn(0, REVERB_PRESET_COUNT - 1)
        runCatching { reverb?.preset = reverbPreset.toShort() }
    }

    fun setLoudnessEnabled(enabled: Boolean) {
        loudnessEnabled = enabled
        runCatching { loudness?.enabled = enabled }
    }

    /**
     * Half-dB steps 0..20 (= 0..10 dB), mapped to millibels for
     * [LoudnessEnhancer.setTargetGain].
     */
    fun setLoudnessGainHalfDb(halfDb: Int) {
        loudnessGainHalfDb = halfDb.coerceIn(0, 20)
        runCatching { loudness?.setTargetGain(loudnessGainHalfDb * 50) }
    }

    /** Restores default values, preserving the enable switches. */
    fun resetValues() {
        setBassStrength(0)
        setVirtualizerStrength(0)
        setReverbPreset(PRESET_NONE)
        setLoudnessGainHalfDb(0)
    }

    fun release() {
        if (isReleased) return
        isReleased = true
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        runCatching { reverb?.release() }
        runCatching { loudness?.release() }
        bassBoost = null
        virtualizer = null
        reverb = null
        loudness = null
    }

    companion object {
        private const val TAG = "FrameworkEnhancements"

        const val PRESET_NONE = 0

        const val REVERB_PRESET_COUNT = 7

        /** Index-aligned with PresetReverb.PRESET_* (0..6). */
        val REVERB_PRESET_NAMES = listOf(
            "None",
            "Small room",
            "Medium room",
            "Large room",
            "Medium hall",
            "Large hall",
            "Plate"
        )
    }
}
