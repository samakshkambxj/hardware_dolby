/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daybreak.dolby.ui.spatial

import android.app.Application
import android.media.AudioManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.daybreak.dolby.R
import com.daybreak.dolby.audio.DolbyController
import com.daybreak.dolby.audio.DsParam
import com.daybreak.dolby.data.DolbyKeys
import com.daybreak.dolby.ui.util.AudioOutputs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SpatialUiState(
    val enabled: Boolean = false,
    val profileName: String? = null,
    val spkVirt: Boolean = false,
    val hpVirt: Boolean = false,
    val stereo: Int = 0,
    val dialogue: Int = 0,
    val onSpeaker: Boolean = true,
)

class SpatialViewModel(application: Application) : AndroidViewModel(application) {

    private val controller = DolbyController.get(application)
    private val audioManager = application.getSystemService(AudioManager::class.java)

    private val _uiState = MutableStateFlow(SpatialUiState())
    val uiState: StateFlow<SpatialUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val profile = controller.currentProfile()
            _uiState.value = SpatialUiState(
                enabled = controller.isEnabled(),
                profileName = profileName(profile),
                spkVirt = controller.getBool(
                    DsParam.SPEAKER_VIRTUALIZER, DolbyKeys.SPK_VIRTUALIZER, false, profile,
                ),
                hpVirt = controller.getBool(
                    DsParam.HEADPHONE_VIRTUALIZER, DolbyKeys.HP_VIRTUALIZER, false, profile,
                ),
                stereo = controller.getAmount(
                    DsParam.STEREO_WIDENING_AMOUNT, DolbyKeys.STEREO, 0, profile,
                ),
                dialogue = controller.getAmount(
                    DsParam.DIALOGUE_ENHANCER_AMOUNT, DolbyKeys.DIALOGUE, 0, profile,
                ),
                onSpeaker = AudioOutputs.isOnSpeaker(audioManager),
            )
        }
    }

    fun setSpeakerVirt(on: Boolean) {
        viewModelScope.launch {
            controller.setBool(DsParam.SPEAKER_VIRTUALIZER, DolbyKeys.SPK_VIRTUALIZER, on)
            refresh()
        }
    }

    fun setHeadphoneVirt(on: Boolean) {
        viewModelScope.launch {
            controller.setBool(DsParam.HEADPHONE_VIRTUALIZER, DolbyKeys.HP_VIRTUALIZER, on)
            refresh()
        }
    }

    fun setStereo(value: Int) {
        viewModelScope.launch {
            controller.setAmount(DsParam.STEREO_WIDENING_AMOUNT, DolbyKeys.STEREO, value)
            refresh()
        }
    }

    fun setDialogue(value: Int) {
        viewModelScope.launch {
            controller.setAmount(DsParam.DIALOGUE_ENHANCER_AMOUNT, DolbyKeys.DIALOGUE, value)
            refresh()
        }
    }

    fun resetProfile() {
        viewModelScope.launch {
            controller.resetProfile()
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
}
