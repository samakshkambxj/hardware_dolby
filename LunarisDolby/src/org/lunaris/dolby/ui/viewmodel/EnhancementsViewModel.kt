/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.lunaris.dolby.audio.FrameworkEnhancementsEngine
import org.lunaris.dolby.data.EnhancementsStateData
import org.lunaris.dolby.data.EnhancementsStateStore

data class EnhancementsUiState(
    val bassSupported: Boolean = false,
    val bassEnabled: Boolean = false,
    val bassStrengthPercent: Int = 0,
    val virtualizerSupported: Boolean = false,
    val virtualizerEnabled: Boolean = false,
    val virtualizerStrengthPercent: Int = 0,
    val virtualizerMode: Int = 1,
    val reverbSupported: Boolean = false,
    val reverbEnabled: Boolean = false,
    val reverbPreset: Int = 0,
    val loudnessSupported: Boolean = false,
    val loudnessEnabled: Boolean = false,
    val loudnessGainHalfDb: Int = 0
)

/**
 * Framework sound-enhancement effects (bass boost, virtualizer, reverb,
 * loudness) on the global output mix. Owns [FrameworkEnhancementsEngine]
 * the same way [DynamicsEqualizerViewModel] owns its engine: created
 * once, restored from disk, released with the ViewModel.
 */
class EnhancementsViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = FrameworkEnhancementsEngine(sessionId = 0)
    private val stateStore = EnhancementsStateStore(application)

    private val _uiState = MutableStateFlow(EnhancementsUiState())
    val uiState: StateFlow<EnhancementsUiState> = _uiState.asStateFlow()

    init {
        engine.init()
        restorePersistedState()
        _uiState.value = buildState()
    }

    private fun restorePersistedState() {
        val stored = stateStore.load() ?: return
        engine.setBassStrength(stored.bassStrengthPercent)
        engine.setBassEnabled(stored.bassEnabled)
        engine.setVirtualizerStrength(stored.virtualizerStrengthPercent)
        engine.setVirtualizerMode(stored.virtualizerMode)
        engine.setVirtualizerEnabled(stored.virtualizerEnabled)
        engine.setReverbPreset(stored.reverbPreset)
        engine.setReverbEnabled(stored.reverbEnabled)
        engine.setLoudnessGainHalfDb(stored.loudnessGainHalfDb)
        engine.setLoudnessEnabled(stored.loudnessEnabled)
    }

    private fun persistState() {
        val s = _uiState.value
        stateStore.save(
            EnhancementsStateData(
                bassEnabled = s.bassEnabled,
                bassStrengthPercent = s.bassStrengthPercent,
                virtualizerEnabled = s.virtualizerEnabled,
                virtualizerStrengthPercent = s.virtualizerStrengthPercent,
                virtualizerMode = s.virtualizerMode,
                reverbEnabled = s.reverbEnabled,
                reverbPreset = s.reverbPreset,
                loudnessEnabled = s.loudnessEnabled,
                loudnessGainHalfDb = s.loudnessGainHalfDb
            )
        )
    }

    private fun buildState(): EnhancementsUiState {
        return EnhancementsUiState(
            bassSupported = engine.bassSupported,
            bassEnabled = engine.bassEnabled,
            bassStrengthPercent = engine.bassStrengthPercent,
            virtualizerSupported = engine.virtualizerSupported,
            virtualizerEnabled = engine.virtualizerEnabled,
            virtualizerStrengthPercent = engine.virtualizerStrengthPercent,
            virtualizerMode = engine.virtualizerMode,
            reverbSupported = engine.reverbSupported,
            reverbEnabled = engine.reverbEnabled,
            reverbPreset = engine.reverbPreset,
            loudnessSupported = engine.loudnessSupported,
            loudnessEnabled = engine.loudnessEnabled,
            loudnessGainHalfDb = engine.loudnessGainHalfDb
        )
    }

    fun setBassEnabled(enabled: Boolean) {
        engine.setBassEnabled(enabled)
        _uiState.value = _uiState.value.copy(bassEnabled = enabled)
        persistState()
    }

    fun setBassStrength(percent: Float) {
        engine.setBassStrength(percent.toInt())
        _uiState.value = _uiState.value.copy(bassStrengthPercent = engine.bassStrengthPercent)
        persistState()
    }

    fun setVirtualizerEnabled(enabled: Boolean) {
        engine.setVirtualizerEnabled(enabled)
        _uiState.value = _uiState.value.copy(virtualizerEnabled = enabled)
        persistState()
    }

    fun setVirtualizerStrength(percent: Float) {
        engine.setVirtualizerStrength(percent.toInt())
        _uiState.value =
            _uiState.value.copy(virtualizerStrengthPercent = engine.virtualizerStrengthPercent)
        persistState()
    }

    fun setVirtualizerMode(mode: Int) {
        engine.setVirtualizerMode(mode)
        _uiState.value = _uiState.value.copy(virtualizerMode = engine.virtualizerMode)
        persistState()
    }

    fun setReverbEnabled(enabled: Boolean) {
        engine.setReverbEnabled(enabled)
        _uiState.value = _uiState.value.copy(reverbEnabled = enabled)
        persistState()
    }

    fun setReverbPreset(preset: Int) {
        engine.setReverbPreset(preset)
        _uiState.value = _uiState.value.copy(reverbPreset = engine.reverbPreset)
        persistState()
    }

    fun setLoudnessEnabled(enabled: Boolean) {
        engine.setLoudnessEnabled(enabled)
        _uiState.value = _uiState.value.copy(loudnessEnabled = enabled)
        persistState()
    }

    fun setLoudnessGainHalfDb(halfDb: Float) {
        engine.setLoudnessGainHalfDb(halfDb.toInt())
        _uiState.value =
            _uiState.value.copy(loudnessGainHalfDb = engine.loudnessGainHalfDb)
        persistState()
    }

    fun resetFx() {
        engine.resetValues()
        _uiState.value = buildState()
        persistState()
    }

    override fun onCleared() {
        engine.release()
        super.onCleared()
    }
}
