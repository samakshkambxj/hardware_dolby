/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daybreak.dolby.ui.equalizer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.daybreak.dolby.R
import com.daybreak.dolby.audio.DolbyController
import com.daybreak.dolby.audio.DsParam
import com.daybreak.dolby.data.GainsCodec
import com.daybreak.dolby.data.PresetStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EqPreset(
    val name: String,
    val gains20: List<Int>,
    val user: Boolean,
)

data class EqUiState(
    val gains10: List<Int> = List(10) { 0 },
    val presetName: String = "",
    val isCustom: Boolean = false,
    val isUserPreset: Boolean = false,
    val presets: List<EqPreset> = emptyList(),
)

/**
 * Ten-band UI over the twenty-band backend. Edits accumulate locally and
 * commit to the DSP once per gesture; preset switches apply immediately.
 */
class EqViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application
    private val controller = DolbyController.get(application)
    private val presets = PresetStore(application)

    private val _uiState = MutableStateFlow(EqUiState())
    val uiState: StateFlow<EqUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val gains20 = controller.getGains()
                ?: List(DsParam.GEQ_BAND_GAINS.length) { 0 }
            val builtIns = builtInPresets()
            val user = presets.names().mapNotNull { name ->
                presets.gains(name, DsParam.GEQ_BAND_GAINS.length)?.let {
                    EqPreset(name, it, user = true)
                }
            }
            val all = user + builtIns
            val match = all.firstOrNull { GainsCodec.matches(it.gains20, gains20) }
            _uiState.value = EqUiState(
                gains10 = collapse(gains20),
                presetName = match?.name
                    ?: context.getString(R.string.eq_custom),
                isCustom = match == null,
                isUserPreset = match?.user == true,
                presets = all,
            )
        }
    }

    /** Live drag feedback: retunes the UI copy only. */
    fun previewGain(index: Int, gain: Int) {
        val current = _uiState.value.gains10.toMutableList()
        if (index in current.indices) {
            current[index] = gain
            _uiState.value = _uiState.value.copy(gains10 = current)
        }
    }

    /** Gesture end: expand to twenty bands and write once. */
    fun commitGains() {
        viewModelScope.launch {
            controller.setGains(expand(_uiState.value.gains10))
            refresh()
        }
    }

    fun setPreset(preset: EqPreset) {
        viewModelScope.launch {
            controller.setGains(preset.gains20)
            refresh()
        }
    }

    fun reset() {
        val flat = List(DsParam.GEQ_BAND_GAINS.length) { 0 }
        viewModelScope.launch {
            if (_uiState.value.isUserPreset) {
                // Flatten and keep the stored preset in sync, so the UI
                // doesn't flip to Custom.
                controller.setGains(flat)
                presets.save(_uiState.value.presetName, flat)
            } else {
                val current = _uiState.value.presets
                    .firstOrNull { it.name == _uiState.value.presetName }
                controller.setGains(current?.gains20 ?: flat)
            }
            refresh()
        }
    }

    fun saveAs(name: String) {
        viewModelScope.launch {
            presets.save(name, expand(_uiState.value.gains10))
            refresh()
        }
    }

    fun renamePreset(old: String, new: String) {
        viewModelScope.launch {
            presets.gains(old, DsParam.GEQ_BAND_GAINS.length)?.let {
                presets.delete(old)
                presets.save(new, it)
            }
            refresh()
        }
    }

    fun deletePreset(name: String) {
        viewModelScope.launch {
            presets.delete(name)
            refresh()
        }
    }

    private fun builtInPresets(): List<EqPreset> {
        val names = context.resources.getStringArray(R.array.preset_entries)
        val values = context.resources.getStringArray(R.array.preset_values)
        return names.mapIndexed { i, name ->
            EqPreset(
                name = name,
                gains20 = GainsCodec.deserialize(
                    values.getOrNull(i), DsParam.GEQ_BAND_GAINS.length,
                ) ?: List(DsParam.GEQ_BAND_GAINS.length) { 0 },
                user = false,
            )
        }
    }

    companion object {
        /** Backend twenty bands collapse to ten UI bands (even indices). */
        fun collapse(gains20: List<Int>): List<Int> =
            gains20.filterIndexed { i, _ -> i % 2 == 0 }.take(10).let {
                if (it.size == 10) it else List(10) { 0 }
            }

        /** Ten UI bands expand by duplicating each value. */
        fun expand(gains10: List<Int>): List<Int> =
            gains10.take(10).flatMap { listOf(it, it) }.let {
                if (it.size == 20) it else List(20) { 0 }
            }
    }
}
