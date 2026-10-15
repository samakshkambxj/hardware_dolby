/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package org.dolbyatmos.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import org.dolbyatmos.DolbyConstants
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

enum class TestEar { LEFT, RIGHT }

/**
 * Pulsed pure-tone player for the hearing test. One-second looped buffer
 * (600 ms tone with soft edges, 400 ms silence) in stereo with the tone
 * baked into the tested ear only; level rides [AudioTrack.setVolume] so
 * descending trials never rebuild the buffer.
 */
class HearingTonePlayer(context: Context) {

    private val appContext = context.applicationContext
    private val audioManager =
        appContext.getSystemService(AudioManager::class.java)

    private var track: AudioTrack? = null
    private var focusRequest: AudioFocusRequest? = null

    var isPlaying = false
        private set

    fun start(freqHz: Int, ear: TestEar, levelDb: Float) {
        stop()
        try {
            requestFocus()
            val sampleRate = 44100
            val onMs = 600
            val frames = sampleRate // 1 s loop
            val amp = dbToAmp(levelDb)
            val buffer = ShortArray(frames * 2)
            for (i in 0 until frames) {
                val ms = i * 1000f / sampleRate
                val edge = 50f
                val gate = if (ms < onMs) {
                    min(min(ms / edge, (onMs - ms) / edge), 1f).coerceIn(0f, 1f)
                } else {
                    0f
                }
                val t = i.toFloat() / sampleRate
                val sample =
                    (sin(2.0 * PI * freqHz * t) * gate * amp * Short.MAX_VALUE).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        .toShort()
                buffer[i * 2] = if (ear == TestEar.LEFT) sample else 0
                buffer[i * 2 + 1] = if (ear == TestEar.RIGHT) sample else 0
            }
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build()
            track = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
                .also {
                    it.write(buffer, 0, buffer.size)
                    it.setLoopPoints(0, frames, -1)
                    it.setVolume(1f)
                    it.play()
                }
            isPlaying = true
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Tone start failed: ${e.message}")
            stop()
        }
    }

    fun setLevel(levelDb: Float) {
        try {
            track?.setVolume(dbToAmp(levelDb))
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Tone level failed: ${e.message}")
        }
    }

    fun stop() {
        try {
            track?.stop()
        } catch (_: Exception) {
        }
        try {
            track?.release()
        } catch (_: Exception) {
        }
        track = null
        isPlaying = false
        abandonFocus()
    }

    private fun requestFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(attributes)
                    .build()
                    .also { audioManager?.requestAudioFocus(it) }
            }
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Focus request failed: ${e.message}")
        }
    }

    private fun abandonFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
            }
        } catch (_: Exception) {
        }
        focusRequest = null
    }

    companion object {
        private const val TAG = "HearingTonePlayer"

        fun dbToAmp(levelDb: Float): Float =
            10.0.pow((levelDb / 20.0).toDouble()).toFloat().coerceIn(0f, 1f)
    }
}
