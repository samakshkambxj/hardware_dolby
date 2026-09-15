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
import org.lunaris.dolby.audio.DynamicsEqualizerEngine
import org.lunaris.dolby.audio.DynamicsVisualizerEngine
import org.lunaris.dolby.data.DynamicsEqualizerPresetRepository
import org.lunaris.dolby.data.DynamicsPresetData

data class MbcBandUiState(
    val label: String,
    val threshold: Float,
    val ratio: Float,
    val attackMs: Float,
    val releaseMs: Float
)

data class DynamicsEqualizerUiState(
    val enabled: Boolean = false,
    val preampDb: Float = 0f,
    val bandGains: List<Float> = List(10) { 0f },
    val bandFrequencies: List<Float> = emptyList(),
    val mbcEnabled: Boolean = false,
    val mbcBands: List<MbcBandUiState> = emptyList(),
    val limiterEnabled: Boolean = false,
    val limiterThreshold: Float = -1f,
    val limiterRelease: Float = 50f,
    val limiterRatio: Float = 10f,
    val limiterPostGain: Float = 0f,
    val presetNames: List<String> = emptyList()
)

class DynamicsEqualizerViewModel(application: Application) : AndroidViewModel(application) {

    private val mbcLabels = listOf("Low", "Mid", "High")

    private val engine = DynamicsEqualizerEngine(sessionId = 0, bandCount = 10)
    private val visualizerEngine = DynamicsVisualizerEngine(sessionId = 0, barCount = 32)
    private val presetRepository = DynamicsEqualizerPresetRepository(application)

    private val _spectrum = MutableStateFlow(FloatArray(32))
    val spectrum: StateFlow<FloatArray> = _spectrum.asStateFlow()

    private val _uiState = MutableStateFlow(DynamicsEqualizerUiState())
    val uiState: StateFlow<DynamicsEqualizerUiState> = _uiState.asStateFlow()

    init {
        engine.init()
        _uiState.value = buildState()
        visualizerEngine.onSpectrumUpdate = { bars -> _spectrum.value = bars }
        visualizerEngine.start()
    }

    private fun buildState(): DynamicsEqualizerUiState {
        return DynamicsEqualizerUiState(
            enabled = engine.enabled,
            preampDb = engine.preampDb,
            bandGains = engine.bandFrequencies.indices.map { engine.getBandGain(it) },
            bandFrequencies = engine.bandFrequencies.toList(),
            mbcEnabled = engine.isMbcEnabled(),
            mbcBands = (0 until engine.mbcBandCount).map { i ->
                val p = engine.getMbcBand(i)
                MbcBandUiState(
                    label = mbcLabels.getOrElse(i) { "Band $i" },
                    threshold = p.thresholdDb,
                    ratio = p.ratio,
                    attackMs = p.attackTimeMs,
                    releaseMs = p.releaseTimeMs
                )
            },
            limiterEnabled = engine.isLimiterEnabled(),
            limiterThreshold = engine.limiterThresholdDb,
            limiterRelease = engine.limiterReleaseMs,
            limiterRatio = engine.limiterRatio,
            limiterPostGain = engine.limiterPostGainDb,
            presetNames = presetRepository.listPresetNames()
        )
    }

    fun setEnabled(enabled: Boolean) {
        engine.setEnabled(enabled)
        _uiState.value = _uiState.value.copy(enabled = enabled)
    }

    fun setBandGain(band: Int, gainDb: Float) {
        engine.setBandGain(band, gainDb)
        val updated = _uiState.value.bandGains.toMutableList()
        if (band in updated.indices) {
            updated[band] = gainDb
        }
        _uiState.value = _uiState.value.copy(bandGains = updated)
    }

    fun setPreamp(gainDb: Float) {
        engine.setPreamp(gainDb)
        _uiState.value = _uiState.value.copy(preampDb = gainDb)
    }

    fun setMbcEnabled(enabled: Boolean) {
        engine.setMbcEnabled(enabled)
        _uiState.value = _uiState.value.copy(mbcEnabled = enabled)
    }

    fun setMbcThreshold(band: Int, value: Float) {
        engine.setMbcThreshold(band, value)
        _uiState.value = buildState()
    }

    fun setMbcRatio(band: Int, value: Float) {
        engine.setMbcRatio(band, value)
        _uiState.value = buildState()
    }

    fun setMbcAttack(band: Int, value: Float) {
        engine.setMbcAttack(band, value)
        _uiState.value = buildState()
    }

    fun setMbcRelease(band: Int, value: Float) {
        engine.setMbcRelease(band, value)
        _uiState.value = buildState()
    }

    fun setLimiterEnabled(enabled: Boolean) {
        engine.setLimiterEnabled(enabled)
        _uiState.value = buildState()
    }

    fun setLimiterThreshold(value: Float) {
        engine.setLimiterThreshold(value)
        _uiState.value = buildState()
    }

    fun setLimiterRelease(value: Float) {
        engine.setLimiterRelease(value)
        _uiState.value = buildState()
    }

    fun setLimiterRatio(value: Float) {
        engine.setLimiterRatio(value)
        _uiState.value = buildState()
    }

    fun setLimiterPostGain(value: Float) {
        engine.setLimiterPostGain(value)
        _uiState.value = buildState()
    }

    fun savePreset(name: String) {
        if (name.isBlank()) return
        val s = _uiState.value
        presetRepository.savePreset(
            DynamicsPresetData(
                name = name,
                preampDb = s.preampDb,
                bandGains = s.bandGains,
                mbcEnabled = s.mbcEnabled,
                mbcThresholds = s.mbcBands.map { it.threshold },
                mbcRatios = s.mbcBands.map { it.ratio },
                mbcAttacks = s.mbcBands.map { it.attackMs },
                mbcReleases = s.mbcBands.map { it.releaseMs },
                limiterEnabled = s.limiterEnabled,
                limiterThreshold = s.limiterThreshold,
                limiterRatio = s.limiterRatio,
                limiterRelease = s.limiterRelease,
                limiterPostGain = s.limiterPostGain
            )
        )
        _uiState.value = buildState()
    }

    fun loadPreset(name: String) {
        val preset = presetRepository.loadPreset(name) ?: return

        engine.setPreamp(preset.preampDb)
        preset.bandGains.forEachIndexed { index, gain -> engine.setBandGain(index, gain) }

        engine.setMbcEnabled(preset.mbcEnabled)
        for (i in preset.mbcThresholds.indices) {
            engine.setMbcThreshold(i, preset.mbcThresholds[i])
            engine.setMbcRatio(i, preset.mbcRatios.getOrElse(i) { 2f })
            engine.setMbcAttack(i, preset.mbcAttacks.getOrElse(i) { 10f })
            engine.setMbcRelease(i, preset.mbcReleases.getOrElse(i) { 100f })
        }

        engine.setLimiterEnabled(preset.limiterEnabled)
        engine.setLimiterThreshold(preset.limiterThreshold)
        engine.setLimiterRatio(preset.limiterRatio)
        engine.setLimiterRelease(preset.limiterRelease)
        engine.setLimiterPostGain(preset.limiterPostGain)

        _uiState.value = buildState()
    }

    fun deletePreset(name: String) {
        presetRepository.deletePreset(name)
        _uiState.value = buildState()
    }

    override fun onCleared() {
        engine.release()
        visualizerEngine.release()
        super.onCleared()
    }
}
