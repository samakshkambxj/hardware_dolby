/*
 * Copyright (C) 2026 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.ui

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** State holder for the Compose UI. Plain class, owned by [co.aospa.dolby.DolbyActivity]. */
class DolbyViewModel(context: Context) {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var deviceCallback: AudioDeviceCallback? = null

    val repository = DolbyRepository(context)

    var state by mutableStateOf(repository.loadState())
        private set

    val presetNames: List<String>
        get() = repository.presetNames()

    fun refresh() {
        state = repository.loadState()
    }

    fun registerDeviceCallback() {
        if (deviceCallback != null) return
        val audioManager = appContext.getSystemService(AudioManager::class.java) ?: return
        val callback =
            object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) {
                    refresh()
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) {
                    refresh()
                }
            }
        deviceCallback = callback
        audioManager.registerAudioDeviceCallback(callback, mainHandler)
    }

    fun unregisterDeviceCallback() {
        val callback = deviceCallback ?: return
        appContext
            .getSystemService(AudioManager::class.java)
            ?.unregisterAudioDeviceCallback(callback)
        deviceCallback = null
    }

    fun setEnabled(enabled: Boolean) {
        state = state.copy(enabled = enabled)
        repository.setEnabled(enabled)
        refresh()
    }

    fun setProfile(profile: Int) {
        if (profile == state.profile) return
        state = state.copy(profile = profile)
        repository.setProfile(profile)
        refresh()
    }

    fun setIeq(value: Int) {
        state = state.copy(ieq = value)
        repository.setIeq(value)
    }

    fun setSpeakerVirtualizer(enabled: Boolean) {
        state = state.copy(speakerVirtualizer = enabled)
        repository.setSpeakerVirtualizer(enabled)
    }

    fun setHeadphoneVirtualizer(enabled: Boolean) {
        state = state.copy(headphoneVirtualizer = enabled)
        repository.setHeadphoneVirtualizer(enabled)
    }

    fun setVolumeLeveler(enabled: Boolean) {
        state = state.copy(volumeLeveler = enabled)
        repository.setVolumeLeveler(enabled)
    }

    fun setDialogueEnhancer(value: Int) {
        state = state.copy(dialogueEnhancer = value)
        repository.setDialogueEnhancer(value)
    }

    fun setStereoWidening(value: Int) {
        state = state.copy(stereoWidening = value)
        repository.setStereoWidening(value)
    }

    fun resetProfile() {
        repository.resetProfile()
        refresh()
    }

    // ---------------------------------------------------------------------------------------------
    // Equalizer
    // ---------------------------------------------------------------------------------------------

    fun applyPreset(name: String) {
        repository.applyPreset(name)
        refresh()
    }

    fun savePreset(name: String) {
        repository.savePreset(name)
        refresh()
    }

    fun deletePreset(name: String) {
        if (!repository.isUserPreset(name)) return
        repository.deletePreset(name)
        repository.resetGains()
        refresh()
    }

    fun isUserPreset(name: String): Boolean = repository.isUserPreset(name)

    fun setBandMode(mode: BandMode) {
        if (mode == state.bandMode) return
        repository.setBandMode(mode)
        state = state.copy(bandMode = mode)
    }

    /** Live preview of a band edit while dragging. */
    fun previewGain(visibleIndex: Int, tenths: Int) {
        state =
            state.copy(
                bandGains =
                    repository.withGain(
                        state.bandGains,
                        visibleIndex,
                        tenths,
                        state.bandMode,
                    )
            )
    }

    /** Commit the gains currently shown to the DSP and prefs. */
    fun commitGains() {
        repository.setGains(state.bandGains)
        state = state.copy(presetName = repository.loadState().presetName)
    }

    fun setGain(visibleIndex: Int, tenths: Int) {
        previewGain(visibleIndex, tenths)
        commitGains()
    }

    fun resetGains() {
        repository.resetGains()
        state = state.copy(bandGains = List(DolbyUiState.BACKEND_BANDS) { 0 })
    }

    // ---------------------------------------------------------------------------------------------
    // Audio tuning
    // ---------------------------------------------------------------------------------------------

    fun setBassEnhancer(enabled: Boolean) {
        // Enabling with a zero level first restores the default so the curve actually applies.
        if (enabled && state.bassLevel == 0) {
            state = state.copy(bassLevel = DEFAULT_BASS_LEVEL)
            repository.setBassLevel(DEFAULT_BASS_LEVEL)
        }
        state = state.copy(bassEnhancer = enabled)
        repository.setBassEnhancer(enabled)
    }

    fun setBassLevel(level: Int) {
        state = state.copy(bassLevel = level)
    }

    fun commitBassLevel() {
        repository.setBassLevel(state.bassLevel)
    }

    fun setBassCurve(curve: Int) {
        state = state.copy(bassCurve = curve)
        repository.setBassCurve(curve)
    }

    fun setMidEnhancer(enabled: Boolean) {
        state = state.copy(midEnhancer = enabled)
        repository.setMidEnhancer(enabled)
    }

    fun setMidLevel(level: Int) {
        state = state.copy(midLevel = level)
    }

    fun commitMidLevel() {
        repository.setMidLevel(state.midLevel)
    }

    fun setTrebleEnhancer(enabled: Boolean) {
        state = state.copy(trebleEnhancer = enabled)
        repository.setTrebleEnhancer(enabled)
    }

    fun setTrebleLevel(level: Int) {
        state = state.copy(trebleLevel = level)
    }

    fun commitTrebleLevel() {
        repository.setTrebleLevel(state.trebleLevel)
    }

    companion object {
        const val DEFAULT_BASS_LEVEL = 50
        const val DEFAULT_MID_LEVEL = 40
        const val DEFAULT_TREBLE_LEVEL = 30
    }
}
