/*
 * Copyright (C) 2026 samakshkambxj
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.ui

import android.app.Application
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import co.aospa.dolby.DolbyConstants
import co.aospa.dolby.DolbyController
import co.aospa.dolby.R
import co.aospa.dolby.data.EqualizerRepository
import co.aospa.dolby.data.Preset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class ProfileOption(val id: Int, val name: String)

enum class PresetNameError { EMPTY, EXISTS, TOO_LONG }

data class DolbyUiState(
    val ready: Boolean = false,
    val available: Boolean = true,
    val enabled: Boolean = true,
    val profile: Int = 0,
    val profileName: String = "",
    val profiles: List<ProfileOption> = emptyList(),
    val onSpeaker: Boolean = true,
    val outputName: String = "",
    val ieq: Int = 0,
    val hpVirtualizer: Boolean = false,
    val spkVirtualizer: Boolean = false,
    val stereoWidening: Int = 0,
    val dialogue: Int = 0,
    val bassLevel: Int = 0,
    val bassCurve: Int = 0,
    val midLevel: Int = 0,
    val trebleLevel: Int = 0,
    val volumeLeveler: Boolean = false,
    val presetName: String = "",
    val userPresetActive: Boolean = false,
    val displayGains: List<Int> = List(DolbyConstants.DISPLAY_BAND_COUNT) { 0 },
    val presets: List<Preset> = emptyList(),
    val stereoSupported: Boolean = true,
    val message: String? = null,
) {
    val hasTuning: Boolean
        get() = bassLevel > 0 || midLevel > 0 || trebleLevel > 0

    val activeVirtualizer: Boolean
        get() = if (onSpeaker) spkVirtualizer else hpVirtualizer
}

class DolbyViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application
    private val controller = DolbyController.getInstance(application)
    private val repository = EqualizerRepository(application)
    private val writeMutex = Mutex()
    private val audioManager = application.getSystemService(AudioManager::class.java)

    private val _state = MutableStateFlow(
        DolbyUiState(
            profiles = loadProfiles(),
            stereoSupported = controller.isStereoWideningSupported(),
        )
    )
    val state: StateFlow<DolbyUiState> = _state.asStateFlow()

    private val stereoValues = app.resources
        .getStringArray(R.array.dolby_stereo_values)
        .mapNotNull { it.toIntOrNull() }
        .ifEmpty { listOf(4, 24, 44, 64) }

    private val dialogueValues = app.resources
        .getStringArray(R.array.dolby_dialogue_values)
        .mapNotNull { it.toIntOrNull() }
        .ifEmpty { listOf(0, 2, 6, 9, 12) }

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) = refresh()
        override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) = refresh()
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            controller.ensureInitialized()
            publish(snapshot())
        }
        try {
            audioManager?.registerAudioDeviceCallback(deviceCallback, Handler(Looper.getMainLooper()))
        } catch (_: Exception) {
        }
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) { publish(snapshot()) }
    }

    // ---------------------------------------------------------------------
    // Master / profile
    // ---------------------------------------------------------------------

    fun setEnabled(enabled: Boolean) {
        _state.update { it.copy(enabled = enabled) }
        mutate { controller.setDsOnAndPersist(enabled) }
    }

    fun setProfile(profile: Int) {
        _state.update {
            it.copy(profile = profile, profileName = nameFor(profile))
        }
        mutate { controller.setProfileAndPersist(profile) }
    }

    // ---------------------------------------------------------------------
    // Quick controls
    // ---------------------------------------------------------------------

    fun setBassEnabled(enabled: Boolean) = mutate {
        controller.setBassLevel(controller.profile, if (enabled) 50 else 0)
    }

    fun setDialogueEnabled(enabled: Boolean) = mutate {
        controller.setDialogueEnhancer(controller.profile, if (enabled) 6 else 0)
    }

    fun setLevelerEnabled(enabled: Boolean) = mutate {
        controller.setVolumeLevelerEnabled(controller.profile, enabled)
    }

    fun setActiveVirtualizer(enabled: Boolean) = mutate {
        val profile = controller.profile
        if (controller.isOnSpeaker()) {
            controller.setSpeakerVirtualizer(profile, enabled)
        } else {
            controller.setHeadphoneVirtualizer(profile, enabled)
        }
    }

    // ---------------------------------------------------------------------
    // Advanced controls
    // ---------------------------------------------------------------------

    fun setIeq(value: Int) = mutate { controller.setIeqPreset(controller.profile, value) }

    fun setSpeakerVirtualizer(enabled: Boolean) = mutate {
        controller.setSpeakerVirtualizer(controller.profile, enabled)
    }

    fun setHeadphoneVirtualizer(enabled: Boolean) = mutate {
        controller.setHeadphoneVirtualizer(controller.profile, enabled)
    }

    fun setStereoWidening(index: Int) = mutate {
        controller.setStereoWidening(
            controller.profile,
            stereoValues[index.coerceIn(stereoValues.indices)],
        )
    }

    fun setDialogue(index: Int) = mutate {
        controller.setDialogueEnhancer(
            controller.profile,
            dialogueValues[index.coerceIn(dialogueValues.indices)],
        )
    }

    fun setBassLevel(level: Int) = mutate {
        controller.setBassLevel(controller.profile, level.coerceIn(0, 100))
    }

    fun setBassCurve(curve: Int) = mutate {
        controller.setBassCurve(controller.profile, curve.coerceIn(0, 2))
    }

    fun setMidLevel(level: Int) = mutate {
        controller.setMidLevel(controller.profile, level.coerceIn(0, 100))
    }

    fun setTrebleLevel(level: Int) = mutate {
        controller.setTrebleLevel(controller.profile, level.coerceIn(0, 100))
    }

    fun setMidEnabled(enabled: Boolean) = setMidLevel(if (enabled) 40 else 0)

    fun setTrebleEnabled(enabled: Boolean) = setTrebleLevel(if (enabled) 30 else 0)

    fun resetProfile() {
        viewModelScope.launch(Dispatchers.IO) {
            writeMutex.withLock {
                val profile = controller.profile
                val name = nameFor(profile)
                controller.resetProfileSpecificSettings(profile)
                publish(snapshot())
                _state.update {
                    it.copy(message = app.getString(R.string.reset_done_toast, name))
                }
            }
        }
    }

    fun dismissMessage() = _state.update { it.copy(message = null) }

    // ---------------------------------------------------------------------
    // Equalizer
    // ---------------------------------------------------------------------

    /** Updates the curve/fader locally without touching the HAL (drag path). */
    fun previewGain(index: Int, gain: Int) {
        _state.update { current ->
            val gains = current.displayGains.toMutableList()
            if (index !in gains.indices) return@update current
            gains[index] = gain.coerceIn(-DolbyConstants.GAIN_LIMIT, DolbyConstants.GAIN_LIMIT)
            current.copy(
                displayGains = gains,
                presetName = app.getString(R.string.dolby_geq_preset_custom),
                userPresetActive = false,
            )
        }
    }

    fun commitGains() {
        val display = _state.value.displayGains
        mutate { repository.applyDisplayGains(controller.profile, display) }
    }

    fun applyPreset(preset: Preset) {
        _state.update {
            it.copy(
                displayGains = repository.displayGains(preset.gains),
                presetName = preset.name,
                userPresetActive = preset.isUserDefined,
            )
        }
        mutate { repository.applyPreset(controller.profile, preset) }
    }

    fun resetEqualizer() = mutate {
        controller.setPreset(controller.profile, repository.defaultPreset.gains.toIntArray())
    }

    fun validatePresetName(name: String, exclude: String? = null): PresetNameError? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> PresetNameError.EMPTY
            trimmed.length > 50 -> PresetNameError.TOO_LONG
            _state.value.presets.any {
                it.name.equals(trimmed, ignoreCase = true) &&
                    !it.name.equals(exclude, ignoreCase = true)
            } -> PresetNameError.EXISTS
            else -> null
        }
    }

    fun savePreset(name: String) {
        val trimmed = name.trim()
        val display = _state.value.displayGains
        mutate {
            repository.savePreset(
                trimmed,
                controller.displayToBackend(display.toIntArray()).toList(),
            )
        }
    }

    fun renamePreset(newName: String) {
        val trimmed = newName.trim()
        val old = _state.value.presetName
        mutate {
            repository.renamePreset(
                old,
                trimmed,
                controller.getGains(controller.profile).toList(),
            )
        }
    }

    fun deletePreset() {
        val old = _state.value.presetName
        mutate {
            repository.deletePreset(old)
            controller.setPreset(controller.profile, repository.defaultPreset.gains.toIntArray())
        }
    }

    // ---------------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------------

    private fun mutate(block: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            writeMutex.withLock {
                try {
                    block()
                } catch (e: Exception) {
                    DolbyConstants.dlog("DolbyViewModel", "mutation failed: ${e.message}")
                }
                publish(snapshot())
            }
        }
    }

    private suspend fun snapshot(): DolbyUiState = withContext(Dispatchers.IO) {
        val profile = controller.profile
        val gains = repository.currentGains(profile)
        val presets = repository.allPresets()
        val active = presets.firstOrNull { it.matches(gains) }
            ?: Preset(app.getString(R.string.dolby_geq_preset_custom), gains)
        val profileName = controller.getProfileName(profile) ?: nameFor(profile)
        DolbyUiState(
            ready = true,
            available = controller.available,
            enabled = controller.dsOn,
            profile = profile,
            profileName = profileName,
            profiles = loadProfiles(),
            onSpeaker = controller.isOnSpeaker(),
            outputName = controller.getOutputName(),
            ieq = controller.getIeqPreset(profile),
            hpVirtualizer = controller.getHeadphoneVirtualizer(profile),
            spkVirtualizer = controller.getSpeakerVirtualizer(profile),
            stereoWidening = controller.getStereoWidening(profile),
            dialogue = controller.getDialogueEnhancer(profile),
            bassLevel = controller.getBassLevel(profile),
            bassCurve = controller.getBassCurve(profile),
            midLevel = controller.getMidLevel(profile),
            trebleLevel = controller.getTrebleLevel(profile),
            volumeLeveler = controller.getVolumeLevelerEnabled(profile),
            presetName = active.name,
            userPresetActive = active.isUserDefined,
            displayGains = repository.displayGains(gains),
            presets = presets,
            stereoSupported = controller.isStereoWideningSupported(),
        )
    }

    private fun publish(newState: DolbyUiState) {
        _state.update { previous ->
            newState.copy(
                profiles = newState.profiles.ifEmpty { previous.profiles },
                message = previous.message,
            )
        }
    }

    private fun loadProfiles(): List<ProfileOption> {
        val names = app.resources.getStringArray(R.array.dolby_profile_entries)
        val values = app.resources.getStringArray(R.array.dolby_profile_values)
        return values.mapIndexedNotNull { index, value ->
            val id = value.toIntOrNull() ?: return@mapIndexedNotNull null
            ProfileOption(id = id, name = names.getOrElse(index) { id.toString() })
        }
    }

    private fun nameFor(profile: Int): String =
        controller.getProfileName(profile)
            ?: _state.value.profiles.firstOrNull { it.id == profile }?.name
            ?: app.getString(R.string.dolby_profile_dynamic)

    override fun onCleared() {
        try {
            audioManager?.unregisterAudioDeviceCallback(deviceCallback)
        } catch (_: Exception) {
        }
        super.onCleared()
    }
}
