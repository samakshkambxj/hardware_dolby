/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daybreak.dolby.ui.volume

import android.app.Application
import android.media.AudioManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.daybreak.dolby.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StreamState(
    val type: Int,
    val labelRes: Int,
    val min: Int,
    val max: Int,
    val current: Int,
)

class VolumeViewModel(application: Application) : AndroidViewModel(application) {

    private val audioManager = application.getSystemService(AudioManager::class.java)

    private val _streams = MutableStateFlow<List<StreamState>>(emptyList())
    val streams: StateFlow<List<StreamState>> = _streams.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _streams.value = STREAMS.map { (type, labelRes) ->
                val max = maxVolume(type)
                val min = minVolume(type)
                StreamState(
                    type = type,
                    labelRes = labelRes,
                    min = min,
                    max = max,
                    current = getVolume(type).coerceIn(min, max),
                )
            }
        }
    }

    fun setVolume(type: Int, value: Int) {
        viewModelScope.launch {
            val state = _streams.value.firstOrNull { it.type == type } ?: return@launch
            val target = value.coerceIn(state.min, state.max)
            try {
                audioManager?.setStreamVolume(type, target, 0)
            } catch (_: Exception) {
            }
            refresh()
        }
    }

    private fun getVolume(type: Int): Int {
        return try {
            audioManager?.getStreamVolume(type) ?: 0
        } catch (_: Exception) {
            0
        }
    }

    private fun maxVolume(type: Int): Int {
        return try {
            audioManager?.getStreamMaxVolume(type) ?: 15
        } catch (_: Exception) {
            15
        }
    }

    private fun minVolume(type: Int): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return 0
        return try {
            audioManager?.getStreamMinVolume(type) ?: 0
        } catch (_: Exception) {
            0
        }
    }

    companion object {
        private val STREAMS = listOf(
            AudioManager.STREAM_MUSIC to R.string.volume_media,
            AudioManager.STREAM_RING to R.string.volume_ring,
            AudioManager.STREAM_NOTIFICATION to R.string.volume_notification,
            AudioManager.STREAM_ALARM to R.string.volume_alarm,
            AudioManager.STREAM_VOICE_CALL to R.string.volume_call,
        )
    }
}
