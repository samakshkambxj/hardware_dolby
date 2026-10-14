/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daybreak.dolby.ui.sound

import android.app.Application
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.daybreak.dolby.R
import com.daybreak.dolby.audio.DolbyController
import com.daybreak.dolby.audio.DsParam
import com.daybreak.dolby.data.DolbyKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** UI state for the Sound tab. Everything is read live, never cached. */
data class SoundUiState(
    val enabled: Boolean = true,
    val profile: Int = 0,
    val profileName: String? = null,
    val ieq: Int = 0,
    val bass: Boolean = false,
    val leveler: Boolean = false,
    val onSpeaker: Boolean = true,
    val outputLabel: String = "",
)

class SoundViewModel(application: Application) : AndroidViewModel(application) {

    private val controller = DolbyController.get(application)
    private val audioManager = application.getSystemService(AudioManager::class.java)
    private val handler = Handler(Looper.getMainLooper())

    private val _uiState = MutableStateFlow(SoundUiState())
    val uiState: StateFlow<SoundUiState> = _uiState.asStateFlow()

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) {
            refresh()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) {
            refresh()
        }
    }

    init {
        audioManager?.registerAudioDeviceCallback(deviceCallback, handler)
        refresh()
    }

    override fun onCleared() {
        audioManager?.unregisterAudioDeviceCallback(deviceCallback)
    }

    fun refresh() {
        viewModelScope.launch {
            val enabled = controller.isEnabled()
            val profile = controller.currentProfile()
            _uiState.value = SoundUiState(
                enabled = enabled,
                profile = profile,
                profileName = profileName(profile),
                ieq = controller.getAmount(DsParam.IEQ_PRESET, DolbyKeys.IEQ, 0, profile),
                bass = controller.getBool(
                    DsParam.BASS_ENHANCER_ENABLE, DolbyKeys.BASS, false, profile,
                ),
                leveler = controller.getBool(
                    DsParam.VOLUME_LEVELER_ENABLE, DolbyKeys.LEVELER, false, profile,
                ),
                onSpeaker = isOnSpeaker(),
                outputLabel = outputLabel(),
            )
        }
    }

    fun setEnabled(on: Boolean) {
        viewModelScope.launch {
            controller.setEnabled(on)
            refresh()
        }
    }

    fun setProfile(id: Int) {
        viewModelScope.launch {
            controller.setProfile(id)
            refresh()
        }
    }

    fun setIeq(value: Int) {
        viewModelScope.launch {
            controller.setAmount(DsParam.IEQ_PRESET, DolbyKeys.IEQ, value)
            refresh()
        }
    }

    fun setBass(on: Boolean) {
        viewModelScope.launch {
            controller.setBool(DsParam.BASS_ENHANCER_ENABLE, DolbyKeys.BASS, on)
            refresh()
        }
    }

    fun setLeveler(on: Boolean) {
        viewModelScope.launch {
            controller.setBool(DsParam.VOLUME_LEVELER_ENABLE, DolbyKeys.LEVELER, on)
            refresh()
        }
    }

    private fun profileName(profile: Int): String? {
        val context = getApplication<Application>()
        val values = context.resources.getStringArray(R.array.profile_values)
        val index = values.indexOf(profile.toString())
        if (index == -1) return null
        return context.resources.getStringArray(R.array.profile_entries)[index]
    }

    private fun isOnSpeaker(): Boolean {
        val type = currentDeviceType()
        return type == null || type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
    }

    private fun outputLabel(): String {
        val context = getApplication<Application>()
        val type = currentDeviceType() ?: return context.getString(R.string.output_speaker)
        return when (type) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> context.getString(R.string.output_speaker)
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> context.getString(R.string.output_headphones)
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> context.getString(R.string.output_bluetooth)
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_DEVICE -> context.getString(R.string.output_usb)
            else -> context.getString(R.string.output_unknown)
        }
    }

    private fun currentDeviceType(): Int? {
        return try {
            audioManager
                ?.getDevicesForAttributes(ATTRIBUTES_MEDIA)
                ?.firstOrNull()?.type
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private val ATTRIBUTES_MEDIA = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()
    }
}
