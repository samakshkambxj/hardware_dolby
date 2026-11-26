/*
 * Copyright (C) 2024 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.geq.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import co.aospa.dolby.DolbyConstants.Companion.dlog
import co.aospa.dolby.geq.data.EqualizerRepository
import co.aospa.dolby.geq.data.Preset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

const val TAG = "EqViewModel"

class EqualizerViewModel(private val repository: EqualizerRepository) : ViewModel() {

    private val _presets = MutableStateFlow(repository.builtInPresets)
    val presets = _presets.asStateFlow()

    private val _preset = MutableStateFlow(repository.defaultPreset)
    val preset = _preset.asStateFlow()

    private var presetRestored = false

    init {
        // Update the list of presets: combined list of user defined presets if any,
        // and then the built in presets.
        repository.userPresets
            .onEach { presets ->
                dlog(TAG, "updated userPresets: $presets")
                _presets.value =
                    mutableListOf<Preset>()
                        .apply {
                            addAll(presets)
                            addAll(repository.builtInPresets)
                        }
                        .toList()

                // We can restore the active preset only after the presets list is populated,
                // since we do not save the preset name but only its gains.
                if (!presetRestored) {
                    val bandGains = repository.getBandGains()
                    _preset.value =
                        _presets.value.find { bandGains == it.bandGains }
                            ?: Preset(name = "Custom", bandGains = bandGains)
                    dlog(TAG, "restored preset: ${_preset.value}")
                    presetRestored = true
                }
            }
            .launchIn(viewModelScope)

        // Update the preset in repository everytime we set it here
        _preset
            .drop(1) // skip the initial value
            .onEach {
                // wait till the active preset is restored
                if (!presetRestored) {
                    return@onEach
                }
                dlog(TAG, "updated preset: $it")
                repository.setBandGains(it.bandGains)
                if (it.isUserDefined) {
                    repository.addPreset(it)
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Re-reads the gains for the currently active Dolby profile. Called when the profile changes
     * on the Home tab, and after a profile reset, so the equalizer never keeps stale values.
     */
    fun reload() {
        repository.updateProfile()
        if (!presetRestored) return
        viewModelScope.launch {
            val bandGains = repository.getBandGains()
            _preset.value =
                _presets.value.find { bandGains == it.bandGains }
                    ?: Preset(name = "Custom", bandGains = bandGains)
        }
    }

    fun reset() {
        dlog(TAG, "reset()")
        if (_preset.value.isUserDefined) {
            // Reset gains to 0
            _preset.value = _preset.value.copy(bandGains = repository.defaultPreset.bandGains)
        } else {
            // Switch to flat preset
            _preset.value = repository.defaultPreset
        }
    }

    fun setPreset(preset: Preset) {
        dlog(TAG, "setPreset($preset)")
        _preset.value = preset
    }

    fun setGain(index: Int, gain: Int) {
        dlog(TAG, "setGain($index, $gain)")
        _preset.value =
            _preset.value.run {
                copy(
                    name = if (!isUserDefined) "Custom" else name,
                    bandGains =
                        bandGains
                            .toMutableList()
                            // create a new object to ensure the flow emits an update.
                            .apply { this[index] = this[index].copy(gain = gain) }
                            .toList(),
                    isMutated = true,
                )
            }
    }

    // Returns string containing the error message if it failed, otherwise null
    private fun validatePresetName(
        name: String,
        except: Preset? = null,
    ): PresetNameValidationError? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> PresetNameValidationError.NAME_EMPTY
            trimmed.length > 50 -> PresetNameValidationError.NAME_TOO_LONG
            // Ignore the preset that is being renamed so keeping its name stays valid.
            _presets.value.any {
                it.name != except?.name && it.name.equals(trimmed, ignoreCase = true)
            } -> PresetNameValidationError.NAME_EXISTS
            else -> null
        }
    }

    fun createNewPreset(name: String): PresetNameValidationError? {
        dlog(TAG, "createNewPreset($name)")
        validatePresetName(name)?.let {
            dlog(TAG, "createNewPreset failed: $it")
            return it
        }
        _preset.value =
            _preset.value.copy(name = name.trim(), isUserDefined = true, isMutated = false)
        return null
    }

    /**
     * Persists edits to the selected user preset in place.
     *
     * The state flow already applies the gains to the effect; flipping [Preset.isMutated] makes it
     * write the updated gains back to the stored preset as well.
     */
    fun saveCurrentPreset() {
        val current = _preset.value
        if (!current.isUserDefined || !current.isMutated) return
        _preset.value = current.copy(isMutated = false)
    }

    fun renamePreset(preset: Preset, name: String): PresetNameValidationError? {
        dlog(TAG, "renamePreset($preset, $name)")
        validatePresetName(name, except = preset)?.let {
            dlog(TAG, "renamePreset failed: $it")
            return it
        }

        val renamed = preset.copy(name = name.trim(), isUserDefined = true, isMutated = false)
        if (renamed.name == preset.name) {
            // Nothing changed; keep the existing entry instead of deleting and re-adding it.
            return null
        }

        if (preset.name == _preset.value.name) {
            // Renaming the selected preset: keep the selection, let the state flow persist the
            // new entry, then remove the old one.
            _preset.value =
                _preset.value.copy(
                    name = renamed.name,
                    isUserDefined = true,
                    isMutated = false,
                )
            viewModelScope.launch { repository.removePreset(preset) }
        } else {
            // Renaming a preset that is not selected must not touch the current selection.
            viewModelScope.launch {
                repository.removePreset(preset)
                repository.addPreset(renamed)
            }
        }
        return null
    }

    fun deletePreset(preset: Preset, shouldReset: Boolean = true) {
        dlog(TAG, "deletePreset($preset)")
        viewModelScope.launch { repository.removePreset(preset) }
        // Only reset the selection when the preset being deleted is the active one.
        if (shouldReset && preset.name == _preset.value.name) {
            _preset.value = repository.defaultPreset
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                EqualizerViewModel(
                    repository =
                        EqualizerRepository(
                            this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                        )
                )
            }
        }
    }
}
