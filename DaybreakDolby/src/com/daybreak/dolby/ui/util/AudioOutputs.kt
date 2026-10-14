/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daybreak.dolby.ui.util

import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build

/** Output-device queries. Safe on every API level. */
object AudioOutputs {

    private val ATTRIBUTES_MEDIA = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .build()

    fun deviceType(audioManager: AudioManager?): Int? {
        if (audioManager == null) return null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        return try {
            audioManager.getDevicesForAttributes(ATTRIBUTES_MEDIA).firstOrNull()?.type
        } catch (_: Exception) {
            null
        }
    }

    fun isOnSpeaker(audioManager: AudioManager?): Boolean {
        val type = deviceType(audioManager)
        return type == null || type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
    }
}
