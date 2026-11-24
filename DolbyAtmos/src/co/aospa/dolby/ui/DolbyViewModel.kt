/*
 * Copyright (C) 2026 samakshkambxj
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.ui

import android.app.Application
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import co.aospa.dolby.DolbyConstants.Companion.PREF_BASS
import co.aospa.dolby.DolbyConstants.Companion.PREF_DIALOGUE
import co.aospa.dolby.DolbyConstants.Companion.PREF_HP_VIRTUALIZER
import co.aospa.dolby.DolbyConstants.Companion.PREF_IEQ
import co.aospa.dolby.DolbyConstants.Companion.PREF_PROFILE
import co.aospa.dolby.DolbyConstants.Companion.PREF_SPK_VIRTUALIZER
import co.aospa.dolby.DolbyConstants.Companion.PREF_STEREO
import co.aospa.dolby.DolbyConstants.Companion.PREF_VOLUME
import co.aospa.dolby.DolbyController
import co.aospa.dolby.R
import co.aospa.dolby.preference.DolbyPreferenceStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Immutable snapshot of everything the Home tab renders.
 *
 * The controller owns the real Dolby state. This model is only a cache that is refreshed after
 * every write, so the Compose UI never has to talk to the effect directly.
 */
data class DolbyUiState(
    val dsOn: Boolean = true,
    val profile: Int = 0,
    val profileName: String = "",
    val presetName: String = "",
    val presetGains: List<Int> = List(10) { 0 },
    val ieq: Int = 0,
    val dialogue: Int = 0,
    val bass: Boolean = false,
    val speakerVirtualizer: Boolean = false,
    val headphoneVirtualizer: Boolean = false,
    val stereo: Int = 0,
    val volumeLeveler: Boolean = false,
    val isOnSpeaker: Boolean = true,
    val stereoSupported: Boolean = true,
) {
    val enabled: Boolean
        get() = dsOn && profile != -1
}

/**
 * Bridges the existing [DolbyController] and the profile-scoped preference stores to Compose.
 *
 * Writes deliberately go through both the controller and [DolbyPreferenceStore]: the controller
 * applies the value to the running effect, while the store keeps the same keys and the same
 * `profile_<id>` files that [DolbyController.onBootCompleted] reads back on boot.
 */
class DolbyViewModel(application: Application) : ViewModel() {

    private val context = application.applicationContext
    private val controller = DolbyController.getInstance(context)
    private val audioManager = context.getSystemService(AudioManager::class.java)!!
    private val handler = Handler(Looper.getMainLooper())

    private val store =
        DolbyPreferenceStore(context).apply {
            // The controller can briefly lose control of the effect; fall back to Dynamic.
            profile = runCatching { controller.profile }.getOrDefault(0)
        }

    private val _uiState = MutableStateFlow(DolbyUiState())
    val uiState = _uiState.asStateFlow()

    private val audioDeviceCallback =
        object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) {
                refresh()
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) {
                refresh()
            }
        }

    init {
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, handler)
        refresh()
    }

    fun refresh() {
        val previous = _uiState.value
        val profile = runCatching { controller.profile }.getOrDefault(previous.profile)
        if (store.profile != profile) store.profile = profile

        val dsOn = runCatching { controller.dsOn }.getOrDefault(previous.dsOn)
        val valid = profile != -1
        val unknown = context.getString(R.string.dolby_unknown)
        val profileName =
            if (valid) {
                runCatching { controller.getProfileName() }.getOrNull() ?: unknown
            } else {
                unknown
            }

        val base =
            previous.copy(
                dsOn = dsOn,
                profile = profile,
                profileName = profileName,
                isOnSpeaker = isOnSpeaker(),
                stereoSupported =
                    context.resources.getBoolean(R.bool.dolby_stereo_widening_supported),
            )

        // Reading DAP parameters requires a live effect. Mirror the old preference screen and
        // skip them while Dolby is off or the profile is unknown.
        if (!dsOn || !valid) {
            _uiState.value = base
            return
        }

        _uiState.value =
            runCatching {
                    base.copy(
                        presetName = controller.getPresetName(),
                        presetGains = parseGains(controller.getPreset(profile)),
                        ieq = controller.getIeqPreset(profile),
                        dialogue = controller.getDialogueEnhancerAmount(profile),
                        bass = controller.getBassEnhancerEnabled(profile),
                        speakerVirtualizer = controller.getSpeakerVirtEnabled(profile),
                        headphoneVirtualizer = controller.getHeadphoneVirtEnabled(profile),
                        stereo = controller.getStereoWideningAmount(profile),
                        volumeLeveler = controller.getVolumeLevelerEnabled(profile),
                    )
                }
                .getOrElse { base }
    }

    fun setDsOn(value: Boolean) {
        controller.setDsOnAndPersist(value)
        refresh()
    }

    fun setProfile(value: Int) {
        controller.profile = value
        store.profile = value
        store.putString(PREF_PROFILE, value.toString())
        refresh()
    }

    fun setIeq(value: Int) {
        controller.setIeqPreset(value)
        store.putString(PREF_IEQ, value.toString())
        refresh()
    }

    fun setDialogue(value: Int) {
        controller.setDialogueEnhancerAmount(value)
        store.putString(PREF_DIALOGUE, value.toString())
        refresh()
    }

    fun setBass(enabled: Boolean) {
        controller.setBassEnhancerEnabled(enabled)
        store.putBoolean(PREF_BASS, enabled)
        refresh()
    }

    fun setVolumeLeveler(enabled: Boolean) {
        controller.setVolumeLevelerEnabled(enabled)
        store.putBoolean(PREF_VOLUME, enabled)
        refresh()
    }

    fun setSpeakerVirtualizer(enabled: Boolean) {
        controller.setSpeakerVirtEnabled(enabled)
        store.putBoolean(PREF_SPK_VIRTUALIZER, enabled)
        refresh()
    }

    fun setHeadphoneVirtualizer(enabled: Boolean) {
        controller.setHeadphoneVirtEnabled(enabled)
        store.putBoolean(PREF_HP_VIRTUALIZER, enabled)
        refresh()
    }

    fun setStereo(value: Int) {
        if (!context.resources.getBoolean(R.bool.dolby_stereo_widening_supported)) return
        controller.setStereoWideningAmount(value)
        store.putString(PREF_STEREO, value.toString())
        refresh()
    }

    fun resetProfile() {
        runCatching { controller.resetProfileSpecificSettings() }
        // The profile file was deleted; re-acquire a fresh SharedPreferences instance.
        store.profile = runCatching { controller.profile }.getOrDefault(store.profile)
        refresh()
    }

    override fun onCleared() {
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        super.onCleared()
    }

    private fun isOnSpeaker(): Boolean =
        runCatching {
                val device = audioManager.getDevicesForAttributes(ATTRIBUTES_MEDIA).firstOrNull()
                device == null || device.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            }
            .getOrDefault(true)

    private fun parseGains(raw: String): List<Int> =
        raw.split(',')
            .mapNotNull { it.trim().toIntOrNull() }
            .filterIndexed { index, _ -> index % 2 == 0 }
            .take(10)
            .let { if (it.size == 10) it else ZERO_GAINS }

    companion object {
        private val ATTRIBUTES_MEDIA =
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build()
        private val ZERO_GAINS = List(10) { 0 }

        val Factory = viewModelFactory {
            initializer {
                DolbyViewModel(
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                )
            }
        }
    }
}
